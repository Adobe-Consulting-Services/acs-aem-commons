package com.adobe.acs.commons.util.datadefinitions;

import java.util.Map;

public interface ResourceDefinition {
    String getPath();

    String getId();

    String getName();

    String getTitle();

    Map<String, String> getLocalizedTitles();

    String getDescription();

    boolean isOrdered();
}
