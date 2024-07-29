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

import it.acsoftware.hyperiot.asset.tag.api.AssetTagSystemApi;
import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.asset.tag.model.AssetTagResource;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.model.HyperIoTAssetOwnerImpl;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.kit.api.KitRepository;
import it.acsoftware.hyperiot.kit.api.KitSystemApi;
import it.acsoftware.hyperiot.kit.model.Kit;
import it.acsoftware.hyperiot.kit.template.api.HDeviceTemplateSystemApi;
import it.acsoftware.hyperiot.kit.template.model.HDeviceTemplate;
import it.acsoftware.hyperiot.kit.template.model.HPacketFieldTemplate;
import it.acsoftware.hyperiot.kit.template.model.HPacketTemplate;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 
 * @author Aristide Cittadino Implementation class of the KitSystemApi
 *         interface. This  class is used to implements all additional
 *         methods to interact with the persistence layer.
 */
@Component(service = KitSystemApi.class, immediate = true)
public final class KitSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<Kit>   implements KitSystemApi {
	
	/**
	 * Injecting the KitRepository to interact with persistence layer
	 */
	private KitRepository repository;

	/**
	 * Injecting the PermissionSystemApi to interact with persistence layer
	 */
	private PermissionSystemApi permissionSystemApi;

	/**
	 * Injecting the AssetTagSystemApi to interact with persistence layer
	 */
	private AssetTagSystemApi assetTagSystemApi;

	/**
	 * Injecting the HDeviceSystemApi to interact with persistence layer
	 */
	private HDeviceSystemApi hDeviceSystemApi;

	/**
	 * Injecting the HProjectSystemApi to interact with persistence layer
	 */
	private HProjectSystemApi hProjectSystemApi;

	/**
	 * Constructor for a KitSystemServiceImpl
	 */
	public KitSystemServiceImpl() {
		super(Kit.class);
	}

	/**
	 * Return the current repository
	 */
	protected KitRepository getRepository() {
		getLog().debug("invoking getRepository, returning: {}" , this.repository);
		return repository;
	}
	
	/**
	 * @param kitRepository The current value of KitRepository to interact with persistence layer
	 */
	@Reference
	protected void setRepository(KitRepository kitRepository) {
		getLog().debug("invoking setRepository, setting: {}" , kitRepository);
		this.repository = kitRepository;
	}

	/**
	 * Return the current permissionSystemApi
	 */
	protected PermissionSystemApi getPermissionSystemApi() {
		getLog().debug("invoking getPermissionSystemApi, returning: {}" , this.permissionSystemApi);
		return permissionSystemApi;
	}

	/**
	 * @param permissionSystemApi The current value of PermissionSystemApi to interact with permissionSystem
	 */
	@Reference
	protected void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
		getLog().debug("invoking setPermissionSystemApi, setting: {}" , permissionSystemApi);
		this.permissionSystemApi = permissionSystemApi;
	}

	/**
	 * Return the current AssetTagSystemApi
	 */
	protected AssetTagSystemApi getAssetTagSystemApi() {
		getLog().debug("invoking getAssetTagSystemApi, returning: {}" , this.assetTagSystemApi);
		return assetTagSystemApi;
	}

	/**
	 * @param assetTagSystemApi The current value of AssetTagSystemApi to interact with assetTagSystem
	 */
	@Reference
	protected void setAssetTagSystemApi(AssetTagSystemApi assetTagSystemApi) {
		getLog().debug("invoking setAssetTagSystemApi, setting: {}" , assetTagSystemApi);
		this.assetTagSystemApi = assetTagSystemApi;
	}

	/**
	 * Return the current HProjectSystemApi
	 */
	protected HProjectSystemApi gethProjectSystemApi() {
		getLog().debug("invoking gethProjectSystemApi, returning: {}" , this.hProjectSystemApi);
		return hProjectSystemApi;
	}

	/**
	 * @param hProjectSystemApi The current value of HProjectSystemApi
	 */
	@Reference
	protected void sethProjectSystemApi(HProjectSystemApi hProjectSystemApi) {
		getLog().debug("invoking sethProjectSystemApi, setting: {}" , hProjectSystemApi);
		this.hProjectSystemApi = hProjectSystemApi;
	}

	/**
	 * Return the current HDeviceSystemApi
	 */
	protected HDeviceSystemApi gethDeviceSystemApi() {
		getLog().debug("invoking gethDeviceSystemApi, returning: {}" , this.hDeviceSystemApi);
		return hDeviceSystemApi;
	}

	/**
	 * @param hDeviceSystemApi The current value of HDeviceSystemApi
	 */
	@Reference
	protected void sethDeviceSystemApi(HDeviceSystemApi hDeviceSystemApi) {
		getLog().debug("invoking sethDeviceSystemApi, setting: {}" , hDeviceSystemApi);
		this.hDeviceSystemApi = hDeviceSystemApi;
	}

	@Override
	public Kit save(Kit entity, HyperIoTContext ctx) {
		if(entity.getDevices() != null && ! entity.getDevices().isEmpty()){
			for(HDeviceTemplate deviceTemplate : entity.getDevices()) {
				deviceTemplate.setKit(entity);
				if(deviceTemplate.getPackets() != null && ! deviceTemplate.getPackets().isEmpty()) {
					for (HPacketTemplate packetTemplate : deviceTemplate.getPackets()) {
						packetTemplate.setDevice(deviceTemplate);
						if (packetTemplate.getFields() != null && !packetTemplate.getFields().isEmpty()) {
							setInnerPacketFieldStructure(packetTemplate);
						}
					}
				}
			}
		}else{
			//If kit's DeviceTemplate list is empty, throw an Exception.
			//todo add a CustomExceptionClass rather to use RuntimeException
			throw new RuntimeException();
		}
		/*
		Like HProjectImport we need to make validation on all subentity
		 */
		validateWithFrameworkValidation(entity);
		/*
		This method is similar on how we work to implement HProject's import.
		Like HProject import we need to set reference between relationship , such that hibernate can
		make cascade persist operation in the right way.
		 */
		return super.save(entity, ctx);
	}

	@Override
	public long[] getKitCategories(long kitId) {
		return this.repository.getKitCategories(kitId);
	}

	@Override
	public Collection<AssetTag> getKitTags(long kitId, HyperIoTContext ctx) {
		Collection<AssetTagResource> assetTagResources = assetTagSystemApi.getAssetTagResourceList(Kit.class.getName(),kitId);
		Set<AssetTag> kitTags = new HashSet<>();
		for(AssetTagResource resource : assetTagResources){
			if(resource.getTag() != null){
				kitTags.add(resource.getTag());
			}
		}
		return kitTags;
	}

	@Override
	public AssetTag addTagToKit(long kitId, AssetTag tag, HyperIoTContext ctx) {
		return this.executeTransactionWithReturn(TransactionType.Required, entityManager -> {
			try {
				repository.find(kitId, null);
			}catch (NoResultException exc){
				throw new HyperIoTEntityNotFound();
			}
			HyperIoTAssetOwnerImpl assetOwner = new HyperIoTAssetOwnerImpl();
			assetOwner.setOwnerResourceId(kitId);
			assetOwner.setOwnerResourceName(Kit.class.getName());
			assetOwner.setUserId(ctx.getLoggedEntityId());
			tag.setOwner(assetOwner);
			AssetTagResource resource= new AssetTagResource();
			resource.setTag(tag);
			resource.setResourceId(kitId);
			resource.setResourceName(Kit.class.getName());
			tag.getResources().add(resource);
			return  assetTagSystemApi.save(tag,ctx);
		});
	}

	@Override
	public void deleteTagFromKit(long kitId, long tagId, HyperIoTContext ctx) {
		try{
			repository.find(kitId,null);
		}catch (NoResultException exc){
			throw new HyperIoTEntityNotFound();
		}
		try{
			assetTagSystemApi.find(tagId, ctx);
		}catch (NoResultException exc){
			throw new HyperIoTEntityNotFound();
		}
		this.executeTransaction(TransactionType.Required,entityManager -> {
			assetTagSystemApi.remove(tagId,ctx);
		});
	}

	@Override
	public HDevice installHDeviceTemplateOnProject(HyperIoTContext ctx, long hProjectId, long kitId, String deviceName,  long hdeviceTemplateId){
		HProject project ;
		HDeviceTemplate deviceTemplate;
		try{
			project = this.hProjectSystemApi.find(hProjectId, ctx);
			Kit kit = this.repository.find(kitId, null);
			//We need to load device template in this way to avoid entity duplication when there are inner field inside HPacket's template.
			//So we load the device template through the eager relation between Kit and HDeviceTemplate entity.
			deviceTemplate = getHDeviceTemplateFromKit(kit, hdeviceTemplateId);
			if(deviceTemplate == null ){
				throw new NoResultException();
			}
		} catch (NoResultException exception){
			throw new HyperIoTEntityNotFound();
		}
		HDevice device = configureHDeviceFromHDeviceTemplate(deviceTemplate,DEFAULT_DEVICE_PASSWORD,DEFAULT_DEVICE_PASSWORD, deviceName, project);
		return this.hDeviceSystemApi.save(device, ctx);
	}

	@Activate
	public void onActivate(){
		this.checkRegisteredUserRoleExists();
	}

	/**
	 * Register permissions for new users
	 */
	private void checkRegisteredUserRoleExists() {
		String kitResourceName = Kit.class.getName();
		List<HyperIoTAction> kitCrudAction = HyperIoTActionsUtil.getHyperIoTCrudActions(kitResourceName);
		this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, kitCrudAction);
	}

	private void setInnerPacketFieldStructure(HPacketTemplate packetTemplate){
		LinkedList<HPacketFieldTemplate> packetFieldStructure = new LinkedList<>();
		for (HPacketFieldTemplate rootNodeField : packetTemplate.getFields()) {
			LinkedList<HPacketFieldTemplate> gerarchyTree = new LinkedList<>();
			gerarchyTree.add(rootNodeField);
			while (!gerarchyTree.isEmpty()) {
				HPacketFieldTemplate currentNode = gerarchyTree.removeFirst();
				currentNode.setPacket(packetTemplate);
				if(currentNode.getInnerFields()!= null && ! currentNode.getInnerFields().isEmpty()) {
					LinkedList<HPacketFieldTemplate> childNode = new LinkedList<>(currentNode.getInnerFields());
					currentNode.setInnerFields(null);
					for (HPacketFieldTemplate fieldChild : childNode) {
						fieldChild.setParentField(currentNode);
					}
					childNode.forEach(gerarchyTree::addLast);
				}
				packetFieldStructure.add(currentNode);
			}
		}
		packetTemplate.setFields(packetFieldStructure);
	}

	//This method is used when saving Kit to do validation on all SubEntity.
	private void validateWithFrameworkValidation(Kit entity){
		super.validate(entity);
		if(entity.getDevices() != null && ! entity.getDevices().isEmpty()){
			for(HDeviceTemplate device : entity.getDevices()) {
				super.validate(device);
				if (device.getPackets() != null && !device.getPackets().isEmpty()) {
					for (HPacketTemplate packet : device.getPackets()) {
						super.validate(packet);
						if(packet.getFields() != null && ! packet.getFields().isEmpty()) {
							for (HPacketFieldTemplate rootNodeField : packet.getFields()){
								validateInnerPacketFieldStructure(rootNodeField);
							}
						}
					}
				}
			}
		}
	}

	private void validateInnerPacketFieldStructure(HPacketFieldTemplate rootNodeField){
		LinkedList<HPacketFieldTemplate> gerarchyTree = new LinkedList<>();
		gerarchyTree.add(rootNodeField);
		while(! gerarchyTree.isEmpty()){
			HPacketFieldTemplate currentNode = gerarchyTree.removeFirst();
			super.validate(currentNode);
			HPacketFieldTemplate parent = currentNode.getParentField();
			while(parent != null){
				gerarchyTree.add(parent);
				parent= parent.getParentField();
			}
		}
	}

	private HDevice configureHDeviceFromHDeviceTemplate(HDeviceTemplate deviceTemplate, String password, String passwordConfirm, String deviceName, HProject project){
		HDevice device = new HDevice();
		device.setDeviceName(deviceName);
		device.setPassword(password);
		device.setPasswordConfirm(passwordConfirm);
		device.setDescription(deviceTemplate.getDescription());
		device.setModel(deviceTemplate.getModel());
		device.setBrand(deviceTemplate.getBrand());
		device.setFirmwareVersion(deviceTemplate.getFirmwareVersion());
		device.setSoftwareVersion(deviceTemplate.getSoftwareVersion());
		device.setProject(project);
		device.setPackets(new HashSet<>());
		for (HPacketTemplate packetTemplate : deviceTemplate.getPackets()){
			HPacket packet = configureHPacketFromHPacketTemplate(packetTemplate, device);
			device.getPackets().add(packet);
		}
		return device;
	}

	private HPacket configureHPacketFromHPacketTemplate(HPacketTemplate packetTemplate, HDevice device){
		HPacket packet = new HPacket();
		packet.setDevice(device);
		packet.setName(packetTemplate.getName());
		packet.setVersion(packetTemplate.getVersion());
		packet.setFormat(packetTemplate.getFormat());
		packet.setType(packetTemplate.getType());
		packet.setSerialization(packetTemplate.getSerialization());
		packet.setTrafficPlan(packet.getTrafficPlan());
		packet.setTimestampFormat(packetTemplate.getTimestampFormat());
		packet.setTimestampField(packetTemplate.getTimestampField());
		packet.setUnixTimestamp(packetTemplate.isUnixTimestamp());
		packet.setUnixTimestampFormatSeconds(packetTemplate.isUnixTimestampFormatSeconds());
		List<HPacketField> fields = configureHPacketsFieldHierarchy(packet, packetTemplate.getFields());
		packet.setFields(new HashSet<>(fields));
		return packet;
	}

	private List<HPacketField> configureHPacketsFieldHierarchy(HPacket packet, List<HPacketFieldTemplate> fieldTemplates){
		List<HPacketField> packetFields = new ArrayList<>();
		HashMap<Long, HPacketField> packetFieldTemplateProcessed= new HashMap<>();
		for(HPacketFieldTemplate fieldTemplate : fieldTemplates){
			HPacketField field = null;
			if(packetFieldTemplateProcessed.containsKey(fieldTemplate.getId())){
				field = packetFieldTemplateProcessed.get(fieldTemplate.getId());
			} else{
				field = configureHPacketFieldFromHPacketFieldTemplate(fieldTemplate,packet);
				packetFieldTemplateProcessed.put(fieldTemplate.getId(), field);
				packetFields.add(field);
			}
			HPacketFieldTemplate parentFieldTemplate = fieldTemplate.getParentField();
			if(parentFieldTemplate != null){
				HPacketField parentField = null;
				if(packetFieldTemplateProcessed.containsKey(parentFieldTemplate.getId())){
					parentField = packetFieldTemplateProcessed.get(parentFieldTemplate.getId());
					field.setParentField(parentField);
				} else {
					parentField = configureHPacketFieldFromHPacketFieldTemplate(parentFieldTemplate, packet);
					field.setParentField(parentField);
					packetFields.add(parentField);
					packetFieldTemplateProcessed.put(parentFieldTemplate.getId(), parentField);
				}
			}
		}
		return packetFields;
	}

	private HPacketField configureHPacketFieldFromHPacketFieldTemplate(HPacketFieldTemplate fieldTemplate, HPacket packet){
		HPacketField field = new HPacketField();
		field.setPacket(packet);
		field.setName(fieldTemplate.getName());
		field.setUnit(fieldTemplate.getUnit());
		field.setDescription(fieldTemplate.getDescription());
		field.setMultiplicity(fieldTemplate.getMultiplicity());
		field.setType(fieldTemplate.getType());
		return field;
	}

	private HDeviceTemplate getHDeviceTemplateFromKit(Kit kit , long hDeviceTemplateId){
		for(HDeviceTemplate deviceTemplate : kit.getDevices()){
			if(deviceTemplate.getId() == hDeviceTemplateId){
				return deviceTemplate;
			}
		}
		return null;
	}

	private static final String DEFAULT_DEVICE_PASSWORD="Hy0!USER";

}
