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

package it.acsoftware.hyperiot.hdevice.service;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.actions.HyperIoTHDeviceAction;
import it.acsoftware.hyperiot.hdevice.api.HDeviceRepository;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hproject.api.HProjectRepository;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import javax.security.auth.x500.X500PrivateCredential;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;

/**
 * @author Aristide Cittadino Implementation class of the HDeviceSystemApi
 * interface. This class is used to implements all additional methods to
 * interact with the persistence layer.
 */
@Component(service = HDeviceSystemApi.class, immediate = true)
public final class HDeviceSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<HDevice>
        implements HDeviceSystemApi {

    /**
     * Injecting the HDeviceRepository to interact with persistence layer
     */
    private HDeviceRepository repository;

    /**
     *
     */
    private HProjectRepository hProjectRepository;

    /**
     * Injecting PermissionSystemApi
     */
    private PermissionSystemApi permissionSystemApi;

    /**
     * Constructor for a HDeviceSystemServiceImpl
     */
    public HDeviceSystemServiceImpl() {
        super(HDevice.class);
    }

    /**
     * Return the current repository
     */
    protected HDeviceRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}" , this.repository);
        return repository;
    }

    /**
     * @param hDeviceRepository The current value of HDeviceRepository to interact
     *                          with persistence layer
     */
    @Reference
    protected void setRepository(HDeviceRepository hDeviceRepository) {
        getLog().debug("invoking setRepository, setting: {}" , hDeviceRepository);
        this.repository = hDeviceRepository;
    }

    /**
     *
     * @param hProjectRepository
     */
    @Reference
    public void sethProjectRepository(HProjectRepository hProjectRepository) {
        this.hProjectRepository = hProjectRepository;
    }

    /**
     * @param permissionSystem Injecting via OSGi DS current PermissionSystemService
     */
    @Reference
    public void setPermissionSystem(PermissionSystemApi permissionSystem) {
        this.permissionSystemApi = permissionSystem;
    }

    @Override
    public void removeByHProjectId(long hProjectId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("hProjectId", hProjectId);
        repository.executeUpdateQuery("delete from HDevice hdevice where hdevice.project.id = :hProjectId", params);
    }

    @Override
    public HDevice login(String deviceName, String password) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("deviceName", deviceName);
        try {
            HDevice device = repository.findDeviceByScreenName(deviceName);
            if (HyperIoTUtil.passwordMatches(password, device.getPassword()))
                return device;
            return null;
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public HDevice findByDeviceName(String deviceName) {
        HyperIoTQuery query = HyperIoTQueryBuilder.newQuery().equals("deviceName",deviceName);
        return repository.find(query,null);
    }


    @Override
    public Collection<HDevice> getProjectDevicesList(long projectId) {
        try {
            hProjectRepository.find(projectId, null);
        } catch (NoResultException e){
            getLog().debug("Entity not found! ");
            throw new HyperIoTEntityNotFound();
        }
        return repository.getProjectDevicesList(projectId);
    }

    /**
     * Method for changing password
     */
    @Override
    public HDevice changePassword(HDevice device, String newPassword, String passwordConfirm) {
        if (device == null)
            throw new HyperIoTEntityNotFound();
        if(newPassword != null && passwordConfirm != null) {
            device.setPassword(newPassword);
            device.setPasswordConfirm(passwordConfirm);
            //Perform device validation on raw password.
            this.validate(device);
            //In repository encrypt password and persist entity.
            return this.repository.changePassword(device, newPassword, passwordConfirm);
        }
        throw new HyperIoTValidationException(new HashSet<>());
    }

    @Override
    public HDevice changePasswordResetCode(HDevice device, String passwordResetCode ){
        if(device == null )
            throw new HyperIoTEntityNotFound();
        String passwordResetCodeHashed = HyperIoTUtil.getPasswordHash(passwordResetCode);
        device.setPasswordResetCode(passwordResetCodeHashed);
        return super.update(device, null);
    }

    @Override
    public HDevice findDeviceByScreenName(String deviceName) {
        return repository.findDeviceByScreenName(deviceName);
    }

    @Override
    public HDevice save(HDevice d, HyperIoTContext ctx) {
        if (d.isLoginWithSSLCert()) {
            try {
                //Generating key pair and storing only pubKey
                KeyPair keyPair = HyperIoTSecurityUtil.generateSSLKeyPairValue(2048);
                //TO DO: substitue servert ca cert with mqtt server cert, now both certs are the same
                X500PrivateCredential credentials = HyperIoTSecurityUtil.createServerClientX509Cert(HDevice.class.getName(), d.getDeviceName(), 365, keyPair, HyperIoTSecurityUtil.getServerRootCert());
                //saved locally
                PublicKey publicKey = keyPair.getPublic();
                d.setPubKey(publicKey.getEncoded());
                //returned to the client and not saved inside database
                d.setX509CertKey(Base64.getEncoder().encodeToString(credentials.getPrivateKey().getEncoded()));
                d.setX509Cert(Base64.getEncoder().encodeToString(credentials.getCertificate().getEncoded()));
            } catch (Exception e) {
                getLog().error(e.getMessage(), e);
            }
        }
        return super.save(d, ctx);
    }

    /**
     * Password cannot be changed by this method
     */
    @Override
    public HDevice update(HDevice d, HyperIoTContext ctx) {
        HDevice dbDevice = this.find(d.getId(), ctx);
        if (dbDevice == null)
            throw new HyperIoTEntityNotFound();
        d.setPassword(dbDevice.getPassword());
        d.setPasswordConfirm(null);
        d.setPasswordResetCode(dbDevice.getPasswordResetCode());
        return super.update(d, ctx);
    }


    /**
     * On Bundle activated
     */
    @Activate
    public void onActivate() {
        this.addDeviceSystemUser();
        this.checkRegisteredUserRoleExists();
    }

    /**
     * Register permissions for new users
     */
    private void checkRegisteredUserRoleExists() {
        String resourceName = HDevice.class.getName();
        List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTCrudActions(resourceName);
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTHDeviceAction.PACKETS_MANAGEMENT));
        this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
    }

    /**
     *
     */
    private void addDeviceSystemUser() {
        HDevice hdeviceAdmin = repository.findHDeviceAdmin();
        if (hdeviceAdmin == null) {
            hdeviceAdmin = new HDevice();
            // hdeviceAdmin.setAdmin(true);
            hdeviceAdmin.setBrand("");
            hdeviceAdmin.setDescription("");
            hdeviceAdmin.setAdmin(true);
            hdeviceAdmin.setDeviceName("hdeviceadmin");
            hdeviceAdmin.setFirmwareVersion("");
            hdeviceAdmin.setModel("");
            String password = "admin";
            hdeviceAdmin.setPassword(password);
            hdeviceAdmin.setPasswordConfirm(password);
            hdeviceAdmin.setSoftwareVersion("");
            repository.save(hdeviceAdmin);
        }
    }


}
