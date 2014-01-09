/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.replicatpageversion.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.adobe.acs.commons.replicatepageversion.ReplicatePageVersionService;
import com.adobe.acs.commons.replicatepageversion.impl.ReplicatePageVersionServiceImpl;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;

public class ReplicatePageVersionServiceImplTest {
    @Mock
    private Replicator replicator;

    @Mock
    private AgentManager agentManager;

    @InjectMocks
    private ReplicatePageVersionService rpvs = new ReplicatePageVersionServiceImpl();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        reset(replicator);
        reset(agentManager);
    }
    
    @Test
    public void testLocateVersionAndReplicateResource() throws Exception {
        final String[] agents = new String[]{"agent1"};
        final String[] rootPaths = new String[]{"/content/geometrixx/en"};
        final Date date = getDate("2013-12-21,00:00:00");
        final Date vDate=getDate("2013-12-01,00:00:00");
        final Date vDate1=getDate("2013-12-22,00:00:00");
        final Calendar cal=GregorianCalendar.getInstance();
        cal.setTime(vDate);
        final Calendar cal1=GregorianCalendar.getInstance();
        cal1.setTime(vDate1);
        final ResourceResolver resourceResolver = mock(ResourceResolver.class);
        final Session session = mock(Session.class);
        final Workspace wk =  mock(Workspace.class);
        final VersionManager vm =  mock(VersionManager.class);
        final VersionHistory vh =  mock(VersionHistory.class);
        final VersionIterator vi =  mock(VersionIterator.class);
        final Version v = mock(Version.class);
        final Version v1 = mock(Version.class);
        when(v.getCreated()).thenReturn(cal);
        when(v1.getCreated()).thenReturn(cal1);
        when(v.getName()).thenReturn("version1");
        when(vi.nextVersion()).thenReturn(v,v1);
        when(vi.hasNext()).thenReturn(true,true,false);
        when(session.getWorkspace()).thenReturn(wk);
        when(wk.getVersionManager()).thenReturn(vm);
        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
 
        when(vm.getVersionHistory(rootPaths[0])).thenReturn(vh);
        when(vh.getAllVersions()).thenReturn(vi);
   
        
        final Resource res = mock(Resource.class);
        when(res.getPath()).thenReturn(rootPaths[0]);
        final Node node = mock(Node.class);
        when(resourceResolver.getResource(rootPaths[0])).thenReturn(res);
        when(res.adaptTo(Node.class)).thenReturn(node);
        when(session.getNode(rootPaths[0])).thenReturn(node);
        when(node.hasNode(NameConstants.NN_CONTENT)).thenReturn(true);
        final Node node1 = mock(Node.class);
        when(node1.getPath()).thenReturn(rootPaths[0]);
        when(node.getNode(NameConstants.NN_CONTENT)).thenReturn(node1);
        when(node1.isNodeType(JcrConstants.MIX_VERSIONABLE)).thenReturn(true);
        when(node.isNodeType("nt:hierarchyNode")).thenReturn(true);
        final Resource res1 = mock(Resource.class);
        final Resource res2 = mock(Resource.class);
        final Resource[] ress = new Resource[]{res1,res2};
        final List<Resource> resources = Arrays.asList(ress);
        final Iterator<Resource> itr = resources.iterator();
        when(resourceResolver.listChildren(res)).thenReturn(itr);
        
        when(res1.adaptTo(Node.class)).thenReturn(node1);
        when(res2.adaptTo(Node.class)).thenReturn(node1);
        when(node1.isNodeType("nt:hierarchyNode")).thenReturn(false);
        rpvs.locateVersionAndReplicateResource(resourceResolver, rootPaths, agents, date);
        verify(replicator, times(1)).replicate(eq(session), eq(ReplicationActionType.ACTIVATE), eq(rootPaths[0]),
                any(ReplicationOptions.class));
        verifyNoMoreInteractions(replicator);
    }
    private Date getDate(String datetime) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd,hh:mm:ss");
            date = sdf.parse(datetime);
        } catch (Exception e) {
           
        }
        return date;
    }
}
