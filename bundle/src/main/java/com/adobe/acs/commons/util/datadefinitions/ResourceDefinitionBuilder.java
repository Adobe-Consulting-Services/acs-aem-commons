package com.adobe.acs.commons.util.datadefinitions;

public interface ResourceDefinitionBuilder {
    String PROP_NAME = "name";

    boolean accepts(String value);

    ResourceDefinition convert(String value);
}
