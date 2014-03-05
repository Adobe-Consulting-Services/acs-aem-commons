package com.adobe.acs.commons.configpage.impl;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.configpage.GridOperationFailedException;
import com.adobe.acs.commons.configpage.GridStoreOperation;
@Component(
        label = "ACS AEM Commons - Update Grid store",
        description = "Service used to persist the changes in grid store",
        immediate = false,
        metatype = false
)
@Service
public class UpdateGridStoreOperation implements GridStoreOperation  {
    private static final Logger log = LoggerFactory
            .getLogger(UpdateGridStoreOperation.class);
    @Override
    public boolean execute(ResourceResolver resolver,
            List<Map<String, String>> rows, Resource resource) throws GridOperationFailedException {
        Node gridNode = resource.adaptTo(Node.class);
        Session session  = resolver.adaptTo(Session.class);
        try {
       for(Map<String,String> row :rows){
           Node rowNode = JcrUtils.getOrAddNode(gridNode  , row.get(COLUMN_KEY), JcrConstants.NT_UNSTRUCTURED);
           for(String key :row.keySet()){
               rowNode.setProperty(key,row.get(key));
           }
       }
     session.save();
     
        } catch (RepositoryException e) {
            log.error(e.getMessage(),e);
            throwException(e);
        }
        return true;
    }

    @Override
    public String getOperationName() {
        // TODO Auto-generated method stub
        return "update";
    }
    protected GridOperationFailedException throwException(Exception e) throws GridOperationFailedException{
        throw new GridOperationFailedException(e.getMessage());
    }

}
