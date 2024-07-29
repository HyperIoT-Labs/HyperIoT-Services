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

import it.acsoftware.hyperiot.algorithm.actions.AlgorithmAction;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmApi;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmSystemApi;
import it.acsoftware.hyperiot.algorithm.model.Algorithm;
import it.acsoftware.hyperiot.algorithm.model.AlgorithmConfig;
import it.acsoftware.hyperiot.algorithm.model.AlgorithmFieldType;
import it.acsoftware.hyperiot.algorithm.model.AlgorithmIOField;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.io.File;


/**
 *
 * @author Aristide Cittadino Implementation class of AlgorithmApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = AlgorithmApi.class, immediate = true)
public final class AlgorithmServiceImpl extends HyperIoTBaseEntityServiceImpl<Algorithm>  implements AlgorithmApi {
	/**
	 * Injecting the AlgorithmSystemApi
	 */
	private AlgorithmSystemApi systemService;

	/**
	 * Constructor for a AlgorithmServiceImpl
	 */
	public AlgorithmServiceImpl() {
		super(Algorithm.class);
	}

	/**
	 *
	 * @return The current AlgorithmSystemApi
	 */
	protected AlgorithmSystemApi getSystemService() {
		getLog().debug( "invoking getSystemService, returning: {}" , this.systemService);
		return systemService;
	}

	/**
	 *
	 * @param algorithmSystemService Injecting via OSGi DS current systemService
	 */
	@Reference
	protected void setSystemService(AlgorithmSystemApi algorithmSystemService) {
		getLog().debug( "invoking setSystemService, setting: {}" , systemService);
		this.systemService = algorithmSystemService ;
	}


	@Override
	@AllowPermissions(actions = AlgorithmAction.Names.ADD_IO_FIELD, checkById = true, idParamIndex = 1)
	public Algorithm addIOField(HyperIoTContext context, long algorithmId, AlgorithmIOField ioField) {
		getLog().debug( "invoking addIOField, on algorithm: {}" , algorithmId);
		if (ioField.getType() == null)
			throw new HyperIoTRuntimeException("type must not be null");
		return systemService.addIOField(algorithmId, ioField);
	}

	@Override
	@AllowPermissions(actions = AlgorithmAction.Names.DELETE_IO_FIELD, checkById = true, idParamIndex = 1)
	public Algorithm deleteIOField(HyperIoTContext context, long algorithmId, AlgorithmFieldType fieldType, long ioFieldId) {
		getLog().debug( "invoking deleteIOField, on algorithm {} and ioFieldId {} of type {}" ,
				algorithmId, ioFieldId, fieldType);
		if (fieldType == null)
			throw new HyperIoTRuntimeException("type must not be null");
		return systemService.deleteIOField(algorithmId, fieldType, ioFieldId);
	}

	@Override
	@AllowPermissions(actions = AlgorithmAction.Names.READ_BASE_CONFIG, checkById = true, idParamIndex = 1)
	public String getBaseConfig(HyperIoTContext context, long algorithmId) {
		getLog().debug( "invoking getBaseConfig, on algorithm: {}" , algorithmId);
		Algorithm algorithm;
		try {
			algorithm = systemService.find(algorithmId, context);
		} catch (NoResultException e) {
			throw new HyperIoTEntityNotFound();
		}
		return algorithm.getBaseConfig();
	}

	@Override
	@AllowPermissions(actions = AlgorithmAction.Names.UPDATE_BASE_CONFIG, checkById = true, idParamIndex = 1)
	public Algorithm updateBaseConfig(HyperIoTContext context, long algorithmId, AlgorithmConfig baseConfig) {
		getLog().debug( "invoking updateBaseConfig, on algorithm: {}" , algorithmId);
		try {
			systemService.find(algorithmId, context);
		} catch (NoResultException e) {
			throw new HyperIoTEntityNotFound();
		}
		if (baseConfig == null)
			throw new HyperIoTRuntimeException("baseConfig must not be null");
		return systemService.updateBaseConfig(algorithmId, baseConfig);
	}

	@Override
	@AllowPermissions(actions = AlgorithmAction.Names.UPDATE_JAR, checkById = true, idParamIndex = 1)
	public Algorithm updateAlgorithmFile(HyperIoTContext context, long algorithmId, String mainClassname, File algorithmFile) {
		getLog().debug( "invoking updateJar, on algorithm: {}" , algorithmId);
		return systemService.updateAlgorithmFile(algorithmId, mainClassname, algorithmFile);
	}

	@Override
	@AllowPermissions(actions = AlgorithmAction.Names.UPDATE_IO_FIELD, checkById = true, idParamIndex = 1)
	public Algorithm updateIOField(HyperIoTContext context, long algorithmId, AlgorithmIOField ioField) {
		getLog().debug( "invoking updateIOField, on algorithm: {}" , algorithmId);
		return systemService.updateIOField(algorithmId, ioField);
	}

}
