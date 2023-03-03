/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.oakpal.checks;

import net.adamcin.oakpal.api.ProgressCheck;
import net.adamcin.oakpal.api.Rules;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static net.adamcin.oakpal.api.JavaxJson.obj;
import static org.junit.Assert.assertEquals;

public class RecommendEnsureOakIndexTest extends CheckTestBase {
    private File pack;
    public static final URL CND = RecommendEnsureOakIndexTest.class
            .getResource("/nodetypes/content_classifications.cnd");

    @Before
    public void setUp() throws Exception {
        initStages.add(new InitStage.Builder().withOrderedCndUrl(CND).build());
        pack = TestPackageUtil.prepareTestPackageFromFolder("oak-index-pack.zip",
                new File("src/test/resources/oak-index-filevault"));
    }

    @Test
    public void testCheckNone() throws Exception {
        ProgressCheck check = new RecommendEnsureOakIndex()
                .newInstance(key("scopePaths", arr(Rules.DEFAULT_DENY)).get());
        CheckReport reportValid = scanWithCheck(check, pack);
        assertEquals("No violations when deny all oak:index children.", 0, reportValid.getViolations().size());
    }

    @Test
    public void testCheckAll() throws Exception {
        ProgressCheck check = new RecommendEnsureOakIndex().newInstance(obj().get());
        CheckReport reportValid = scanWithCheck(check, pack);
        assertEquals("3 violations when allow all oak:index children.", 3, reportValid.getViolations().size());
    }


}
