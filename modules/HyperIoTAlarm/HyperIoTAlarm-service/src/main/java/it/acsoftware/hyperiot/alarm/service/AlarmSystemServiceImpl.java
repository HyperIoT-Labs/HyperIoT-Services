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

package it.acsoftware.hyperiot.alarm.service;

import it.acsoftware.hyperiot.alarm.api.AlarmRepository;
import it.acsoftware.hyperiot.alarm.api.AlarmSystemApi;
import it.acsoftware.hyperiot.alarm.event.api.AlarmEventSystemApi;
import it.acsoftware.hyperiot.alarm.event.model.*;
import it.acsoftware.hyperiot.alarm.model.Alarm;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.facts.FiredRule;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.*;

/**
 * @author Aristide Cittadino Implementation class of the AlarmSystemApi
 * interface. This  class is used to implements all additional
 * methods to interact with the persistence layer.
 */
@Component(service = AlarmSystemApi.class, immediate = true)
public final class AlarmSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<Alarm> implements AlarmSystemApi {

    /**
     * Injecting the AlarmRepository to interact with persistence layer
     */
    private AlarmRepository repository;

    /**
     * Injecting the PermissionSystemApi to interact with permission layer
     */
    private PermissionSystemApi permissionSystemApi;

    private AlarmEventSystemApi alarmEventSystemApi;
    private RuleEngineSystemApi ruleEngineSystemApi;

    /**
     * Constructor for a AlarmSystemServiceImpl
     */
    public AlarmSystemServiceImpl() {
        super(Alarm.class);
    }

    /**
     * Return the current repository
     */
    protected AlarmRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}", this.repository);
        return repository;
    }

    /**
     * @param alarmRepository The current value of AlarmRepository to interact with persistence layer
     */
    @Reference
    public void setRepository(AlarmRepository alarmRepository) {
        getLog().debug("invoking setRepository, setting: {}", alarmRepository);
        this.repository = alarmRepository;
    }

    @Reference
    public void setAlarmEventSystemApi(AlarmEventSystemApi alarmEventSystemApi) {
        this.alarmEventSystemApi = alarmEventSystemApi;
    }

    @Reference
    public void setRuleEngineSystemApi(RuleEngineSystemApi ruleEngineSystemApi) {
        this.ruleEngineSystemApi = ruleEngineSystemApi;
    }


    public PermissionSystemApi getPermissionSystemApi() {
        getLog().debug("invoking getPermissionSystemApi, returning: {}", this.permissionSystemApi);
        return this.permissionSystemApi;
    }

    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

    @Override
    public Alarm update(Alarm entity, HyperIoTContext ctx) {
        Alarm dbEntity = null;
        try {
            dbEntity = this.find(entity.getId(), ctx);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        boolean alarmNameChange = dbEntity.getName().equals(entity.getName());
        Alarm alarm = super.update(entity, ctx);
        alarm = this.find(entity.getId(), ctx);
        if (alarmNameChange) {
            for (AlarmEvent event : alarm.getAlarmEventList()) {
                this.alarmEventSystemApi.update(event, ctx);
            }
        }
        return alarm;
    }

    @Override
    public Set<Alarm> findAlarmByProjectId(HyperIoTContext hyperIoTContext, long projectId) {
        Collection<Rule> projectRules = this.ruleEngineSystemApi.findAllRuleByProjectId(projectId);
        if (projectRules == null || projectRules.isEmpty())
            return new HashSet<>();
        HyperIoTQuery byRuleId = HyperIoTQueryBuilder.newQuery();
        boolean firstRule = true;
        for (Rule rule : projectRules) {
            if (firstRule) {
                byRuleId = byRuleId.equals("event", rule.getId());
                firstRule = false;
            } else {
                byRuleId = byRuleId.or(HyperIoTQueryBuilder.newQuery().equals("event", rule.getId()));
            }
        }
        Collection<AlarmEvent> alarmEvents = this.alarmEventSystemApi.findAll(byRuleId, hyperIoTContext);
        Set<Alarm> alarm = new HashSet<>();
        alarmEvents.forEach((event) -> alarm.add(event.getAlarm()));
        return alarm;
    }


    @Override
    public Alarm saveAlarmAndEvents(Alarm alarm, Collection<AlarmEvent> alarmEvents, HyperIoTContext ctx) {
        return repository.executeTransactionWithReturn(TransactionType.Required, (em -> {
            Alarm savedAlarm = save(alarm, null);
            if (alarmEvents != null && !alarmEvents.isEmpty()) {
                for (AlarmEvent alarmEvent : alarmEvents) {
                    alarmEvent.setAlarm(alarm);
                    AlarmEvent alarmEv = alarmEventSystemApi.save(alarmEvent, ctx);
                    savedAlarm.getAlarmEventList().add(alarmEv);
                }
            }
            return savedAlarm;
        }));
    }

    public ProjectsAlarmsStatus getProjectsAlarmStatuses(long[] projectIds) {
        List<ProjectAlarmsStatus> projectAlarmsStatuses = new ArrayList<>();
        for (int i = 0; i < projectIds.length; i++) {
            List<AlarmStatus> alarmStatuses = new ArrayList<>();
            long projectId = projectIds[i];
            Set<Alarm> alarms = this.findAlarmByProjectId(null, projectId);
            alarms.forEach(alarm -> {
                Collection<AlarmEvent> alarmEvents = this.alarmEventSystemApi.findAllEventsByAlarmId(alarm.getId());
                List<AlarmEventStatus> alarmEventStatuses = new ArrayList<>();
                alarmEvents.forEach(alarmEvent -> {
                    FiredRule firedRule = alarmEventSystemApi.getFiredRule(projectId, alarmEvent.getEvent().getId());
                    Date firedTimestamp = (firedRule != null && firedRule.getLastFiredTimestamp() != null) ? Date.from(firedRule.getLastFiredTimestamp().toInstant()) : null;
                    boolean isFired = firedRule != null && firedRule.isFired();
                    AlarmEventStatus alarmEventStatus = new AlarmEventStatus(alarmEvent, firedTimestamp, isFired);
                    alarmEventStatuses.add(alarmEventStatus);
                });
                alarmStatuses.add(new AlarmStatus(alarm.getId(), alarm.getName(), alarmEventStatuses));
            });
            projectAlarmsStatuses.add(new ProjectAlarmsStatus(projectId, alarmStatuses));
        }
        return new ProjectsAlarmsStatus(projectAlarmsStatuses);
    }

    @Activate
    public void onActivate() {
        this.checkRegisteredUserRoleExist();
    }

    private void checkRegisteredUserRoleExist() {
        String alarmResourceName = Alarm.class.getName();
        List<HyperIoTAction> alarmCrudAction = HyperIoTActionsUtil.getHyperIoTCrudActions(alarmResourceName);
        this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, alarmCrudAction);
    }

}
