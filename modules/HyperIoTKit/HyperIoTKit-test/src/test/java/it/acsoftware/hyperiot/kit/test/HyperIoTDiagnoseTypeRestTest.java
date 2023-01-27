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

package it.acsoftware.hyperiot.kit.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.area.api.AreaDeviceRepository;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.asset.category.api.AssetCategorySystemApi;
import it.acsoftware.hyperiot.asset.category.model.AssetCategory;
import it.acsoftware.hyperiot.asset.category.model.AssetCategoryResource;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.exception.HyperIoTDuplicateEntityException;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.base.model.HyperIoTAssetOwnerImpl;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.util.HyperIoTTestUtils;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.api.HUserRepository;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.huser.test.util.HyperIoTHUserTestUtils;
import it.acsoftware.hyperiot.kit.api.KitSystemApi;
import it.acsoftware.hyperiot.kit.category.api.DiagnoseTypeSystemApi;
import it.acsoftware.hyperiot.kit.category.model.DiagnoseType;
import it.acsoftware.hyperiot.kit.category.service.rest.DiagnoseTypeRestApi;
import it.acsoftware.hyperiot.kit.model.Kit;
import it.acsoftware.hyperiot.kit.service.KitUtils;
import it.acsoftware.hyperiot.kit.service.rest.KitRestApi;
import it.acsoftware.hyperiot.kit.template.model.HDeviceTemplate;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.test.util.HyperIoTPermissionTestUtil;
import it.acsoftware.hyperiot.role.api.RoleSystemApi;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import it.acsoftware.hyperiot.shared.entity.service.rest.SharedEntityRestApi;
import org.apache.aries.jpa.template.TransactionType;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTDiagnoseTypeRestTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        return HyperIoTServicesTestConfigurationBuilder
                .createStandardConfiguration()
                .build();
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    private HyperIoTAction getHyperIoTAction(String resourceName,
                                             HyperIoTActionName action, long timeout) {
        String actionFilter = OSGiFilterBuilder
                .createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
                .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
        return getOsgiService(HyperIoTAction.class, actionFilter, timeout);
    }

    @Test
    public void test_000_hyperIoTFrameworkShouldBeInstalled() throws Exception {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class,0);
        String features = executeCommand("feature:list -i");
        assertContains("HyperIoTBase-features ", features);
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
        assertContains("HyperIoTSharedEntity-features",features);
        assertContains("HyperIoTKit-features", features);
        assertContains("HyperIoTHProject-features", features);
        String datasource = executeCommand("jdbc:ds-list");
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test_001_hyperiotPlatformShouldBeInstalled() throws Exception{
        assertServiceAvailable(FeaturesService.class,0);
        String features = executeCommand("feature:list -i");
        assertContains("HyperIoTHProject-features ", features);
        assertContains("HyperIoTKit-features ", features);
    }

    @Test
    public void test_002_should_checkModuleWorking_works(){
        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(diagnoseTypeRestService, adminUser);
        Response restResponse = diagnoseTypeRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HyperIoT DiagnoseType module works", restResponse.getEntity());
    }

    @Test
    public void test_003_should_saveDiagnoseType_fail_whenDiagnoseTypeLabelIsNull(){
        //Create generic user
        //Log as generic user
        //Create DiagnoseType template
        //Set DiagnoseType's label to null
        //Save DiagnoseType
        //Asser that operation is not valid (label field cannot be null).

        HUser genericUser = registerHUserAndActivateAccount();
        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);
        diagnoseType.setLabel(null);
        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(422, saveDiagnoseTypeResponse.getStatus());
        assertEquals(getHyperIoTValidationExceptionClassName(), ((HyperIoTBaseError) saveDiagnoseTypeResponse.getEntity()).getType());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) saveDiagnoseTypeResponse.getEntity();
        assertEquals(2,errorResponse.getValidationErrors().size());
        assertEquals(errorResponse.getValidationErrors().get(0).getField(),"diagnosetype-label");
        assertEquals(errorResponse.getValidationErrors().get(1).getField(),"diagnosetype-label");
    }

    @Test
    public void test_004_should_saveDiagnoseType_fail_whenDiagnoseTypeLabelIsEmpty(){
        //Create generic user
        //Log as generic user
        //Create DiagnoseType template
        //Set DiagnoseType's label to empty string
        //Save DiagnoseType
        //Asser that operation is not valid (label field cannot be empty).

        HUser genericUser = registerHUserAndActivateAccount();
        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);
        diagnoseType.setLabel("");
        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(422, saveDiagnoseTypeResponse.getStatus());
        assertEquals(getHyperIoTValidationExceptionClassName(), ((HyperIoTBaseError) saveDiagnoseTypeResponse.getEntity()).getType());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) saveDiagnoseTypeResponse.getEntity();
        assertEquals(1,errorResponse.getValidationErrors().size());
        assertEquals(errorResponse.getValidationErrors().get(0).getField(),"diagnosetype-label");
    }

    @Test
    public void test_005_should_saveDiagnoseType_fail_whenDiagnoseTypeLabelContainMaliciousCode(){
        //Create generic user
        //Log as generic user
        //Create DiagnoseType template
        //Set DiagnoseType's label with malicious value
        //Save DiagnoseType
        //Asser that operation is not valid (label field cannot contain malicious value).

        HUser genericUser = registerHUserAndActivateAccount();
        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);
        diagnoseType.setLabel(getMaliciousValue());
        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(422, saveDiagnoseTypeResponse.getStatus());
        assertEquals(getHyperIoTValidationExceptionClassName(), ((HyperIoTBaseError) saveDiagnoseTypeResponse.getEntity()).getType());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) saveDiagnoseTypeResponse.getEntity();
        assertEquals(1,errorResponse.getValidationErrors().size());
        assertEquals(errorResponse.getValidationErrors().get(0).getField(),"diagnosetype-label");
    }

    @Test
    public void test_006_should_saveDiagnoseType_fail_whenDuplicatedEntityException(){
        //Create generic user
        //Log as generic user
        //Create DiagnoseType template
        //Save diagnoseType
        //Create a new Diagnose type template such that label is equal to label of the diagnose type saved before
        //Asser that operation is not fail for DuplicatedEntityException
        //Assert the field duplicated in error response is how we expected(user_id,label).

        HUser genericUser = registerHUserAndActivateAccount();
        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);
        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType =(DiagnoseType) saveDiagnoseTypeResponse.getEntity();

        DiagnoseType duplicatedDiagnoseType = createDiagnoseTypeTemplate(genericUser);
        duplicatedDiagnoseType.setLabel(savedDiagnoseType.getLabel());
        Response duplicatedResponse = diagnoseTypeRestService.save(duplicatedDiagnoseType);
        assertTrue(duplicatedResponse.getEntity() instanceof HyperIoTBaseError);
        HyperIoTBaseError errorResponse = ((HyperIoTBaseError) duplicatedResponse.getEntity());
        assertNotNull(errorResponse);
        assertEquals(HyperIoTDuplicateEntityException.class.getName(),errorResponse.getType());
        assertEquals(2,errorResponse.getErrorMessages().size());
        assertEquals(errorResponse.getErrorMessages().get(0),"label");
        assertEquals(errorResponse.getErrorMessages().get(1),"user_id");
    }

    @Test
    public void test_007_should_saveDiagnoseType_works() throws JsonProcessingException {
        //Create generic user
        //Log as generic user
        //Create DiagnoseType template
        //Save diagnoseType
        //Assert that operation is successful
        //Assert the semantic of the operation.

        HUser genericUser = registerHUserAndActivateAccount();
        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);

        DiagnoseType clonedDiagnoseType = cloneDiagnoseType(diagnoseType);

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType =(DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        assertNotNull(savedDiagnoseType);
        assertNotEquals(0, savedDiagnoseType.getId());
        assertEquals(clonedDiagnoseType.getLabel(),savedDiagnoseType.getLabel());
        assertNotNull(savedDiagnoseType.getUser());
        assertEquals(savedDiagnoseType.getUser().getId(),clonedDiagnoseType.getUser().getId());
        assertNotNull(savedDiagnoseType.getCategory());
        AssetCategory diagnoseTypeCategory = savedDiagnoseType.getCategory();
        assertEquals(diagnoseTypeCategory.getName(), String.format("%s_%s", savedDiagnoseType.getLabel(), genericUser.getId()));
        assertNotNull(diagnoseTypeCategory.getOwner());
        HyperIoTAssetOwnerImpl categoryOwner = diagnoseTypeCategory.getOwner();
        assertEquals(DiagnoseType.class.getName(), categoryOwner.getOwnerResourceName());
        assertEquals(genericUser.getId(), categoryOwner.getUserId());
        assertEquals((long) categoryOwner.getOwnerResourceId(), savedDiagnoseType.getId());
    }

    @Test
    public void test_008_should_findDiagnoseType_works()  {
        //Create generic user
        //Log as generic user
        //Create DiagnoseType template
        //Save diagnoseType
        //Find diagnoseType
        //Assert that operation is successful

        HUser genericUser = registerHUserAndActivateAccount();
        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);


        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType =(DiagnoseType) saveDiagnoseTypeResponse.getEntity();

        Response findDiagnoseTypeResponse = diagnoseTypeRestService.findDiagnoseType(savedDiagnoseType.getId());
        assertEquals(200,findDiagnoseTypeResponse.getStatus());
        assertNotNull(findDiagnoseTypeResponse.getEntity());
        DiagnoseType findDiagnoseType = (DiagnoseType) findDiagnoseTypeResponse.getEntity();
        assertEquals(findDiagnoseType.getId(),savedDiagnoseType.getId());
    }

    @Test
    public void test_009_should_findDiagnoseType_cannotRetrieveADiagnoseTypeSavedByAnotherUser()  {
        //Create generic user
        //Log as generic user
        //Create DiagnoseType template
        //Save diagnoseType
        //Create another user
        //Log as this user
        //Find diagnoseType
        //Assert that operation is not authorized.

        HUser genericUser = registerHUserAndActivateAccount();
        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);


        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType =(DiagnoseType) saveDiagnoseTypeResponse.getEntity();

        //Create another user
        HUser unauthorizedUser = registerHUserAndActivateAccount();

        //log as this user
        impersonateUser(diagnoseTypeRestService,unauthorizedUser);

        Response findDiagnoseTypeResponse = diagnoseTypeRestService.findDiagnoseType(savedDiagnoseType.getId());
        //Assert that the user cannot retrieve
        assertEquals(404,findDiagnoseTypeResponse.getStatus());
    }

    @Test
    public void test_010_should_deleteDiagnoseType_works()  {
        //Create generic user
        //Log as generic user
        //Create DiagnoseType template
        //Save diagnoseType
        //Delete diagnose type
        //Assert the semantic of the operation.

        HUser genericUser = registerHUserAndActivateAccount();
        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);


        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType =(DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        long diagnoseTypeId = savedDiagnoseType.getId();
        assertNotNull(savedDiagnoseType.getCategory());
        AssetCategory diagnoseTypeCategory = savedDiagnoseType.getCategory();
        long categoryId = diagnoseTypeCategory.getId();

        Response deleteDiagnoseTypeResponse = diagnoseTypeRestService.deleteDiagnoseType(diagnoseTypeId);
        assertEquals(200,deleteDiagnoseTypeResponse.getStatus());
        //Assert that the category relative to the diagnose type was removed.
        AssetCategorySystemApi categorySystemApi = getOsgiService(AssetCategorySystemApi.class);
        //We use this try catch to assert that category is removed because jUnit4 assertThrows not works.
        boolean findCategory=true;
        try{
            categorySystemApi.find(categoryId,null);
        }catch (Throwable exc){
            findCategory=false;
        }
        assertFalse(findCategory);
    }

    @Test
    public void test_011_should_deleteDiagnoseType_isNotAuthorized_whenUserNotOwnsDiagnoseType()  {
        //Create generic user
        //Log as generic user
        //Create DiagnoseType template
        //Save diagnoseType
        //Create another user
        //Log as this user
        //Delete diagnose type
        //Assert that the operation is not authorized.

        HUser genericUser = registerHUserAndActivateAccount();
        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);


        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType =(DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        long diagnoseTypeId = savedDiagnoseType.getId();
        assertNotNull(savedDiagnoseType.getCategory());
        AssetCategory diagnoseTypeCategory = savedDiagnoseType.getCategory();
        long categoryId = diagnoseTypeCategory.getId();

        HUser unauthorizedUser = createHUserTemplate();

        impersonateUser(diagnoseTypeRestService,unauthorizedUser);

        Response deleteDiagnoseTypeResponse = diagnoseTypeRestService.deleteDiagnoseType(diagnoseTypeId);
        //Assert that operation is not authorized.
        assertEquals(403,deleteDiagnoseTypeResponse.getStatus());

        //Assert that the diagnose type is not removed.
        DiagnoseTypeSystemApi diagnoseTypeSystemApi = getOsgiService(DiagnoseTypeSystemApi.class);
        boolean findDiagnoseType = true;
        try{
            diagnoseTypeSystemApi.find(savedDiagnoseType.getId(),null);
        }catch (Throwable exc){
            findDiagnoseType=false;
        }
        assertTrue(findDiagnoseType);

        //Assert that the category relative to the diagnose type is not removed removed.
        AssetCategorySystemApi categorySystemApi = getOsgiService(AssetCategorySystemApi.class);
        boolean findCategory=true;
        try{
            categorySystemApi.find(categoryId,null);
        }catch (Throwable exc){
            findCategory=false;
        }
        assertTrue(findCategory);
    }

    @Test
    public void test_012_should_addKitToDiagnoseTypeCategory_works()  {
        //Create generic user
        //Create HProject
        //Create a kit relative to this project.
        //Save kit
        //Create DiagnoseType template
        //Save diagnoseType
        //Add kit to diagnoseType category
        //Assert that the operation is authorized (Because the user is owner of kit project)
        //Assert the semantic of the operation .

        HUser genericUser = registerHUserAndActivateAccount();
        HProject hProject = saveNewHProject(genericUser,USER_PASSWORD);
        Kit kit = createKitTemplate("kitLabel",hProject.getId());
        addDeviceTemplateToKit(kit,createHDeviceTemplate("hdeviceLabel"));

        KitRestApi kitRestService = getOsgiService(KitRestApi.class);
        impersonateUser(kitRestService,genericUser);
        Response saveKitResponse = kitRestService.saveKit(kit);
        assertEquals(200,saveKitResponse.getStatus());
        Kit savedKit = (Kit) saveKitResponse.getEntity();
        assertNotNull(savedKit);
        assertNotEquals(0,savedKit.getId());
        long savedKitId = savedKit.getId();

        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType =(DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        long diagnoseTypeId = savedDiagnoseType.getId();
        assertNotNull(savedDiagnoseType.getCategory());
        AssetCategory diagnoseTypeCategory = savedDiagnoseType.getCategory();
        long categoryId = diagnoseTypeCategory.getId();


        //Add kit to diagnoseType category
        Response addKitToDiagnoseTypeResponse = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(diagnoseTypeId,savedKitId);
        assertEquals(200,addKitToDiagnoseTypeResponse.getStatus());
        DiagnoseType diagnoseTypeAfterAddedKit = (DiagnoseType) addKitToDiagnoseTypeResponse.getEntity();
        assertNotNull(diagnoseTypeAfterAddedKit);
        assertEquals(diagnoseTypeAfterAddedKit.getId(),diagnoseTypeId);
        assertEquals(diagnoseTypeAfterAddedKit.getCategory().getId(),categoryId);

        //Assert that an AssetCategoryResource is added to the category related to the diagnose type.
        AssetCategorySystemApi assetCategorySystemApi = getOsgiService(AssetCategorySystemApi.class);
        AssetCategory categoryWithResource = assetCategorySystemApi.executeTransactionWithReturn(TransactionType.Required, (entityManager)-> {
            AssetCategory categoryInTransaction = assetCategorySystemApi.find(categoryId,null);
            //Trigger loading of the resources
            Set<AssetCategoryResource> lazyResources = categoryInTransaction.getResources();
            //Trigger loading of the resources
            lazyResources.size();
            return categoryInTransaction;
        });
        assertNotNull(categoryWithResource);

        Set<AssetCategoryResource> categoryResources = categoryWithResource.getResources();
        //We added only a kit to the diagnose type.
        assertTrue(categoryResources != null && categoryResources.size()==1);
        //Assert that the category resource related to the category is how we expected.
        AssetCategoryResource resourceRelativeToAddedKit = new ArrayList<>(categoryWithResource.getResources()).get(0);
        assertEquals(resourceRelativeToAddedKit.getCategory(),categoryWithResource);
        assertEquals(resourceRelativeToAddedKit.getResourceId(),savedKitId);
        assertEquals(resourceRelativeToAddedKit.getResourceName(),Kit.class.getName());

    }

    @Test
    public void test_013_should_addKitToDiagnoseTypeCategory_isNotAuthorized_whenUserIsNotTheOwnerOfTheProjectRelatedToKit()  {
        //Create  user
        //Create HProject
        //Create a kit relative to this project.
        //Save kit
        //Create DiagnoseType template
        //Save diagnoseType
        //Create another user
        //Log as this user
        //Assert that this user is not authorized to add  kit to diagnoseType category
        //After that , log as project owner, share the project with this user and log again as this user
        //Assert that, after sharing operation, user is not  authorized to add  kit to diagnoseType category

        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser projectOwner = (HUser) authService.login("hadmin", "admin");

        //Save project
        HProject project = saveNewHProject(projectOwner,"admin");
        //Create kit related to this project
        Kit kit = createKitTemplate("kitLabel",project.getId());
        addDeviceTemplateToKit(kit,createHDeviceTemplate("hdeviceLabel"));

        KitRestApi kitRestService = getOsgiService(KitRestApi.class);
        impersonateUser(kitRestService, projectOwner);
        Response saveKitResponse = kitRestService.saveKit(kit);
        assertEquals(200, saveKitResponse.getStatus());
        Kit savedKit = (Kit) saveKitResponse.getEntity();
        assertNotNull(savedKit);
        assertNotEquals(0, savedKit.getId());
        long savedKitId = savedKit.getId();

        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(projectOwner);

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, projectOwner);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType = (DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        long diagnoseTypeId = savedDiagnoseType.getId();

        //Create  user
        HUser genericUser = registerHUserAndActivateAccount();
        //Log as generic user
        impersonateUser(diagnoseTypeRestService,genericUser);

        //Assert that user is not authorized to add the kit to the diagnose type category
        Response responseWhenProjectIsNotShared = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(diagnoseTypeId,savedKitId);
        assertEquals(403,responseWhenProjectIsNotShared.getStatus());

        //Share project with this user
        shareHProjectWithUser(project,projectOwner,"admin",genericUser);

        //Log as generic user
        impersonateUser(diagnoseTypeRestService,genericUser);

        //Assert that , after sharing the project, the user is not authorized to add the kit to the diagnose type category
        Response responseWhenProjectIsShared = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(diagnoseTypeId,savedKitId);
        assertEquals(403,responseWhenProjectIsShared.getStatus());


    }

    @Test
    public void test_014_should_deleteKitFromDiagnoseTypeCategory_works()  {
        //Create generic user
        //Create HProject
        //Create a kit relative to this project.
        //Save kit
        //Create DiagnoseType template
        //Save diagnoseType
        //Add kit to diagnoseType category
        //Delete kit from diagnoseType category
        //Assert that the operation is authorized (Because the user is owner of kit project)
        //Assert the semantic of the operation .

        HUser genericUser = registerHUserAndActivateAccount();
        HProject hProject = saveNewHProject(genericUser, USER_PASSWORD);
        Kit kit = createKitTemplate("kitLabel", hProject.getId());
        addDeviceTemplateToKit(kit, createHDeviceTemplate("hdeviceLabel"));

        KitRestApi kitRestService = getOsgiService(KitRestApi.class);
        impersonateUser(kitRestService, genericUser);
        Response saveKitResponse = kitRestService.saveKit(kit);
        assertEquals(200, saveKitResponse.getStatus());
        Kit savedKit = (Kit) saveKitResponse.getEntity();
        assertNotNull(savedKit);
        assertNotEquals(0, savedKit.getId());
        long savedKitId = savedKit.getId();

        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType = (DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        long diagnoseTypeId = savedDiagnoseType.getId();
        assertNotNull(savedDiagnoseType.getCategory());
        AssetCategory diagnoseTypeCategory = savedDiagnoseType.getCategory();
        long categoryId = diagnoseTypeCategory.getId();


        //Add kit to diagnoseType category
        Response addKitToDiagnoseTypeResponse = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(diagnoseTypeId, savedKitId);
        assertEquals(200, addKitToDiagnoseTypeResponse.getStatus());

        //Delete kit from diagnoseType category
        Response deleteKitFromDiagnoseTypeCategoryResponse = diagnoseTypeRestService.deleteKitFromDiagnoseTypeCategory(diagnoseTypeId,savedKitId);
        assertEquals(200,deleteKitFromDiagnoseTypeCategoryResponse.getStatus());

        //Assert that kit is not associated to the diagnose type
        Response getDiagnoseTypeKitsResponse = diagnoseTypeRestService.getKitsFromDiagnoseType(diagnoseTypeId);
        assertEquals(200,getDiagnoseTypeKitsResponse.getStatus());
        Collection<Kit> diagnoseTypeKitsList = (Collection<Kit>) getDiagnoseTypeKitsResponse.getEntity();
        assertNotNull(diagnoseTypeKitsList);
        assertTrue(diagnoseTypeKitsList.isEmpty());

        //Assert that the assetCategoryResource related to DiagnoseType's asset category related to kit is removed.
        AssetCategorySystemApi assetCategorySystemApi = getOsgiService(AssetCategorySystemApi.class);

        assetCategorySystemApi.executeTransaction(TransactionType.Required, (em -> {
            AssetCategory categoryInTransaction = assetCategorySystemApi.find(categoryId,null);
            Set<AssetCategoryResource> categoryResources = categoryInTransaction.getResources();
            assertTrue(categoryResources == null || categoryResources.isEmpty());
        }));

    }

    @Test
    public void test_015_should_deleteKitFromDiagnoseTypeCategory_isNotAuthorized_whenTheUserIsNotTheOwnerOfProjectRelatedToKit()  {
        //Create  user
        //Create HProject
        //Create a kit relative to this project.
        //Save kit
        //Create DiagnoseType template
        //Save diagnoseType
        //Add kit to diagnoseType category
        //Create another user
        //Log as this user
        //Assert that this user is not authorized to delete kit from diagnoseType category
        //After that , log as project owner, share the project with this user and log again as this user
        //Assert that, after sharing operation, user is not  authorized delete kit from diagnoseType category

        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser projectOwner = (HUser) authService.login("hadmin", "admin");

        //Save project
        HProject project = saveNewHProject(projectOwner,"admin");
        //Create kit related to this project
        Kit kit = createKitTemplate("kitLabel",project.getId());
        addDeviceTemplateToKit(kit,createHDeviceTemplate("hdeviceLabel"));

        KitRestApi kitRestService = getOsgiService(KitRestApi.class);
        impersonateUser(kitRestService, projectOwner);

        Response saveKitResponse = kitRestService.saveKit(kit);
        assertEquals(200, saveKitResponse.getStatus());
        Kit savedKit = (Kit) saveKitResponse.getEntity();
        assertNotNull(savedKit);
        assertNotEquals(0, savedKit.getId());
        long savedKitId = savedKit.getId();

        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(projectOwner);

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, projectOwner);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType = (DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        long diagnoseTypeId = savedDiagnoseType.getId();
        assertNotNull(savedDiagnoseType.getCategory());
        AssetCategory diagnoseTypeCategory = savedDiagnoseType.getCategory();
        long categoryId = diagnoseTypeCategory.getId();


        //Add kit to diagnoseType category
        Response addKitToDiagnoseTypeResponse = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(diagnoseTypeId, savedKitId);
        assertEquals(200, addKitToDiagnoseTypeResponse.getStatus());

        //Create other user
        HUser genericUser =  registerHUserAndActivateAccount();
        //Log as other user
        impersonateUser(diagnoseTypeRestService,genericUser);

        //Assert that the user cannot delete a kit from a diagnose type category
        Response deleteKitFromDiagnoseTypeCategoryResponseWhenProjectNotShared = diagnoseTypeRestService.deleteKitFromDiagnoseTypeCategory(diagnoseTypeId,savedKitId);
        assertEquals(403,deleteKitFromDiagnoseTypeCategoryResponseWhenProjectNotShared.getStatus());

        //Share project with the user
        shareHProjectWithUser(project,projectOwner,"admin",genericUser);

        //Log as generic user.
        impersonateUser(diagnoseTypeRestService,genericUser);
        //Assert that the user cannot delete a kit from a diagnose type category
        Response deleteKitFromDiagnoseTypeCategoryResponseWhenProjectIsShared = diagnoseTypeRestService.deleteKitFromDiagnoseTypeCategory(diagnoseTypeId,savedKitId);
        assertEquals(403,deleteKitFromDiagnoseTypeCategoryResponseWhenProjectIsShared.getStatus());


        //Assert that the kit is not removed by the diagnose type
        impersonateUser(diagnoseTypeRestService,projectOwner);
        Response getDiagnoseTypeKitsResponse = diagnoseTypeRestService.getKitsFromDiagnoseType(diagnoseTypeId);
        assertEquals(200,getDiagnoseTypeKitsResponse.getStatus());
        Collection<Kit> diagnoseTypeKitsList = (Collection<Kit>) getDiagnoseTypeKitsResponse.getEntity();
        assertNotNull(diagnoseTypeKitsList);
        assertEquals(1,diagnoseTypeKitsList.size());
        assertEquals(new ArrayList<>(diagnoseTypeKitsList).get(0).getId(),savedKitId);

        //Assert that the assetCategoryResource related to DiagnoseType's asset category related to kit is not removed.
        AssetCategorySystemApi assetCategorySystemApi = getOsgiService(AssetCategorySystemApi.class);

        assetCategorySystemApi.executeTransaction(TransactionType.Required, (em -> {
            AssetCategory categoryInTransaction = assetCategorySystemApi.find(categoryId,null);
            Set<AssetCategoryResource> categoryResources = categoryInTransaction.getResources();
            assertEquals(1,categoryResources.size());
            AssetCategoryResource resourceRelatedToKit = new ArrayList<>(categoryResources).get(0);
            assertEquals(resourceRelatedToKit.getResourceName(),Kit.class.getName());
            assertEquals(resourceRelatedToKit.getResourceId(),savedKitId);
        }));
    }

    @Test
    public void test_016_should_getKitsFromDiagnoseType_works()  {
        //Create  user
        //Create HProject
        //Create a kit relative to this project.
        //Save kit
        //Create DiagnoseType template
        //Save diagnoseType
        //Add kit to diagnoseType category
        //Retrieve DiagnoseType's kit
        //Assert that the semantic of the operation is how we expected.

        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser projectOwner = (HUser) authService.login("hadmin", "admin");

        //Save project
        HProject project = saveNewHProject(projectOwner, "admin");
        //Create kit related to this project
        Kit kit = createKitTemplate("kitLabel", project.getId());
        addDeviceTemplateToKit(kit, createHDeviceTemplate("hdeviceLabel"));

        KitRestApi kitRestService = getOsgiService(KitRestApi.class);
        impersonateUser(kitRestService, projectOwner);

        Response saveKitResponse = kitRestService.saveKit(kit);
        assertEquals(200, saveKitResponse.getStatus());
        Kit savedKit = (Kit) saveKitResponse.getEntity();
        assertNotNull(savedKit);
        assertNotEquals(0, savedKit.getId());
        long savedKitId = savedKit.getId();

        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(projectOwner);

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, projectOwner);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType = (DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        long diagnoseTypeId = savedDiagnoseType.getId();
        assertNotNull(savedDiagnoseType.getCategory());


        //Add kit to diagnoseType category
        Response addKitToDiagnoseTypeResponse = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(diagnoseTypeId, savedKitId);
        assertEquals(200, addKitToDiagnoseTypeResponse.getStatus());

        Response findDiagnoseTypeKitsResponse = diagnoseTypeRestService.getKitsFromDiagnoseType(diagnoseTypeId);
        assertEquals(200,findDiagnoseTypeKitsResponse.getStatus());
        Collection<Kit> diagnoseTypeKitsList = (Collection<Kit>) findDiagnoseTypeKitsResponse.getEntity();
        assertNotNull(diagnoseTypeKitsList);
        assertEquals(diagnoseTypeKitsList.size(),1);
        assertEquals(new ArrayList<>(diagnoseTypeKitsList).get(0).getId(),savedKitId);

    }

    @Test
    public void test_017_should_getKitsFromDiagnoseType_return200Status_whenDiagnoseTypeKitsListIsEmpty()  {
        //Create generic  user
        //Create diagnose type
        //Save diagnose type
        //Load kit related to the diagnose type(Not kit is related no the diagnose type)
        //Assert that response return 200 status.

        HUser genericUser = registerHUserAndActivateAccount();


        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(genericUser);

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType = (DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        long diagnoseTypeId = savedDiagnoseType.getId();

        Response findDiagnoseTypeKitsResponse = diagnoseTypeRestService.getKitsFromDiagnoseType(diagnoseTypeId);
        assertEquals(200,findDiagnoseTypeKitsResponse.getStatus());
        Collection<Kit> diagnoseTypeKitsList =(Collection<Kit>) findDiagnoseTypeKitsResponse.getEntity();
        assertNotNull(diagnoseTypeKitsList);
        assertTrue(diagnoseTypeKitsList.isEmpty());
    }

    @Test
    public void test_018_should_getKitsFromDiagnoseType_isNotAuthorized_whenUserTryToFindKitsRelatedToDiagnoseTypeSuchThatHeNotOwns() {
        //Create  user
        //Create HProject
        //Create a kit relative to this project.
        //Save kit
        //Create DiagnoseType template
        //Save diagnoseType
        //Add a kit to the diagnose type
        //Log as another user
        //Assert that the user is not authorized to find the kit related to a diagnose type such that this diagnose type is owned by another user.

        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser projectOwner = (HUser) authService.login("hadmin", "admin");

        //Save project
        HProject project = saveNewHProject(projectOwner, "admin");
        //Create kit related to this project
        Kit kit = createKitTemplate("kitLabel", project.getId());
        addDeviceTemplateToKit(kit, createHDeviceTemplate("hdeviceLabel"));

        KitRestApi kitRestService = getOsgiService(KitRestApi.class);
        impersonateUser(kitRestService, projectOwner);

        Response saveKitResponse = kitRestService.saveKit(kit);
        assertEquals(200, saveKitResponse.getStatus());
        Kit savedKit = (Kit) saveKitResponse.getEntity();
        assertNotNull(savedKit);
        assertNotEquals(0, savedKit.getId());
        long savedKitId = savedKit.getId();

        DiagnoseType diagnoseType = createDiagnoseTypeTemplate(projectOwner);

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, projectOwner);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(diagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType = (DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        long diagnoseTypeId = savedDiagnoseType.getId();
        assertNotNull(savedDiagnoseType.getCategory());

        //Add kit to diagnoseType category
        Response addKitToDiagnoseTypeResponse = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(diagnoseTypeId, savedKitId);
        assertEquals(200, addKitToDiagnoseTypeResponse.getStatus());

        HUser genericUser = registerHUserAndActivateAccount();

        impersonateUser(diagnoseTypeRestService,genericUser);

        Response findDiagnoseTypeKitsResponse = diagnoseTypeRestService.getKitsFromDiagnoseType(diagnoseTypeId);
        assertEquals(403,findDiagnoseTypeKitsResponse.getStatus());

    }

    @Test
    public void test_019_should_getDiagnoseTypeFromKit_works(){
        //Create  user
        //Create HProject
        //Create a kits relative to this project.
        //Save kit
        //Create two DiagnoseType template
        //Save diagnoseTypes
        //Add diagnose types to kit's diagnose type list
        //Retrieve Kit's diagnoseTypes list
        //Assert that the semantic of the operation is how we expected.

        HUser genericUser = registerHUserAndActivateAccount();

        //Save project
        HProject project = saveNewHProject(genericUser, USER_PASSWORD);
        //Create kit related to this project
        Kit kit = createKitTemplate("kitLabel", project.getId());
        addDeviceTemplateToKit(kit, createHDeviceTemplate("hdeviceLabel"));


        KitRestApi kitRestService = getOsgiService(KitRestApi.class);
        impersonateUser(kitRestService, genericUser);

        Response saveKitResponse = kitRestService.saveKit(kit);
        assertEquals(200, saveKitResponse.getStatus());
        Kit savedKit = (Kit) saveKitResponse.getEntity();
        assertNotNull(savedKit);
        assertNotEquals(0, savedKit.getId());
        long savedKitId = savedKit.getId();


        DiagnoseType firstDiagnoseType = createDiagnoseTypeTemplate(genericUser,"first");

        DiagnoseType secondDiagnoseType = createDiagnoseTypeTemplate(genericUser,"second");

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService, genericUser);

        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(firstDiagnoseType);
        assertEquals(200, saveDiagnoseTypeResponse.getStatus());
        DiagnoseType firstSavedDiagnoseType = (DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        long firstSavedDiagnoseTypeId = firstSavedDiagnoseType.getId();

        Response secondDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(secondDiagnoseType);
        assertEquals(200, secondDiagnoseTypeResponse.getStatus());
        DiagnoseType secondSavedDiagnoseType = (DiagnoseType) secondDiagnoseTypeResponse.getEntity();
        long secondSavedDiagnoseTypeId = secondSavedDiagnoseType.getId();

        List<Long> expectedDiagnoseTypeIds = new LinkedList<>();
        expectedDiagnoseTypeIds.add(firstSavedDiagnoseTypeId);
        expectedDiagnoseTypeIds.add(secondSavedDiagnoseTypeId);


        //Add first diagnose type to kit.
        Response addFirstDiagnoseTypeToKitResponse = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(firstSavedDiagnoseTypeId, savedKitId);
        assertEquals(200, addFirstDiagnoseTypeToKitResponse.getStatus());

        //Add second diagnose type to kit.
        Response addSecondDiagnoseTypeToKitResponse = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(secondSavedDiagnoseTypeId, savedKitId);
        assertEquals(200, addSecondDiagnoseTypeToKitResponse.getStatus());

        //Find
        Response findDiagnoseTypeKitsResponse = diagnoseTypeRestService.getDiagnoseTypeFromKit(savedKitId);
        assertEquals(200,findDiagnoseTypeKitsResponse.getStatus());
        Collection<DiagnoseType> kitDiagnoseTypesList = (Collection<DiagnoseType>) findDiagnoseTypeKitsResponse.getEntity();
        assertNotNull(kitDiagnoseTypesList);
        assertEquals(kitDiagnoseTypesList.size(),expectedDiagnoseTypeIds.size());
        assertTrue(kitDiagnoseTypesList.stream().map(DiagnoseType::getId).collect(Collectors.toList()).containsAll(expectedDiagnoseTypeIds));
    }

    @Test
    public void test_020_should_getDiagnoseTypeFromKit_return200Status_whenKitDiagnoseTypesListIsEmpty(){
        //Create  user
        //Create HProject
        //Create a kits relative to this project.
        //Save kit
        //Retrieve Kit's diagnoseTypes list
        //Assert that the response's status is 200.

        HUser genericUser = registerHUserAndActivateAccount();

        //Save project
        HProject project = saveNewHProject(genericUser, USER_PASSWORD);
        //Create kit related to this project
        Kit kit = createKitTemplate("kitLabel", project.getId());
        addDeviceTemplateToKit(kit, createHDeviceTemplate("hdeviceLabel"));


        KitRestApi kitRestService = getOsgiService(KitRestApi.class);
        impersonateUser(kitRestService, genericUser);

        Response saveKitResponse = kitRestService.saveKit(kit);
        assertEquals(200, saveKitResponse.getStatus());
        Kit savedKit = (Kit) saveKitResponse.getEntity();
        assertNotNull(savedKit);
        assertNotEquals(0, savedKit.getId());
        long savedKitId = savedKit.getId();

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService,genericUser);

        Response findKitDiagnoseTypesListResponse = diagnoseTypeRestService.getDiagnoseTypeFromKit(savedKitId);
        assertEquals(200,findKitDiagnoseTypesListResponse.getStatus());
        Collection<DiagnoseType> kitDiagnoseTypesList = (Collection<DiagnoseType>) findKitDiagnoseTypesListResponse.getEntity();
        assertNotNull(kitDiagnoseTypesList);
        assertTrue(kitDiagnoseTypesList.isEmpty());

    }

    @Test
    public void test_021_should_getDiagnoseTypeFromKit_isAuthorizedToRetrieveDiagnoseTypeRelatedToASystemKit(){
        //Log ad admin  user
        //Save a system kit
        //Add diagnose type to the system kit
        //Create a generic user
        //Log as generic user
        //Asser that the generic user it authorized retrieve the diagnose type related to the system kiit

        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser adminUser = (HUser) authService.login("hadmin", "admin");

        Kit systemKit = createKitTemplate("kitLabel", KitUtils.SYSTEM_KIT_PROJECT_ID);
        addDeviceTemplateToKit(systemKit, createHDeviceTemplate("hdeviceLabel"));


        KitRestApi kitRestService = getOsgiService(KitRestApi.class);
        impersonateUser(kitRestService, adminUser);

        Response saveKitResponse = kitRestService.saveKit(systemKit);
        assertEquals(200, saveKitResponse.getStatus());
        Kit savedKit = (Kit) saveKitResponse.getEntity();
        assertNotNull(savedKit);
        assertNotEquals(0, savedKit.getId());
        long savedKitId = savedKit.getId();

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService,adminUser);

        DiagnoseType systemKitDiagnoseType = createDiagnoseTypeTemplate(adminUser);
        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(systemKitDiagnoseType);
        assertEquals(200,saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType = (DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        assertNotNull(savedDiagnoseType);
        assertNotEquals(savedDiagnoseType.getId(),0);
        long savedDiagnoseTypeId = savedDiagnoseType.getId();

        Response addDiagnoseTypeToKitResponse = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(savedDiagnoseTypeId,savedKitId);
        assertEquals(200,addDiagnoseTypeToKitResponse.getStatus());


        HUser genericUser = registerHUserAndActivateAccount();

        impersonateUser(diagnoseTypeRestService,genericUser);

        Response findKitDiagnoseTypesListResponse = diagnoseTypeRestService.getDiagnoseTypeFromKit(savedKitId);
        assertEquals(200,findKitDiagnoseTypesListResponse.getStatus());
        Collection<DiagnoseType> kitDiagnoseTypesList = (Collection<DiagnoseType>) findKitDiagnoseTypesListResponse.getEntity();
        assertNotNull(kitDiagnoseTypesList);
        assertEquals(1,kitDiagnoseTypesList.size());
        assertEquals(new ArrayList<>(kitDiagnoseTypesList).get(0).getId(),savedDiagnoseTypeId);

    }

    @Test
    public void test_022_should_getDiagnoseTypeFromKit_isAuthorizedToRetrieveDiagnoseTypeRelatedToAKitSuchThatKitProjectIsSharedWithUser(){
        //Log ad admin  user
        //Save a  kit
        //Add diagnose type to the system kit
        //Create a generic user
        //Share project with this user
        //Log as generic user
        //Assert that the generic user it authorized retrieve the diagnose type related to this kit.

        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = saveNewHProject(adminUser,"admin");

        Kit kit = createKitTemplate("kitLabel", project.getId());
        addDeviceTemplateToKit(kit, createHDeviceTemplate("hdeviceLabel"));


        KitRestApi kitRestService = getOsgiService(KitRestApi.class);
        impersonateUser(kitRestService, adminUser);

        Response saveKitResponse = kitRestService.saveKit(kit);
        assertEquals(200, saveKitResponse.getStatus());
        Kit savedKit = (Kit) saveKitResponse.getEntity();
        assertNotNull(savedKit);
        assertNotEquals(0, savedKit.getId());
        long savedKitId = savedKit.getId();

        DiagnoseTypeRestApi diagnoseTypeRestService = getOsgiService(DiagnoseTypeRestApi.class);
        impersonateUser(diagnoseTypeRestService,adminUser);

        DiagnoseType systemKitDiagnoseType = createDiagnoseTypeTemplate(adminUser);
        Response saveDiagnoseTypeResponse = diagnoseTypeRestService.saveDiagnoseType(systemKitDiagnoseType);
        assertEquals(200,saveDiagnoseTypeResponse.getStatus());
        DiagnoseType savedDiagnoseType = (DiagnoseType) saveDiagnoseTypeResponse.getEntity();
        assertNotNull(savedDiagnoseType);
        assertNotEquals(savedDiagnoseType.getId(),0);
        long savedDiagnoseTypeId = savedDiagnoseType.getId();

        Response addDiagnoseTypeToKitResponse = diagnoseTypeRestService.addKitToDiagnoseTypeCategory(savedDiagnoseTypeId,savedKitId);
        assertEquals(200,addDiagnoseTypeToKitResponse.getStatus());

        HUser genericUser = registerHUserAndActivateAccount();

        shareHProjectWithUser(project,adminUser,"admin",genericUser);

        impersonateUser(diagnoseTypeRestService,genericUser);

        Response findKitDiagnoseTypesListResponse = diagnoseTypeRestService.getDiagnoseTypeFromKit(savedKitId);
        assertEquals(200,findKitDiagnoseTypesListResponse.getStatus());
        Collection<DiagnoseType> kitDiagnoseTypesList = (Collection<DiagnoseType>) findKitDiagnoseTypesListResponse.getEntity();
        assertNotNull(kitDiagnoseTypesList);
        assertEquals(1,kitDiagnoseTypesList.size());
        assertEquals(new ArrayList<>(kitDiagnoseTypesList).get(0).getId(),savedDiagnoseTypeId);

    }



    @After
    public void afterTest(){
        AreaDeviceRepository areaDeviceRepository = getOsgiService(AreaDeviceRepository.class);
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        HDeviceSystemApi hDeviceSystemApi = getOsgiService(HDeviceSystemApi.class);
        HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
        KitSystemApi kitSystemApi = getOsgiService(KitSystemApi.class);
        SharedEntitySystemApi sharedEntitySystemApi = getOsgiService(SharedEntitySystemApi.class);
        PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
        RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
        HUserSystemApi hUserSystemApi = getOsgiService(HUserSystemApi.class);
        DiagnoseTypeSystemApi diagnoseTypeSystemApi = getOsgiService(DiagnoseTypeSystemApi.class);

        HyperIoTTestUtils.truncateTables(areaDeviceRepository, null);
        HyperIoTTestUtils.truncateTables(areaSystemApi, null);
        HyperIoTTestUtils.truncateTables(hDeviceSystemApi, null);
        HyperIoTTestUtils.truncateTables(hProjectSystemApi, null);
        HyperIoTTestUtils.truncateTables(kitSystemApi,null);
        HyperIoTTestUtils.truncateTables(sharedEntitySystemApi, null);
        HyperIoTTestUtils.truncateTables(diagnoseTypeSystemApi,null);

        HyperIoTPermissionTestUtil.dropPermissions(roleSystemApi,permissionSystemApi);
        HyperIoTHUserTestUtils.truncateRoles(roleSystemApi);
        HyperIoTHUserTestUtils.truncateHUsers(hUserSystemApi);
    }


    private Kit createKitTemplate(String kitLabelPrefix, long projectId){
        Kit kit = new Kit();
        String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
        kit.setLabel(kitLabelPrefix.concat(randomUUID));
        kit.setKitVersion(randomUUID);
        kit.setProjectId(projectId);
        kit.setDevices(new LinkedList<>());
        return kit;
    }

    private HDeviceTemplate createHDeviceTemplate(String hDeviceLabelPrefix){
        HDeviceTemplate hDeviceTemplate = new HDeviceTemplate();
        String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
        hDeviceTemplate.setDeviceLabel(hDeviceLabelPrefix.concat(randomUUID));
        hDeviceTemplate.setBrand("brand".concat(randomUUID));
        hDeviceTemplate.setModel("model".concat(randomUUID));
        hDeviceTemplate.setFirmwareVersion("firmwareversion".concat(randomUUID));
        hDeviceTemplate.setSoftwareVersion("softwareversion".concat(randomUUID));
        hDeviceTemplate.setDescription("description".concat(randomUUID));
        hDeviceTemplate.setPackets(new LinkedList<>());
        return hDeviceTemplate;
    }

    private void addDeviceTemplateToKit(Kit kit, HDeviceTemplate device){
        kit.getDevices().add(device);
    }

    private DiagnoseType cloneDiagnoseType(DiagnoseType diagnoseType ) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonKit = mapper.writeValueAsString(diagnoseType);
        return mapper.readValue(jsonKit,DiagnoseType.class);
    }

    private static  DiagnoseType createDiagnoseTypeTemplate(HUser owner){
        DiagnoseType diagnoseType = new DiagnoseType();
        String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
        diagnoseType.setLabel("label".concat(randomUUID));
        diagnoseType.setUserOwner(owner);
        return diagnoseType;
    }

    private static  DiagnoseType createDiagnoseTypeTemplate(HUser owner, String labelPrefix){
        DiagnoseType diagnoseType = new DiagnoseType();
        String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
        diagnoseType.setLabel(labelPrefix.concat(randomUUID));
        diagnoseType.setUserOwner(owner);
        return diagnoseType;
    }

    private HUser registerHUserAndActivateAccount() {
        HUser registeredUser = registerNewHUserForTest();
        activateHUserAccount(registeredUser.getId());
        return registeredUser;
    }

    private HUser registerNewHUserForTest() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HUser userToRegister = createHUserTemplate();
        this.impersonateUser(huserRestService, adminUser);
        Response restResponse = huserRestService.register(userToRegister);
        Assert.assertEquals(200, restResponse.getStatus());
        HUser registeredUser = (HUser) restResponse.getEntity();
        Assert.assertNotNull(registeredUser);
        Assert.assertNotEquals(0, (registeredUser.getId()));
        Assert.assertFalse(registeredUser.isActive());
        return registeredUser;
    }

    private void activateHUserAccount(long userId) {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        HUserRepository hUserRepository = getOsgiService(HUserRepository.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(huserRestService, adminUser);
        HUser userToActivate = hUserRepository.find(userId, null);
        Response restResponse = huserRestService.activate(userToActivate.getEmail(), userToActivate.getActivateCode());
        assertEquals(200, restResponse.getStatus());
    }

    private HUser createHUserTemplate(){
        HUser huser = new HUser();
        huser.setName("name");
        huser.setLastname("lastname");
        huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
        huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
        huser.setPassword(USER_PASSWORD);
        huser.setPasswordConfirm(USER_PASSWORD);
        return huser;
    }

    private HProject saveNewHProject(HUser user, String userPassword) {
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser userLogged = (HUser) authService.login(user.getUsername(), userPassword);
        this.impersonateUser(hProjectRestService, userLogged);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Project of user: " + userLogged.getUsername());
        hproject.setUser((HUser) userLogged);
        Response restResponse = hProjectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals("Project of user: " + userLogged.getUsername(),
                ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(userLogged.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
        return hproject;
    }

    private void shareHProjectWithUser(HProject project, HUser projectOwner, String projectOwnerPassword, HUser sharingUser){
        SharedEntityRestApi sharedEntityRestService = getOsgiService(SharedEntityRestApi.class);
        //Log as project owener.
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser userLogged = (HUser) authService.login(projectOwner.getUsername(), projectOwnerPassword);
        this.impersonateUser(sharedEntityRestService,projectOwner);
        //Share project with sharing user.
        SharedEntity sharedEntity = new SharedEntity();
        sharedEntity.setEntityId(project.getId());
        sharedEntity.setEntityResourceName(HPROJECT_RESOURCE_NAME);
        sharedEntity.setUserId(sharingUser.getId());
        Response restResponse = sharedEntityRestService.saveSharedEntity(sharedEntity);
        assertEquals(200, restResponse.getStatus());
        assertEquals(project.getId(), ((SharedEntity) restResponse.getEntity()).getEntityId());
        assertEquals(sharingUser.getId(), ((SharedEntity) restResponse.getEntity()).getUserId());
        assertEquals(HPROJECT_RESOURCE_NAME, ((SharedEntity) restResponse.getEntity()).getEntityResourceName());
    }

    public static final String HPROJECT_RESOURCE_NAME=HProject.class.getName();

    public static final String USER_PASSWORD= "passwordPass&01";

    private String getHyperIoTValidationExceptionClassName(){
        return HyperIoTValidationException.class.getName();
    }

    private String getHyperIoTDuplicateEntityExceptionClassName(){return HyperIoTDuplicateEntityException.class.getName();}

    private String getMaliciousValue(){
        return "<script>console.log('hello')</script>";
    }

}
