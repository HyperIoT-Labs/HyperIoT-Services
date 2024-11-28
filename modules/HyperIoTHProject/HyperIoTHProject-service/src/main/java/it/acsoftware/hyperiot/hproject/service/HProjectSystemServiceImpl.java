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

package it.acsoftware.hyperiot.hproject.service;

import it.acsoftware.hyperiot.algorithm.model.Algorithm;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTShareAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostSaveAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.api.HPacketFieldSystemApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmSystemApi;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.hproject.algorithm.model.dto.ImportLogLevel;
import it.acsoftware.hyperiot.hproject.algorithm.model.dto.ImportLogReport;
import it.acsoftware.hyperiot.hproject.algorithm.model.dto.ExportProjectDTO;
import it.acsoftware.hyperiot.hproject.algorithm.model.dto.ImportReportStatus;
import it.acsoftware.hyperiot.hproject.api.HProjectRepository;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.model.HyperIoTTopicType;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import javax.persistence.NoResultException;
import javax.security.auth.x500.X500PrivateCredential;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;

/**
 * @author Aristide Cittadino Implementation class of the HProjectSystemApi
 * interface. This class is used to implements all additional methods to
 * interact with the persistence layer.
 */
@Component(service = HProjectSystemApi.class, immediate = true)
public final class HProjectSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<HProject>
    implements HProjectSystemApi {


    private HProjectAlgorithmSystemApi hProjectAlgorithmSystemApi;

    private HPacketFieldSystemApi hPacketFieldSystemService;

    private HUserSystemApi hUserSystemService;

    /**
     * Injecting the HProjectRepository to interact with persistence layer
     */
    private HProjectRepository repository;

    /**
     * Injecting the AreaSystemApi
     */
    private AreaSystemApi areaSystemService;

    /**
     * Injecting device system service
     */
    private HDeviceSystemApi deviceSystemService;

    /**
     * Injecting permission system
     */
    private PermissionSystemApi permissionSystemApi;

    /**
     * Injecting HProjectSystemApi
     */
    private HPacketSystemApi hPacketSystemApi;

    /**
     * Constructor for a HProjectSystemServiceImpl
     */
    public HProjectSystemServiceImpl() {
        super(HProject.class);
    }

    /**
     * Return the current repository
     */
    protected HProjectRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}", this.repository);
        return repository;
    }

    /**
     * @param hProjectRepository The current value of HProjectRepository to interact
     *                           with persistence layer
     */
    @Reference
    protected void setRepository(HProjectRepository hProjectRepository) {
        getLog().debug("invoking setRepository, setting: {}", hProjectRepository);
        this.repository = hProjectRepository;
    }

    /**
     * @param areaSystemService Injecting via OSGi DS current AreaSystemService
     */
    @Reference
    protected void setAreaSystemService(AreaSystemApi areaSystemService) {
        getLog().debug("invoking setAreaSystemService, setting: {}", areaSystemService);
        this.areaSystemService = areaSystemService;
    }

    /**
     * @param deviceSystemService Injecting via OSGi DS current HDeviceSystemApi
     */
    @Reference
    public void setDeviceSystemService(HDeviceSystemApi deviceSystemService) {
        this.deviceSystemService = deviceSystemService;
    }

    /**
     * @param permissionSystemApi Injecting via OSGi DS current PermissionSystemApi
     */
    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

    /**
     * @param hPacketSystemApi
     */
    @Reference
    public void sethPacketSystemApi(HPacketSystemApi hPacketSystemApi) {
        this.hPacketSystemApi = hPacketSystemApi;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setHProjectAlgorithmSystemApi(HProjectAlgorithmSystemApi hProjectAlgorithmSystemApi) {
        getLog().debug("invoking setHProjectAlgorithmSystemApi, setting: {}", hProjectAlgorithmSystemApi);
        this.hProjectAlgorithmSystemApi = hProjectAlgorithmSystemApi;
    }

    @Reference
    public void setHPacketFieldSystemApi(HPacketFieldSystemApi hPacketFieldSystemService) {
        getLog().debug("invoking setHPacketFieldSystemService, setting: {}", hPacketFieldSystemService);
        this.hPacketFieldSystemService = hPacketFieldSystemService;
    }

    @Reference
    public void sethUserSystemService(HUserSystemApi hUserSystemService) {
        getLog().debug("invoking setRepository, setting: {}", hUserSystemService);
        this.hUserSystemService = hUserSystemService;
    }

    @SuppressWarnings("serial")
    @Override
    public Collection<Area> getAreasList(long projectId) {
        try {
            repository.find(projectId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        try {
            this.find(projectId, null);
            return areaSystemService.getAreaListByProjectId(projectId);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
    }

    @Override
    public List<String> getUserProjectTopics(HyperIoTTopicType type, long projectId) {
        HProject hProject = this.find(projectId, null);
        ArrayList<String> topics = new ArrayList<>();
        //Adding relatime topics
        topics.addAll(this.getDeviceTopics(type, hProject.getId()));
        //Adding more topic related to project
        topics.addAll(hProject.getProjectTopics(type));
        return topics;
    }

    @Override
    public Collection<HPacket> getProjectTreeViewJson(long projectId) {
        try {
            repository.find(projectId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        return hPacketSystemApi.getProjectPacketsTree(projectId);
    }

    @Override
    public boolean autoRegister(HProject p, List<HPacket> packets) {
        return this.repository.executeTransactionWithReturn(TransactionType.RequiresNew, entityManager -> {
            try {
                for (HPacket packet : packets) {
                    HDevice d = packet.getDevice();
                    d.setProject(p);
                    d.getPackets().add(packet);
                    packet.setDevice(d);
                    entityManager.persist(d);
                    entityManager.persist(packet);
                }
            } catch (Exception e) {
                return false;
            } finally {
                //always reset  the challenge
                p.setGeneratedChallenge(null);
                this.save(p, null);
            }
            return true;
        });
    }

    @Override
    public String createAutoRegisterChallenge(long projectId) {
        return this.executeTransactionWithReturn(TransactionType.Required, entityManager -> {
            HProject project = this.find(projectId, null);
            String uuid = UUID.randomUUID().toString();
            project.setGeneratedChallenge(uuid);
            this.update(project, null);
            return uuid;
        });
    }
    /*
    Export the entire structure of an HProject and the algorithm associated to it.
     */
    @Override
    public ExportProjectDTO loadHProjectForExport(HProject projectToExport) {
        //The check on existence of the HProject is done before in the service's method.
        try {
            projectToExport.setDevices(new HashSet<>(deviceSystemService.
                    getProjectDevicesList(projectToExport.getId())));
            for (HDevice pHDevice : projectToExport.getDevices()) {
                for (HPacket dPacket : pHDevice.getPackets()) {
                    List<HPacketField> dPacketField = hPacketFieldSystemService.getHPacketRootField(dPacket.getId());
                    dPacket.setFields(new HashSet<>(dPacketField));
                }
            }
            projectToExport.setAreas(new HashSet<>(areaSystemService.getRootProjectArea(projectToExport.getId())));
            for (Area area : projectToExport.getAreas()) {
                areaSystemService.getAll(area);
                LinkedList<Area> hierarchyArea = new LinkedList<>();
                hierarchyArea.add(area);
                while(! hierarchyArea.isEmpty()){
                    Area currentArea = hierarchyArea.removeFirst();
                    hierarchyArea.addAll(currentArea.getInnerArea());
                    currentArea.setAreaDevices(areaSystemService.getAreaDevicesList(currentArea));
                }
            }
            LinkedList<HProjectAlgorithm> projectAlgorithms = new LinkedList<>(hProjectAlgorithmSystemApi.findByHProjectId(projectToExport.getId()));
            for(HProjectAlgorithm pAlgorithm: projectAlgorithms){
                pAlgorithm.setProject(projectToExport);
            }
            ExportProjectDTO exportProjectDto = new ExportProjectDTO() ;
            exportProjectDto.setProject(projectToExport);
            exportProjectDto.setAlgorithmsList(projectAlgorithms);
            return exportProjectDto;
        } catch (NoResultException exc) {
            throw new HyperIoTEntityNotFound();
        }
    }


    /*
    If the constraint's validation is satisfied this method import a new project in database.
     */
    @Override
    public ImportLogReport importHProject(ExportProjectDTO dtoProject, HyperIoTContext context) {
        ImportLogReport logReport = new ImportLogReport();
        //Take the user associated to the request
        // This method can throw exception if the request isn't link to user.
        //The exception is caught in ProjectRestApi's method.
        HUser userLogged = hUserSystemService.findUserByUsername(context.getLoggedUsername());
        HProject projectToImport = dtoProject.getProject();
        projectToImport.setUser(userLogged);
        //Validate a subset of database's constraint field related to project's device tree.
        //If validation is succesful add device tree structure to the project.
        //Else throw an exception that contain error's information.
        ifValidAddDeviceStructureToProjectWhenImport(projectToImport,context);
        //Validate a subset of database's constraint field related to project's area tree.
        //If validatoin is succesful add area tree structure to the project.
        //Else throw an exception that contain error's information.
        ifValidAddAreaStructureToProjectWhenImport(projectToImport);
        //Verify contraint's validation through framework validation.
        //Previous validation are used only because we have a finer control on the error message send to end user.
        //A subset of previous validation are redundant respect the framework's validation.
        //We use framework validation despite the fact that error's message is not human-friendly.
        validateWithFrameworkValidation(projectToImport);
        hashEveryDevicePasswordAfterFrameworkValidation(projectToImport);
        //Try to save the project on database
        HProject importedProject = executeTransactionWithReturn(TransactionType.Required,entityMan -> {
            HProject persistedProject = save(projectToImport, context);
            for(Area area : persistedProject.getAreas()){
                HyperIoTUtil.invokePostActions(area, HyperIoTPostSaveAction.class);
            }
            return persistedProject;
        });
        //Append to report the result of the operation.
        logReport.setImportResult(ImportReportStatus.COMPLETED);
        logReport.setProjectName(importedProject.getName());
        //At this point the project(without the algorithm) is saved on the database.
        //Try to save HProjectAlgorithm.
        //If it's not possible to save the algorithm , log the reason in the logReport.
        //Project's saving is indipendent of the project's algorithms saving(Different transaction).
        //When we import the project we assume that the algorithm associate to the HProjectAlgorithm's list
        // is present in the database
        // (So if for an HProjectAlgorithm the algorithm associate doesn't exist in the database,the registration fail.
        try {
            saveProjectAlgorithmForImport(importedProject, dtoProject, logReport, context);
        }catch (Exception e ){
            logReport.addLogMessage(ImportLogLevel.WARNING,"An exception happen during Statistic's saving");
        }
        getLog().debug("Import a new project with id : {}", importedProject.getId());
        return logReport;


    }

    @Override
    public HProject updateHProjectOwner(HyperIoTContext ctx, long projectId, long userId) {
        return this.repository.updateHProjectOwner(projectId, userId);
    }

    @Override
    public X500PrivateCredential createEmptyAutoRegisterProject(HProject project, HyperIoTContext ctx) {
        return this.executeTransactionWithReturn(TransactionType.Required, entityManager -> {
            try {
                entityManager.persist(project);
                KeyPair keyPair = HyperIoTSecurityUtil.generateSSLKeyPairValue(2048);
                //TO DO: substitue servert ca cert with mqtt server cert, now both certs are the same
                X500PrivateCredential credentials = HyperIoTSecurityUtil.createServerClientX509Cert(HProject.class.getName(), String.valueOf(project.getId()), 365, keyPair, HyperIoTSecurityUtil.getServerRootCert());
                //saved locally
                PublicKey publicKey = keyPair.getPublic();
                //set publick Key to the project
                project.setPubKey(publicKey.getEncoded());
                this.update(project, ctx);
                return credentials;
            } catch (Exception e) {
                getLog().error(e.getMessage(), e);
            }
            return null;
        });
    }

    /**
     * On Bundle activated
     */
    @Activate
    public void onActivate() {
        this.checkRegisteredUserRoleExists();
    }

    /**
     * Register permissions for new users
     */
    private void checkRegisteredUserRoleExists() {
        String hProjectResourceName = HProject.class.getName();
        List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTCrudActions(hProjectResourceName);
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.MANAGE_RULES));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.DEVICE_LIST));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.ALGORITHMS_MANAGEMENT));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.AREAS_MANAGEMENT));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.ACTIVATE_TOPOLOGY));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.ADD_TOPOLOGY));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.DEACTIVATE_TOPOLOGY));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.GET_TOPOLOGY));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.GET_TOPOLOGY_LIST));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.KILL_TOPOLOGY));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.SCAN_HBASE_DATA));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTHProjectAction.DELETE_HADOOP_DATA));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName, HyperIoTShareAction.SHARE));
        this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
    }

    /*
    This method is used during the import operation to insert the project's algorithm.
     */
    public void saveProjectAlgorithmForImport(HProject importedProject,ExportProjectDTO dtoProject
            ,ImportLogReport logReport,HyperIoTContext context ){
        if (dtoProject.getAlgorithmsList() != null && !dtoProject.getAlgorithmsList().isEmpty()) {
            List<HProjectAlgorithm> projectAlgorithms = dtoProject.getAlgorithmsList();
            for (HProjectAlgorithm projectAlgorithm : projectAlgorithms) {
                if (projectAlgorithm.getAlgorithm() != null) {
                    Algorithm algo ;
                    try {
                        algo = hProjectAlgorithmSystemApi.findAlgorithmByName(projectAlgorithm.getAlgorithm().getName());
                    } catch (NoResultException exc) {
                        logReport.addLogMessage(ImportLogLevel.WARNING,
                                String.format("Uploading of Statistics %s failed,the Algorithm %s isn't register",
                                        projectAlgorithm.getName(),
                                        projectAlgorithm.getAlgorithm().getName()));
                        continue;
                    } catch (Exception exc) {
                        logReport.addLogMessage(ImportLogLevel.WARNING,
                                String.format("Uploading of Statistics %s failed. Error message: %s",
                                        projectAlgorithm.getName(),
                                        exc.getMessage()));
                        continue;
                    }
                    projectAlgorithm.setAlgorithm(algo);
                    projectAlgorithm.setProject(importedProject);
                    try {
                        hProjectAlgorithmSystemApi.save(projectAlgorithm, context);
                    } catch (HyperIoTValidationException exc) {
                        exc.getViolations().forEach(violation ->
                            logReport.addLogMessage(ImportLogLevel.WARNING, violation.getMessage())
                        );
                    } catch (Exception exc) {
                        logReport.addLogMessage(ImportLogLevel.WARNING, exc.getMessage());
                    }
                } else {
                    logReport.addLogMessage(ImportLogLevel.WARNING,
                            String.format("Cannot add project Statistics %s ," +
                                    " because it has not algorithm associated", projectAlgorithm.getName()));
                }
            }
        } else {
            logReport.addLogMessage(ImportLogLevel.INFO, "There are no algorithms to associate with the project");
        }
    }

    @Override
    public HProject load(long projectId) {
        return this.getRepository().load(projectId);
    }

    @Override
    public Collection<HProject> load(HyperIoTQuery filter) {
        return this.getRepository().load(filter);
    }

    /*
            Add device project structure to project when import if constraint's validation isn't violated.
            The method has side effect on project parameter.
            The method can throw an exception .
            To understand exception's reason look validation's check in this method.
             */
    private void ifValidAddDeviceStructureToProjectWhenImport(HProject projectToImport , HyperIoTContext context){
        //Add device project structure to project when import if constraint's validation isn't violated.
        //The method has side effect on project parameter.
        validateProjectFieldWhenImport(projectToImport,context);
        if(projectToImport.getDevices() != null && !projectToImport.getDevices().isEmpty()) {
            validateIfProjectDeviceIsDuplicateWhenImport(projectToImport);
            for (HDevice device : projectToImport.getDevices()) {
                String devName = device.getDeviceName();
                validateDeviceFieldWhenImport(devName,context);
                device.setPassword(DEFAULT_IMPORT_PASSWORD);
                device.setPasswordConfirm(DEFAULT_IMPORT_PASSWORD);
                device.setProject(projectToImport);
                if(device.getPackets()!= null && !device.getPackets().isEmpty()) {
                    for (HPacket packet : device.getPackets()) {
                        validateDevicePacketWhenImport(packet, device);
                        packet.setDevice(device);
                        if (packet.getFields() != null && ! packet.getFields().isEmpty()) {
                            LinkedList<HPacketField> packetFieldStructure = new LinkedList<>();
                            for (HPacketField rootNodeField : packet.getFields()) {
                                LinkedList<HPacketField> gerarchyTree = new LinkedList<>();
                                gerarchyTree.add(rootNodeField);
                                while (!gerarchyTree.isEmpty()) {
                                    HPacketField currentNode = gerarchyTree.removeFirst();
                                    currentNode.setPacket(packet);
                                    validateHPacketFieldWhenImport( device,  packet, currentNode);
                                    if(currentNode.getInnerFields()!= null && ! currentNode.getInnerFields().isEmpty()) {
                                        validateHPacketInnerFieldWhenImport(device,packet, currentNode);
                                        LinkedList<HPacketField> childNode = new LinkedList<>(currentNode.getInnerFields());
                                        currentNode.setInnerFields(null);
                                        for (HPacketField fieldChild : childNode) {
                                            fieldChild.setParentField(currentNode);
                                        }
                                        childNode.forEach(gerarchyTree::addLast);
                                    }
                                    packetFieldStructure.add(currentNode);
                                }
                            }
                            packet.setFields(new HashSet<>(packetFieldStructure));
                        }
                    }
                }
            }
        }
    }

    /*
    Add area project structure to project when import if constraint's validation isn't violated.
    The method has side effect on project parameter.
    The method can throw an exception .
    To understand exception's reason look validation's check in this method.
     */
    private void ifValidAddAreaStructureToProjectWhenImport(HProject project) {
        if (project.getAreas() != null) {
            List<Area> projectAreaStructure = new LinkedList<>();
            for (Area rootNodeArea : project.getAreas()) {
                LinkedList<Area> gerarchyTree = new LinkedList<>();
                gerarchyTree.addLast(rootNodeArea);
                while (!gerarchyTree.isEmpty()) {
                    Area currentNode = gerarchyTree.removeFirst();
                    currentNode.setProject(project);
                    validateAreaWhenImport(currentNode);
                    if(currentNode.getInnerArea() != null && ! currentNode.getInnerArea().isEmpty()) {
                        validateInnerAreaWhenImport(currentNode);
                        LinkedList<Area> childNode = new LinkedList<>(currentNode.getInnerArea());
                        currentNode.setInnerArea(null);
                        for (Area areaChild : childNode) {
                            areaChild.setParentArea(currentNode);
                        }
                        childNode.forEach(gerarchyTree::addLast);
                    }

                    if (currentNode.getAreaDevices() != null && !currentNode.getAreaDevices().isEmpty()) {
                        for (AreaDevice areaDev : currentNode.getAreaDevices()) {
                            boolean findDevice = false;
                            HDevice deviceInProject = null;
                            validateAreaDeviceWhenImport(currentNode,areaDev,project);
                            for (HDevice projectDev : project.getDevices()) {
                                if (areaDev.getDevice().getDeviceName().equals(projectDev.getDeviceName())) {
                                    findDevice = true;
                                    deviceInProject = projectDev;
                                }
                            }
                            if (!findDevice) {
                                throw new HyperIoTRuntimeException(
                                        String.format("Import failed. Area %s has associated an area device %s " +
                                                "that is not present in project's device list",
                                                currentNode.getName(),
                                                areaDev.getDevice().getDeviceName()
                                        ) );
                            }
                            areaDev.setArea(currentNode);
                            areaDev.setDevice(deviceInProject);
                        }
                    }
                    projectAreaStructure.add(currentNode);
                }
            }
            project.setAreas(new HashSet<>(projectAreaStructure));
        }
    }

    private void hashEveryDevicePasswordAfterFrameworkValidation(HProject projects ){
        if(projects.getDevices() != null && !projects.getDevices().isEmpty()){
            for(HDevice projDevice : projects.getDevices()){
                String passwordHash = HyperIoTUtil.getPasswordHash(projDevice.getPassword());
                projDevice.setPassword(passwordHash);
                projDevice.setPasswordConfirm(passwordHash);
            }
        }
    }


    private void validateProjectFieldWhenImport(HProject projectToImport, HyperIoTContext context){
        if(projectToImport.getName()== null || projectToImport.getName().equals("")){
            throw new HyperIoTRuntimeException("Import failed. Your project is unnamed");
        }
        HyperIoTQuery projectByNameAndUserId= HyperIoTQueryBuilder.newQuery().equals("name",projectToImport.getName()).and(
                HyperIoTQueryBuilder.newQuery().equals("user.id",projectToImport.getUser().getId()));
        try{
            this.find(projectByNameAndUserId, context);
            throw new HyperIoTRuntimeException(
                    String.format("Import failed. You have just upload a project with name : %s . Change project's name",
                            projectToImport.getName()));
        } catch (NoResultException exc){
            getLog().debug(String.format("Name %s project is valid.",projectToImport.getName()));
        }
    }

    private void validateDeviceFieldWhenImport(String deviceName , HyperIoTContext context){
        HyperIoTQuery devByDevName = HyperIoTQueryBuilder.newQuery().equals("deviceName", deviceName);
        try {
            this.deviceSystemService.find(devByDevName, context);
            throw new HyperIoTRuntimeException(
                    String.format("Import failed. You must change the name of the device called %s",
                            deviceName));
        }catch (NoResultException exc){
            getLog().debug(String.format("Device name %s is valid",deviceName));
        }
    }

    private void validateIfProjectDeviceIsDuplicateWhenImport(HProject project){
        for(HDevice device : project.getDevices()){
            if(device.getDeviceName() == null || device.getDeviceName().equals("")){
                throw new HyperIoTRuntimeException("Import failed. Your project has an unnamed device ");
            }
            LinkedList<HDevice> duplicatedDevice = new LinkedList<>();
            for(HDevice devicex : project.getDevices()){
                if(devicex.getDeviceName() == null || devicex.getDeviceName().equals("")){
                    throw new HyperIoTRuntimeException("Import failed. Your project has an unnamed device ");
                }
                if(device.getDeviceName().equals(devicex.getDeviceName())){
                    duplicatedDevice.add(devicex);
                }
            }
            if(duplicatedDevice.size() > 1 ){
                throw new HyperIoTRuntimeException(
                        String.format("Import failed. Your project has %d device with the same deviceName %s ",
                                duplicatedDevice.size(),
                                device.getDeviceName()
                                ));
            }
        }
    }

    private void validateDevicePacketWhenImport(HPacket packet , HDevice device ){
        //Packet validation for import not need database query.
        if(packet.getName() == null || packet.getName().equals("")){
            throw new HyperIoTRuntimeException(
                    String.format("Import failed. The device %s contain an unnamed packet ",device.getDeviceName() ));
        }
        if(packet.getVersion() == null || packet.getVersion().equals("")){
            throw new HyperIoTRuntimeException(
                    String.format("Import failed. The device %s contain a packet %s without version specified ",
                            device.getDeviceName(),
                            packet.getName()
                    ));
        }
        List<HPacket> packetDuplicateList = new LinkedList<>();
        for(HPacket packz : device.getPackets()){
            if(packz.getName() == null || packz.getName().equals("")){
                throw new HyperIoTRuntimeException(
                        String.format("Import failed. The device %s contain an unnamed packet ",device.getDeviceName() ));
            }
            if(packz.getVersion() == null || packz.getVersion().equals("")){
                throw new HyperIoTRuntimeException(
                        String.format("Import failed. The device %s contain a packet %s without version specified ",
                                device.getDeviceName(),
                                packz.getName()
                        ));
            }
            if(packet.getName().equals(packz.getName()) && packet.getVersion().equals(packz.getVersion())){
                packetDuplicateList.add(packz);
            }
        }
        if(packetDuplicateList.size() > 1){
            throw new HyperIoTRuntimeException(
                    String.format("Import failed. The device %s contain two packet with the same name and " +
                            "same version , change this before upload the project ",device.getDeviceName() ));
        }
    }
    private void validateHPacketFieldWhenImport(HDevice device, HPacket packet, HPacketField currentNode){
        if(currentNode.getName()== null || currentNode.getName().equals("")){
            throw new HyperIoTRuntimeException(
                    String.format("Import failed. The device %s contain the packet %s that contain an unnamed packet field",
                            device.getDeviceName(),
                            packet.getName()
                    ));
        }
    }
    private void validateHPacketInnerFieldWhenImport(HDevice device, HPacket packet, HPacketField currentNode){
        for(HPacketField inner : currentNode.getInnerFields()){
            if(inner.getName()== null || inner.getName().equals("")){
                throw new HyperIoTRuntimeException(
                        String.format("Import failed .The device %s contain packet %s that contain a packet field %s " +
                                        "with unnamed inner field",
                                device.getDeviceName(),
                                packet.getName(),
                                currentNode.getName()));
            }
            List<HPacketField> duplicateInnerField = new LinkedList<>();
            for(HPacketField innerx : currentNode.getInnerFields()){
                if(innerx.getName()== null || innerx.getName().equals("")){
                    throw new HyperIoTRuntimeException(
                            String.format("Import failed .The device %s contain packet %s that contain a packet field %s" +
                                            " with unnamed inner field",
                                    device.getDeviceName(),
                                    packet.getName(),
                                    currentNode.getName()
                            ));
                }
                if(inner.getName().equals(innerx.getName())){
                    duplicateInnerField.add(innerx);
                }
            }
            if(duplicateInnerField.size() > 1 ){
                throw new HyperIoTRuntimeException(
                        String.format("Import failed. The device %s contain packet %s that contain a packet field" +
                                        " %s such that two inner field has the same name %s ",
                                device.getDeviceName(),
                                packet.getName(),
                                currentNode.getName(),
                                inner.getName()));
            }
        }
    }
    private void validateAreaWhenImport(Area currentNode){
        if(currentNode.getName() == null || currentNode.getName().equals("")){
            throw new HyperIoTRuntimeException("Import failed. The project contain unnamed area");
        }
    }
    private void validateInnerAreaWhenImport(Area currentNode){
        for(Area childArea : currentNode.getInnerArea()){
            if(childArea.getName() == null || childArea.getName().equals("")){
                throw new HyperIoTRuntimeException(
                        String.format("Import failed. The area %s contain a child unnamed area",currentNode.getName()));
            }
            LinkedList<Area> duplicateArea = new LinkedList<>();
            for(Area childAreaz : currentNode.getInnerArea()){
                if(childAreaz.getName() == null || childAreaz.getName().equals("")){
                    throw new HyperIoTRuntimeException(
                            String.format("Import failed. The area %s contain a child unnamed area",
                                    currentNode.getName()));

                }
                if(childArea.getName().equals(childAreaz.getName())){
                    duplicateArea.add(childAreaz);
                }
            }
            if(duplicateArea.size() > 1 ){
                throw new HyperIoTRuntimeException(
                        String.format("Import failed. The area %s , contain two inner area with the same name %s",
                                currentNode.getName(),
                                childArea.getName()
                        ));
            }
        }
    }
    public void validateAreaDeviceWhenImport(Area area, AreaDevice areaDev , HProject project){
        if (areaDev.getDevice() == null) {
            throw new HyperIoTRuntimeException(
                    String.format("Import failed. Area %s contain an area device without the associated device",
                            area.getName()));
        }
        if(areaDev.getDevice().getDeviceName()== null || areaDev.getDevice().getDeviceName().equals("")){
            throw new HyperIoTRuntimeException(
                    String.format("Import failed. Area %s contain an area device with unnamed device",
                            area.getName()));
        }
        if(project.getDevices() == null ){
            throw new HyperIoTRuntimeException(
                    String.format("Import failed . Area %s has associated a device %s but project's device list is empty",
                            area.getName(),
                            areaDev.getDevice().getDeviceName()
                    ));

        }
    }


    private void validateWithFrameworkValidation(HProject entity) {
        super.validate(entity);
        if(entity.getDevices() != null && ! entity.getDevices().isEmpty()){
            for(HDevice device : entity.getDevices()) {
                super.validate(device);
                if (device.getPackets() != null && !device.getPackets().isEmpty()) {
                    for (HPacket packet : device.getPackets()) {
                        super.validate(packet);
                        if(packet.getFields() != null && ! packet.getFields().isEmpty()) {
                            for (HPacketField field : packet.getFields()){
                                super.validate(field);
                                }
                            }
                        }
                    }
                }
            }
        if(entity.getAreas() != null && ! entity.getAreas().isEmpty()){
            for(Area area : entity.getAreas()){
                super.validate(area);
                if(area.getAreaDevices() != null && !area.getAreaDevices().isEmpty()){
                    for(AreaDevice areaDevice : area.getAreaDevices()){
                        super.validate(areaDevice);
                    }
                }
            }
        }
    }

    private final static String DEFAULT_IMPORT_PASSWORD="Hy0!USER";

}
