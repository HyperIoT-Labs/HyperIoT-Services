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

public class HProjectSharingInfo {
    private long ownerId;
    private String ownerName;
    private String ownerLastName;
    private int collaboratorCounters;

    public HProjectSharingInfo(long ownerId, String ownerName, String ownerLastName, int collaboratorCounters) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.ownerLastName = ownerLastName;
        this.collaboratorCounters = collaboratorCounters;
    }

    public HProjectSharingInfo() {
    }

    public long getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerLastName() {
        return ownerLastName;
    }

    public int getCollaboratorCounters() {
        return collaboratorCounters;
    }
}
