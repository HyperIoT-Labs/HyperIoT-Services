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

package it.acsoftware.hyperiot.rule.service;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.io.Resource;

import java.util.List;

public class RuleEngineKieBase {

    private static RuleEngineKieBase instance;
    private final KieBase kieBase;

    private RuleEngineKieBase(List<Resource> ruleResources) {
        KieServices kieServices = KieServices.Factory.get();
        // create kie module xml file programmatically
        KieModuleModel kieModuleModel = kieServices.newKieModuleModel();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem().writeKModuleXML(kieModuleModel.toXML());
        for (Resource resource : ruleResources) {
            // add all rules to kie
            kieFileSystem.write(resource);
        }

        kieServices.newKieBuilder(kieFileSystem).buildAll();
        KieRepository kieRepository = kieServices.getRepository();
        ReleaseId krDefaultReleaseId = kieRepository.getDefaultReleaseId();
        kieBase = kieServices.newKieContainer(krDefaultReleaseId).getKieBase();
    }

    public static synchronized RuleEngineKieBase getInstance(List<Resource> ruleResources) {
        if (instance != null)
            return instance;
        instance = new RuleEngineKieBase(ruleResources);
        return instance;
    }

    public KieBase getKieBase() {
        return kieBase;
    }

}
