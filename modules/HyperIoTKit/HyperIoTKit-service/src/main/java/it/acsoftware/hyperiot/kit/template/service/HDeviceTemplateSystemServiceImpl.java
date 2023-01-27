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


import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.kit.template.api.HDeviceTemplateRepository;
import it.acsoftware.hyperiot.kit.template.api.HDeviceTemplateSystemApi;
import it.acsoftware.hyperiot.kit.template.model.HDeviceTemplate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = HDeviceTemplateSystemApi.class, immediate = true)
public class HDeviceTemplateSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<HDeviceTemplate> implements HDeviceTemplateSystemApi{

    private HDeviceTemplateRepository repository;

    public HDeviceTemplateSystemServiceImpl() {
        super(HDeviceTemplate.class);
    }

    @Override
    protected HDeviceTemplateRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}" , this.repository);
        return repository;
    }

    @Reference
    protected void setRepository(HDeviceTemplateRepository repository) {
        getLog().debug("invoking setRepository, setting: {}" , repository);
        this.repository = repository;
    }
}
