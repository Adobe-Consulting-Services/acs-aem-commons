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
package com.adobe.acs.commons.it.build;

import static org.junit.Assert.assertEquals;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.Test;

public class BundleContainsTldsIT {

    private static final int EXPECTED_TLD_FILES = 4;

    @Test
    public void test() throws Exception {
        String artifactPath = System.getProperty("artifactPath");
        if (artifactPath == null) {
            System.err.println("Artifact Path not set, presumably because this test is run from an IDE. Not checking JAR contents.");//NOPMD
            return;
        }

        int found = 0;

        JarFile jarFile = new JarFile(artifactPath);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".tld")) {
                found++;
            }
        }
        assertEquals("Expected number of TLDs not found", EXPECTED_TLD_FILES, found);
    }

}
