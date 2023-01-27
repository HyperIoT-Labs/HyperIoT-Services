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

package it.acsoftware.hyperiot.stormmanager.util;


import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;


@Component(service = StormManagerUtil.class, immediate = true)
public class StormManagerUtilImpl implements StormManagerUtil {

    private static final Logger log = LoggerFactory.getLogger(StormManagerUtilImpl.class.getName());

    public static final String HYPERIOT_PROPERTY_STORM_TOPOLOGY_JAR_NAME = "it.acsoftware.hyperiot.stormmanager.topology.jar";

    public static final String HYPERIOT_PROPERTY_STORM_TOPOLOGY_DIR = "it.acsoftware.hyperiot.stormmanager.topology.dir";


    private String topologyPath ;

    @Activate
    public void onActivate(){
        String topologyJarName = (String) HyperIoTUtil.getHyperIoTProperty(HYPERIOT_PROPERTY_STORM_TOPOLOGY_JAR_NAME);
        String topologyDir = (String) HyperIoTUtil.getHyperIoTProperty(HYPERIOT_PROPERTY_STORM_TOPOLOGY_DIR);
        this.topologyPath = topologyDir + topologyJarName + ".jar";
        Enumeration<URL> results = HyperIoTUtil.getBundleContext(this.getClass()).getBundle().findEntries("/", topologyJarName + "*.jar", false);
        if (results.hasMoreElements()) {
            try {
                URL result = results.nextElement();
                //copy the inner jar outside the bundle
                copyFile(result.openStream(), this.topologyPath);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            throw new HyperIoTRuntimeException("Cannot find any " + topologyJarName + ".jar file inside storm manager client!");
        }
    }

    private void copyFile(InputStream inputStream, String outputPath) throws IOException {
        File outputFile = new File(outputPath);
        if (outputFile.exists())
            outputFile.delete();

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputPath));
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        try {
            byte[] buffer = new byte[1024];
            int len;
            // read bytes from the input stream and store them in the buffer
            while ((len = bis.read(buffer)) != -1) {
                // write bytes from the buffer into the output stream
                bos.write(buffer, 0, len);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                bos.close();
                bis.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }


}
