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

package it.acsoftware.hyperiot.storm.builder;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacketTrafficPlan;
import it.acsoftware.hyperiot.hproject.model.HProject;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public enum HyperIoTTopologyPerformanceConfig {
    LOW_PERFORMANCE("21", "128", "64m", "64m", 1),
    MEDIUM_PERFORMANCE( "41", "256", "64m", "64m", 1),
    HIGH_PERFORMANCE( "128", "768", "64m", "64m", 1),
    BEST_PERFORMANCE( "200", "1024", "64m", "64m", 1);
    private String resourcesOnHeapMemory;
    private String topologyWorkerMaxHeapSize;
    private String logWriterXms;
    private String logWriterXmx;
    private int topologyWorkers;

    private int kafkaSpoutParallelism = 1;
    private int deserializationParallelism = 1;
    private int enrichmentParallelism = 1;
    private int eventsProcessingParallelism = 1;
    private int eventsToKafkaParallelism = 1;
    private int eventSourcingParallelism = 1;
    private int kafkaRealtimeParallelism = 1;
    private int alarmManagementParallelism = 1;
    private int alarmCountParallelism = 1;
    private int alarmEventRuleParallelism = 1;
    private int selectionBoltParallelism = 1;
    private int timelineParallelism = 1;
    private int avroProcessingParallelism = 1;
    private int hdfsHourParallelism = 1;
    private int hdfsDayParallelism = 1;
    private int hdfsMonthParallelism = 1;
    private int hdfsYearParallelism = 1;
    private int hdfsSemesterParallelism = 1;
    private int hdfsQuarterParallelism = 1;
    private int dlqParallelism = 1;

    HyperIoTTopologyPerformanceConfig(String resourcesOnHeapMemory, String topologyWorkerMaxHeapSize, String logWriterXms, String logWriterXmx, int topologyWorkers) {
        this.resourcesOnHeapMemory = resourcesOnHeapMemory;
        this.topologyWorkerMaxHeapSize = topologyWorkerMaxHeapSize;
        this.logWriterXms = logWriterXms;
        this.logWriterXmx = logWriterXmx;
        this.topologyWorkers = topologyWorkers;
    }

    public String getResourcesOnHeapMemory() {
        return resourcesOnHeapMemory;
    }

    public String getTopologyWorkerMaxHeapSize() {
        return topologyWorkerMaxHeapSize;
    }

    public String getLogWriterXms() {
        return logWriterXms;
    }

    public String getLogWriterXmx() {
        return logWriterXmx;
    }

    public int getTopologyWorkers() {
        return topologyWorkers;
    }

    public int getKafkaSpoutParallelism() {
        return kafkaSpoutParallelism;
    }

    public int getDeserializationParallelism() {
        return deserializationParallelism;
    }

    public int getEnrichmentParallelism() {
        return enrichmentParallelism;
    }

    public int getEventsProcessingParallelism() {
        return eventsProcessingParallelism;
    }

    public int getEventsToKafkaParallelism() {
        return eventsToKafkaParallelism;
    }

    public int getEventSourcingParallelism() {
        return eventSourcingParallelism;
    }

    public int getKafkaRealtimeParallelism() {
        return kafkaRealtimeParallelism;
    }

    public int getAlarmManagementParallelism() {
        return alarmManagementParallelism;
    }

    public int getAlarmCountParallelism() {
        return alarmCountParallelism;
    }

    public int getAlarmEventRuleParallelism() {
        return alarmEventRuleParallelism;
    }

    public int getSelectionBoltParallelism() {
        return selectionBoltParallelism;
    }

    public int getTimelineParallelism() {
        return timelineParallelism;
    }

    public int getAvroProcessingParallelism() {
        return avroProcessingParallelism;
    }

    public int getHdfsHourParallelism() {
        return hdfsHourParallelism;
    }

    public int getHdfsDayParallelism() {
        return hdfsDayParallelism;
    }

    public int getHdfsMonthParallelism() {
        return hdfsMonthParallelism;
    }

    public int getHdfsYearParallelism() {
        return hdfsYearParallelism;
    }

    public int getHdfsSemesterParallelism() {
        return hdfsSemesterParallelism;
    }

    public int getHdfsQuarterParallelism() {
        return hdfsQuarterParallelism;
    }

    public int getDlqParallelism() {
        return dlqParallelism;
    }

    public static HyperIoTTopologyPerformanceConfig fromHProject(HProject project) {
        AtomicReference<HPacketTrafficPlan> worstCase = new AtomicReference<>();
        worstCase.set(HPacketTrafficPlan.LOW);
        HDeviceSystemApi hDeviceSystemApi = (HDeviceSystemApi) HyperIoTUtil.getService(HDeviceSystemApi.class);
        HPacketSystemApi hPacketSystemApi = (HPacketSystemApi) HyperIoTUtil.getService(HPacketSystemApi.class);
        Collection<HDevice> deviceList = hDeviceSystemApi.getProjectDevicesList(project.getId());
        deviceList.stream().flatMap(device -> hPacketSystemApi.getPacketsList(device.getId()).stream()).forEach(packet -> {
            if (packet.getTrafficPlan().getOrder() > worstCase.get().getOrder())
                worstCase.set(packet.getTrafficPlan());
        });
        return fromTrafficPlan(worstCase.get());
    }

    private static HyperIoTTopologyPerformanceConfig fromTrafficPlan(HPacketTrafficPlan plan) {
        if (plan == HPacketTrafficPlan.LOW)
            return LOW_PERFORMANCE;
        else if (plan == HPacketTrafficPlan.MEDIUM)
            return MEDIUM_PERFORMANCE;
        else if (plan == HPacketTrafficPlan.HIGH)
            return HIGH_PERFORMANCE;
        else
            return BEST_PERFORMANCE;
    }
}
