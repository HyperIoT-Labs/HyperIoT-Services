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

package it.acsoftware.hyperiot.hproject.algorithm.service.preaction;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPreRemoveAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmSystemApi;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

@Component(service = HyperIoTPreRemoveAction.class, property = {"type=it.acsoftware.hyperiot.algorithm.model.Algorithm"})
public class AlgorithmPreRemoveAction <T extends HyperIoTBaseEntity> implements HyperIoTPreRemoveAction<T> {

    private static final Logger log = LoggerFactory.getLogger(AlgorithmPreRemoveAction.class.getName());
    private HProjectAlgorithmSystemApi hProjectAlgorithmSystemApi;

    @Override
    public void execute(T entity) {
        long algorithmId = entity.getId();
        log.debug("Delete hprojectAlgorithms related to algorithm with id {}", algorithmId);
        HyperIoTQuery byAlgorithmId = HyperIoTQueryBuilder.newQuery().equals("algorithm.id", algorithmId);
        Collection<HProjectAlgorithm> hProjectAlgorithms =  hProjectAlgorithmSystemApi.findAll(byAlgorithmId,null);
        hProjectAlgorithms.stream().parallel().forEach(
                hProjectAlgorithm -> hProjectAlgorithmSystemApi.remove(hProjectAlgorithm.getId(), null)
        );
    }

    @Reference
    public void sethProjectAlgorithmSystemApi(HProjectAlgorithmSystemApi hProjectAlgorithmSystemApi) {
        this.hProjectAlgorithmSystemApi = hProjectAlgorithmSystemApi;
    }

}