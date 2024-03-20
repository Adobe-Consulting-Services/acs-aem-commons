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
import net.adamcin.oakpal.api.Violation;
import net.adamcin.oakpal.core.CheckReport;
import net.adamcin.oakpal.core.InitStage;
import net.adamcin.oakpal.testing.TestPackageUtil;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Version;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.json.Json;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.adamcin.oakpal.api.JavaxJson.arr;
import static net.adamcin.oakpal.api.JavaxJson.key;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test(expected = IllegalArgumentException.class)
    public void testNewInstance_missingBundleInfo() throws Exception {
        new ImportedPackages().newInstance(key("aemVersion", arr("5.4")).get());
    }

    @Test
    public void testParseExportPackage() {
        CompletableFuture<Boolean> expectNeverDone = new CompletableFuture<>();
        ImportedPackages.Check.parseExportPackage(null, part -> expectNeverDone.complete(true));
        assertFalse("null header should not complete future", expectNeverDone.isDone());
    }

    @Test
    public void testParseImportPackageHeader() {
        Set<ImportedPackages.ImportedPackage> emptyHeader = ImportedPackages.parseImportPackageHeader(null);
        assertNotNull("set should not be null", emptyHeader);
        assertTrue("set should be empty", emptyHeader.isEmpty());

        final String expectOptionalImport = "javax.json;resolution:=optional;version=\"[1.0.0,2.0.0)\"";
        final Set<ImportedPackages.ImportedPackage> optionalImportSet = ImportedPackages
                .parseImportPackageHeader(expectOptionalImport);
        assertNotNull("set should not be null", optionalImportSet);
        assertFalse("set should not be empty", optionalImportSet.isEmpty());
        final ImportedPackages.ImportedPackage optionalImport = optionalImportSet.iterator().next();
        assertEquals("import should match", expectOptionalImport, optionalImport.toString());

        final String expectMandatoryImport = "javax.json;version=\"[1.0.0,2.0.0)\"";
        final Set<ImportedPackages.ImportedPackage> mandatoryImportSet = ImportedPackages
                .parseImportPackageHeader(expectMandatoryImport);
        assertNotNull("set should not be null", mandatoryImportSet);
        assertFalse("set should not be empty", mandatoryImportSet.isEmpty());
        final ImportedPackages.ImportedPackage mandatoryImport = mandatoryImportSet.iterator().next();
        assertEquals("import should match", expectMandatoryImport, mandatoryImport.toString());

        ImportedPackages.Result mandatoryResult = mandatoryImport
                .satisfied(Collections.singletonMap("javax.json", Collections.singleton(Version.valueOf("2.0.0"))));

        assertNotSame("expect non-same result", ImportedPackages.ImportedPackage.OK, mandatoryResult);
        assertNotSame("expect non-same result", ImportedPackages.ImportedPackage.NO_EXPORTS, mandatoryResult);
    }

    @Test(expected = RepositoryException.class)
    public void testHandleIoExceptionAsRepositoryException() throws Exception {
        final String path = "/apps/test/install/test.jar";
        final Node ntFileNode = mock(Node.class);
        when(ntFileNode.isNodeType(JcrConstants.NT_FILE)).thenReturn(true);
        when(ntFileNode.hasProperty(Property.JCR_DATA)).thenReturn(true);

        final Property data = mock(Property.class);
        when(ntFileNode.getProperty(Property.JCR_DATA)).thenReturn(data);
        final Binary binary = mock(Binary.class);
        when(data.getBinary()).thenReturn(binary);
        final InputStream throwingInput = mock(InputStream.class);
        doThrow(IOException.class).when(throwingInput)
                .read(any(byte[].class), anyInt(), anyInt());

        when(binary.getStream()).thenReturn(throwingInput);

        ProgressCheck check = new ImportedPackages.Check(Collections.emptyMap());
        check.importedPath(PackageId.fromString("my_packages:test"), path, ntFileNode);
    }


    @Test
    public void testMismatchedVersions() throws Exception {
        final File jarFile = new File(pack.getParentFile(), "package-imports-version-mismatch.jar");
        if (jarFile.exists()) {
            jarFile.delete();
        }
        TestPackageUtil.buildJarFromDir(new File("src/test/resources/package-imports-version-mismatch-jar"),
                jarFile, Collections.emptyMap());

        final ProgressCheck check = new ImportedPackages().newInstance(key("aemVersion", arr("6.5")).get());
        final PackageId packageId = PackageId.fromString("my_packages:test");
        check.startedScan();
        check.beforeExtract(packageId, null, null, null, Collections.emptyList());

        final String path = "/apps/test/install/test.jar";
        final Node ntFileNode = mock(Node.class);
        when(ntFileNode.isNodeType(JcrConstants.NT_FILE)).thenReturn(true);
        when(ntFileNode.hasProperty(Property.JCR_DATA)).thenReturn(true);

        final Property data = mock(Property.class);
        when(ntFileNode.getProperty(Property.JCR_DATA)).thenReturn(data);
        final Binary binary = mock(Binary.class);
        when(data.getBinary()).thenReturn(binary);
        doAnswer(call -> new FileInputStream(jarFile)).when(binary).getStream();

        check.importedPath(packageId, path, ntFileNode);

        check.finishedScan();

        assertEquals("reported violations", 1, check.getReportedViolations().size());
    }
}
