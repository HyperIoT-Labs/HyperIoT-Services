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

package it.acsoftware.hyperiot.hproject.algorithm.model;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedChildResource;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.model.HyperIoTInnerEntityJSONSerializer;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.base.validation.Pattern;
import it.acsoftware.hyperiot.hproject.model.HProjectJSONView;
import it.acsoftware.hyperiot.jobscheduler.api.HyperIoTJob;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.acsoftware.hyperiot.algorithm.model.Algorithm;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.hproject.model.HProject;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import static org.quartz.JobBuilder.newJob;

/**
 * This class maps relationship between HProject and Algorithm. It is a particular instance of an Algorithm, which
 * is scheduled for a HProject.
 * @author Aristide Cittadino Model class for HProjectAlgorithm of HyperIoT platform. This
 *         class is used to map HProjectAlgorithm with the database.
 *
 */

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class HProjectAlgorithm extends HyperIoTAbstractEntity implements HyperIoTProtectedEntity, HyperIoTOwnedChildResource, HyperIoTJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(HProjectAlgorithm.class.getName());
    private static final String ALGORITHM_KEY = "algorithmId";
    private static final String CONFIG_KEY = "config";
    private static final String CRON_EXPRESSION_KEY = "cronExpression";
    private static final String PROJECT_KEY = "projectId";
    private static final String NAME_KEY = "name";
    private static final String VALID_NAME_REGEX = "[A-Za-z0-9][ .A-Za-z0-9_-]*";
    @JsonView({HProjectJSONView.Export.class,HyperIoTJSONView.Public.class})
    private String name;

    /**
     * HProject which algorithm is scheduled for
     */
    @JsonView(HyperIoTJSONView.Public.class)
    private HProject project;
    /**
     * Scheduled algorithm
     */
    @JsonView({HProjectJSONView.Export.class, HyperIoTJSONView.Public.class})
    private Algorithm algorithm;

    /**
     * Configuration of Algorithm
     */
    @JsonView({HProjectJSONView.Export.class,HyperIoTJSONView.Public.class})
    private String config;

    /**
     * Implementation of org.quartz.Job interface
     */
    @JsonView({HProjectJSONView.Export.class,HyperIoTJSONView.Public.class})
    String className;

    /**
     * Job cron expression
     */
    @JsonView({HProjectJSONView.Export.class,HyperIoTJSONView.Public.class})
    private String cronExpression;

    /**
     * Timezone
     */
    @JsonView({HProjectJSONView.Export.class,HyperIoTJSONView.Public.class})
    private String timezoneId;

    /**
     * This object contains detail of job, i.e. id, parameters and org.quartz.Job implementation
     */
    private JobDetail jobDetail;

    /**
     * Job parameters
     */
    @JsonView({HProjectJSONView.Export.class,HyperIoTJSONView.Public.class})
    private Map<String, Object> jobParams;

    /**
     * Job key
     */
    private JobKey jobKey;

    /**
     * If algorithm must be scheduled or not
     */
    @JsonView({HProjectJSONView.Export.class,HyperIoTJSONView.Public.class})
    private boolean active;

    public HProjectAlgorithm() {
        //inserted as a string to avoid direct dependency
        className = "it.acsoftware.hyperiot.hproject.algorithm.job.HProjectAlgorithmJob";
        jobParams = new HashMap<>();
    }

    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    @Pattern(regexp = VALID_NAME_REGEX)
    @Size( max = 255)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        jobParams.put(NAME_KEY, name);
    }

    /**
     * @return the related HProject
     */
    @NotNullOnPersist
    @ManyToOne(targetEntity = HProject.class)
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    public HProject getProject() {
        return project;
    }

    /**
     * @param project the related HProject
     */
    public void setProject(HProject project) {
        this.project = project;
        jobParams.put(PROJECT_KEY, String.valueOf(project.getId()));
    }

    /**
     * Get Algorithm
     * @return algorithm
     */
    @ManyToOne(targetEntity = Algorithm.class)
    @NotNullOnPersist
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Set algorithm
     * @param algorithm algorithm
     */
    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
        AlgorithmUtil util = this.getAlgorithmUtilFromOsgi();
        jobParams.put(ALGORITHM_KEY, String.valueOf(algorithm.getId()));
        // Spark hidden REST api needs these parameters
        jobParams.put("mainClass", algorithm.getMainClassname());   // Class containing Spark job main method
        jobParams.put("appResource", util.getJarFullPath(algorithm));       // Spark job jar
        jobParams.put("spark.jars", util.getJarFullPath(algorithm));        // Jars which Spark needs
    }

    /**
     * Get configuration of algorithm for this hProject
     * @return configuration of algorithm
     */
    @NoMalitiusCode
    @NotNullOnPersist
    @NotEmpty
    @Column(columnDefinition = "TEXT")
    public String getConfig() {
        return config;
    }

    /**
     * Set configuration of algorithm
     * @param config configuration of algorithm
     */
    public void setConfig(String config) {
        this.config = config;
        jobParams.put(CONFIG_KEY, config);
    }

    /**
     * Get implementation class name of org.quartz.Job interface
     * @return className
     */
    @NoMalitiusCode
    @NotNullOnPersist
    @NotEmpty
    public String getClassName() {
        return className;
    }

    /**
     * Set implementation class name of org.quartz.Job interface
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Get cron expression
     * @return cron expression
     */
    @NoMalitiusCode
    @NotNullOnPersist
    @NotEmpty
    @Size( max = 255)
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Set cron expression
     * @param cronExpression cron expression
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
        jobParams.put(CRON_EXPRESSION_KEY, cronExpression);
    }

    @NoMalitiusCode
    @NotNullOnPersist
    @NotEmpty
    @Size( max = 255)
    @Override
    public String getTimezoneId() {
        return timezoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timezoneId = timeZoneId;
    }

    @Override
    @Transient
    @JsonIgnore
    public JobDetail getJobDetail() {
        return jobDetail;
    }

    /**
     * It returns job key
     * @return job key
     */
    @Override
    @Transient
    @JsonIgnore
    public JobKey getJobKey() {
        return jobKey;
    }

    /**
     * Get job parameters
     * @return job parameters
     */
    @Override
    @Transient
    @JsonIgnore
    public Map<String, Object> getJobParams() {
        return jobParams;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Set job parameters
     * @param jobParams job parameters
     */
    public void setJobParams(Map<String, Object> jobParams) {
        this.jobParams = jobParams;
    }

    public void addJobParam(String key, Object value) {
        jobParams.put(key, value);
    }

    public Object removeJobParam(String key) {
        if (jobParams.containsKey(key))
            return jobParams.remove(key);
        return null;
    }

    @Override
    @Transient
    @JsonIgnore
    public HyperIoTBaseEntity getParent() {
        return this.project;
    }

    /**
     * This method set parameters required by HyperIoTJobScheduler in order to schedule the job
     */
    @PostLoad
    @PostPersist
    @PostUpdate
    public void loadJobConfig() {
        jobKey = new JobKey(String.valueOf(getId()));
        JobDataMap jobDataMap = new JobDataMap(jobParams);
        Class<? extends Job> jobClass;
        try {
            jobClass = (Class<? extends Job>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.error( "Could not load job configuration: {}", e);
            return;
        }
        jobDetail = newJob(jobClass)
                .withIdentity(getJobKey())
                .setJobData(jobDataMap)
                .storeDurably() // Define a durable job instance (durable jobs can exist without triggers)
                .build();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((project == null) ? 0 : project.hashCode());
        result = prime * result + ((algorithm == null) ? 0 : algorithm.hashCode());
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        result = prime * result + ((cronExpression == null) ? 0 : cronExpression.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HProjectAlgorithm other = (HProjectAlgorithm) obj;
        if (this.getId() > 0 && other.getId() > 0)
            return this.getId() == other.getId();
        if (project == null) {
            if (other.project != null)
                return false;
        } else if (!project.equals(other.project))
            return false;
        if (algorithm == null) {
            if (other.algorithm != null)
                return false;
        } else if (!algorithm.equals(other.algorithm))
            return false;
        if (config == null) {
            if (other.config != null)
                return false;
        } else if (!config.equals(other.config))
            return false;
        if (cronExpression == null) {
            if (other.cronExpression != null)
                return false;
        } else if (!cronExpression.equals(other.cronExpression))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HProjectAlgorithm{" +
                "hProject=" + project.getId() +
                ", algorithm=" + algorithm.getId() +
                ", config='" + config + '\'' +
                ", cronExpression='" + cronExpression + '\'' +
                '}';
    }

    @Transient
    @JsonIgnore
    private AlgorithmUtil getAlgorithmUtilFromOsgi(){
        BundleContext ctx = HyperIoTUtil.getBundleContext(this.getClass());
        ServiceReference<AlgorithmUtil> ref = ctx.getServiceReference(AlgorithmUtil.class);
        if(ref != null)
            return ctx.getService(ref);
        throw new HyperIoTRuntimeException("Impossibile to retrieve AlgorithmUtil check installed bundles!");
    }


}
