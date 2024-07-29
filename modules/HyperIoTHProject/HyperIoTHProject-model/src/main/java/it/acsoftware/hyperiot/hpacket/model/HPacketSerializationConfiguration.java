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

import java.time.format.DateTimeFormatter;

public class HPacketSerializationConfiguration {
    private DateTimeFormatter dateTimeFormatter;
    private boolean useFieldIds;
    private boolean prettyTimestamp;
    private boolean exportTimestampField;
    private HPacketFormat format;

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public void setDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public boolean isUseFieldIds() {
        return useFieldIds;
    }

    public void setUseFieldIds(boolean useFieldIds) {
        this.useFieldIds = useFieldIds;
    }

    public boolean isPrettyTimestamp() {
        return prettyTimestamp;
    }

    public void setPrettyTimestamp(boolean prettyTimestamp) {
        this.prettyTimestamp = prettyTimestamp;
    }

    public HPacketFormat getFormat() {
        return format;
    }

    public void setFormat(HPacketFormat format) {
        this.format = format;
    }

    public boolean isExportTimestampField() {
        return exportTimestampField;
    }

    public void setExportTimestampField(boolean exportTimestampField) {
        this.exportTimestampField = exportTimestampField;
    }
}
