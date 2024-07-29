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

package it.acsoftware.hyperiot.hproject.model;

import it.acsoftware.hyperiot.hpacket.model.HPacket;

import javax.validation.constraints.NotNull;
import java.util.List;

public class AutoRegisterProjectRequest {

    private String cipherTextChallenge;
    private long projectId;
    private List<HPacket> packets;
    

    @NotNull
    public String getCipherTextChallenge() {
        return cipherTextChallenge;
    }

    public void setCipherTextChallenge(String cipherTextChallenge) {
        this.cipherTextChallenge = cipherTextChallenge;
    }

    @NotNull
    public List<HPacket> getPackets() {
        return packets;
    }

    public void setPackets(List<HPacket> packets) {
        this.packets = packets;
    }

    @NotNull
    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }
}
