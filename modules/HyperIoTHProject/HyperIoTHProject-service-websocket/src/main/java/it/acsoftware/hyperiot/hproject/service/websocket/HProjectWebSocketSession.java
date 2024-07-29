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

package it.acsoftware.hyperiot.hproject.service.websocket;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.api.HProjectApi;
import it.acsoftware.hyperiot.hproject.model.HyperIoTTopicType;
import it.acsoftware.hyperiot.kafka.connector.service.websocket.KafkaAbstractWebSocketSession;
import it.acsoftware.hyperiot.websocket.model.message.HyperIoTWebSocketMessage;
import it.acsoftware.hyperiot.websocket.model.message.HyperIoTWebSocketMessageType;
import org.eclipse.jetty.websocket.api.Session;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Author Generoso Martello.
 * Class which exposes web socket session for HProjects
 */
public class HProjectWebSocketSession extends KafkaAbstractWebSocketSession {
    private Logger logger = LoggerFactory.getLogger(HProjectWebSocketSession.class.getName());

    private static final String HPROJECT_KAFKA_POLL_TIME = "it.acsoftware.hyperiot.hproject.kafka.connector.poll.time";
    /**
     * HProject System Api
     */
    private HProjectApi projectApi;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public HProjectWebSocketSession(Session session) {
        super(session);

    }

    /**
     * @return List of topics the logged user can connect to
     */
    private List<String> getUserRealtimeTopics(long projectId) {
        //retrieving projectId if it is related to the current logged user
        return this.projectApi.getUserProjectRealtimeTopics(getContext(), HyperIoTTopicType.KAFKA, projectId);
    }

    /**
     * @param throwable
     */
    @Override
    public void onError(Throwable throwable) {
        try {
            HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, throwable.getMessage().getBytes(), HyperIoTWebSocketMessageType.ERROR);
            this.sendRemote(m, false);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            logger.error(throwable.getMessage(), throwable);
        }
    }

    /**
     * @param key
     * @param value
     * @param session
     * @throws IOException
     */
    @Override
    public void send(byte[] key, byte[] value, Session session) throws IOException {
        HyperIoTWebSocketMessage m = HProjectWebSocketMessage.createMessage(null, value, key, HyperIoTWebSocketMessageType.APPLICATION);
        this.sendRemote(m, false);
    }

    @Override
    public void initialize() {
        // HProject System API
        ServiceReference serviceReference = getBundleContext()
                .getServiceReference(HProjectApi.class);
        projectApi = (HProjectApi) getBundleContext()
                .getService(serviceReference);
        List<String> projectsIdsStr = null;
        if (getSession().getUpgradeRequest().getParameterMap().containsKey("projectId")) {
            try {
                projectsIdsStr = getSession().getUpgradeRequest().getParameterMap().get("projectId");
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (projectsIdsStr.size() == 0) {
            HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, "Ivalid project Id!".getBytes(), HyperIoTWebSocketMessageType.ERROR);
            this.sendRemote(m, false);
            getSession().close();
            return;
        }
        //setting topics from user
        List<String> topics = new ArrayList<>();
        projectsIdsStr.stream().forEach(projectIdStr -> {
            long projectId = Long.parseLong(projectIdStr);
            topics.addAll(getUserRealtimeTopics(projectId));
        });

        if (!topics.isEmpty()) {
            this.setTopics(topics);
            this.start();
            // send HPacket AVRO schema
            HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(
                    null,
                    new HPacket().getJsonSchema().getBytes(),
                    HyperIoTWebSocketMessageType.INFO
            );
            this.sendRemote(m, false);
        } else {
            HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, "No data to receive, wrong  project id or not related to the current user...".getBytes(), HyperIoTWebSocketMessageType.ERROR);
            this.sendRemote(m, false);
            this.getSession().close();
        }
    }

    @Override
    public void onMessage(String s) {
        // Do nothing
    }

    @Override
    public void close(String s) {
        /*
            TODO implement logic
         */
    }

    @Override
    public long getKafkaPollTime() {
        String kafkaPollTime = (String) HyperIoTUtil.getHyperIoTProperty(HPROJECT_KAFKA_POLL_TIME);
        return Long.parseLong(kafkaPollTime);
    }

}
