package com.adobe.acs.commons.cloudservices.pwa.impl;

import com.day.cq.commons.jcr.JcrConstants;

public class Constants {
    private Constants() {}

    public static final String HTML_EXTENSION = ".html";

    /***
     *  Manifest
     ***/

    /** Node Names **/
    public static final String NN_ICONS = "icons";

    /** Property Names **/
    public static final String PN_NAME = JcrConstants.JCR_TITLE;
    public static final String PN_SHORT_NAME = "shortName";
    public static final String PN_START_PATH = "startPath";
    public static final String PN_THEME_COLOR = "themeColor";
    public static final String PN_BACKGROUND_COLOR = "backgroundColor";
    public static final String PN_DISPLAY = "display";
    public static final String PN_VERSION = "version";
    public static final String PN_ICON_PATH = "fileReference";
    public static final String PN_ICON_SIZE = "size";

    /** JSON Keys **/
    public static final String KEY_NAME = "name";
    public static final String KEY_SHORT_NAME = "short_name";
    public static final String KEY_START_URL = "start_url";
    public static final String KEY_THEME_COLOR = "theme_color";
    public static final String KEY_BACKGROUND_COLOR = "background_color";
    public static final String KEY_DISPLAY = "display";
    public static final String KEY_SCOPE = "scope";
    public static final String KEY_ICONS = "icons";
    public static final String KEY_ICON_SRC = "src";
    public static final String KEY_ICON_SIZE = "sizes";
    public static final String KEY_ICON_TYPE = "type";

    /***
     *  Service Worker Configuration
     ***/

    /** Node Names **/
    public static final String NN_FALLBACK = "fallback";
    public static final String NN_PRE_CACHE= "pre-cache";
    public static final String NN_NO_CACHE = "no-cache";

    /** Property Names **/
    public static final String PN_PATH = "path";
    public static final String PN_PATTERN = "pattern";

    /** JSON Keys **/
    public static final String KEY_CACHE_NAME = "cache_name";
    public static final String KEY_VERSION = "version";
    public static final String KEY_FALLBACK = "fallback";
    public static final String KEY_PRE_CACHE= "pre_cache";
    public static final String KEY_NO_CACHE = "no_cache";
    public static final String KEY_PATTERN = "pattern";
    public static final String KEY_PATH = "path";


}
