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

package it.acsoftware.hyperiot.hpacket.service.export;

import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketDataExportManager;
import it.acsoftware.hyperiot.hpacket.api.HPacketDataExportRepository;
import it.acsoftware.hyperiot.hpacket.api.HPacketDataExporter;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketDataExport;
import it.acsoftware.hyperiot.hpacket.model.HPacketDataExportStatus;
import it.acsoftware.hyperiot.hpacket.model.HPacketFormat;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.builder.HPacketSerializerBuilder;
import it.acsoftware.hyperiot.zookeeper.connector.api.ZookeeperConnectorSystemApi;
import it.acsoftware.hyperiot.zookeeper.connector.util.HyperIoTZookeeperConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component(service = HPacketDataExportManager.class, immediate = true)
public class HPacketDataExporterManagerImpl implements HPacketDataExportManager {
    private static Logger log = LoggerFactory.getLogger(HPacketDataExporterManagerImpl.class);
    private static final String DATE_DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private HPacketSystemApi hPacketSystemApi;
    private ZookeeperConnectorSystemApi zookeeperConnectorSystemApi;
    private HPacketDataExportRepository hPacketDataExportRepository;
    private HadoopManagerSystemApi hadoopManagerSystemApi;
    private Map<String, HPacketDataExporter> localRunningExports = new HashMap<>();


    @Reference
    public void sethPacketSystemApi(HPacketSystemApi hPacketSystemApi) {
        this.hPacketSystemApi = hPacketSystemApi;
    }

    @Reference
    public void setZookeeperConnectorSystemApi(ZookeeperConnectorSystemApi zookeeperConnectorSystemApi) {
        this.zookeeperConnectorSystemApi = zookeeperConnectorSystemApi;
    }

    //inject not working on mqtt server
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void sethPacketDataExportRepository(HPacketDataExportRepository hPacketDataExportRepository) {
        this.hPacketDataExportRepository = hPacketDataExportRepository;
    }

    //inject not working on mqtt server
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setHadoopManagerSystemApi(HadoopManagerSystemApi hadoopManagerSystemApi) {
        this.hadoopManagerSystemApi = hadoopManagerSystemApi;
    }

    @Deactivate
    public void onDeactivate() {
        //forcing stop on all exports
        this.localRunningExports.keySet().iterator().forEachRemaining(exportId -> this.localRunningExports.get(exportId).stop());
    }

    @Override
    public HPacketDataExporter createExporter(HPacketFormat exportFormat, String exportName, long hProjectId, long hPacketId, boolean prettifyTimestamp, String timestampPattern) {
        String exportId = UUID.randomUUID().toString();
        HPacket hPacketDefinition = hPacketSystemApi.find(hPacketId, null);
        HPacketSerializerBuilder hPacketSerializerBuilder = HPacketSerializerBuilder.newBuilder()
                .withFormat(exportFormat)
                .withTimestampField(true)
                .withHPacketDefinition(hPacketDefinition);
        if (prettifyTimestamp) {
            DateTimeFormatter dateTimeFormatter = null;
            if (timestampPattern == null)
                dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_DEFAULT_PATTERN);
            else
                dateTimeFormatter = DateTimeFormatter.ofPattern(timestampPattern);
            hPacketSerializerBuilder.withPrettyTimestamp(true)
                    .withDateTimeFormatter(dateTimeFormatter);
        }
        HPacketSerializer serializer = hPacketSerializerBuilder.build();
        HPacketDataExporter exporter = new HPacketDataExporterImpl(exportId, exportName, getZookeeperPath(exportId), hProjectId, hPacketId, serializer, exportFormat);
        localRunningExports.put(exportId, exporter);
        return exporter;
    }

    @Override
    public void forceStop(String exportId) {
        if (localRunningExports.containsKey(exportId)) localRunningExports.get(exportId).stop();
    }

    @Override
    public String getJsonStatus(String exportId) {
        if (localRunningExports.containsKey(exportId))
            return localRunningExports.get(exportId).getStatus().toJson();
        try {
            if (this.zookeeperConnectorSystemApi.checkExists(getZookeeperPath(exportId)))
                return new String(readHPacketDataExportStatus(exportId));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        throw new HyperIoTEntityNotFound();
    }

    @Override
    public InputStream getExportStream(String exportId) {
        HPacketDataExportStatus status = getStatus(exportId);
        if (status != null && status.isStarted() && status.isCompleted()) {
            try {
                String hadoopFilePath = HPacketDataExporterImpl.getHadoopPath(status.getFileName());
                return hadoopManagerSystemApi.readFile(hadoopFilePath);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new HyperIoTRuntimeException("Errore while reading the export file...");
            }
        }
        throw new HyperIoTEntityNotFound();
    }

    @Override
    public HPacketDataExport getHPacketDataExport(String exportId) {
        return this.hPacketDataExportRepository.findByExportId(exportId);
    }

    @Override
    public void finalizeDownload(String exportId) {
        try {
            HPacketDataExportStatus status = getStatus(exportId);
            HPacketDataExport dataExport = hPacketDataExportRepository.findByExportId(exportId);
            dataExport.setCompleted(true);
            dataExport.setDownloaded(true);
            String hadoopFilePath = HPacketDataExporterImpl.getHadoopPath(status.getFileName());
            hPacketDataExportRepository.update(dataExport);
            hadoopManagerSystemApi.deleteFile(hadoopFilePath);
            zookeeperConnectorSystemApi.delete(getZookeeperPath(exportId));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private HPacketDataExportStatus getStatus(String exportId) {
        if (localRunningExports.containsKey(exportId))
            return localRunningExports.get(exportId).getStatus();
        else
            return HPacketDataExportStatus.fromJsonBytes(readHPacketDataExportStatus(exportId));
    }

    private String getZookeeperPath(String exportId) {
        String layer = HyperIoTUtil.getLayer();
        return HyperIoTZookeeperConstants.HYPERIOT_ZOOKEEPER_BASE_PATH + "/" + layer + "/hprojects/exports/" + exportId;
    }

    private byte[] readHPacketDataExportStatus(String exportId) {
        try {
            return this.zookeeperConnectorSystemApi.read(getZookeeperPath(exportId), true);
        } catch (Exception e) {
            throw new HyperIoTRuntimeException("Impossible to retrieve export status with id" + exportId);
        }
    }
}
