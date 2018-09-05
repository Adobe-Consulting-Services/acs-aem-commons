/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
 * %%
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
 * #L%
 */
package com.adobe.acs.commons.mcp.impl.processes;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Revision;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.api.msm.Blueprint;
import java.util.Calendar;
import java.util.Collection;
import javax.jcr.Node;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Stubs out the move method for testing purposes
 */
public class MockPageManager implements PageManager {

    ResourceResolver rr;
    Replicator replicator;

    public MockPageManager(ResourceResolver rr) {
        this.rr = rr;
    }

    @Override
    public Page getPage(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page getContainingPage(Resource rsrc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page getContainingPage(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page create(String string, String string1, String string2, String string3) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page create(String string, String string1, String string2, String string3, boolean bln) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page move(final Page page, final String destination, final String beforeName, final boolean shallow,
                     final boolean resolveConflict, final String[] adjustRefs) throws WCMException {
        try {
            if (rr == null) {
                throw new RuntimeException("Resource resolver was null");
            }

            if (replicator == null) {
                throw new RuntimeException("Replicator was not changed out -- will not work properly");
            }

            replicator.replicate(null, ReplicationActionType.DEACTIVATE, page.getPath());
            replicator.replicate(null, ReplicationActionType.ACTIVATE, destination + "/" + page.getName());

            return page;
        } catch (ReplicationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Resource move(final Resource resource, final String destination, final String beforeName,
            final boolean shallow, final boolean resolveConflict, final String[] adjustRefs) throws WCMException {
        return move(resource, destination, beforeName, shallow, resolveConflict, adjustRefs, new String[]{});
    }
    
    @Override
    public Resource move(Resource resource, String destination, String beforeName, boolean shallow,
            boolean resolveConflict, String[] adjustRefs, String[] publishRefs) throws WCMException {
        try {
            if (rr == null) {
                throw new RuntimeException("Resource resolver was null");
            }
            
            if (replicator == null) {
                throw new RuntimeException("Replicator was not changed out -- will not work properly");
            }
            
            replicator.replicate(null, ReplicationActionType.DEACTIVATE, resource.getPath());
            replicator.replicate(null, ReplicationActionType.ACTIVATE, destination + "/" + resource.getName());
            
            return resource;
        } catch (ReplicationException ex) {
            throw new WCMException(ex);
        }
    }    

    @Override
    public Page move(Page page, String destination, String beforeName, boolean shallow,
            boolean resolveConflict, String[] adjustRefs, String[] publishRefs) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Resource copy(CopyOptions co) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page copy(Page page, String string, String string1, boolean bln, boolean bln1) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page copy(Page page, String string, String string1, boolean bln, boolean bln1, boolean bln2) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Resource copy(Resource rsrc, String string, String string1, boolean bln, boolean bln1) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Resource copy(Resource rsrc, String string, String string1, boolean bln, boolean bln1, boolean bln2) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(Page page, boolean bln) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(Page page, boolean bln, boolean bln1) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(Resource rsrc, boolean bln) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(Resource rsrc, boolean bln, boolean bln1) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void order(Page page, String string) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void order(Page page, String string, boolean bln) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void order(Resource rsrc, String string) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void order(Resource rsrc, String string, boolean bln) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Template getTemplate(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Template> getTemplates(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Blueprint> getBlueprints(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Revision createRevision(Page page) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Revision createRevision(Page page, String string, String string1) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Revision> getRevisions(String string, Calendar clndr) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Revision> getRevisions(String string, Calendar clndr, boolean bln) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Revision> getChildRevisions(String string, Calendar clndr) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Revision> getChildRevisions(String string, Calendar clndr, boolean bln) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Revision> getChildRevisions(String string, String string1, Calendar clndr) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page restore(String string, String string1) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page restoreTree(String string, Calendar clndr) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Page restoreTree(String string, Calendar clndr, boolean bln) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void touch(Node node, boolean bln, Calendar clndr, boolean bln1) throws WCMException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
