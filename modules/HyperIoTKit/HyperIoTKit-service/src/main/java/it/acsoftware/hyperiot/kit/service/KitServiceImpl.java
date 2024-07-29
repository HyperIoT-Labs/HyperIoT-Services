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

package it.acsoftware.hyperiot.kit.service;

import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.api.HProjectApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.kit.api.KitApi;
import it.acsoftware.hyperiot.kit.api.KitSystemApi;
import it.acsoftware.hyperiot.kit.model.Kit;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntityApi;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 
 * @author Aristide Cittadino Implementation class of KitApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = KitApi.class, immediate = true)
public final class KitServiceImpl extends HyperIoTBaseEntityServiceImpl<Kit>  implements KitApi {
	/**
	 * Injecting the KitSystemApi
	 */
	private KitSystemApi systemService;

	private HProjectApi hProjectApi;

	private SharedEntitySystemApi sharedEntitySystemApi;
	
	/**
	 * Constructor for a KitServiceImpl
	 */
	public KitServiceImpl() {
		super(Kit.class);
	}
	
	/**
	 * 
	 * @return The current KitSystemApi
	 */
	protected KitSystemApi getSystemService() {
		getLog().debug("invoking getSystemService, returning: {}" , this.systemService);
		return systemService;
	}

	/**
	 * 
	 * @param kitSystemService Injecting via OSGi DS current systemService 
	 */
	@Reference
	protected void setSystemService(KitSystemApi kitSystemService) {
		getLog().debug("invoking setSystemService, setting: {}" , systemService);
		this.systemService = kitSystemService ;
	}

	/**
	 *
	 * @return The current HProjectApi
	 */
	protected HProjectApi getHProjectApi() {
		getLog().debug("invoking getHProjectApi, returning: {}" , this.hProjectApi);
		return hProjectApi;
	}

	/**
	 *
	 * @param hProjectApi Injecting via OSGi DS current systemService
	 */
	@Reference
	protected void sethProjectApi(HProjectApi hProjectApi) {
		getLog().debug("invoking sethProjectApi, setting: {}" , hProjectApi);
		this.hProjectApi = hProjectApi ;
	}

	/**
	 *
	 * @return The current SharedEntitySystemApi
	 */
	protected SharedEntitySystemApi getSharedEntitySystemApi() {
		getLog().debug("invoking getSharedEntitySystemApi, returning: {}" , this.sharedEntitySystemApi);
		return sharedEntitySystemApi;
	}

	/**
	 *
	 * @param sharedEntitySystemApi Injecting via OSGi DS current systemService
	 */
	@Reference
	protected void setSharedEntitySystemApi(SharedEntitySystemApi sharedEntitySystemApi) {
		getLog().debug("invoking setSharedEntitySystemApi, setting: {}" , sharedEntitySystemApi);
		this.sharedEntitySystemApi = sharedEntitySystemApi ;
	}



	@Override
	public Kit find(long id, HyperIoTContext ctx) {
		HyperIoTQuery queryFilter = HyperIoTQueryBuilder.newQuery().equals("id", id);
		Kit kit= systemService.find(queryFilter, ctx);
		KitPermissionUtils.checkUserCanFindKit(this.hProjectApi,this.sharedEntitySystemApi,kit,ctx);
		return kit;
	}

	@Override
	public Collection<Kit> findAll(HyperIoTQuery filter, HyperIoTContext ctx) {
		filter= (filter == null) ? KitPermissionUtils.getQueryForFindOnlyPermittedKit(this.hProjectApi,ctx) : filter.or(KitPermissionUtils.getQueryForFindOnlyPermittedKit(this.hProjectApi,ctx));
		return systemService.findAll(filter, ctx);
	}

	@Override
	public HyperIoTPaginableResult<Kit> findAll(HyperIoTQuery filter, HyperIoTContext ctx, int delta, int page) {
		filter= (filter == null) ? KitPermissionUtils.getQueryForFindOnlyPermittedKit(this.hProjectApi,ctx) : filter.or(KitPermissionUtils.getQueryForFindOnlyPermittedKit(this.hProjectApi,ctx));
		return systemService.findAll(filter, ctx, delta, page);
	}

	@Override
	public Kit save(Kit entity, HyperIoTContext ctx) {
		KitPermissionUtils.checkUserHasPermissionOnKit(this.hProjectApi,entity,ctx);
		return systemService.save(entity, ctx);
	}

	@Override
	public Kit update(Kit entity, HyperIoTContext ctx) {
		Kit kit = null;
		try{
			kit = this.find(entity.getId(),ctx);
		}catch (NoResultException exc){
			throw new HyperIoTEntityNotFound();
		}
		entity.setProjectId(kit.getProjectId());
		entity.setDevices(kit.getDevices());
		KitPermissionUtils.checkUserHasPermissionOnKit(this.hProjectApi,entity,ctx);
		return systemService.update(entity, ctx);
	}

	@Override
	public void remove(long id, HyperIoTContext ctx) {
		Kit kit = this.find(id,ctx);
		KitPermissionUtils.checkUserHasPermissionOnKit(this.hProjectApi,kit,ctx);
		systemService.remove(id, ctx);
	}

	@Override
	public Collection<AssetTag> getKitTags(long kitId, HyperIoTContext ctx) {
		//Calling this find method of KitSystemApi, we implicitily check if the user has permission to find the  kit.
		this.find(kitId,ctx);
		return systemService.getKitTags(kitId,ctx);
	}

	@Override
	public AssetTag addTagToKit(long kitId, AssetTag tag, HyperIoTContext ctx) {
		Kit kit = this.find(kitId,ctx);
		KitPermissionUtils.checkUserHasPermissionOnKit(this.hProjectApi,kit,ctx);
		return systemService.addTagToKit(kitId,tag,ctx);
	}

	@Override
	public void deleteTagFromKit(long kitId, long tagId, HyperIoTContext ctx) {
		Kit kit = this.find(kitId,ctx);
		KitPermissionUtils.checkUserHasPermissionOnKit(this.hProjectApi,kit,ctx);
		systemService.deleteTagFromKit(kitId,tagId,ctx);
	}

	@Override
	@AllowPermissions(actions = HyperIoTHProjectAction.Names.DEVICE_LIST, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
	public HDevice installHDeviceTemplateOnProject(HyperIoTContext ctx, long hProjectId, long kitId, String deviceName, long hdeviceTemplateId){
		return systemService.installHDeviceTemplateOnProject(ctx, hProjectId, kitId, deviceName, hdeviceTemplateId);
	}
}
