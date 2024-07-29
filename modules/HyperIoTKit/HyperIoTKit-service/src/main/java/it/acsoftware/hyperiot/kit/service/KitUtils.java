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

package it.acsoftware.hyperiot.kit.service;

import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.kit.model.Kit;

public class KitUtils {

    public final static long SYSTEM_KIT_PROJECT_ID=0;

    public static boolean isSystemKit(Kit kit){
        if(kit==null){
            throw new HyperIoTRuntimeException();
        }
        return kit.getProjectId() == SYSTEM_KIT_PROJECT_ID;
    }
}
