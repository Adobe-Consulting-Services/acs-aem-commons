package com.adobe.acs.commons.version.impl;

import org.apache.commons.lang.StringUtils;

public class EvolutionPathUtil {

    public static int getDepthForPath(String path) {
        return StringUtils.countMatches(StringUtils.substringAfterLast(path, "jcr:frozenNode"), "/");
    }

    public static String getRelativePropertyName(String path) {
        return StringUtils.substringAfterLast(path, "jcr:frozenNode").replaceFirst("/", "");
    }

    public static String getRelativeResourceName(String path) {
        return StringUtils.substringAfterLast(path, "jcr:frozenNode/");
    }

    public static int getLastDepthForPath(String path) {
        return StringUtils.countMatches(StringUtils.substringAfterLast(path, "jcr:content"), "/");
    }

    public static String getLastRelativePropertyName(String path) {
        return StringUtils.substringAfterLast(path, "jcr:content").replaceFirst("/", "");
    }

    public static String getLastRelativeResourceName(String path) {
        return StringUtils.substringAfterLast(path, "jcr:content/");
    }
}
