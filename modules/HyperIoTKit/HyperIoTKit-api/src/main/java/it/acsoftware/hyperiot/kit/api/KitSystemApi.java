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

package it.acsoftware.hyperiot.kit.api;

import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntitySystemApi;

import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.kit.model.Kit;

import java.util.Collection;

/**
 * 
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 *         interface defines methods for additional operations.
 *
 */
public interface KitSystemApi extends HyperIoTBaseEntitySystemApi<Kit> {

    long[] getKitCategories(long kitId);

    Collection<AssetTag> getKitTags(long kitId, HyperIoTContext ctx);

    AssetTag addTagToKit(long kitId , AssetTag tag, HyperIoTContext ctx);

    void deleteTagFromKit(long kitId,long tagId, HyperIoTContext ctx);

    HDevice installHDeviceTemplateOnProject(HyperIoTContext ctx, long hProjectId, long kitId, String deviceName, long hdeviceTemplateId);

}