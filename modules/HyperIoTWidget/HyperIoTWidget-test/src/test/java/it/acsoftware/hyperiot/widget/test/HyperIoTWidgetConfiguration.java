package it.acsoftware.hyperiot.widget.test;

import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.widget.model.Widget;
import org.ops4j.pax.exam.ConfigurationFactory;
import org.ops4j.pax.exam.Option;

public class HyperIoTWidgetConfiguration implements ConfigurationFactory {
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String widgetResourceName = Widget.class.getName();

    static final int maxLengthName = 501;
    static final int maxLengthDescription = 3001;
    static final String widgetDescription = "Display image based on thermal camera data array";
    static final byte[] widgetImageData = new byte[1000];//"13.572";
    static final byte[] widgetImageDataPreview = new byte[1000];

    static final String permissionWidget = "it.acsoftware.hyperiot.widget.model.Widget";
    static final String nameRegisteredPermission = " RegisteredUser Permissions";

    static final int defaultDelta = 10;
    static final int defaultPage = 1;

    @Override
    public Option[] createConfiguration() {
        return HyperIoTServicesTestConfigurationBuilder.createStandardConfiguration()
                .withCodeCoverage("it.acsoftware.hyperiot.widget.*")
                .keepRuntime()
                .build();
    }
}
