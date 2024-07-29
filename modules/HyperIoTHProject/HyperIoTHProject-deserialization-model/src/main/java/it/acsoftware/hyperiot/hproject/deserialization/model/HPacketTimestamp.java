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

package it.acsoftware.hyperiot.hproject.deserialization.model;

public class HPacketTimestamp {

    private String format;
    private String field;
    private boolean createDefaultIfNotExists;

    public HPacketTimestamp() {
        this.createDefaultIfNotExists = true;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public boolean isCreateDefaultIfNotExists() {
        return createDefaultIfNotExists;
    }

    public void setCreateDefaultIfNotExists(boolean createDefaultIfNotExists) {
        this.createDefaultIfNotExists = createDefaultIfNotExists;
    }
}
