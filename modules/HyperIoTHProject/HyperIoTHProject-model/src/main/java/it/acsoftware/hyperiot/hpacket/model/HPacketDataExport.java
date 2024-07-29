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

import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"exportId"})})
public class HPacketDataExport extends HyperIoTAbstractEntity {
    @JsonView({HyperIoTJSONView.Public.class})
    private long hProjectId;
    @JsonView({HyperIoTJSONView.Public.class})
    private long hPacketId;
    @JsonView({HyperIoTJSONView.Public.class})
    private HPacketFormat exportFormat;
    @JsonView({HyperIoTJSONView.Public.class})
    private String exportId;
    @JsonView({HyperIoTJSONView.Public.class})
    private String exportName;
    @JsonView({HyperIoTJSONView.Public.class})
    private boolean completed;
    @JsonView({HyperIoTJSONView.Public.class})
    private boolean downloaded;
    @JsonView({HyperIoTJSONView.Compact.class})
    private String filePath;

    public long gethProjectId() {
        return hProjectId;
    }

    public void sethProjectId(long hProjectId) {
        this.hProjectId = hProjectId;
    }

    public long gethPacketId() {
        return hPacketId;
    }

    public void sethPacketId(long hPacketId) {
        this.hPacketId = hPacketId;
    }

    @NotNull
    @NotNullOnPersist
    @NoMalitiusCode
    public String getExportId() {
        return exportId;
    }

    public void setExportId(String exportId) {
        this.exportId = exportId;
    }

    @NotNull
    @NotNullOnPersist
    @NoMalitiusCode
    @Length(max = 1000)
    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    @NotNull
    @NotNullOnPersist
    @NoMalitiusCode
    @Length(max = 2000)
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @NotNull
    @NotNullOnPersist
    @Enumerated
    public HPacketFormat getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(HPacketFormat exportFormat) {
        this.exportFormat = exportFormat;
    }
}
