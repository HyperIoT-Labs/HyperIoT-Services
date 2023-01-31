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

import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAuthenticationProvider;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedResource;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnershipResourceService;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTAuthenticable;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.base.security.annotations.AllowGenericPermissions;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTOwnedChildBaseEntityServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.api.HDeviceApi;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.mail.api.MailSystemApi;
import it.acsoftware.hyperiot.mail.util.MailConstants;
import it.acsoftware.hyperiot.mail.util.MailUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.*;

import static it.acsoftware.hyperiot.base.util.HyperIoTConstants.OSGI_AUTH_PROVIDER_RESOURCE;

/**
 * @author Aristide Cittadino Implementation class of HDeviceApi interface. It
 * is used to implement all additional methods in order to interact with
 * the system layer.
 */
@Component(service = {HDeviceApi.class, HyperIoTAuthenticationProvider.class}, immediate = true, property = {
        OSGI_AUTH_PROVIDER_RESOURCE + "=it.acsoftware.hyperiot.hdevice.model.HDevice"
})
public final class HDeviceServiceImpl extends HyperIoTOwnedChildBaseEntityServiceImpl<HDevice> implements HDeviceApi, HyperIoTAuthenticationProvider, HyperIoTOwnershipResourceService {
    /**
     * Injecting the HDeviceSystemApi
     */
    private HDeviceSystemApi systemService;

    /**
     * Injecting HProject System Api
     */
    private HProjectSystemApi projectSystemApi;

    private HUserSystemApi hUserSystemApi;

    private MailSystemApi mailSystemApi;

    /**
     * Constructor for a HDeviceServiceImpl
     */
    public HDeviceServiceImpl() {
        super(HDevice.class);
    }

    /**
     * @return The current HDeviceSystemApi
     */
    protected HDeviceSystemApi getSystemService() {
        getLog().debug("invoking getSystemService, returning: {}" , this.systemService);
        return systemService;
    }

    /**
     * @param hDeviceSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setSystemService(HDeviceSystemApi hDeviceSystemService) {
        getLog().debug("invoking setSystemService, setting: {}" , systemService);
        this.systemService = hDeviceSystemService;
    }

    /**
     * @param projectSystemApi
     */
    @Reference
    public void setProjectSystemApi(HProjectSystemApi projectSystemApi) {
        this.projectSystemApi = projectSystemApi;
    }

    /**
     * @param hUserSystemApi
     */
    @Reference
    public void sethUserSystemApi(HUserSystemApi hUserSystemApi) {
        getLog().debug("invoking sethUserSystemApi, setting: {}" , hUserSystemApi);
        this.hUserSystemApi = hUserSystemApi;
    }

    /**
     * @param mailSystemApi
     */
    @Reference
    public void setMailSystemApi(MailSystemApi mailSystemApi) {
        getLog().debug("invoking setMailSystemApi, setting: {}" , mailSystemApi);
        this.mailSystemApi = mailSystemApi;
    }

    @Override
    @AllowPermissions(actions = HyperIoTHProjectAction.Names.DEVICE_LIST, checkById = true, idParamIndex = 1 , systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public Collection<HDevice> getProjectDevicesList(HyperIoTContext context, long projectId) {
        //TO DO: move DEVICE_LIST action to project entity
        return systemService.getProjectDevicesList(projectId);
    }

    /**
     * @param context         The HyperIoTContext instance
     * @param deviceId        The device id
     * @param oldPassword     The old password
     * @param newPassword
     * @param passwordConfirm The new password for verification check
     * @return
     */
    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.UPDATE, checkById = true, idParamIndex = 1)
    public HDevice changePassword(HyperIoTContext context, long deviceId, String oldPassword, String newPassword,
                                  String passwordConfirm) {
        HDevice device;
        try {
            device = this.systemService.find(deviceId, context);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        if (oldPassword != null && newPassword != null && passwordConfirm != null) {
            if ( HyperIoTUtil.passwordMatches(oldPassword,device.getPassword())) {
                return this.systemService.changePassword(device, newPassword, passwordConfirm);
            } else {
                throw new HyperIoTRuntimeException("it.acsoftware.hyperiot.error.password.not.match");
            }
        } else {
            throw new HyperIoTRuntimeException("it.acsoftware.hyperiot.error.password.not.null");
        }

    }

    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.UPDATE, checkById = true, idParamIndex = 1)
    public void requestDevicePasswordReset(HyperIoTContext context, long deviceId) {
        HUser u ;
        HDevice device ;
        try {
            u = this.hUserSystemApi.find(context.getLoggedEntityId(), null);
            device = this.systemService.find(deviceId, context);
        } catch (NoResultException e ){
            throw new HyperIoTEntityNotFound();
        }
        if (! HyperIoTSecurityUtil.checkUserOwnsResource(context, u, device)){
            throw new HyperIoTUnauthorizedException();
        }
        String pwdResetCode = UUID.randomUUID().toString();
        device = this.systemService.changePasswordResetCode(device, pwdResetCode);
        List<String> recipients = new ArrayList<>();
        recipients.add(u.getEmail());
        HashMap<String, Object> params = new HashMap<>();
        params.put("username", u.getUsername());
        params.put("deviceUsername", device.getDeviceName());
        params.put("resetPwdCode", pwdResetCode);
        try {
            String mailBody = this.mailSystemApi.generateTextFromTemplate(MailConstants.MAIL_TEMPLATE_DEVICE_PWD_RESET, params);
            this.mailSystemApi.sendMail(MailUtil.getUsername(), recipients, null, null, "Reset Password Device", mailBody, null);
        } catch (Exception e) {
                getLog().error(e.getMessage(), e);
        }
    }

    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.UPDATE, checkById = true, idParamIndex = 1)
    public void resetHDevicePassword(HyperIoTContext context, long deviceId, String resetCode, String password, String passwordConfirm) {
        HDevice hDevice ;
        HUser u ;
        try{
            u = this.hUserSystemApi.find(context.getLoggedEntityId(), null);
            hDevice = this.systemService.find(deviceId, null);
        } catch (NoResultException e){
            throw new HyperIoTEntityNotFound();
        }
        if (! HyperIoTSecurityUtil.checkUserOwnsResource(context, u, hDevice)) {
            throw new HyperIoTUnauthorizedException();
        }
        if ( hDevice.getPasswordResetCode() == null || ! HyperIoTUtil.passwordMatches(resetCode, hDevice.getPasswordResetCode())) {
            throw new HyperIoTValidationException(new HashSet<>());
        }
        this.systemService.changePassword(hDevice, password, passwordConfirm);
    }

    /**
     * @param username Username which must be found
     * @return
     */
    @Override
    public HyperIoTAuthenticable findByUsername(String username) {
        return this.systemService.findByDeviceName(username);
    }

    /**
     * @param username Username
     * @param password Password
     * @return
     */
    @Override
    public HyperIoTAuthenticable login(String username, String password) {
        return this.systemService.login(username, password);
    }

    /**
     * @return Valid issuers for this class
     */
    @Override
    public String[] validIssuers() {
        return new String[]{HDevice.class.getName()};
    }

    /**
     * @param hyperIoTAuthenticable
     * @return
     */
    @Override
    public boolean screeNameAlreadyExists(HyperIoTAuthenticable hyperIoTAuthenticable) {
        try {
            HDevice device = this.systemService.findDeviceByScreenName(hyperIoTAuthenticable.getScreenName());
            //if the authenticable is the current device is ok
            if (hyperIoTAuthenticable instanceof HDevice) {
                HDevice hdeviceAuthenticable = (HDevice) hyperIoTAuthenticable;
                return !hdeviceAuthenticable.equals(device);
            }
            return device != null;
        } catch (NoResultException e) {
            getLog().debug("No devices with device name: {}" ,
                    hyperIoTAuthenticable.getScreenName());
        }
        return false;
    }

    @Override
    public String getOwnerFieldPath() {
        return "project.user.id";
    }

    @Override
    protected String getRootParentFieldPath() {
        return "project.id";
    }

    @Override
    protected Class<? extends HyperIoTOwnedResource> getParentResourceClass() {
        return HProject.class;
    }


}
