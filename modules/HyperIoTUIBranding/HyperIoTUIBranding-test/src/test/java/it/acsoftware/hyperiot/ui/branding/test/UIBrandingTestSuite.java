package it.acsoftware.hyperiot.ui.branding.test;

import it.acsoftware.hyperiot.base.test.HyperIoTTestRunner;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@RunWith(AllTests.class)
public class UIBrandingTestSuite {

    public static TestSuite suite() throws Throwable {
        return HyperIoTTestRunner.createHyperIoTTestSuite("it.acsoftware.hyperiot.ui.branding.test");
    }

}