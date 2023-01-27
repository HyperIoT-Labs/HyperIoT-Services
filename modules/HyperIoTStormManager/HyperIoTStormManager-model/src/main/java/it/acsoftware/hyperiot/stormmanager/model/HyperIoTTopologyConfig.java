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

package it.acsoftware.hyperiot.stormmanager.model;

import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.model.HProject;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @Author Aristide Cittadino
 * This class is used to collect all info related to packet and device configuration of storm topology.
 */
public class HyperIoTTopologyConfig {
    public HProject project;
    public HDevice device;
    public Collection<HPacket> packets = new LinkedList<>();
    public StringBuilder packetConfig = new StringBuilder();
    public StringBuilder properties = new StringBuilder();
}
