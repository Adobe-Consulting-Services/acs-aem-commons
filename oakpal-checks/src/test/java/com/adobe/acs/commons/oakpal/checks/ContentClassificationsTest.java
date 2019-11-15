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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.junit.Before;
import org.junit.Test;

public class ContentClassificationsTest extends CheckTestBase {
    public static final String NS_URI_SLING = "http://sling.apache.org/jcr/sling/1.0";
    public static final String NS_URI_GRANITE = "http://www.adobe.com/jcr/granite/1.0";

    public static final URL CND = ContentClassificationsTest.class
            .getResource("/nodetypes/content_classifications.cnd");
    public static final InitStage INIT;

    static {
        INIT = new InitStage.Builder()
                .withOrderedCndUrl(CND)
                .withNs("sling", NS_URI_SLING)
                .withNs("granite", NS_URI_GRANITE)
                .withForcedRoot("/libs/acs/internal", "sling:Folder",
                        "granite:InternalArea")
                .withForcedRoot("/libs/acs/internal/public", "sling:Folder",
                        "granite:PublicArea")
                .withForcedRoot("/libs/acs/final", "sling:Folder",
                        "granite:FinalArea")
                .withForcedRoot("/libs/acs/final/child",
                        "sling:Folder")
                .withForcedRoot("/libs/acs/final/public", "sling:Folder",
                        "granite:PublicArea")
                .withForcedRoot("/libs/acs/abstract", "sling:Folder",
                        "granite:AbstractArea")
                .withForcedRoot("/libs/acs/abstract/public", "sling:Folder",
                        "granite:PublicArea")
                .withForcedRoot("/libs/acs/public", "sling:Folder",
                        "granite:PublicArea")
                .withForcedRoot("/libs/acs/public/internal", "sling:Folder",
                        "granite:InternalArea")
                .withForcedRoot("/libs/acs/public/final", "sling:Folder",
                        "granite:FinalArea")
                .withForcedRoot("/libs/acs/public/abstract", "sling:Folder",
                        "granite:AbstractArea").build();
    }

    private File pack;

    @Before
    public void setUp() throws Exception {
        pack = TestPackageUtil.prepareTestPackageFromFolder("content-class-pack.zip",
                new File("src/test/resources/content-classifications-filevault"));
        initStages.add(INIT);
    }

    private ProgressCheck checkForPath(final String path) {
        return new ContentClassifications()
                .newInstance(key("scopePaths", arr(key("type", "allow").key("pattern", path))).get());
    }

    @Test
    public void testCheckAllValid() throws Exception {
        ProgressCheck checkValid = new ContentClassifications()
                .newInstance(obj()
                        .key("scopePaths", arr()
                                .val(key("type", "allow").key("pattern", ".*/valid.*"))
                                .val(key("type", "allow").key("pattern", "/apps/acs/(abstract|public).*")))
                        .key("searchPaths", arr("/apps", "/libs"))
                        .get());
        CheckReport reportValid = scanWithCheck(checkValid, pack);
        assertEquals("No violations when deny invalid paths.", 0, reportValid.getViolations().size());
    }

    private void checkInvalidPath(final String path, final String marked, final String description) throws Exception {
        ProgressCheck check = checkForPath(path);
        CheckReport report = scanWithCheck(check, pack);
        assertEquals(String.format("One violation: %s", description), 1, report.getViolations().size());
        assertTrue(
                String.format("Violation contains 'marked %s' (actual: %s): %s.", marked,
                        report.getViolations().iterator().next().getDescription(), description),
                report.getViolations().iterator().next().getDescription().contains(String.format("marked %s", marked)));
    }

    @Test
    public void testCheckInvalidOverlayFinal() throws Exception {
        checkInvalidPath("/apps/acs/final", "FINAL", "overlay final");
    }

    @Test
    public void testCheckInvalidOverlayInternal() throws Exception {
        checkInvalidPath("/apps/acs/internal", "INTERNAL", "overlay final");
    }

    @Test
    public void testCheckInvalidCmpFinal() throws Exception {
        checkInvalidPath("/apps/acs/invalidcmp_final", "FINAL", "extend final");
    }

    @Test
    public void testCheckInvalidCmpFinalChild() throws Exception {
        checkInvalidPath("/apps/acs/invalidcmp_final_child", "INTERNAL", "extend final child");
    }

    @Test
    public void testCheckInvalidCmpInternal() throws Exception {
        checkInvalidPath("/apps/acs/invalidcmp_internal", "INTERNAL", "extend internal");
    }

    @Test
    public void testCheckInvalidCmpInternalChild() throws Exception {
        checkInvalidPath("/apps/acs/invalidcmp_internal_child", "INTERNAL", "extend internal child");
    }

    @Test
    public void testCheckInvalidPageAbstract() throws Exception {
        checkInvalidPath("/content/acs/invalidpage_abstract", "ABSTRACT", "use abstract type");
    }

    @Test
    public void testCheckInvalidPageFinalChild() throws Exception {
        checkInvalidPath("/content/acs/invalidpage_final_child", "INTERNAL", "use final child");
    }

    @Test
    public void testCheckInvalidPageFinalImplicitChild() throws Exception {
        checkInvalidPath("/content/acs/invalidpage_final_implicitchild", "INTERNAL", "use final implicit child");
    }

    @Test
    public void testCheckInvalidPageInternal() throws Exception {
        checkInvalidPath("/content/acs/invalidpage_internal", "INTERNAL", "use internal");
    }

}
