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

import it.acsoftware.hyperiot.websocket.model.message.HyperIoTWebSocketMessage;
import it.acsoftware.hyperiot.websocket.model.message.HyperIoTWebSocketMessageType;

import java.util.Date;

public class HProjectWebSocketMessage extends HyperIoTWebSocketMessage {
    private byte[] key;

    @Override
    public String getCmd() {
        return super.getCmd();
    }

    @Override
    public byte[] getPayload() {
        return super.getPayload();
    }

    @Override
    public Date getTimestamp() {
        return super.getTimestamp();
    }

    @Override
    public HyperIoTWebSocketMessageType getType() {
        return super.getType();
    }

    @Override
    public String getContentType() {
        return super.getContentType();
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public static HProjectWebSocketMessage createMessage(String cmd, byte[] payload,byte[] key, HyperIoTWebSocketMessageType type){
        HProjectWebSocketMessage m = new HProjectWebSocketMessage();
        m.setTimestamp(new Date());
        m.setCmd(cmd);
        m.setPayload(payload);
        m.setType(type);
        m.setKey(key);
        return m;
    }
}
