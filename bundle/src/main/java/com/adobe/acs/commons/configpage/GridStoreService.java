package com.adobe.acs.commons.configpage;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public interface GridStoreService {
    public static final String COLUMN_UID = "uid";
    public boolean deleteRows(ResourceResolver resolver,
            List<String> rows, Resource resource)
            throws GridOperationFailedException;
    public boolean addOrUpdateRows(ResourceResolver resolver,
            List<Map<String, String>> rows, Resource resource) throws GridOperationFailedException;
}
