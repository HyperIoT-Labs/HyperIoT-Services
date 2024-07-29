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

package it.acsoftware.hyperiot.hpacket.repository;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.hpacket.api.HPacketDataExportRepository;
import it.acsoftware.hyperiot.hpacket.model.HPacketDataExport;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import org.apache.aries.jpa.template.JpaTemplate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Aristide Cittadino Implementation class of the HProject. This
 * class is used to interact with the persistence layer.
 */
@Component(service = HPacketDataExportRepository.class, immediate = true)
public class HPacketDataExportRepositoryImpl extends HyperIoTBaseRepositoryImpl<HPacketDataExport> implements HPacketDataExportRepository {
    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;


    /**
     * Constructor for a HProjectRepositoryImpl
     */
    public HPacketDataExportRepositoryImpl() {
        super(HPacketDataExport.class);
    }

    /**
     * @return The current jpaTemplate
     */
    @Override
    protected JpaTemplate getJpa() {
        getLog().debug("invoking getJpa, returning: {}", jpa);
        return jpa;
    }

    /**
     * @param jpa Injection of JpaTemplate
     */
    @Override
    @Reference(target = "(osgi.unit.name=hyperiot-hProject-persistence-unit)")
    protected void setJpa(JpaTemplate jpa) {
        getLog().debug("invoking setJpa, setting: {}", jpa);
        this.jpa = jpa;
    }

    @Override
    public HPacketDataExport findByExportId(String exportId) {
        HyperIoTQuery q = HyperIoTQueryBuilder.newQuery().equals("exportId", exportId);
        return this.find(q, null);
    }
}
