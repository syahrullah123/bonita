/*******************************************************************************
 * Copyright (C) 2009, 2013 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package com.bonitasoft.engine;

import static org.junit.Assert.assertEquals;

import org.bonitasoft.engine.identity.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class LocalLogTest extends CommonAPISPTest {

    @After
    public void afterTest() throws Exception {
        logout();
    }

    @Before
    public void beforeTest() throws Exception {
        login();
    }

    // run this test in local test suite only, otherwise it's necessary to use a command to set the system property on the server side
    @Ignore("This test fails because Property 'org.bonitasoft.engine.services.queryablelog.disable' is only read at startup, so change is not taken into account")
    @Test
    public void testDisableLogs() throws Exception {
        final int initNumberOfLogs = getLogAPI().getNumberOfLogs();
        User user1 = getIdentityAPI().createUser("user1WrongSortKey", "bpm");
        getIdentityAPI().deleteUser(user1.getId());
        int numberOfLogs = getLogAPI().getNumberOfLogs();
        assertEquals("Number of logs should have increase of 1!", initNumberOfLogs + 2, numberOfLogs);

        System.setProperty("org.bonitasoft.engine.services.queryablelog.disable", "true");

        user1 = getIdentityAPI().createUser("user1WrongSortKey", "bpm");
        getIdentityAPI().deleteUser(user1.getId());
        numberOfLogs = getLogAPI().getNumberOfLogs();

        assertEquals("Number of logs should not have changed!", initNumberOfLogs + 2, numberOfLogs);

        System.clearProperty("org.bonitasoft.engine.services.queryablelog.disable");
    }

}
