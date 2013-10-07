package com.adobe.acs.commons.dam.audio;

import tldgen.Function;

import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.video.VideoProfile;

/**
 * Audio JSP functions.
 */
public class AudioFunctions {

    private AudioFunctions() {
    }

    /**
     * Get the HTML source for a rendition based on a profile.
     * 
     * @param rendition the rendition
     * @param profile the profile
     * @return the resulting HTML source
     */
    @Function
    public static String getHtmlSource(Rendition rendition, VideoProfile profile) {
        return profile.getHtmlSource(rendition);
    }

}
