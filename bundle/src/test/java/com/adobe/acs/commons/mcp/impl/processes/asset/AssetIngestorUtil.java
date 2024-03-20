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
package com.adobe.acs.commons.mcp.impl.processes.asset;

import java.util.Arrays;
import java.util.List;

/**
 * Util class for AssetIngestor tests
 */
public class AssetIngestorUtil {

    static final List<AssetIngestorPaths> FILE_PATHS = Arrays.asList(
            new AssetIngestorPaths("/qwew/qwewqewqe.jpg"),
            new AssetIngestorPaths("/qwew/qwew-#/qwewqewqe123213*(0-%.jpg", "/qwew/qwew--/qwewqewqe123213--0--.jpg", "/qwew/qwew-#/qwewqewqe123213*(0-%.jpg"),
            new AssetIngestorPaths("/qwew/qwew-#/qwewqe-.jpg", "/qwew/qwew--/qwewqe-.jpg"),
            new AssetIngestorPaths("/?/qwew/qwew-#/qwewqe-.jpg", "/-/qwew/qwew--/qwewqe-.jpg", "/-/qwew/qwew-#/qwewqe-.jpg"),
            new AssetIngestorPaths("/qwe    wqe-.jpg", "/qwe----wqe-.jpg")
    );

    static final List<AssetIngestorPaths> FOLDER_PATHS = Arrays.asList(
            new AssetIngestorPaths("/qwewqewqe123213"),
            new AssetIngestorPaths("/Fwewqewqe123213"),
            new AssetIngestorPaths("/qw-/ewqewqe123213"),
            new AssetIngestorPaths("/qwew/qwew-#/qwewqewqe123213*(0-%.jpg", "/qwew/qwew--/qwewqewqe123213--0---jpg", "/qwew/qwew-#/qwewqewqe123213-0jpg")
    );

    static class AssetIngestorPaths {
        String actualPath;
        String expectedPath;
        String expectedPreservedPath;

        public AssetIngestorPaths(String actualPath) {
            this(actualPath, actualPath, actualPath);
        }

        public AssetIngestorPaths(String actualPath, String expectedPath) {
            this(actualPath, expectedPath, actualPath);
        }

        public AssetIngestorPaths(String actualPath, String expectedPath, String expectedPreservedPath) {
            this.actualPath = actualPath;
            this.expectedPath = expectedPath;
            this.expectedPreservedPath = expectedPreservedPath;
        }

        public String getActualPath() {
            return actualPath;
        }

        public String getExpectedPath() {
            return expectedPath;
        }

        public String getExpectedPreservedPath() {
            return expectedPreservedPath;
        }
    }
}
