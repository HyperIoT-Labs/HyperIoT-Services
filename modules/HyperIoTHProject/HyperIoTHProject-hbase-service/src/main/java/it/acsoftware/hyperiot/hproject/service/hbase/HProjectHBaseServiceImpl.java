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

package it.acsoftware.hyperiot.hproject.service.hbase;

import it.acsoftware.hyperiot.base.api.HyperIoTBaseSystemApi;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.security.annotations.AllowGenericPermissions;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseServiceImpl;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmHBaseResult;
import it.acsoftware.hyperiot.hproject.api.hbase.HProjectHBaseApi;
import it.acsoftware.hyperiot.hproject.api.hbase.HProjectHBaseSystemApi;
import it.acsoftware.hyperiot.hproject.model.hbase.HPacketCount;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineElement;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Component(service = HProjectHBaseApi.class, immediate = true)
public class HProjectHBaseServiceImpl extends HyperIoTBaseServiceImpl implements HProjectHBaseApi {

    private HProjectHBaseSystemApi systemService;

    @Override
    @AllowPermissions(actions = HyperIoTHProjectAction.Names.SCAN_HBASE_DATA, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public void scanHProject(HyperIoTContext context, long hProjectId, List<String> hPacketIds, List<String> hDeviceIds,
                             long rowKeyLowerBound, long rowKeyUpperBound, int limit, String alarmState,
                             OutputStream outputStream)
            throws IOException {
        systemService.scanHProject(hProjectId, hPacketIds, hDeviceIds, rowKeyLowerBound, rowKeyUpperBound, limit, alarmState, outputStream);
    }

    @Override
    @AllowPermissions(actions = HyperIoTHProjectAction.Names.SCAN_HBASE_DATA, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public List<HPacketCount> timelineEventCount(HyperIoTContext context, long projectId,
                                                 List<String> packetIds, List<String> deviceIds, long startTime, long endTime) throws Throwable {
        return systemService.timelineEventCount(projectId, packetIds, deviceIds, startTime, endTime);
    }

    /*
       TODO
           To secure this service we must update the signature of the method to include the hprojectId relative to this packet.
           After that , we must replace allowgenericpermission with allowpermission like defined in the above method.
     */
    @Override
    @AllowGenericPermissions(actions = HyperIoTHProjectAction.Names.SCAN_HBASE_DATA, resourceName = "it.acsoftware.hyperiot.hproject.model.HProject")
    public List<TimelineElement> timelineScan(HyperIoTContext context, String tableName,
                                              List<String> packetIds, List<String> deviceIds, TimelineColumnFamily step, long startTime,
                                              long endTime, String timezone)
            throws Exception {
        return systemService.timelineScan(tableName, packetIds, deviceIds, step, startTime, endTime, timezone);
    }

    @Override
    @AllowPermissions(actions = HyperIoTHProjectAction.Names.SCAN_HBASE_DATA, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public HProjectAlgorithmHBaseResult getAlgorithmOutputs(HyperIoTContext hyperIoTContext, long projectId,
                                                            long hProjectAlgorithmId)
            throws IOException {
        getLog().debug("invoking getAlgorithmOutputs, on project: {} and hProjectAlgorithm {}",
                projectId, hProjectAlgorithmId);
        return systemService.getAlgorithmOutputs(projectId, hProjectAlgorithmId);
    }

    @AllowPermissions(actions = HyperIoTHProjectAction.Names.SCAN_HBASE_DATA, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public byte[] getHPacketAttachment(HyperIoTContext context, long hProjectId, long packetId, long fieldId,long rowKeyLowerBound, long rowKeyUpperBound) throws IOException {
        return systemService.getHPacketAttachment(hProjectId, packetId, fieldId, rowKeyLowerBound, rowKeyUpperBound);
    }

    /**
     * @param hProjectHBaseSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setSystemService(HProjectHBaseSystemApi hProjectHBaseSystemService) {
        this.systemService = hProjectHBaseSystemService;
    }

    @Override
    protected HyperIoTBaseSystemApi getSystemService() {
        return systemService;
    }
}
