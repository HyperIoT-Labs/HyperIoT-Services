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

package it.acsoftware.hyperiot.hdevice.repository;

import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.api.HDeviceRepository;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;

/**
 * @author Aristide Cittadino Implementation class of the HDevice. This
 * class is used to interact with the persistence layer.
 */
@Component(service = HDeviceRepository.class, immediate = true)
public class HDeviceRepositoryImpl extends HyperIoTBaseRepositoryImpl<HDevice> implements HDeviceRepository {
    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;

    /**
     * Constructor for a HDeviceRepositoryImpl
     */
    public HDeviceRepositoryImpl() {
        super(HDevice.class);
    }

    /**
     * @return The current jpaTemplate
     */
    @Override
    protected JpaTemplate getJpa() {
        getLog().debug("invoking getJpa, returning: {}" , jpa);
        return jpa;
    }

    /**
     * @param jpa Injection of JpaTemplate
     */
    @Override
    @Reference(target = "(osgi.unit.name=hyperiot-hProject-persistence-unit)")
    protected void setJpa(JpaTemplate jpa) {
        getLog().debug("invoking setJpa, setting: {}" , jpa);
        this.jpa = jpa;
    }

    /**
     * Find a user with admin role via query
     *
     * @return the user with admin role
     */
    public HDevice findHDeviceAdmin() {
        getLog().debug("Invoking findHDeviceAdmin ");
        return this.getJpa().txExpr(TransactionType.Required, entityManager -> {
            getLog().debug("Transaction found, invoke persist");
            HDevice entity = null;
            try {
                entity = entityManager.createQuery("from HDevice h where h.deviceName = 'hdeviceadmin'", HDevice.class)
                        .getSingleResult();
                getLog().debug("Entity persisted: {}", entity);
            } catch (NoResultException e) {
                getLog().debug("Entity NOT FOUND ");
            }
            return entity;
        });
    }

    @Override
    public HDevice findDeviceByScreenName(String deviceName) {
        return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
            String query = "Select d from HDevice d where lower(d.deviceName) = lower(:deviceName)";
            return (HDevice) entityManager.createQuery(query).setParameter("deviceName",deviceName).getSingleResult();
        });
    }

    @Override
    public Collection<HDevice> getProjectDevicesList(long projectId) {
        return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
            String query = "Select dv FROM HDevice dv WHERE dv.project.id=:projectId";
            return  entityManager.createQuery(query).setParameter("projectId",projectId).getResultList();
        });
    }

    @Override
    public HDevice changePassword(HDevice device, String newPassword, String passwordConfirm) {
        String newPasswordHashed = HyperIoTUtil.getPasswordHash(newPassword);
        device.setPassword(newPasswordHashed);
        device.setPasswordConfirm(newPasswordHashed);
        //forcing password reset code to be empty on each pwd change
        device.setPasswordResetCode(null);
        super.update(device);
        return device;
    }

    /**
     * Forcing Password Hash
     */
    @Override
    public HDevice save(HDevice entity) {
        // forcing hash password on new users
        String password = entity.getPassword();
        String passwordHashed = HyperIoTUtil.getPasswordHash(password);
        entity.setPassword(passwordHashed);
        entity.setPasswordConfirm(passwordHashed);
        //forcing password reset code to be null when user save device
        entity.setPasswordResetCode(null);
        return super.save(entity);
    }
}
