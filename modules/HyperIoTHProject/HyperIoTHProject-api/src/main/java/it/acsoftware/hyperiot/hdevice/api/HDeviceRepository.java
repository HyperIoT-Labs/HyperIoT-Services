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

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseRepository;
import it.acsoftware.hyperiot.hdevice.model.HDevice;

import java.util.Collection;

/**
 * 
 * @author Aristide Cittadino Interface component for HDevice Repository.
 *         It is used for CRUD operations,
 *         and to interact with the persistence layer.
 *
 */
public interface HDeviceRepository extends HyperIoTBaseRepository<HDevice> {
	public HDevice findHDeviceAdmin();

	HDevice findDeviceByScreenName(String deviceName);

	Collection<HDevice> getProjectDevicesList(long projectId);

	/**
	 * @param device HDevice instance on which perform action
	 * @param newPassword newPassword
	 * @param passwordConfirm passwordConfirm
	 * @return HDevice instance
	 */
	HDevice changePassword(HDevice device, String newPassword, String passwordConfirm);
}
