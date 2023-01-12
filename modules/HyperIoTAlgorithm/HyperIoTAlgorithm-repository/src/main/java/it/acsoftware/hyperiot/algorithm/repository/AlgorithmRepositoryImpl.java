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
    public Algorithm updateAlgorithmFile(Algorithm algorithm, String mainClassname, File jar) {
        return this.jpa.txExpr(TransactionType.Required, entityManager -> {
            //used to remove oldJar
            String oldJarPath = algorithm.getAlgorithmFilePath();
            String jarName = algorithm.getName().trim().replace(" ", "_").toLowerCase() + ".jar";
            String jarBasePath = algorithmUtil.getJarBasePath() + "/" + jarName;
            //removing only if old name is different from new name
            boolean removeOldJar =  oldJarPath != null && !oldJarPath.equals(jarBasePath);
            try {
                hadoopManagerSystemApi.copyFile(jar, jarBasePath, true); // overwrite file if it exists
            } catch (IOException e) {
                getLog().error(e.getMessage(), e);
                throw new HyperIoTRuntimeException(e.getMessage());
            } catch (NullPointerException e1) {
                getLog().error(e1.getMessage(), e1);
                throw new HyperIoTRuntimeException("No such file, must not be null");
            }
            algorithm.setAlgorithmFileName(jarName);
            algorithm.setAlgorithmFilePath(jarBasePath);
            algorithm.setMainClassname(mainClassname);
            Algorithm updated = entityManager.merge(algorithm);
            entityManager.flush();
            //If no exception until now then old jar is removed
            if (removeOldJar) {
                try {
                    this.hadoopManagerSystemApi.deleteFile(oldJarPath);
                } catch (Throwable t) {
                    getLog().warn("Impossibile to remove old jar file {}, {}", oldJarPath, t);
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
