/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2019 Adobe
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

import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.ProgressCheck;
import net.adamcin.oakpal.core.Violation;
import net.adamcin.oakpal.core.checks.Rule;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.registry.impl.JcrPackageRegistry;
import org.junit.Before;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.json.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static net.adamcin.oakpal.core.JavaxJson.arr;
import static net.adamcin.oakpal.core.JavaxJson.obj;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class CompositeStoreAlignmentTest extends CheckTestBase {

    private File simpleContent;
    private File simpleLibs;
    private File simpleMixed;

    @Before
    public void setUp() throws Exception {
        simpleContent = TestPackageUtil.prepareTestPackageFromFolder("simple-content.zip",
                new File("src/test/resources/simple-content"));
        simpleLibs = TestPackageUtil.prepareTestPackageFromFolder("simple-libs.zip",
                new File("src/test/resources/simple-libs"));
        simpleMixed = TestPackageUtil.prepareTestPackageFromFolder("simple-mixed.zip",
                new File("src/test/resources/simple-mixed"));
    }

    @Test
    public void testSimpleContent() throws Exception {
        ProgressCheck check = new CompositeStoreAlignment().newInstance(obj().get());
        CheckReport reportValid = scanWithCheck(check, simpleContent);
        assertEquals("No violations with simple content.", 0, reportValid.getViolations().size());
    }

    @Test
    public void testSimpleLibs() throws Exception {
        ProgressCheck check = new CompositeStoreAlignment().newInstance(obj().get());
        CheckReport reportValid = scanWithCheck(check, simpleLibs);
        assertEquals("No violations with simple libs.", 0, reportValid.getViolations().size());
    }

    @Test
    public void testSimpleMixed() throws Exception {
        ProgressCheck check = new CompositeStoreAlignment().newInstance(obj().get());
        CheckReport reportValid = scanWithCheck(check, simpleMixed);
        assertEquals("One violation with simple mixed. " + reportValid.getViolations().iterator().next(),
                1, reportValid.getViolations().size());
    }

    private static String getInstallationPath(final PackageId packageId) {
        return JcrPackageRegistry.DEFAULT_PACKAGE_ROOT_PATH_PREFIX + packageId.getDownloadName();
    }

    @Test
    public void testGetCheckName() throws Exception {
        final ProgressCheck check = new CompositeStoreAlignment().newInstance(obj().get());
        assertEquals("check name should be", CompositeStoreAlignment.class.getName(), check.getCheckName());
    }

    final PackageId root = PackageId.fromString("my_packages:simple-mixed:1.0");
    final PackageId subAlpha = PackageId.fromString("my_packages:simple-mixed-sub-a:1.0");
    final PackageId subAlphaAlpha = PackageId.fromString("my_packages:simple-mixed-sub-a-a:1.0");
    final PackageId subBravo = PackageId.fromString("my_packages:simple-mixed-sub-b:1.0");
    final PackageId subCharlie = PackageId.fromString("my_packages:simple-mixed-sub-c:1.0");
    final PackageId subCharlieCharlie = PackageId.fromString("my_packages:simple-mixed-sub-c-c:1.0");
    final PackageId subDelta = PackageId.fromString("my_packages:simple-mixed-old-d:1.0");

    private List<Violation> virtualSubpackageScan(final JsonObject checkConfig) throws Exception {
        final ProgressCheck check = new CompositeStoreAlignment().newInstance(checkConfig);
        final Session session = mock(Session.class);
        final Node node = mock(Node.class);
        final PackageProperties rootProps = mock(PackageProperties.class);
        final MetaInf rootMeta = mock(MetaInf.class);

        check.startedScan();
        check.identifyPackage(root, simpleMixed);
        check.beforeExtract(root, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(root, "/", node);
        check.importedPath(root, "/etc", node);
        check.importedPath(root, JcrPackageRegistry.DEFAULT_PACKAGE_ROOT_PATH, node);
        check.importedPath(root, getInstallationPath(subAlpha), node);
        check.importedPath(root, getInstallationPath(subBravo), node);
        check.importedPath(root, getInstallationPath(subCharlie), node);
        check.deletedPath(root, getInstallationPath(subDelta), session);
        check.afterExtract(root, session);
        check.identifySubpackage(subAlpha, root);
        check.beforeExtract(subAlpha, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(subAlpha, "/", node);
        check.importedPath(subAlpha, "/etc", node);
        check.importedPath(subAlpha, JcrPackageRegistry.DEFAULT_PACKAGE_ROOT_PATH, node);
        check.importedPath(subAlpha, getInstallationPath(subAlphaAlpha), node);
        check.importedPath(subAlpha, "/apps", node);
        check.importedPath(subAlpha, "/apps/example-a", node);
        check.afterExtract(subAlpha, session);
        check.identifySubpackage(subAlphaAlpha, subAlpha);
        check.beforeExtract(subAlphaAlpha, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(subAlphaAlpha, "/", node);
        check.importedPath(subAlphaAlpha, "/apps", node);
        check.importedPath(subAlphaAlpha, "/apps/example-a-a", node);
        check.afterExtract(subAlphaAlpha, session);
        check.identifySubpackage(subBravo, root);
        check.beforeExtract(subBravo, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(subBravo, "/", node);
        check.importedPath(subBravo, "/etc", node);
        check.importedPath(subBravo, "/etc/clientlibs", node);
        check.importedPath(subBravo, "/etc/clientlibs/example-b", node);
        check.importedPath(subBravo, "/apps", node);
        check.importedPath(subBravo, "/apps/example-b", node);
        check.afterExtract(subBravo, session);
        check.identifySubpackage(subCharlie, root);
        check.beforeExtract(subCharlie, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(subCharlie, "/", node);
        check.importedPath(subCharlie, "/etc", node);
        check.importedPath(subCharlie, JcrPackageRegistry.DEFAULT_PACKAGE_ROOT_PATH, node);
        check.importedPath(subCharlie, getInstallationPath(subCharlieCharlie), node);
        check.importedPath(subCharlie, "/apps", node);
        check.importedPath(subCharlie, "/apps/example-c", node);
        check.afterExtract(subCharlie, session);
        check.identifySubpackage(subCharlieCharlie, subCharlie);
        check.beforeExtract(subCharlieCharlie, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(subCharlieCharlie, "/", node);
        check.importedPath(subCharlieCharlie, "/content", node);
        check.importedPath(subCharlieCharlie, "/content/example-c-c", node);
        check.afterExtract(subCharlieCharlie, session);
        check.finishedScan();

        return new ArrayList<>(check.getReportedViolations());
    }

    @Test
    public void testSubpackageDefaults() throws Exception {
        final List<Violation> reports = virtualSubpackageScan(obj().get());
        assertFalse("reports not contains root (container): " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(root)));
        assertFalse("reports not contains subA: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subAlpha)));
        assertFalse("reports not contains subAA: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subAlphaAlpha)));
        assertTrue("reports contains subB: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subBravo)));
        assertTrue("reports contains subC: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subCharlie)
                        && violation.getDescription().startsWith("recursive")));
        assertFalse("reports not contains subCC: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subCharlieCharlie)));
    }

    @Test
    public void testIgnoredSubpackages() throws Exception {
        final List<Violation> reports = virtualSubpackageScan(obj()
                .key(CompositeStoreAlignment.CONFIG_SCOPE_PACKAGE_IDS, arr()
                        .val(new Rule(Rule.RuleType.EXCLUDE, Pattern.compile(subBravo.toString()))))
                .get());
        assertFalse("reports not contains root (container): " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(root)));
        assertFalse("reports not contains subA: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subAlpha)));
        assertFalse("reports not contains subAA: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subAlphaAlpha)));
        assertFalse("reports not contains subB (ignored): " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subBravo)));
        assertTrue("reports contains subC: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subCharlie)
                        && violation.getDescription().startsWith("recursive")));
        assertFalse("reports not contains subCC: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subCharlieCharlie)));

    }

    @Test
    public void testConfigSeverity() throws Exception {
        final List<Violation> reportsDefault = virtualSubpackageScan(obj()
                .get());
        assertEquals("reportsDefault has this many violations", 2, reportsDefault.size());
        assertFalse("reportsDefault has no severity minor",
                reportsDefault.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.MINOR));
        assertTrue("reportsDefault has severity major",
                reportsDefault.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.MAJOR));
        assertFalse("reportsDefault has no severity severe",
                reportsDefault.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.SEVERE));

        final List<Violation> reportsMinor = virtualSubpackageScan(obj()
                .key(CompositeStoreAlignment.CONFIG_SEVERITY, "minor")
                .get());
        assertEquals("reportsMinor has this many violations", 2, reportsMinor.size());
        assertTrue("reportsMinor has severity minor",
                reportsMinor.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.MINOR));
        assertFalse("reportsMinor has no severity major",
                reportsMinor.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.MAJOR));
        assertFalse("reportsMinor has no severity severe",
                reportsMinor.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.SEVERE));

        final List<Violation> reportsMajor = virtualSubpackageScan(obj()
                .key(CompositeStoreAlignment.CONFIG_SEVERITY, "major")
                .get());
        assertEquals("reportsMajor has this many violations", 2, reportsMajor.size());
        assertFalse("reportsMajor has no severity minor",
                reportsMajor.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.MINOR));
        assertTrue("reportsMajor has severity major",
                reportsMajor.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.MAJOR));
        assertFalse("reportsMajor has no severity severe",
                reportsMajor.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.SEVERE));

        final List<Violation> reportsSevere = virtualSubpackageScan(obj()
                .key(CompositeStoreAlignment.CONFIG_SEVERITY, "severe")
                .get());
        assertEquals("reportsSevere has this many violations", 2, reportsSevere.size());
        assertFalse("reportsSevere has no severity minor",
                reportsSevere.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.MINOR));
        assertFalse("reportsSevere has no severity major",
                reportsSevere.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.MAJOR));
        assertTrue("reportsSevere has severity severe",
                reportsSevere.stream().anyMatch(violation -> violation.getSeverity() == Violation.Severity.SEVERE));
    }

    @Test
    public void testConfigMounts() throws Exception {
        final List<Violation> reportsDefault = virtualSubpackageScan(obj()
                .get());
        assertEquals("reportsDefault has this many violations", 2, reportsDefault.size());

        final List<Violation> reportsNoMounts = virtualSubpackageScan(obj()
                .key(CompositeStoreAlignment.CONFIG_MOUNTS, obj())
                .get());
        assertEquals("reportsNoMounts has this many violations", 0, reportsNoMounts.size());

        final List<Violation> reportsWithDefaultMount = virtualSubpackageScan(obj()
                .key(CompositeStoreAlignment.CONFIG_MOUNTS, obj()
                        .key("<default>", "/apps"))

                .get());
        assertEquals("reportsWithDefaultMount has this many violations", 0, reportsWithDefaultMount.size());

        final List<Violation> reportsJustApps = virtualSubpackageScan(obj()
                .key(CompositeStoreAlignment.CONFIG_MOUNTS, obj()
                        .key("justApps", "/apps")
                        .get())
                .get());
        assertEquals("reportsJustApps has this many violations", 2, reportsJustApps.size());

        final List<Violation> reportsClientlibs = virtualSubpackageScan(obj()
                .key(CompositeStoreAlignment.CONFIG_MOUNTS, obj()
                        .key("clientlibs", arr("/etc/clientlibs", "/var/clientlibs"))
                        .get())
                .get());
        assertEquals("reportsClientlibs has this many violations", 1, reportsClientlibs.size());
    }
}