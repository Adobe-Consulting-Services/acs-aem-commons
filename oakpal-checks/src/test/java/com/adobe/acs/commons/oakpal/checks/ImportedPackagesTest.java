package com.adobe.acs.commons.oakpal.checks;

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.Violation;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ImportedPackagesTest extends CheckTestBase {
    public static final InitStage INIT;

    static {
        INIT = new InitStage.Builder().build();
    }

    private File pack;


    @Before
    public void setUp() throws Exception {
        pack = TestPackageUtil.prepareTestPackageFromFolder("package-imports-pack.zip",
            new File("src/test/resources/package-imports"));
        initStages.add(INIT);
    }


    @Test
    public void testDefaultConfig() throws Exception {
        ProgressCheck check = new ImportedPackages().newInstance(Json.createObjectBuilder().build());
        CheckReport report = scanWithCheck(check, pack);

        assertEquals(0, report.getViolations().size());
    }

    @Test
    public void testWithEmpty() throws Exception {
        ProgressCheck check = new ImportedPackages.Check(Collections.emptyMap());
        CheckReport report = scanWithCheck(check, pack);
        assertEquals(0, report.getViolations().size());
    }

    @Test
    public void testWithEmptyVersion() throws Exception {
        ProgressCheck check = new ImportedPackages.Check(Collections.singletonMap("6.2", Collections.emptyMap()));
        CheckReport report = scanWithCheck(check, pack);
        assertEquals(179, report.getViolations().size());

        Set<String> descriptions = report.getViolations().stream().map(Violation::getDescription).collect(Collectors.toSet());
        assertTrue(descriptions.contains("Package import com.day.cq.replication;version=\"[6.4.0,7.0.0)\" cannot be satisified by AEM Version 6.2. Package is not exported."));
    }
}
