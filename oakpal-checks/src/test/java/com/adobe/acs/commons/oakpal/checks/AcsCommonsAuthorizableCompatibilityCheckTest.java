package com.adobe.acs.commons.oakpal.checks;

import static org.junit.Assert.assertEquals;

import java.io.File;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class AcsCommonsAuthorizableCompatibilityCheckTest extends CheckTestBase {

    private File pack;

    @Before
    public void setUp() throws Exception {
        pack = TestPackageUtil.prepareTestPackageFromFolder("content-class-pack.zip",
                new File("src/test/resources/home-users-filevault"));
    }

    @Test
    public void testCheckNone() throws Exception {
        ProgressCheck check = new AcsCommonsAuthorizableCompatibilityCheck().newInstance(
                new JSONObject("{\"scopeIds\":[{\"type\":\"deny\",\"pattern\":\".*\"}]}"));
        CheckReport reportValid = scanWithCheck(check, pack);
        assertEquals("No violations when deny all authorizable ids.", 0, reportValid.getViolations().size());
    }

    @Test
    public void testCheckAll() throws Exception {
        ProgressCheck check = new AcsCommonsAuthorizableCompatibilityCheck().newInstance(
                new JSONObject("{}"));
        CheckReport reportValid = scanWithCheck(check, pack);
        assertEquals("3 violations when allow all authorizable ids.", 3, reportValid.getViolations().size());
    }

    private void checkSpecificId(final String authorizableId, final boolean expectValid) throws Exception {
        ProgressCheck checkValid = new AcsCommonsAuthorizableCompatibilityCheck().newInstance(
                new JSONObject(String.format("{\"scopeIds\":[{\"type\":\"allow\",\"pattern\":\"%s\"}]}", authorizableId)));
        CheckReport reportValid = scanWithCheck(checkValid, pack);
        assertEquals("check specific authorizableId: " + authorizableId, expectValid ? 0 : 1,
                reportValid.getViolations().size());
    }

    @Test
    public void testCheckAcsCommonsDerivedGroup() throws Exception {
        checkSpecificId("acs-commons-derived", false);
    }

    @Test
    public void testCheckDevelopers() throws Exception {
        checkSpecificId("developers", true);
    }

    @Test
    public void testCheckAcme() throws Exception {
        checkSpecificId("acme", true);
    }

    @Test
    public void testCheckAcsCommonsDeveloper() throws Exception {
        checkSpecificId("acs-commons-developer", false);
    }

    @Test
    public void testCheckAcsCommonsServiceUserForAcme() throws Exception {
        checkSpecificId("acs-commons-service-user-for-acme", false);
    }
}
