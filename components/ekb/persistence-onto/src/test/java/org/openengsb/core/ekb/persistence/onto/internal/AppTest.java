package org.openengsb.core.ekb.persistence.onto.internal;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openengsb.core.ekb.api.EKBService;
import org.openengsb.core.ekb.persistence.onto.internal.EKBServiceOnto;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {
    /**
     * Create the test case
     * 
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        EKBService ekb = new EKBServiceOnto(null, null, null, null, null);

        assertTrue(true);
    }
}
