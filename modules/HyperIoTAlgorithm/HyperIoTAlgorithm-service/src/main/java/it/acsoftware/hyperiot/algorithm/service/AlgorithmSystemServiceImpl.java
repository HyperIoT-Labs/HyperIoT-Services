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

package it.acsoftware.hyperiot.algorithm.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmUtil;
import it.acsoftware.hyperiot.algorithm.model.*;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.exception.HyperIoTDuplicateEntityException;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.algorithm.api.AlgorithmSystemApi;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmRepository;

import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;

import javax.persistence.NoResultException;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

/**
 * @author Aristide Cittadino Implementation class of the AlgorithmSystemApi
 * interface. This  class is used to implements all additional
 * methods to interact with the persistence layer.
 */
@Component(service = AlgorithmSystemApi.class, immediate = true)
public final class AlgorithmSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<Algorithm>
        implements AlgorithmSystemApi {

    /**
     * Injecting the AlgorithmRepository to interact with persistence layer
     */
    private AlgorithmRepository repository;

    private AlgorithmUtil algorithmUtil;

    private PermissionSystemApi permissionSystemApi;

    /**
     * Constructor for a AlgorithmSystemServiceImpl
     */
    public AlgorithmSystemServiceImpl() {
        super(Algorithm.class);
    }

    /**
     * Return the current repository
     */
    protected AlgorithmRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}", this.repository);
        return repository;
    }

    /**
     * @param algorithmRepository The current value of AlgorithmRepository to interact with persistence layer
     */
    @Reference
    protected void setRepository(AlgorithmRepository algorithmRepository) {
        getLog().debug("invoking setRepository, setting: {}", algorithmRepository);
        this.repository = algorithmRepository;
    }

    @Activate
    public void activate() {
        checkRegisteredUserRoleExists();
    }

    @Override
    public Algorithm addIOField(long algorithmId, AlgorithmIOField ioField) {
        try {
            Algorithm algorithm = repository.find(algorithmId, null);
            // Jackson deserialization
            ObjectMapper objectMapper = new ObjectMapper();
            AlgorithmConfig baseConfig = objectMapper.readValue(algorithm.getBaseConfig(), AlgorithmConfig.class);

            // check what kind of field (input or output) has to be added
            List<AlgorithmIOField> ioFieldList = ioField.getType().equals(AlgorithmFieldType.INPUT) ?
                    baseConfig.getInput() : baseConfig.getOutput();
            if (ioFieldList == null) {
                // create if it has not been created yet
                ioFieldList = new ArrayList<>();
            } else {
                // check duplicate
                for (AlgorithmIOField x : ioFieldList)
                    if (x.equals(ioField))
                        throw new HyperIoTDuplicateEntityException(new String[]{"name"});
            }
            ioField.setId(algorithm.getEntityVersion());    // set ID to entity version
            ioFieldList.add(ioField);

            // Jackson serialization
            String baseConfigString = objectMapper.writeValueAsString(baseConfig);
            // update of algorithm
            algorithm.setBaseConfig(baseConfigString);
            return repository.update(algorithm);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        } catch (IOException e) {
            e.printStackTrace();
            throw new HyperIoTRuntimeException(e);
        }
    }

    private void checkRegisteredUserRoleExists() {
        String resourceName = Algorithm.class.getName();
        List<HyperIoTAction> actions = new ArrayList<>();
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTCrudAction.FINDALL));
        this.permissionSystemApi
                .checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
    }

    @Override
    public Algorithm deleteIOField(long algorithmId, AlgorithmFieldType fieldType, long ioFieldId) {
        try {
            Algorithm algorithm = repository.find(algorithmId, null);
            // Jackson deserialization
            ObjectMapper objectMapper = new ObjectMapper();
            AlgorithmConfig baseConfig = objectMapper.readValue(algorithm.getBaseConfig(), AlgorithmConfig.class);
            List<AlgorithmIOField> ioFieldList = fieldType.equals(AlgorithmFieldType.INPUT) ?
                    baseConfig.getInput() : baseConfig.getOutput();
            for (int i = 0; i < ioFieldList.size(); i++) {
                if (ioFieldList.get(i).getId() == ioFieldId) {
                    ioFieldList.remove(i);
                    // Jackson serialization
                    String baseConfigString = objectMapper.writeValueAsString(baseConfig);
                    // update of algorithm
                    algorithm.setBaseConfig(baseConfigString);
                    return repository.update(algorithm);
                }
            }
            throw new HyperIoTEntityNotFound();
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        } catch (IOException e) {
            e.printStackTrace();
            throw new HyperIoTRuntimeException(e);
        }
    }

    @Override
    public Algorithm updateBaseConfig(long algorithmId, AlgorithmConfig baseConfig) {
        try {
            Algorithm algorithm = repository.find(algorithmId, null);
            String baseConfigString = algorithmUtil.getBaseConfigString(baseConfig);
            algorithm.setBaseConfig(baseConfigString);
            return repository.update(algorithm);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        } catch (JsonProcessingException e) {
            throw new HyperIoTRuntimeException(e);
        }
    }

    @Override
    public Algorithm updateAlgorithmFile(long algorithmId, String mainClassname, File algorithmFile) {
        if (algorithmFile == null) {
            throw new HyperIoTRuntimeException("algorithm file cannot be null");
        }

        Algorithm algorithm;
        try {
            algorithm = repository.find(algorithmId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        algorithm.setMainClassname(mainClassname);
        validate(algorithm);
        return repository.updateAlgorithmFile(algorithm, mainClassname, algorithmFile);
    }

    @Override
    public Algorithm updateIOField(long algorithmId, AlgorithmIOField ioField) {
        try {
            Algorithm algorithm = repository.find(algorithmId, null);
            // Jackson deserialization
            ObjectMapper objectMapper = new ObjectMapper();
            AlgorithmConfig baseConfig = objectMapper.readValue(algorithm.getBaseConfig(), AlgorithmConfig.class);

            // check what kind of field (input or output) has to be updated
            List<AlgorithmIOField> ioFieldList = ioField.getType().equals(AlgorithmFieldType.INPUT) ?
                    baseConfig.getInput() : baseConfig.getOutput();

            boolean ioFieldIdExists = false;
            boolean ioFieldNameExists = false;
            long ioFieldId1 = 0L;
            long ioFieldId2 = 0L;
            String ioFieldName2 = null;
            for (AlgorithmIOField algorithmIOField : ioFieldList) {
                if (algorithmIOField.getId() == ioField.getId()) {
                    ioFieldIdExists = true;
                    ioFieldId1 = algorithmIOField.getId();
                }
                if (algorithmIOField.getName().equals(ioField.getName())) {
                    ioFieldNameExists = true;
                    ioFieldId2 = algorithmIOField.getId();
                    ioFieldName2 = algorithmIOField.getName();
                }
            }
            for (AlgorithmIOField x : ioFieldList) {
                if (ioField.getId() > 0 && ioFieldIdExists) {
                    // check duplicate
                    if (ioFieldNameExists && ioFieldId1 != ioFieldId2 && ioField.getName().contentEquals(ioFieldName2)) {
                        throw new HyperIoTDuplicateEntityException(new String[]{"name"});
                    }
                    if ((x.getName().equals(ioField.getName()) && (x.getId() == ioField.getId()))
                            || (!x.getName().equals(ioField.getName()) && (x.getId() == ioField.getId()))) {
                        // If we do not check on ID property, we do not have a way to modify properties such as name
                        x.setName(ioField.getName());
                        x.setDescription(ioField.getDescription());
                        x.setMultiplicity(ioField.getMultiplicity());
                        x.setFieldType(ioField.getFieldType());
                        // Jackson serialization
                        String baseConfigString = objectMapper.writeValueAsString(baseConfig);
                        // update of algorithm
                        algorithm.setBaseConfig(baseConfigString);
                        return repository.update(algorithm);
                    }
                } else {
                    throw new HyperIoTEntityNotFound();
                }
            }
            throw new HyperIoTEntityNotFound();
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        } catch (IOException e) {
            e.printStackTrace();
            throw new HyperIoTRuntimeException(e);
        }
    }

    @Reference
    protected void setAlgorithmUtil(AlgorithmUtil algorithmUtil) {
        this.algorithmUtil = algorithmUtil;
    }

    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

}
