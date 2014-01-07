package com.adobe.acs.commons.replicatepageversion.impl;

import static org.junit.Assert.assertEquals;
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
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import aQute.bnd.resolver.Resolver;

import com.adobe.acs.commons.replicatepageversion.ReplicatePageVersionService;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;

public class ReplicatePageVersionServiceImplTest {

    @Mock
    private AgentManager agentManager;

    @Mock
    private  Replicator replicator;
    
    @Mock
    private ResourceResolverFactory resolverFactory;

    @InjectMocks
    private ReplicatePageVersionService rps = new ReplicatePageVersionServiceImpl();
    
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
    public void testgetAppropriateVersion() throws Exception {
        final Resource resource=mock(Resource.class);
        final Session session=mock(Session.class);
        final String path="/content/geometrixx/en";
        when(resource.getPath()).thenReturn(path);
      
        final Date date=getDate("2013-12-01,00:00:00");
        final Node node=mock(Node.class);
        when(session.getNode(path)).thenReturn(node);
        final Node contentNode=mock(Node.class);
     
        when(node.hasNode(NameConstants.NN_CONTENT)).thenReturn(true);
        when(node.getNode(NameConstants.NN_CONTENT)).thenReturn(contentNode);
        when(contentNode.isNodeType(JcrConstants.MIX_VERSIONABLE)).thenReturn(true);
        when(contentNode.getPath()).thenReturn(path+"/"+NameConstants.NN_CONTENT);
        Workspace wrkspce=mock(Workspace.class);
        when(session.getWorkspace()).thenReturn(wrkspce);
        VersionManager vm=mock(VersionManager.class);
        when(wrkspce.getVersionManager()).thenReturn(vm);
        VersionHistory gh=mock(VersionHistory.class);
        when(vm.getVersionHistory(contentNode.getPath())).thenReturn(gh);
        VersionIterator iter=mock(VersionIterator.class);
        when(gh.getAllVersions()).thenReturn(iter);
        when(iter.hasNext()).thenReturn(true, false);
        final String vName="version1";
        Version v=mock(Version.class);
        when(iter.nextVersion()).thenReturn(v);
        when(v.getName()).thenReturn(vName);
        Date vDate=getDate("2013-11-01,00:00:00");
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(vDate);
        when(v.getCreated()).thenReturn(cal);
        Version actual=rps.getAppropriateVersion(resource, date, session);
        assertEquals("version1", v.getName());
    }
    private List<Version> getVersions() throws Exception{
       final String path="/content/geometrixx/en";
       final String vName="version1";
       final Session session=mock(Session.class);
       final Node node=mock(Node.class);
       final Node contentNode=mock(Node.class);
        VersionIterator iter=mock(VersionIterator.class);
        Version v=mock(Version.class);
        when(session.getNode(path)).thenReturn(node);
        when(node.hasNode(NameConstants.NN_CONTENT)).thenReturn(true);
        when(node.getNode(NameConstants.NN_CONTENT)).thenReturn(contentNode);
        when(contentNode.isNodeType(JcrConstants.MIX_VERSIONABLE)).thenReturn(true);
        when(contentNode.getPath()).thenReturn(path+"/"+NameConstants.NN_CONTENT);
        Workspace wrkspce=mock(Workspace.class);
        when(session.getWorkspace()).thenReturn(wrkspce);
        VersionManager vm=mock(VersionManager.class);
        when(wrkspce.getVersionManager()).thenReturn(vm);
        VersionHistory gh=mock(VersionHistory.class);
        when(vm.getVersionHistory(contentNode.getPath())).thenReturn(gh);
        when(gh.getAllVersions()).thenReturn(iter);
       
        when(iter.hasNext()).thenReturn(true, false);
        when(iter.nextVersion()).thenReturn(v);
        when(v.getName()).thenReturn(vName);
        Date vDate=getDate("2013-11-01,00:00:00");
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(vDate);
        when(v.getCreated()).thenReturn(cal);
        List<Version> versions=rps.findAllVersions(path, session);  
        return versions;
    }
    @Test
    public void testgetResources() throws Exception {
       final ResourceResolver resolver=mock(ResourceResolver.class);
       final Resource res = mock(Resource.class);
       final String path="/content/geometrixx/en";
       when(resolver.getResource(path)).thenReturn(res);
      final Node node=mock(Node.class);
      when(node.isNodeType("nt:hierarchyNode")).thenReturn(true);
      when(res.adaptTo(Node.class)).thenReturn(node);
      String resname="res";
      when(res.getName()).thenReturn(resname);
      final Resource res1=mock(Resource.class);
      final Resource res2=mock(Resource.class);
      final Node node1=mock(Node.class);
      when(node1.isNodeType("nt:hierarchyNode")).thenReturn(false);
      when(res1.adaptTo(Node.class)).thenReturn(node1);
      when(res2.adaptTo(Node.class)).thenReturn(node1);
      
      final List<Resource> resources=Arrays.asList(new Resource[]{res1,res2});
     final Iterator<Resource> itr=resources.iterator();
     when(resolver.listChildren(res)).thenReturn(itr);
     
     List<Resource> actual=rps.getResources(resolver, path);
     assertEquals(1, actual.size());
     assertEquals(resname, actual.get(0).getName());
    }
   private List<Resource> getResources() throws Exception{
       final ResourceResolver resolver=mock(ResourceResolver.class);
       final Resource res = mock(Resource.class);
       final String path="/content/geometrixx/en";
       when(resolver.getResource(path)).thenReturn(res);
      final Node node=mock(Node.class);
      when(node.isNodeType("nt:hierarchyNode")).thenReturn(true);
      when(res.adaptTo(Node.class)).thenReturn(node);
      String resname="res";
      when(res.getName()).thenReturn(resname);
      final Resource res1=mock(Resource.class);
      final Resource res2=mock(Resource.class);
      final Node node1=mock(Node.class);
      when(node1.isNodeType("nt:hierarchyNode")).thenReturn(false);
      when(res1.adaptTo(Node.class)).thenReturn(node1);
      when(res2.adaptTo(Node.class)).thenReturn(node1);
      
      final List<Resource> resources=Arrays.asList(new Resource[]{res1,res2});
     final Iterator<Resource> itr=resources.iterator();
     when(resolver.listChildren(res)).thenReturn(itr);
     
     List<Resource> actual=rps.getResources(resolver, path);
     return actual;
   }
    @Test
    public void testfindAllVersions() throws Exception {
        final String vName="version1";
        final List<Version> versions=getVersions();
        assertEquals(1, versions.size());
        assertEquals(vName, versions.get(0).getName());
    }
//  
    @Test
    public void testReplicateResource() throws Exception {
        ResourceResolver resolver=mock(ResourceResolver.class);
        Session session=mock(Session.class);
        when(resolver.adaptTo(Session.class)).thenReturn(session);
        Date date =getDate("2013-12-01,00:00:00");
      Resource res=mock(Resource.class);
       String path="/content/geometrixx/en";
       when(resolver.getResource(path)).thenReturn(res);
       final Node node1=mock(Node.class);
       when(node1.isNodeType("nt:hierarchyNode")).thenReturn(true);
       when(res.adaptTo(Node.class)).thenReturn(node1);
       String resname="res";
       when(res.getName()).thenReturn(resname);
       when(res.getPath()).thenReturn(path);
       List<Resource> ress=Arrays.asList(new Resource[]{res});
       String vName="version1";
       final Node node=mock(Node.class);
       final Node contentNode=mock(Node.class);
        VersionIterator iter=mock(VersionIterator.class);
        Version v=mock(Version.class);
        when(session.getNode(path)).thenReturn(node1);
        when(node1.hasNode(NameConstants.NN_CONTENT)).thenReturn(true);
        when(node1.getNode(NameConstants.NN_CONTENT)).thenReturn(contentNode);
        when(contentNode.isNodeType(JcrConstants.MIX_VERSIONABLE)).thenReturn(true);
        Workspace wrkspce=mock(Workspace.class);
        when(session.getWorkspace()).thenReturn(wrkspce);
        VersionManager vm=mock(VersionManager.class);
        when(wrkspce.getVersionManager()).thenReturn(vm);
        VersionHistory gh=mock(VersionHistory.class);
        when(vm.getVersionHistory(contentNode.getPath())).thenReturn(gh);
        when(gh.getAllVersions()).thenReturn(iter);   
        when(iter.hasNext()).thenReturn(true,false);
        when(iter.nextVersion()).thenReturn(v);
        when(v.getName()).thenReturn(vName);
        Date vDate=getDate("2013-11-01,00:00:00");
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(vDate);
        when(v.getCreated()).thenReturn(cal);
       rps.replicateResource(resolver, ress.iterator(), "agent1", date);
       verify(replicator, times(1)).replicate(eq(session), eq(ReplicationActionType.ACTIVATE), eq(path),
               any(ReplicationOptions.class));
       verifyNoMoreInteractions(replicator);
    }
    @Test
    public void testGetAgent() throws Exception {
        final String agentId="Agent1";
        final Agent agent1 = mock(Agent.class);
        final Agent agent2 = mock(Agent.class);

        final AgentConfig agentConfig1 = mock(AgentConfig.class);
        final AgentConfig agentConfig2 = mock(AgentConfig.class);
        final Map<String, Agent> agents = mock(Map.class);
        final Collection<Agent> agentValues = Arrays.asList(new Agent[]{ agent1, agent2 });
        when(agentManager.getAgents()).thenReturn(agents);
        when(agents.values()).thenReturn(agentValues);
        when(agent1.getId()).thenReturn("Agent1");
        when(agent2.getId()).thenReturn("Agent 2");
        Agent agentActual=rps.getAgent(agentId);
        assertEquals(agentId, agentActual.getId());
    }
    @Test
    public void testgetAgents() throws Exception {
        final Agent agent1 = mock(Agent.class);
        final Agent agent2 = mock(Agent.class);

        final AgentConfig agentConfig1 = mock(AgentConfig.class);
        final AgentConfig agentConfig2 = mock(AgentConfig.class);
        final Map<String, Agent> agents = mock(Map.class);
        final Collection<Agent> agentValues = Arrays.asList(new Agent[]{ agent1, agent2 });
        when(agentManager.getAgents()).thenReturn(agents);
        when(agents.values()).thenReturn(agentValues);
        when(agent1.getId()).thenReturn("Agent 1");
        when(agent1.isEnabled()).thenReturn(true);
        when(agent1.isValid()).thenReturn(true);
        when(agent1.getConfiguration()).thenReturn(agentConfig1);

        when(agent2.getId()).thenReturn("Agent 2");
        when(agent2.isEnabled()).thenReturn(true);
        when(agent2.isValid()).thenReturn(true);
        when(agent2.getConfiguration()).thenReturn(agentConfig2);
        when(agentConfig1.getSerializationType()).thenReturn("durbo");
        when(agentConfig2.getSerializationType()).thenReturn("notdurbo");
        when(agentConfig1.getTransportURI()).thenReturn("http://localhost:4503/bin/receive?sling:authRequestLogin=1");
        when(agentConfig2.getTransportURI()).thenReturn("repo://var");
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("reversereplication", "false");
        when(agentConfig1.getProperties()).thenReturn(new ValueMapDecorator(tmp));
        Map<String, Object> tmp1 = new HashMap<String, Object>();
        tmp.put("reversereplication", "true");
        when(agentConfig2.getProperties()).thenReturn(new ValueMapDecorator(tmp1));
        
        List<Agent> agentsList = rps.getAgents();
        assertEquals(1, agentsList.size());

        assertEquals("Agent 1", agentsList.get(0).getId());
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
