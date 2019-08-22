/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.oakpal.checks;

import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.key;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.assertEquals;

import java.io.File;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.checks.Rule;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.junit.Before;
import org.junit.Test;

public class AcsCommonsAuthorizableCompatibilityCheckTest extends CheckTestBase {

    private File pack;

    @Before
    public void setUp() throws Exception {
        pack = TestPackageUtil.prepareTestPackageFromFolder("home-users-pack.zip",
                new File("src/test/resources/home-users-filevault"));
    }

    @Test
    public void testCheckNone() throws Exception {
        ProgressCheck check = new AcsCommonsAuthorizableCompatibilityCheck()
                .newInstance(key("scopeIds", arr(Rule.DEFAULT_DENY)).get());
        CheckReport reportValid = scanWithCheck(check, pack);
        assertEquals("No violations when deny all authorizable ids.", 0, reportValid.getViolations().size());
    }

    @Test
    public void testCheckAll() throws Exception {
        ProgressCheck check = new AcsCommonsAuthorizableCompatibilityCheck().newInstance(obj().get());
        CheckReport reportValid = scanWithCheck(check, pack);
        assertEquals("3 violations when allow all authorizable ids.", 3, reportValid.getViolations().size());
    }

    private void checkSpecificId(final String authorizableId, final boolean expectValid) throws Exception {
        ProgressCheck checkValid = new AcsCommonsAuthorizableCompatibilityCheck().newInstance(
                key("scopeIds", arr(key("type", "allow").key("pattern", authorizableId))).get());
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
