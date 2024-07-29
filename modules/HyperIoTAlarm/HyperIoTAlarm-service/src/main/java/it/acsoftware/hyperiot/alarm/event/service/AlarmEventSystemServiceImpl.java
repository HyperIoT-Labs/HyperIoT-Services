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

package it.acsoftware.hyperiot.alarm.event.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.alarm.api.AlarmRepository;
import it.acsoftware.hyperiot.alarm.event.api.AlarmEventRepository;
import it.acsoftware.hyperiot.alarm.event.api.AlarmEventSystemApi;
import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.alarm.model.Alarm;
import it.acsoftware.hyperiot.alarm.service.actions.AlarmAction;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTException;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.facts.FiredRule;
import org.apache.hadoop.hbase.util.Bytes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.io.IOException;
import java.util.*;

/**
 * @author Aristide Cittadino Implementation class of the AlarmEventSystemApi
 * interface. This  class is used to implements all additional
 * methods to interact with the persistence layer.
 */
@Component(service = AlarmEventSystemApi.class, immediate = true)
public final class AlarmEventSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<AlarmEvent> implements AlarmEventSystemApi {
    private static final String EVENT_STATUS_HBASE_TABLE = "hproject_event_rule_state_";
    private static final ObjectMapper internalMapper = new ObjectMapper();
    /**
     * Injecting the AlarmEventRepository to interact with persistence layer
     */
    private AlarmEventRepository repository;

    /**
     * Injecting the PermissionSystemApi to interact with permission layer
     */
    private PermissionSystemApi permissionSystemApi;

    /**
     * Inject the AlarmEventSystemApi
     */
    private RuleEngineSystemApi ruleEngineSystemApi;

    /**
     * Inject the HPacketSystemApi
     */
    private HPacketSystemApi hPacketSystemApi;


    /**
     * Inject the AlarmRepository
     */
    private AlarmRepository alarmRepository;

    /**
     * read event status if it is fired or not
     */
    private HBaseConnectorSystemApi hBaseConnectorSystemApi;

    /**
     * Constructor for a AlarmEventSystemServiceImpl
     */
    public AlarmEventSystemServiceImpl() {
        super(AlarmEvent.class);
    }

    /**
     * Return the current repository
     */
    protected AlarmEventRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}", this.repository);
        return repository;
    }

    /**
     * @param alarmEventRepository The current value of AlarmEventRepository to interact with persistence layer
     */
    @Reference
    protected void setRepository(AlarmEventRepository alarmEventRepository) {
        getLog().debug("invoking setRepository, setting: {}", alarmEventRepository);
        this.repository = alarmEventRepository;
    }


    /**
     * @return The current PermissionSystemAPi
     */
    public PermissionSystemApi getPermissionSystemApi() {
        getLog().debug("invoking getPermissionSystemApi, returning: {}", this.permissionSystemApi);
        return this.permissionSystemApi;
    }

    /**
     * @param permissionSystemApi The current value of PermissionSystemApi to interact with permission layer
     */
    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

    protected RuleEngineSystemApi getRuleEngineSystemApi() {
        getLog().debug("invoking getRuleEngineSystemApi, returning: {}", this.ruleEngineSystemApi);
        return ruleEngineSystemApi;
    }

    /**
     * @param ruleEngineSystemApi The current value of RuleEngineSystemApi
     */
    @Reference
    protected void setRuleEngineSystemApi(RuleEngineSystemApi ruleEngineSystemApi) {
        getLog().debug("invoking setRuleEngineSystemApi, setting: {}", ruleEngineSystemApi);
        this.ruleEngineSystemApi = ruleEngineSystemApi;
    }

    /**
     * @param alarmRepository The current value of AlarmRepository
     */
    @Reference
    protected void setAlarmRepository(AlarmRepository alarmRepository) {
        getLog().debug("invoking setAlarmRepository, setting: {}", alarmRepository);
        this.alarmRepository = alarmRepository;
    }

    protected AlarmRepository getAlarmRepository() {
        getLog().debug("invoking getAlarmRepository, returning: {}", this.alarmRepository);
        return alarmRepository;
    }

    /**
     * @param hPacketSystemApi The current value of HPacketSystemApi
     */
    @Reference
    protected void sethPacketSystemApi(HPacketSystemApi hPacketSystemApi) {
        getLog().debug("invoking sethPacketSystemApi, setting: {}", hPacketSystemApi);
        this.hPacketSystemApi = hPacketSystemApi;
    }

    protected HPacketSystemApi gethPacketSystemApi() {
        getLog().debug("invoking gethPacketSystemApi, returning: {}", this.hPacketSystemApi);
        return hPacketSystemApi;
    }

    @Reference
    public void sethBaseConnectorSystemApi(HBaseConnectorSystemApi hBaseConnectorSystemApi) {
        this.hBaseConnectorSystemApi = hBaseConnectorSystemApi;
    }

    @Override
    public AlarmEvent findByAlarmIdAndEventId(long alarmId, long eventId) {
        return repository.findByAlarmIdAndEventid(alarmId, eventId);
    }

    @Override
    public Collection<AlarmEvent> findByEventId(long eventId) {
        return repository.findByEventId(eventId);
    }

    @Override
    public Collection<AlarmEvent> findAllEventsByAlarmId(long alarmId) {
        HyperIoTQuery byAlarmId = HyperIoTQueryBuilder.newQuery().equals("alarm.id", alarmId);
        return this.findAll(byAlarmId, null);
    }

    @Override
    public void remove(long id, HyperIoTContext ctx) {
        try {
            AlarmEvent alarmEvent = this.find(id, ctx);
            this.ruleEngineSystemApi.remove(alarmEvent.getEvent().getId(), ctx);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
    }

    @Override
    public AlarmEvent save(AlarmEvent entity, HyperIoTContext ctx) {
        Alarm alarm;
        try {
            alarm = this.getAlarmRepository().find(entity.getAlarm().getId(), ctx);
        } catch (NoResultException exception) {
            throw new HyperIoTEntityNotFound();
        }
        entity.setAlarm(alarm);
        entity.getEvent().setType(RuleType.ALARM_EVENT);
        String jsonAction = configureJsonActionForRuleEvent(entity);
        try {
            entity.getEvent().setJsonActions(jsonAction);
        } catch (IOException e) {
            //TODO improve the exception that is thrown.
            throw new HyperIoTRuntimeException();
        }
        ruleEngineSystemApi.save(entity.getEvent(), ctx);
        return super.save(entity, ctx);
    }

    @Override
    public AlarmEvent update(AlarmEvent entity, HyperIoTContext ctx) {
        try {
            if (entity.getEvent() == null) {
                throw new NoResultException();
            }
            AlarmEvent dbEntity = null;
            try {
                dbEntity = this.find(entity.getId(), ctx);
            } catch (NoResultException e) {
                throw new HyperIoTEntityNotFound();
            }
            entity.setAlarm(dbEntity.getAlarm());
            entity.getEvent().setType(RuleType.ALARM_EVENT);
            String jsonAction = configureJsonActionForRuleEvent(entity);
            try {
                entity.getEvent().setJsonActions(jsonAction);
            } catch (IOException e) {
                //TODO improve the exception that is thrown.
                throw new HyperIoTRuntimeException();
            }
            Rule event = ruleEngineSystemApi.update(entity.getEvent(), ctx);
            entity.setEvent(event);
            return super.update(entity, ctx);
        } catch (NoResultException exc) {
            throw new HyperIoTEntityNotFound();
        }
    }

    public FiredRule getFiredRule(long projectId, long eventId) {
        try {
            String tableName = EVENT_STATUS_HBASE_TABLE + projectId;
            if (hBaseConnectorSystemApi.tableExists(tableName)) {
                //to do pass info correctly
                byte[] columnFamily = Bytes.toBytes("rule");
                byte[] key = Bytes.toBytes(eventId);
                List<byte[]> results = hBaseConnectorSystemApi.scan(tableName, columnFamily, key, key, key);
                if (results.size() > 0) {
                    //there should be only one result we always get the first one.
                    byte[] result = results.get(0);
                    return internalMapper.readValue(result, new TypeReference<FiredRule>() {
                    });
                }
            }
        } catch (IOException e) {
            getLog().error(e.getMessage(), e);
        }
        return null;
    }

    @Activate
    public void onActivate() {
        this.checkRegisteredUserRoleExist();
    }

    private void checkRegisteredUserRoleExist() {
        String alarmEventResourceName = AlarmEvent.class.getName();
        List<HyperIoTAction> alarmEventCrudAction = HyperIoTActionsUtil.getHyperIoTCrudActions(alarmEventResourceName);
        this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, alarmEventCrudAction);
    }

    private String configureJsonActionForRuleEvent(AlarmEvent event) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Rule eventRule = event.getEvent();
            TypeReference<ArrayList<String>> typeRefList = new TypeReference<ArrayList<String>>() {
            };
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayList<String> actionList = objectMapper.readValue(eventRule.getJsonActions(), typeRefList);
            ArrayList<String> actionListGenerated = new ArrayList<>();
            for (String jsonAction : actionList) {
                TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
                };
                HashMap<String, Object> jsonActionMap = mapper.readValue(jsonAction, typeRef);
                String actionClassName = jsonActionMap.get("actionName").toString();
                Class<?> ruleActionClass = Class.forName(actionClassName);
                if (!AlarmAction.class.isAssignableFrom(ruleActionClass)) {
                    throw new HyperIoTException();
                }
                //todo update information in a correct way
                //String deviceName = getDeviceNameFromRuleDefinition(eventRule);
                updateAlarmIdOnEventJsonActionMap(jsonActionMap, event.getAlarm().getId());
                updateSeverityOnEventJsonActionMap(jsonActionMap, event.getSeverity());
                updateAlarmNameOnEventJsonActionMap(jsonActionMap, event.getAlarm().getName());
                //updateDeviceNameOnEventJsonActionMap(jsonActionMap, deviceName);
                actionListGenerated.add(mapper.writeValueAsString(jsonActionMap));
            }
            return mapper.writeValueAsString(actionListGenerated);
        } catch (Throwable e) {
            //TODO Improve the Exception returned .
            getLog().debug(e.getMessage(), e);
            throw new HyperIoTValidationException(new HashSet<>());
        }
    }

    private void updateAlarmIdOnEventJsonActionMap(HashMap<String, Object> jsonActionsAsMap, long alarmId) {
        jsonActionsAsMap.remove("alarmId");
        jsonActionsAsMap.put("alarmId", alarmId);
    }


    private void updateSeverityOnEventJsonActionMap(HashMap<String, Object> jsonActionsAsMap, int severity) {
        jsonActionsAsMap.remove("severity");
        jsonActionsAsMap.put("severity", severity);
    }

    private void updateAlarmNameOnEventJsonActionMap(HashMap<String, Object> jsonActionsAsMap, String alarmName) {
        jsonActionsAsMap.remove("alarmName");
        jsonActionsAsMap.put("alarmName", alarmName);
    }

    private void updateDeviceNameOnEventJsonActionMap(HashMap<String, Object> jsonActionsAsMap, String deviceName) {
        jsonActionsAsMap.remove("deviceName");
        jsonActionsAsMap.put("deviceName", deviceName);
    }

    private String getDeviceNameFromRuleDefinition(Rule eventRule) {
        long hPacketId;
        try {
            hPacketId = retrievePacketIdFromRuleDefinition(eventRule);
        } catch (Exception e) {
            //illegal input data exception.
            throw new HyperIoTRuntimeException();
        }
        try {
            HPacket packet = hPacketSystemApi.find(hPacketId, null);
            return packet.getDevice().getDeviceName();
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
    }

    private long retrievePacketIdFromRuleDefinition(Rule eventRule) {
        return Long.parseLong(eventRule.getRuleDefinition().replace("\"", "").split("\\.")[0]);
    }

}
