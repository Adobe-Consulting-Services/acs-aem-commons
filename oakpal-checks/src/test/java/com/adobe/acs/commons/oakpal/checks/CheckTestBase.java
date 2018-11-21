package com.adobe.acs.commons.oakpal.checks;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.core.OakMachine;
import net.adamcin.oakpal.core.ProgressCheck;

public class CheckTestBase {

    protected final List<InitStage> initStages = new ArrayList<>();

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
