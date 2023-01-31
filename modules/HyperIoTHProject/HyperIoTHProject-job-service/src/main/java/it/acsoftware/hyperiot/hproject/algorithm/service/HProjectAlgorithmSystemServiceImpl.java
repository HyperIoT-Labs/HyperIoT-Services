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

package it.acsoftware.hyperiot.hproject.algorithm.service;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;

import it.acsoftware.hyperiot.algorithm.api.AlgorithmSystemApi;
import it.acsoftware.hyperiot.algorithm.model.Algorithm;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.hproject.algorithm.actions.HProjectAlgorithmAction;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmUtil;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmConfig;

import it.acsoftware.hyperiot.hproject.api.HProjectRepository;
import it.acsoftware.hyperiot.jobscheduler.api.JobSchedulerSystemApi;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmSystemApi;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmRepository;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;

import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl ;

import javax.persistence.NoResultException;


/**
 *
 * @author Aristide Cittadino Implementation class of the HProjectAlgorithmSystemApi
 *         interface. This  class is used to implements all additional
 *         methods to interact with the persistence layer.
 */
@Component(service = HProjectAlgorithmSystemApi.class, immediate = true)
public final class HProjectAlgorithmSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<HProjectAlgorithm>   implements HProjectAlgorithmSystemApi {

	private AlgorithmSystemApi algorithmSystemApi;

	/**
	 * Injecting the HProjectAlgorithmRepository to interact with persistence layer
	 */
	private HProjectAlgorithmRepository repository;

	private HProjectRepository hProjectRepository;

	private HProjectAlgorithmUtil hProjectAlgorithmUtil;
	private JobSchedulerSystemApi jobSchedulerSystemApi;
	private PermissionSystemApi permissionSystemApi;

	/**
	 * Constructor for a HProjectAlgorithmSystemServiceImpl
	 */
	public HProjectAlgorithmSystemServiceImpl() {
		super(HProjectAlgorithm.class);
	}

	/**
	 * Return the current repository
	 */
	protected HProjectAlgorithmRepository getRepository() {
		getLog().debug( "invoking getRepository, returning: {}" , this.repository);
		return repository;
	}

	@Activate
	public void onActivate() {
		this.checkRegisteredUserRoleExists();
	}

	private void checkRegisteredUserRoleExists() {
		String resourceName = HProjectAlgorithm.class.getName();
		List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTCrudActions(resourceName);
		actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HProjectAlgorithmAction.UPDATE_CONFIG));
		this.permissionSystemApi
				.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
	}

	@Override
	public Collection<HProjectAlgorithm> findByHProjectId(long hProjectId) {
		try {
			hProjectRepository.find(hProjectId, null);
		} catch (NoResultException e) {
			throw new HyperIoTEntityNotFound();
		}
		return repository.findAllHProjectAlgorithmByHProjectId(hProjectId);
	}

	@Override
	public void remove(long id, HyperIoTContext ctx) {
		repository.executeTransaction(TransactionType.Required, (em -> {
			HProjectAlgorithm hProjectAlgorithm = find(id, null);
			jobSchedulerSystemApi.deleteJob(hProjectAlgorithm);
			super.remove(id, ctx);
		}));
	}

	@Override
	public void removeByHProjectId(long hProjectId) {
		HashMap<String, Object> params = new HashMap<>();
		params.put("hProjectId", hProjectId);
		repository.executeUpdateQuery("delete from HProjectAlgorithm hprojectalgorithm where hprojectalgorithm.project.id = :hProjectId", params);
	}

	@Override
	public HProjectAlgorithm save(HProjectAlgorithm entity, HyperIoTContext ctx) {
		return repository.executeTransactionWithReturn(TransactionType.Required, (em -> {
			HProjectAlgorithm hProjectAlgorithm = super.save(entity, ctx);
			if (hProjectAlgorithm.getJobDetail() == null) {
				String errorMessage = "Could not save entity: jobDetail was null";
				getLog().error(errorMessage);
				throw new HyperIoTRuntimeException(errorMessage);
			}
			jobSchedulerSystemApi.addJob(hProjectAlgorithm);
			return hProjectAlgorithm;
		}));
	}

	@Override
	public HProjectAlgorithm update(HProjectAlgorithm entity, HyperIoTContext ctx) {
		return repository.executeTransactionWithReturn(TransactionType.Required, (em -> {
			HProjectAlgorithm hProjectAlgorithm = super.update(entity, ctx);
			if (hProjectAlgorithm.getJobDetail() == null) {
				String errorMessage = "Could not update entity: jobDetail was null";
				getLog().error(errorMessage);
				throw new HyperIoTRuntimeException(errorMessage);
			}
			jobSchedulerSystemApi.updateJob(hProjectAlgorithm);
			return hProjectAlgorithm;
		}));
	}

	@Override
	public HProjectAlgorithm updateConfig(long hProjectAlgorithmId, HProjectAlgorithmConfig config) {
		HProjectAlgorithm hProjectAlgorithm;
		try {
			hProjectAlgorithm = repository.find(hProjectAlgorithmId, null);
			String jsonConfig = hProjectAlgorithmUtil.getConfigString(config);
			hProjectAlgorithm.setConfig(jsonConfig);
		} catch (NoResultException e) {
			throw new HyperIoTEntityNotFound();
		} catch (JsonProcessingException e) {
			throw new HyperIoTRuntimeException(e);
		}
		return repository.update(hProjectAlgorithm);
	}

	@Override
	public Algorithm findAlgorithmByName(String algorithmName) {
		HyperIoTQuery queryByName = HyperIoTQueryBuilder.newQuery().equals("name",algorithmName);
		return algorithmSystemApi.find(queryByName,null);
	}

	/**
	 * @param hProjectAlgorithmRepository The current value of HProjectAlgorithmRepository to interact with persistence layer
	 */
	@Reference
	protected void setRepository(HProjectAlgorithmRepository hProjectAlgorithmRepository) {
		getLog().debug( "invoking setRepository, setting: {}" , hProjectAlgorithmRepository);
		this.repository = hProjectAlgorithmRepository;
	}

	@Reference
	protected void setHProjectRepository(HProjectRepository hProjectRepository) {
		this.hProjectRepository = hProjectRepository;
	}

	@Reference
	protected void setHProjectAlgorithmUtil(HProjectAlgorithmUtil hProjectAlgorithmUtil) {
		this.hProjectAlgorithmUtil = hProjectAlgorithmUtil;
	}

	@Reference
	protected void setJobSchedulerSystemApi(JobSchedulerSystemApi jobSchedulerSystemApi) {
		this.jobSchedulerSystemApi = jobSchedulerSystemApi;
	}

	@Reference
	public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
		this.permissionSystemApi = permissionSystemApi;
	}

	@Reference
	protected void setAlgorithmSystemApi(AlgorithmSystemApi algorithmSystemApi) {
		this.algorithmSystemApi = algorithmSystemApi;
	}
}
