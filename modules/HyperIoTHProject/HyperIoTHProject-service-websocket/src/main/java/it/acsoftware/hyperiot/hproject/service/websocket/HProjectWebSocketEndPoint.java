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

import it.acsoftware.hyperiot.websocket.api.HyperIoTWebSocketEndPoint;
import it.acsoftware.hyperiot.websocket.api.HyperIoTWebSocketSession;
import org.eclipse.jetty.websocket.api.Session;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true)
public class HProjectWebSocketEndPoint implements HyperIoTWebSocketEndPoint {

    /**
     * Gets the relative path name of this WebSocket endpoint
     *
     * @return The path name
     */
    public String getPath() {
        return "project";
    }

    /**
     * Get the WebSocket handler for a given session
     *
     * @param session The session instance
     * @return The WebSocket session handler
     */
    public HyperIoTWebSocketSession getHandler(Session session) {
        return new HProjectWebSocketSession(session);
    }

}
