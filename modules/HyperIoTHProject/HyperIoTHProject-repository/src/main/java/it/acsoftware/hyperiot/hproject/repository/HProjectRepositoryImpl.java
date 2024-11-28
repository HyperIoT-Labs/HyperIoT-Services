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

package it.acsoftware.hyperiot.hproject.repository;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTNoResultException;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.hproject.api.HProjectRepository;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.huser.api.HUserRepository;
import it.acsoftware.hyperiot.huser.model.HUser;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.EntityGraph;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Aristide Cittadino Implementation class of the HProject. This
 * class is used to interact with the persistence layer.
 */
@Component(service = HProjectRepository.class, immediate = true)
public class HProjectRepositoryImpl extends HyperIoTBaseRepositoryImpl<HProject> implements HProjectRepository {
    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;

    /**
     * Injecting the HUserRepository to interact with database
     */
    private HUserRepository hUserRepository;

    /**
     * Constructor for a HProjectRepositoryImpl
     */
    public HProjectRepositoryImpl() {
        super(HProject.class);
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

    /**
     * @return The current hUserRepository
     */
    protected HUserRepository gethUserRepository() {
        getLog().debug("invoking gethUserRepository, returning: {}", hUserRepository);
        return hUserRepository;
    }

    /**
     * @param hUserRepository Injection of HUserRepository
     */
    @Reference
    protected void sethUserRepository(HUserRepository hUserRepository) {
        getLog().debug("invoking sethUserRepository, setting: {}", hUserRepository);
        this.hUserRepository = hUserRepository;
    }

    @Override
    public HProject updateHProjectOwner(long projectId, long userId) {
        HProject hProject = this.jpa.txExpr(TransactionType.Required, entityManager -> {
            HProject project = this.find(projectId, null);
            HUser user = this.hUserRepository.find(userId, null);
            project.setUser(user);
            return entityManager.merge(project);
        });
        getLog().debug("In HProjectRepositoryImpl the user with id : {} is the new owner of the HProject with id : {}", userId, projectId);
        return hProject;
    }

    public HProject load(long projectId) {
        return this.jpa.txExpr(TransactionType.RequiresNew, entityManager -> {
            try {
                EntityGraph<?> entityGraph = entityManager.getEntityGraph("completeProject");
                Map<String, Object> properties = new HashMap<>();
                properties.put("javax.persistence.fetchgraph", entityGraph);
                return entityManager.find(HProject.class, projectId, properties);
            } catch (NoResultException e) {
                throw new HyperIoTNoResultException();
            } catch (Exception e) {
                throw e;
            }
        });
    }

    public Collection<HProject> load(HyperIoTQuery filter) {
        return this.jpa.txExpr(TransactionType.RequiresNew, entityManager -> {
            try {
                EntityGraph<?> entityGraph = entityManager.getEntityGraph("completeProject");
                Map<String, Object> hints = new HashMap<>();
                hints.put("javax.persistence.fetchgraph", entityGraph);
                Query q = createQuery(filter, null, entityManager, hints);
                return q.getResultList();
            } catch (NoResultException e) {
                throw new HyperIoTNoResultException();
            } catch (Exception e) {
                throw e;
            }
        });
    }
}
