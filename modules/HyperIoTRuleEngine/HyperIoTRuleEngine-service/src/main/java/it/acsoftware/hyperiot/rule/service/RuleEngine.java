/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.acsoftware.hyperiot.rule.service;

import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.rule.model.RuleEngineAsynchronousObserver;
import it.acsoftware.hyperiot.rule.model.facts.FiredRule;
import it.acsoftware.hyperiot.rule.model.facts.LastReceivedPacket;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.KieSessionConfiguration;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.conf.TimedRuleExecutionOption;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RuleEngine {
    private static int FORCE_RULE_REFRESH_INTERVAL_SECONDS = 5;
    private Logger logger = LoggerFactory.getLogger(RuleEngine.class);
    private long projectId;
    private KieBase kbase;
    private KieSession session;

    /**
     * Map for updating facts, when they are encountered more than once
     */
    private Map<Long, FactHandle> packetsFacts;
    private Map<Long, FactHandle> lastReceivedPacketsFacts;
    private Set<RuleEngineAsynchronousObserver> asynchronousObserverSet;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private long lastRun;
    private boolean startAsyncRuleCheck;

    public RuleEngine(List<String> droolsRules, long projectId) {
        super();
        logger.info("Creating RuleEngine for {}", projectId);
        this.projectId = projectId;
        List<Resource> resources = new ArrayList<>();
        this.asynchronousObserverSet = new HashSet<>();
        int i = 0;
        for (String rule : droolsRules) {
            // create one resource for each rule
            Resource ruleResource = ResourceFactory.newReaderResource(new StringReader(rule));
            ruleResource.setResourceType(ResourceType.DRL);
            ruleResource.setSourcePath(String.format("drools/%d/%d", projectId, i++));
            resources.add(ruleResource);
        }
        this.session = getKieSession(resources);
        logger.debug("Current kie session instance is {} and hashcode : {} ", this.session, this.session.hashCode());
        this.session.setGlobal("actions", new ArrayList<String>());
        packetsFacts = new HashMap<>();
        lastReceivedPacketsFacts = new HashMap<>();
        this.startAsyncRuleCheck = false;
    }

    public RuleEngine(List<String> droolsRules, long projectId, boolean startAsyncRuleCheck) {
        this(droolsRules, projectId);
        this.startAsyncRuleCheck = startAsyncRuleCheck;
    }

    public void addAsyncObserver(RuleEngineAsynchronousObserver observer) {
        this.asynchronousObserverSet.add(observer);
    }

    public void removeAsyncObserver(RuleEngineAsynchronousObserver observer) {
        this.asynchronousObserverSet.remove(observer);
    }

    public void start() {
        if (this.startAsyncRuleCheck) {
            executorService.scheduleAtFixedRate(() -> {
                //force re-evaluation of rules after 5 seconds in order to support
                //timed rules for example data must be sent in x seconds
                if (System.currentTimeMillis() - lastRun >= FORCE_RULE_REFRESH_INTERVAL_SECONDS * 1000) {
                    logger.debug("Forcing rules evaluation after {} seconds of inactivity", FORCE_RULE_REFRESH_INTERVAL_SECONDS);
                    //just re-inserting last sent packets in order to re-evaluate rules where last sent packet is important
                    try {
                        lastReceivedPacketsFacts.keySet().stream().forEach(packetId -> {
                            LastReceivedPacket lastReceivedPacket = (LastReceivedPacket) this.session.getObject(lastReceivedPacketsFacts.get(packetId));
                            this.session.delete(lastReceivedPacketsFacts.get(packetId));
                            this.lastReceivedPacketsFacts.put(packetId, this.session.insert(lastReceivedPacket));
                            logger.debug("Updated last sent packets...");
                            //laoding last sent packet
                            HPacket packet = (HPacket) this.session.getObject(packetsFacts.get(packetId));
                            //checking with the arrival timestamp
                            this.check(packet, lastReceivedPacket.getLastReceivedDateMillis());
                            //CALLING BACK ASYNC OBSERVES
                            asynchronousObserverSet.forEach(asynchronousObserverSet -> {
                                try {
                                    asynchronousObserverSet.processData(packet);
                                } catch (Exception e) {
                                    logger.error("errore while invoking async observers: ", e.getMessage());
                                }
                            });

                        });
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }, 10, 5, TimeUnit.SECONDS);
        }
        this.session.fireAllRules();
    }

    public void stop() {
        disposeSession();
        this.executorService.shutdown();
    }

    public void disposeSession() {
        session.dispose();
    }

    public KieSession getSession() {
        return this.session;
    }

    public synchronized void check(HPacket packet, long packetArrivalTime) {
        this.lastRun = System.currentTimeMillis();
        //passing null packet will just force to re-evaluate rules
        LastReceivedPacket lastReceivedPacket = new LastReceivedPacket();
        lastReceivedPacket.setPacketId(packet.getId());
        lastReceivedPacket.setLastReceivedDateMillis(packetArrivalTime);
        //updating last packets received facts
        if (lastReceivedPacketsFacts.containsKey(packet.getId())) {
            LastReceivedPacket previuosReceivedPacket = (LastReceivedPacket) this.session.getObject(lastReceivedPacketsFacts.get(packet.getId()));
            //if for some reason we are processing an old packet, we just exit
            if (lastReceivedPacket.getLastReceivedDateMillis() < previuosReceivedPacket.getLastReceivedDateMillis()) {
                logger.debug("Skipping packet rule evaluation since there's one newer");
                return;
            }
            session.update(lastReceivedPacketsFacts.get(packet.getId()), lastReceivedPacket);
        } else {
            lastReceivedPacketsFacts.put(packet.getId(), session.insert(lastReceivedPacket));
        }
        //updating packets facts
        if (packetsFacts.containsKey(packet.getId())) {
            session.update(packetsFacts.get(packet.getId()), packet);
        } else {
            packetsFacts.put(packet.getId(), session.insert(packet));
        }

        logger.debug("KieSession instance is: {}", session);
        logger.debug("KieSession Globals: {}", session.getGlobals());
        session.fireAllRules();
    }

    public KieSession getKieSession(List<Resource> ruleResources) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            KieSessionConfiguration ksconf = KieServices.Factory.get().newKieSessionConfiguration();
            //start timers
            ksconf.setOption(TimedRuleExecutionOption.YES);
            //use realtime clock
            ksconf.setOption(ClockTypeOption.REALTIME);
            return RuleEngineKieBase.getInstance(ruleResources).getKieBase().newKieSession(ksconf, null);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    public void insertFiredRuleFact(long ruleId, boolean fired, Date lastFiredTimestamp) {
        logger.debug("Adding FiredRule with Id : {} ", ruleId);
        session.insert(new FiredRule(ruleId, fired, lastFiredTimestamp));
        session.fireAllRules();
    }

    public String getPacketsContentFromSession(List<Long> packetIds) {
        logger.debug("Getting packet contents for packet ids: {}", packetIds);
        StringBuilder sb = new StringBuilder();
        packetIds.stream().forEach(packetId -> {
            HPacket packet = (HPacket) session.getObject(packetsFacts.get(packetId));
            logger.debug("Extracting last packet contents for packet id: {}", packetId);
            if (packet.getFields().size() > 0) {
                sb.append("\n" + packet.getName() + ":\n");
                packet.getFields().forEach(packetField -> {
                    String fieldName = packetField.getName();
                    String fieldValue = packetField.getFieldValue().toString();
                    //skipping timestamp field
                    if (!fieldName.equalsIgnoreCase(packet.getTimestampField())) {
                        sb.append("\t" + fieldName + " : " + fieldValue + "\n");
                        logger.debug("Adding field to packet contents info: {}:{}", fieldName, fieldValue.toString());
                    }
                });
                //adding timestamp readable field
                sb.append("\t" + packet.getTimestampField() + " : " + packet.getTimestampValueAsString() + "\n");
                sb.append("\n");
            }
        });
        return sb.toString();
    }

}
