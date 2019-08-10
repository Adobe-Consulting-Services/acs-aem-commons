/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.dam.audio.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@SuppressWarnings("checkstyle:abbreviationaswordinname")
class FFMpegAudioUtils {

    private static final Logger log = LoggerFactory.getLogger(FFMpegAudioUtils.class);

    private FFMpegAudioUtils() {
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN") // path sources are trusted
    static File resolveWorkingDir(String slingHome, String path) {
        if (path == null) {
            path = "";
        }
        // ensure proper separator in the path (esp. for systems, which do
        // not use "slash" as a separator, e.g Windows)
        path = path.replace('/', File.separatorChar);

        // create a file instance and check whether this is absolute. If not
        // create a new absolute file instance with the base dir (sling.home or
        // working dir of current JVM) and get the absolute path name from that
        File workingDir = new File(path);
        if (!workingDir.isAbsolute()) {
            File baseDir;
            if (slingHome == null) {
                /* use jvm working dir */
                baseDir = new File("").getAbsoluteFile();
            } else {
                baseDir = new File(slingHome).getAbsoluteFile();
            }
            workingDir = new File(baseDir, path).getAbsoluteFile();
        }
        try {
            log.info("ffmpeg working directory: {}", workingDir.getCanonicalPath());
        } catch (IOException e) {
            log.info("ffmpeg working directory: {}", workingDir.getAbsolutePath());
        }

        return workingDir;
    }

    static File createTempDir(File parentDir) throws IOException {
        return Files.createTempDirectory(parentDir.toPath(), "cqdam").toFile();
    }
}
