package com.adobe.acs.commons.configpage;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public interface GridStoreOperation {
    public static final String COLUMN_KEY = "uid";
public boolean execute(ResourceResolver resolver, List<Map<String,String >> rows , Resource resource)  throws GridOperationFailedException;
String getOperationName();
}
