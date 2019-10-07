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
package com.adobe.acs.commons.oakpal.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.adamcin.oakpal.core.CheckSpec;
import net.adamcin.oakpal.core.ChecklistPlanner;
import org.junit.Test;

/**
 * Integration test to enforce expectations for the exported OakPAL checklists. Must be done during ITs because manifest
 * is generated at package time.
 */
public class ChecklistIT {
    public static final String OAKPAL_MODULE_NAME = "com.adobe.acs.acs-aem-commons-oakpal-checks";
    public static final String OAKPAL_CHECKLIST_INTERNAL = "acs-internal";
    public static final String OAKPAL_CHECKLIST_CONTENT_CLASS_AEM64 = "content-class-aem64";
    public static final String OAKPAL_CHECKLIST_CONTENT_CLASS_AEM65 = "content-class-aem65";
    public static final String OAKPAL_CHECKLIST_PUBLIC = "acs-commons-integrators";

    @Test
    public void testLoadChecklists() throws Exception {
        ChecklistPlanner planner = new ChecklistPlanner(Arrays.asList(
                OAKPAL_CHECKLIST_INTERNAL,
                OAKPAL_CHECKLIST_CONTENT_CLASS_AEM64,
                OAKPAL_CHECKLIST_CONTENT_CLASS_AEM65,
                OAKPAL_CHECKLIST_PUBLIC));
        planner.discoverChecklists();

        assertEquals("expect four init stages, representing the four active checklists",
                4, planner.getInitStages().size());

        List<CheckSpec> specs = planner.getEffectiveCheckSpecs(Collections.emptyList());

        List<String> expectNames = new ArrayList<>();
        expectNames.addAll(Stream.of(
                "acHandling-merge-or-better",
                "enforce-no-libs",
                "enforce-no-deletes",
                "base-version-compatibility",
                "apps-composite-store-alignment"
        ).map(name -> OAKPAL_MODULE_NAME + "/" + OAKPAL_CHECKLIST_INTERNAL + "/" + name)
                .collect(Collectors.toList()));

        expectNames.addAll(Stream.of(
                "content-classifications"
        ).map(name -> OAKPAL_MODULE_NAME + "/" + OAKPAL_CHECKLIST_CONTENT_CLASS_AEM64 + "/" + name)
                .collect(Collectors.toList()));

        expectNames.addAll(Stream.of(
                "content-classifications"
        ).map(name -> OAKPAL_MODULE_NAME + "/" + OAKPAL_CHECKLIST_CONTENT_CLASS_AEM65 + "/" + name)
                .collect(Collectors.toList()));

        expectNames.addAll(Stream.of(
                "authorizable-compatibility-check",
                "recommend-ensure-authorizable",
                "recommend-ensure-oak-index"
        ).map(name -> OAKPAL_MODULE_NAME + "/" + OAKPAL_CHECKLIST_PUBLIC + "/" + name)
                .collect(Collectors.toList()));

        for (String expectName : expectNames) {
            assertTrue("expect effective check: " + expectName,
                    specs.stream().anyMatch(spec -> expectName.equals(spec.getName())));
        }
    }
}
