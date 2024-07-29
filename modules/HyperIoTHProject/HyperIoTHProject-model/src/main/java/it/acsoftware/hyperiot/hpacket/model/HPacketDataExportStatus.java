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

package it.acsoftware.hyperiot.hpacket.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;

import java.io.IOException;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HPacketDataExportStatus {
    private static ObjectMapper mapper = new ObjectMapper();
    private long processedRecords;
    private long totalRecords;
    private String exportId;
    private boolean started;
    private boolean completed;
    private String fileName;
    private List<String> errorMessages;

    private HPacketDataExportStatus(){
        //do nothing just for jackson
    }
    public HPacketDataExportStatus(String fileName, String exportId, boolean started, boolean completed, long currentRecord, long totalRecords, List<String> errorMessages) {
        this.exportId = exportId;
        this.processedRecords = currentRecord;
        this.totalRecords = totalRecords;
        this.started = started;
        this.completed = completed;
        this.fileName = fileName;
        this.errorMessages = errorMessages;
    }

    public long getProcessedRecords() {
        return processedRecords;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public String getExportId() {
        return exportId;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isCompleted() {
        return completed;
    }

    @JsonProperty("hasErrors")
    public boolean hasErrors() {
        return this.errorMessages != null && !this.errorMessages.isEmpty();
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static HPacketDataExportStatus fromJsonBytes(byte[] jsonData) {
        try {
            return mapper.readValue(jsonData, HPacketDataExportStatus.class);
        } catch (IOException e) {
            throw new HyperIoTRuntimeException("Impossibile to get export status...");
        }
    }
}
