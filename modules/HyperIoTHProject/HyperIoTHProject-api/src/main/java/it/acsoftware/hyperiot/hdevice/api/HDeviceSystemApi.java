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

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntitySystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;

import java.util.Collection;

/**
 * 
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 *         interface defines methods for additional operations.
 *
 */
public interface HDeviceSystemApi extends HyperIoTBaseEntitySystemApi<HDevice> {

    void removeByHProjectId(long hProjectId);

    /**
     * Performs a device authentication login
     *
     * @param deviceName The device name
     * @param password The device password
     * @return The device instance
     */
    HDevice login(String deviceName,String password);

    /**
     * Performs a device authentication login
     *
     * @param deviceName The device name
     * @return The device instance
     */
    HDevice findByDeviceName(String deviceName);


    /**
     * Gets the list of devices defined for the project with id `projectId`
     *
     * @param projectId The project id
     * @return List of devices
     */
    Collection<HDevice> getProjectDevicesList(long projectId);

    /**
     * Changes the password of the device with id `deviceId`
     *
     * @param device The device instance
     * @param newPassword The new password
     * @param passwordConfirm The new password for verification check
     * @return The device instance
     */
    HDevice changePassword(HDevice device, String newPassword, String passwordConfirm);

    /**
     * Change the passwordResetCode of the HDevice device
     * @param device the device instance
     * @param passwordResetCode the new password request code
     * @return The device instance
     */
    HDevice changePasswordResetCode(HDevice device, String passwordResetCode);

    HDevice findDeviceByScreenName(String deviceName);

}