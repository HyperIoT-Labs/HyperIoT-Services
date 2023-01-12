package it.acsoftware.hyperiot.algorithm.api;

import it.acsoftware.hyperiot.algorithm.model.AlgorithmConfig;
import it.acsoftware.hyperiot.algorithm.model.AlgorithmFieldType;
import it.acsoftware.hyperiot.algorithm.model.AlgorithmIOField;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntitySystemApi;

import it.acsoftware.hyperiot.algorithm.model.Algorithm;

import java.io.File;

/**
 * 
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 *         interface defines methods for additional operations.
 *
 */
public interface AlgorithmSystemApi extends HyperIoTBaseEntitySystemApi<Algorithm> {


    /**
     * It add IO field to Algorithm with given ID
     * @param algorithmId Algorithm ID
     * @param ioField IO field
     * @return Updated algorithm
     */
    Algorithm addIOField(long algorithmId, AlgorithmIOField ioField);

    /**
     * It delete output field of Algorithm with given ID
     * @param algorithmId Algorithm ID
     * @param fieldType Field type, i.e. input or output
     * @param ioFieldId IO field ID
     * @return Updated algorithm
     */
    Algorithm deleteIOField(long algorithmId, AlgorithmFieldType fieldType, long ioFieldId);

    /**
     * It updates base configuration of Algorithm
     * @param algorithmId Algorithm ID
     * @param baseConfig Base Configuration
     * @return Updated algorithm
     */
    Algorithm updateBaseConfig(long algorithmId, AlgorithmConfig baseConfig);

    /**
     * It updates algorithmFile of Algorithm
     * @param algorithmId Algorithm ID
     * @param mainClassname Class containing main method
     * @param algorithmFile Jar file
     * @return Updated algorithm
     */
    Algorithm updateAlgorithmFile(long algorithmId, String mainClassname, File algorithmFile);

    /**
     * It update IO field of Algorithm with given ID
     * @param algorithmId Algorithm ID
     * @param ioField IO field
     * @return Updated algorithm
     */
    Algorithm updateIOField(long algorithmId, AlgorithmIOField ioField);

}