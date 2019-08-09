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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static net.adamcin.oakpal.core.JavaxJson.*;
import static org.junit.Assert.*;
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
    final PackageId subA = PackageId.fromString("my_packages:simple-mixed-sub-a:1.0");
    final PackageId subAA = PackageId.fromString("my_packages:simple-mixed-sub-a-a:1.0");
    final PackageId subB = PackageId.fromString("my_packages:simple-mixed-sub-b:1.0");
    final PackageId subC = PackageId.fromString("my_packages:simple-mixed-sub-c:1.0");
    final PackageId subCC = PackageId.fromString("my_packages:simple-mixed-sub-c-c:1.0");
    final PackageId subD = PackageId.fromString("my_packages:simple-mixed-old-d:1.0");

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
        check.importedPath(root, getInstallationPath(subA), node);
        check.importedPath(root, getInstallationPath(subB), node);
        check.importedPath(root, getInstallationPath(subC), node);
        check.deletedPath(root, getInstallationPath(subD), session);
        check.afterExtract(root, session);
        check.identifySubpackage(subA, root);
        check.beforeExtract(subA, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(subA, "/", node);
        check.importedPath(subA, "/etc", node);
        check.importedPath(subA, JcrPackageRegistry.DEFAULT_PACKAGE_ROOT_PATH, node);
        check.importedPath(subA, getInstallationPath(subAA), node);
        check.importedPath(subA, "/apps", node);
        check.importedPath(subA, "/apps/example-a", node);
        check.afterExtract(subA, session);
        check.identifySubpackage(subAA, subA);
        check.beforeExtract(subAA, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(subAA, "/", node);
        check.importedPath(subAA, "/apps", node);
        check.importedPath(subAA, "/apps/example-a-a", node);
        check.afterExtract(subAA, session);
        check.identifySubpackage(subB, root);
        check.beforeExtract(subB, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(subB, "/", node);
        check.importedPath(subB, "/etc", node);
        check.importedPath(subB, "/etc/clientlibs", node);
        check.importedPath(subB, "/etc/clientlibs/example-b", node);
        check.importedPath(subB, "/apps", node);
        check.importedPath(subB, "/apps/example-b", node);
        check.afterExtract(subB, session);
        check.identifySubpackage(subC, root);
        check.beforeExtract(subC, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(subC, "/", node);
        check.importedPath(subC, "/etc", node);
        check.importedPath(subC, JcrPackageRegistry.DEFAULT_PACKAGE_ROOT_PATH, node);
        check.importedPath(subC, getInstallationPath(subCC), node);
        check.importedPath(subC, "/apps", node);
        check.importedPath(subC, "/apps/example-c", node);
        check.afterExtract(subC, session);
        check.identifySubpackage(subCC, subC);
        check.beforeExtract(subCC, session, rootProps, rootMeta, Collections.emptyList());
        check.importedPath(subCC, "/", node);
        check.importedPath(subCC, "/content", node);
        check.importedPath(subCC, "/content/example-c-c", node);
        check.afterExtract(subCC, session);
        check.finishedScan();

        return new ArrayList<>(check.getReportedViolations());
    }

    @Test
    public void testSubpackageDefaults() throws Exception {
        final List<Violation> reports = virtualSubpackageScan(obj().get());
        assertFalse("reports not contains root (container): " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(root)));
        assertFalse("reports not contains subA: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subA)));
        assertFalse("reports not contains subAA: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subAA)));
        assertTrue("reports contains subB: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subB)));
        assertTrue("reports contains subC: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subC)
                        && violation.getDescription().startsWith("recursive")));
        assertFalse("reports not contains subCC: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subCC)));
    }

    @Test
    public void testIgnoredSubpackages() throws Exception {
        final List<Violation> reports = virtualSubpackageScan(obj()
                .key(CompositeStoreAlignment.CONFIG_SCOPE_PACKAGE_IDS, arr()
                        .val(new Rule(Rule.RuleType.EXCLUDE, Pattern.compile(subB.toString()))))
                .get());
        assertFalse("reports not contains root (container): " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(root)));
        assertFalse("reports not contains subA: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subA)));
        assertFalse("reports not contains subAA: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subAA)));
        assertFalse("reports not contains subB (ignored): " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subB)));
        assertTrue("reports contains subC: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subC)
                        && violation.getDescription().startsWith("recursive")));
        assertFalse("reports not contains subCC: " + reports,
                reports.stream().anyMatch(violation -> violation.getPackages().contains(subCC)));

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