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

package it.acsoftware.hyperiot.area.repository;

import it.acsoftware.hyperiot.area.api.AreaRepository;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Collection;
import java.util.List;

/**
 * @author Aristide Cittadino Implementation class of the Area. This
 * class is used to interact with the persistence layer.
 */
@Component(service = AreaRepository.class, immediate = true)
public class AreaRepositoryImpl extends HyperIoTBaseRepositoryImpl<Area> implements AreaRepository {
    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;

    /**
     * Constructor for a AreaRepositoryImpl
     */
    public AreaRepositoryImpl() {
        super(Area.class);
    }

    /**
     * @return The current jpaTemplate
     */
    @Override
    protected JpaTemplate getJpa() {
        getLog().debug( "invoking getJpa, returning: {}", jpa);
        return jpa;
    }

    /**
     * @param jpa Injection of JpaTemplate
     */
    @Override
    @Reference(target = "(osgi.unit.name=hyperiot-hProject-persistence-unit)")
    protected void setJpa(JpaTemplate jpa) {
        getLog().debug( "invoking setJpa, setting: {}", jpa);
        this.jpa = jpa;
    }

    public Collection<AreaDevice> getAreaDevicesDeepList(long areaId){
        return  jpa.txExpr(entityNamanger -> {
            return entityNamanger.createNativeQuery(
                    "WITH RECURSIVE cte_area AS (\n" +
                            "    SELECT\n" +
                            "        id\n" +
                            "    FROM\n" +
                            "        area\n" +
                            "    WHERE area.id = :areaId \n" +
                            "    UNION ALL\n" +
                            "    SELECT\n" +
                            "        e.id\n" +
                            "    FROM\n" +
                            "        area e\n" +
                            "        INNER JOIN cte_area o\n" +
                            "            ON o.id = e.parentarea_id\n" +
                            ")\n" +
                            "SELECT\n *\n" +
                            "FROM\n" +
                            "    areadevice\n" +
                            "INNER JOIN cte_area ON areadevice.area_id = cte_area.id;", AreaDevice.class
            ).setParameter("areaId",areaId).getResultList();
        });
    }

    @Override
    public List<Area> getRootProjectArea(long projectId) {
        return jpa.txExpr(TransactionType.Required,entityManager -> {
            return entityManager.createNativeQuery(
                    "SELECT * FROM area a \n" +
                            "WHERE a.project_id = :projectId \n" +
                            "and a.parentarea_id is NULL",Area.class
            ).setParameter("projectId",projectId).getResultList();
        });
    }

    @Override
    public Collection<Area> getAreaListByProjectId(long projectId) {
        return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
            String query = "Select a FROM Area a WHERE a.project.id=:projectId AND (a.parentArea.id = 0 OR a.parentArea.id = NULL)";
            return entityManager.createQuery(query).setParameter("projectId",projectId).getResultList();
        });
    }

    @Override
    public Collection<Area> getInnerArea(long areaId) {
        return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
            String query = "Select a from Area a where a.parentArea.id = :areaId";
            return entityManager.createQuery(query).setParameter("areaId",areaId).getResultList();
        });
    }

}
