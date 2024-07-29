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

package it.acsoftware.hyperiot.algorithm.repository;

import it.acsoftware.hyperiot.algorithm.api.AlgorithmRepository;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmUtil;
import it.acsoftware.hyperiot.algorithm.model.Algorithm;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.File;
import java.io.IOException;

/**
 * @author Aristide Cittadino Implementation class of the Algorithm. This
 * class is used to interact with the persistence layer.
 */
@Component(service = AlgorithmRepository.class, immediate = true)
public class AlgorithmRepositoryImpl extends HyperIoTBaseRepositoryImpl<Algorithm> implements AlgorithmRepository {
    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;

    private AlgorithmUtil algorithmUtil;
    private HadoopManagerSystemApi hadoopManagerSystemApi;

    /**
     * Constructor for a AlgorithmRepositoryImpl
     */
    public AlgorithmRepositoryImpl() {
        super(Algorithm.class);
    }

    /**
     * @return The current jpaTemplate
     */
    @Override
    protected JpaTemplate getJpa() {
        getLog().debug("invoking getJpa, returning: {}", jpa);
        return jpa;
    }

    /**
     * @param jpa Injection of JpaTemplate
     */
    @Override
    @Reference(target = "(osgi.unit.name=hyperiot-algorithm-persistence-unit)")
    protected void setJpa(JpaTemplate jpa) {
        getLog().debug("invoking setJpa, setting: {}", jpa);
        this.jpa = jpa;
    }

    @Override
    public Algorithm updateAlgorithmFile(Algorithm algorithm, String mainClassname, File file) {
        return this.jpa.txExpr(TransactionType.Required, entityManager -> {
            //used to remove oldJar
            String oldFilePath = algorithm.getAlgorithmFilePath();
            String fileName = "";

            // Retrieve file extension: //PYTHON
            if (file.getName().substring(file.getName().length()-3).equals(".py")) {
                fileName = algorithm.getName().trim().replace(" ", "_").toLowerCase() + ".py";
            }
            else { // JAR
                fileName = algorithm.getName().trim().replace(" ", "_").toLowerCase() + ".jar";
            }

            String fileBasePath = algorithmUtil.getJarBasePath() + "/" + fileName;
            //removing only if old name is different from new name
            boolean removeOldJar =  oldFilePath != null && !oldFilePath.equals(fileBasePath);
            try {
                hadoopManagerSystemApi.copyFile(file, fileBasePath, true); // overwrite file if it exists
            } catch (IOException e) {
                getLog().error(e.getMessage(), e);
                throw new HyperIoTRuntimeException(e.getMessage());
            } catch (NullPointerException e1) {
                getLog().error(e1.getMessage(), e1);
                throw new HyperIoTRuntimeException("No such file, must not be null");
            }
            algorithm.setAlgorithmFileName(fileName);
            algorithm.setAlgorithmFilePath(fileBasePath);
            algorithm.setMainClassname(mainClassname);
            Algorithm updated = entityManager.merge(algorithm);
            entityManager.flush();
            //If no exception until now then old jar is removed
            if (removeOldJar) {
                try {
                    this.hadoopManagerSystemApi.deleteFile(oldFilePath);
                } catch (Throwable t) {
                    getLog().warn("Impossibile to remove old jar file {}, {}", oldFilePath, t);
                }
            }
            return updated;
        });
    }

    @Reference
    protected void setAlgorithmUtil(AlgorithmUtil algorithmUtil) {
        this.algorithmUtil = algorithmUtil;
    }

    @Reference
    protected void setHadoopManagerSystemApi(HadoopManagerSystemApi hadoopManagerSystemApi) {
        this.hadoopManagerSystemApi = hadoopManagerSystemApi;
    }

}
