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

package it.acsoftware.hyperiot.hproject.algorithm.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerUtil;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorUtil;
import it.acsoftware.hyperiot.sparkmanager.job.HyperIoTSparkJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HProjectAlgorithmJob extends HyperIoTSparkJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(HProjectAlgorithmJob.class.getName());
    private static final String EMPTY_STRING = "";
    private static final String WARNING_MESSAGE = "Job {} is going to be fired with empty value for parameter {}";

    public HProjectAlgorithmJob() {}

    @Override
    public String[] getAppArgs(JobDetail jobDetail) {
        JobKey jobKey = jobDetail.getKey();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        String projectId = getJobArg(jobDataMap, jobKey, "projectId");
        String algorithmId = getJobArg(jobDataMap, jobKey, "algorithmId");
        String name = getJobArg(jobDataMap, jobKey, "name");
        String jobConfig = getJobArg(jobDataMap, jobKey, "config");
        String hadoopConfig = getHadoopConfig(jobKey);
        return new String[] {projectId, algorithmId, name, hadoopConfig, jobConfig};
    }

    /**
     * This method returns a json string containing configuration parameters about HDFS and HBase
     * @param jobKey JobKey
     * @return Json string
     */
    private String getHadoopConfig(JobKey jobKey) {
        HBaseConnectorUtil hBaseConnectorUtil =
                (HBaseConnectorUtil) HyperIoTUtil.getService(HBaseConnectorUtil.class);
        HadoopManagerUtil hadoopManagerUtil =
                (HadoopManagerUtil) HyperIoTUtil.getService(HadoopManagerUtil.class);
        Map<String, String> hadoopConfig = new HashMap<>();
        hadoopConfig.put("fsDefaultFs", hadoopManagerUtil.getDefaultFS());
        hadoopConfig.put("hbaseClusterDistributed", String.valueOf(hBaseConnectorUtil.getClusterDistributed()));
        hadoopConfig.put("hbaseMaster", hBaseConnectorUtil.getMaster());
        hadoopConfig.put("hbaseMasterHostname", hBaseConnectorUtil.getMasterHostname());
        hadoopConfig.put("hbaseMasterInfoPort", String.valueOf(hBaseConnectorUtil.getMasterInfoPort()));
        hadoopConfig.put("hbaseMasterPort", String.valueOf(hBaseConnectorUtil.getMasterPort()));
        hadoopConfig.put("hbaseRegionserverInfoPort", String.valueOf(hBaseConnectorUtil.getRegionserverInfoPort()));
        hadoopConfig.put("hbaseRegionserverPort", String.valueOf(hBaseConnectorUtil.getRegionserverPort()));
        hadoopConfig.put("hbaseRootdir", hBaseConnectorUtil.getRootdir());
        hadoopConfig.put("hbaseZookeeperQuorum", hBaseConnectorUtil.getZookeeperQuorum());
        hadoopConfig.put("hdfsWriteDir",
                (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.hproject.hdfs.write.dir"));
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(hadoopConfig);
        } catch (JsonProcessingException e) {
            LOGGER.error( e.getMessage(), e);
            LOGGER.warn( WARNING_MESSAGE, jobKey, "hadoopConfig");
            return EMPTY_STRING;
        }
    }

    private String getJobArg(JobDataMap jobDataMap, JobKey jobKey, String key) {
        if (jobDataMap.containsKey(key))
            return jobDataMap.getString(key);
        else {
            LOGGER.warn( WARNING_MESSAGE, new Object[]{jobKey, key});
            return EMPTY_STRING;
        }
    }

}
