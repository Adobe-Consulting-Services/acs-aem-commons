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
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.core.OakMachine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

/**
 * Convenient base class for testing progress checks.
 */
public class CheckTestBase {

    protected final List<InitStage> initStages = new ArrayList<>();

    /**
     * Run a single check on one or more package files and return only the CheckReport associated with the check.
     *
     * @param check the progress check to run
     * @param files one or more filevault package files to scan
     * @return the check report associated with the provided progress check
     * @throws Exception for any exceptional error
     */
    protected CheckReport scanWithCheck(final ProgressCheck check, final File... files) throws Exception {
        List<File> artifacts = Arrays.asList(files);
        Optional<CheckReport> reports = new OakMachine.Builder().withInitStages(initStages).withProgressChecks(check)
                .build().scanPackages(artifacts).stream()
                .filter(report -> check.getCheckName().equals(report.getCheckName()))
                .findFirst();
        assertTrue(String.format("report for %s is present", check.getCheckName()), reports.isPresent());
        return reports.get();
    }

}
