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

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketDataExportRepository;
import it.acsoftware.hyperiot.hpacket.model.HPacketDataExport;
import it.acsoftware.hyperiot.hpacket.model.HPacketDataExportInterruptedException;
import it.acsoftware.hyperiot.hpacket.model.HPacketDataExportStatus;
import it.acsoftware.hyperiot.hpacket.model.HPacketFormat;
import it.acsoftware.hyperiot.hproject.api.hbase.HProjectHBaseSystemApi;
import it.acsoftware.hyperiot.hproject.model.hbase.HPacketCount;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;
import it.acsoftware.hyperiot.zookeeper.connector.api.ZookeeperConnectorSystemApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class HPacketDataExporterImpl implements it.acsoftware.hyperiot.hpacket.api.HPacketDataExporter, Runnable {
    private static Logger log = LoggerFactory.getLogger(HPacketDataExporterImpl.class);
    private HProjectHBaseSystemApi hProjectHBaseSystemApi;
    private HadoopManagerSystemApi hadoopManagerSystemApi;
    private HPacketDataExportRepository hPacketDataExportRepository;
    //using zookeeper to store export state
    private ZookeeperConnectorSystemApi zookeeperConnectorSystemApi;
    private HPacketSerializer hPacketSerializer;
    private HPacketFormat hPacketFormat;
    private long hProjectId;
    private long hPacketId;
    private long from;
    private long to;
    private String fileName;
    private String hadoopCompletePath;
    private String zookeeperPath;
    private boolean started;
    private boolean completed;
    private boolean forceStop;
    private AtomicReference<Long> currentCount;
    private String exportName;
    private String exportId;
    private long totalCount;
    private Executor executor;
    private Set<String> errorMessages;
    private HPacketDataExport hPacketDataExport;

    public HPacketDataExporterImpl(String exportId, String exportName, String zookeeperPath, long hProjectId, long hPacketId, HPacketSerializer hPacketSerializer, HPacketFormat hPacketFormat) {
        this.exportId = exportId;
        this.exportName = exportName;
        this.hProjectId = hProjectId;
        this.hPacketId = hPacketId;
        this.hPacketSerializer = hPacketSerializer;
        this.hPacketFormat = hPacketFormat;
        this.started = false;
        this.completed = false;
        this.forceStop = false;
        this.currentCount = new AtomicReference<>();
        this.currentCount.set(0l);
        this.totalCount = 0;

        this.fileName = exportId + "." + hPacketFormat.getName();
        this.hadoopCompletePath = getHadoopPath(fileName);
        this.zookeeperPath = zookeeperPath;
        this.executor = Executors.newSingleThreadExecutor();
        //default is now
        this.from = System.currentTimeMillis();
        this.to = System.currentTimeMillis();
        this.hPacketDataExport = createHPacketDataExport();
        this.errorMessages = new HashSet<>();
        this.hProjectHBaseSystemApi = (HProjectHBaseSystemApi) HyperIoTUtil.getService(HProjectHBaseSystemApi.class);
        this.hadoopManagerSystemApi = (HadoopManagerSystemApi) HyperIoTUtil.getService(HadoopManagerSystemApi.class);
        this.hPacketDataExportRepository = (HPacketDataExportRepository) HyperIoTUtil.getService(HPacketDataExportRepository.class);
        this.zookeeperConnectorSystemApi = (ZookeeperConnectorSystemApi) HyperIoTUtil.getService(ZookeeperConnectorSystemApi.class);
    }

    private HPacketDataExportStatus createStatus() {
        return new HPacketDataExportStatus(fileName, exportId, started, completed, currentCount.get(), totalCount, Collections.unmodifiableList(new ArrayList<>(this.errorMessages)));
    }

    @Override
    public HPacketDataExportStatus getStatus() {
        return this.createStatus();
    }

    public void setFrom(long from) {
        this.from = from;
    }

    public void setTo(long to) {
        this.to = to;
    }

    protected long countResults(long hProjectId, long hPacketId) {
        try {
            List<HPacketCount> count = hProjectHBaseSystemApi.timelineEventCount(hProjectId, Collections.singletonList(String.valueOf(hPacketId)), Collections.emptyList(), from, to);
            if (count.size() > 0) return count.get(0).getTotalCount();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public void run() {
        hPacketDataExport = hPacketDataExportRepository.save(createHPacketDataExport());
        final BufferedOutputStream exportOutputStreamWriter = getFileOutputStream(hadoopCompletePath);
        try {
            this.hProjectHBaseSystemApi.scanHProject(this.hProjectId, this.hPacketId, from, to, hPacket -> {
                if (forceStop) throw new HPacketDataExportInterruptedException();
                //add packet to the final result
                try {
                    byte[] data = this.hPacketSerializer.serialize(hPacket);
                    exportOutputStreamWriter.write(data);
                    zookeeperConnectorSystemApi.update(zookeeperPath, this.createStatus().toJson().getBytes());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    this.errorMessages.add(e.getMessage());
                } finally {
                    currentCount.getAndUpdate(current -> current + 1);
                }
            });
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            closeOutputStream(exportOutputStreamWriter);
            updateExportCompleted();
        }
    }

    @Override
    public HPacketDataExportStatus start() {
        this.started = true;
        this.totalCount = countResults(hProjectId, hPacketId);
        if (totalCount > 0) {
            try {
                HPacketDataExportStatus initialStatus = createStatus();
                this.zookeeperConnectorSystemApi.createPersistent(zookeeperPath, initialStatus.toJson().getBytes(), true);
                executor.execute(this);
                return initialStatus;
            } catch (Exception e) {
                this.errorMessages.add("Impossibile to create status node: " + e.getMessage());
                this.started = false;
                this.completed = true;
            }
        } else {
            this.started = true;
            this.completed = true;
        }
        return this.createStatus();
    }

    @Override
    public void stop() {
        this.forceStop = true;
    }

    private HPacketDataExport createHPacketDataExport() {
        Date now = new Date(Instant.now().toEpochMilli());
        HPacketDataExport hPacketDataExport = new HPacketDataExport();
        hPacketDataExport.setExportFormat(this.hPacketFormat);
        hPacketDataExport.setExportName(this.exportName);
        hPacketDataExport.setExportId(this.exportId);
        hPacketDataExport.sethPacketId(this.hPacketId);
        hPacketDataExport.sethProjectId(this.hProjectId);
        hPacketDataExport.setFilePath(this.hadoopCompletePath);
        hPacketDataExport.setEntityCreateDate(now);
        hPacketDataExport.setEntityModifyDate(now);
        hPacketDataExport.setEntityVersion(1);
        return hPacketDataExport;
    }

    private BufferedOutputStream getFileOutputStream(String filePath) {
        try {
            OutputStream exportOutputStream = hadoopManagerSystemApi.appendToFile(filePath);
            return new BufferedOutputStream(exportOutputStream);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private void closeOutputStream(BufferedOutputStream bufferedOutputStream) {
        try {
            bufferedOutputStream.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void updateExportCompleted() {
        this.hPacketDataExport.setCompleted(true);
        this.completed = true;
        hPacketDataExportRepository.update(this.hPacketDataExport);
    }

    public static String getHadoopPath(String fileName) {
        return "/exports/" + fileName;
    }
}
