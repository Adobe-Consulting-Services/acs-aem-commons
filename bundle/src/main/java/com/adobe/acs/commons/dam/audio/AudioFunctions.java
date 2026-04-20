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
package com.adobe.acs.commons.dam.audio;

import org.osgi.annotation.versioning.ProviderType;

import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.video.VideoProfile;

/**
 * Audio JSP functions.
 */
@ProviderType
public final class AudioFunctions {

    private AudioFunctions() {
    }

    /**
     * Get the HTML source for a rendition based on a profile.
     * 
     * @param rendition the rendition
     * @param profile the profile
     * @return the resulting HTML source
     */
    public static String getHtmlSource(Rendition rendition, VideoProfile profile) {
        return profile.getHtmlSource(rendition);
    }

}
