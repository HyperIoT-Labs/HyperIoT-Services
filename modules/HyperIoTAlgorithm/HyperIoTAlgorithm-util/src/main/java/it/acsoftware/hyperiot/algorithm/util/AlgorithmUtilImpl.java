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

package it.acsoftware.hyperiot.algorithm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmUtil;
import it.acsoftware.hyperiot.algorithm.model.Algorithm;
import it.acsoftware.hyperiot.algorithm.model.AlgorithmConfig;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component(service = AlgorithmUtil.class, immediate = true)
public class AlgorithmUtilImpl implements AlgorithmUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlgorithmUtilImpl.class.getName());

    private Properties props;

    private HadoopManagerUtil hadoopManagerUtil;

    @Activate
    private void loadAlgorithmConfiguration() {
        BundleContext context = HyperIoTUtil.getBundleContext(AlgorithmUtilImpl.class);
        LOGGER.debug( "Reading Algorithm Properties from .cfg file");
        ServiceReference<?> configurationAdminReference = context.getServiceReference(ConfigurationAdmin.class.getName());
        if (configurationAdminReference != null) {
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context.getService(configurationAdminReference);
            try {
                Configuration configuration = confAdmin.getConfiguration(AlgorithmConstants.ALGORITHM_CONFIG_FILE_NAME);
                if (configuration != null && configuration.getProperties() != null) {
                    LOGGER.debug( "Reading properties for Algorithm ....");
                    Dictionary<String, Object> dict = configuration.getProperties();
                    List<String> keys = Collections.list(dict.keys());
                    Map<String, Object> dictCopy = keys.stream().collect(Collectors.toMap(Function.identity(), dict::get));
                    props = new Properties();
                    props.putAll(dictCopy);
                } else
                    LOGGER.error( "Impossible to find Configuration admin reference, Algorithm won't start!");
            } catch (IOException e) {
                LOGGER.error( "Impossible to find it.acsoftware.hyperiot.algorithm.cfg, please create it!", e);
            }
        }
    }

    public String getBaseConfigString(AlgorithmConfig baseConfig) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        if (baseConfig.getInput() == null) {
            baseConfig.setInput(new ArrayList<>());
        }
        if (baseConfig.getOutput() == null)
            baseConfig.setOutput(new ArrayList<>());
        return objectMapper.writeValueAsString(baseConfig);
    }

    @Override
    public String getJarBasePath() {
        final String DEFAULT_ALGORITHM_PROPERTY_JAR_BASE_PATH = "/spark/jobs";
        return props.getProperty(AlgorithmConstants.ALGORITHM_PROPERTY_JAR_BASE_PATH,
                DEFAULT_ALGORITHM_PROPERTY_JAR_BASE_PATH);
    }

    @Override
    public String getJarFullPath(Algorithm a) {
        return hadoopManagerUtil.getDefaultFS() + a.getAlgorithmFilePath();
    }

    @Reference
    protected void setHadoopManagerUtil(HadoopManagerUtil hadoopManagerUtil) {
        this.hadoopManagerUtil = hadoopManagerUtil;
    }

}
