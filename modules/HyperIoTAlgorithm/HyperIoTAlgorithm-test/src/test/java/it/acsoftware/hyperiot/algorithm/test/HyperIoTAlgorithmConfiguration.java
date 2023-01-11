package it.acsoftware.hyperiot.algorithm.test;

import it.acsoftware.hyperiot.algorithm.model.Algorithm;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import org.ops4j.pax.exam.ConfigurationFactory;
import org.ops4j.pax.exam.Option;

public class HyperIoTAlgorithmConfiguration implements ConfigurationFactory {
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String algorithmResourceName = Algorithm.class.getName();
    static final int maxLengthDescription = 3001;

    //jar file
    static final String jarName = "algorithm_test001.jar";
    static final String jarPath = "resources/";

    static final int defaultDelta = 10;
    static final int defaultPage = 1;

    static final String permissionAlgorithm = "it.acsoftware.hyperiot.algorithm.model.Algorithm";
    static final String nameRegisteredPermission = " RegisteredUser Permissions";

    @Override
    public Option[] createConfiguration() {
        return HyperIoTServicesTestConfigurationBuilder.createStandardConfiguration()
                .withCodeCoverage("it.acsoftware.hyperiot.algorithm.*")
                .withDebug("5005",false)
                .build();
    }
}
