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

package it.acsoftware.hyperiot.hdevice.api;

import it.acsoftware.hyperiot.base.api.HyperIoTAuthenticationProvider;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;

import java.util.Collection;

/**
 * 
 * @author Aristide Cittadino Interface component for HDeviceApi. This interface
 *         defines methods for additional operations.
 *
 */
public interface HDeviceApi extends HyperIoTBaseEntityApi<HDevice>, HyperIoTAuthenticationProvider {
    /**
     * Gets the list of devices defined for the project with id `projectId`
     *
     * @param context The HyperIoTContext instance
     * @param projectId The project id
     * @return List of devices
     */
    Collection<HDevice> getProjectDevicesList(HyperIoTContext context, long projectId);

    /**
     * Changes the password of the device with id `deviceId`
     *
     * @param context The HyperIoTContext instance
     * @param deviceId The device id
     * @param oldPassword The old password
     * @param newPassowrod The new password
     * @param passwordConfirm The new password for verification check
     * @return The device
     */
    HDevice changePassword(HyperIoTContext context,long deviceId,String oldPassword,String newPassowrod,String passwordConfirm);

    /**
     * Request reset of hdevice password
     *
     * @param context The HyperIoTContext instance bounded to request
     * @param deviceId id of the hdevice on which perform operation
     */
    void requestDevicePasswordReset(HyperIoTContext context, long deviceId);

    /** Reset HDevicePassoword
     *
     * @param context The HyperIoTContext instance bounded to request
     * @param deviceId id of the hdevice on which perform operation
     * @param resetCode reset code
     * @param password new hdevice password
     * @param passwordConfirm password confirm
     */
    void resetHDevicePassword(HyperIoTContext context, long deviceId, String resetCode, String password, String passwordConfirm);
}