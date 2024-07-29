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

package it.acsoftware.hyperiot.kit.template.service;

import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.kit.template.api.HPacketFieldTemplateApi;
import it.acsoftware.hyperiot.kit.template.api.HPacketFieldTemplateSystemApi;
import it.acsoftware.hyperiot.kit.template.model.HPacketFieldTemplate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = HPacketFieldTemplateApi.class, immediate = true)
public class HPacketFieldTemplateServiceImpl extends HyperIoTBaseEntityServiceImpl<HPacketFieldTemplate> implements HPacketFieldTemplateApi {

    private HPacketFieldTemplateSystemApi systemService;

    public HPacketFieldTemplateServiceImpl() {
        super(HPacketFieldTemplate.class);
    }

    @Override
    protected HPacketFieldTemplateSystemApi getSystemService() {
        getLog().debug("invoking getSystemService, returning: {}" , this.systemService);
        return systemService;
    }

    @Reference
    protected void setSystemService(HPacketFieldTemplateSystemApi systemService) {
        getLog().debug("invoking setSystemService, setting: {}" , systemService);
        this.systemService = systemService ;
    }
}
