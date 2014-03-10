package com.adobe.acs.commons.configpage.impl;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.configpage.GridOperationFailedException;
import com.adobe.acs.commons.configpage.GridStoreOperation;
@Component(
        label = "ACS AEM Commons - delete Grid store",
        description = "Service used to persist the deletions in grid store",
        immediate = false,
        metatype = false
)
@Service
public class DeleteGridStoreOperation implements GridStoreOperation {
    private static final Logger log = LoggerFactory
            .getLogger(DeleteGridStoreOperation.class);

    @Override
    public boolean execute(ResourceResolver resolver,
            List<Map<String, String>> rows, Resource resource)
            throws GridOperationFailedException {
        try {
            Session session = resolver.adaptTo(Session.class);
            for (Map<String, String> row : rows) {
                Resource rowResource = resource.getChild(row.get(COLUMN_KEY));
                if (rowResource != null) {
                    Node node = rowResource.adaptTo(Node.class);
                    node.remove();
                }
            }

            session.save();
            return true;
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            throwException(e);
        }
        return false;
    }

    @Override
    public String getOperationName() {
        return "delete";
    }

    protected GridOperationFailedException throwException(Exception e)
            throws GridOperationFailedException {
        throw new GridOperationFailedException(e.getMessage());
    }

}
