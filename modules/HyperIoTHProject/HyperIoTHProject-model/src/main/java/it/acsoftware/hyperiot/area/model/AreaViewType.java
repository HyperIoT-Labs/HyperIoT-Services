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

package it.acsoftware.hyperiot.area.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum AreaViewType {
    //MAP supports image types just for uploading miniatures
    MAP("jpg", "jpeg", "svg", "webp","png"),
    BIM_XKT("xkt"),
    BIM_IFC("ifc"),
    IMAGE("jpg", "jpeg", "svg", "webp","png");

    private List<String> supportedFileExentsions;

    AreaViewType(String... supportedFileExensions) {
        if (supportedFileExensions != null && supportedFileExensions.length > 0)
            this.supportedFileExentsions = Arrays.asList(supportedFileExensions);
        else
            this.supportedFileExentsions = Collections.emptyList();
    }

    public List<String> getSupportedFileExentsions() {
        return this.supportedFileExentsions;
    }
}
