package it.acsoftware.hyperiot.ui.branding.test;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.ConfigurationFactory;

import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
/**
 * 
 * @author Aristide Cittadino UIBrandingTestConfiguration
 * Used for setting test global configs with ConfigurationFactory.
 * This class is defined as SPI inside META-INF/services/org.ops4j.pax.exam.ConfigurationFactory
 */

public class UIBrandingTestConfiguration implements ConfigurationFactory {

	@Override
    public Option[] createConfiguration() {
		Option[] customOptions = { new KarafDistributionConfigurationFileExtendOption(
				new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
						"featuresRepositories"),
				",mvn:it.acsoftware.hyperiot.ui.branding/HyperIoTUIBranding-features/1.3.5" + 
						"/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(
						new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"),
						",hyperiot-uibranding") };
		return HyperIoTTestConfigurationBuilder.createStandardConfiguration()
			.withCodeCoverage("it.acsoftware.hyperiot.ui.branding.*")
			.append(customOptions)
			.build();
	}
}