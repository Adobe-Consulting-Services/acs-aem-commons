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
package com.adobe.acs.commons.replicatevepageversion;


import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.AgentIdFilter;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;

@Component(policy = ConfigurationPolicy.REQUIRE)
@Service(javax.servlet.Servlet.class)
@Properties({
    @Property(name="felix.webconsole.label", value="replicatepageversion"),
    @Property(name="felix.webconsole.title", value="replicatepageversion"),
    @Property(name="felix.webconsole.category", value="Sling")
})
public class ReplicatePageVersionConsolePlugin extends HttpServlet {

	private static final long serialVersionUID = -26387035376464182L;
	private static final Logger log = LoggerFactory
			.getLogger(ReplicatePageVersionConsolePlugin.class);
	
	@Reference
	private transient ResourceResolverFactory resolverFactory;
	
	private transient ResourceResolver resolver;
	
    @Reference
    private transient Replicator replicator;
    
    @Activate
    @Modified
    protected void activate( ComponentContext ctx ) throws InterruptedException{

    	try {
			resolver=resolverFactory.getAdministrativeResourceResolver(null);
		} catch (LoginException e) {
			
		}
    }
    

    
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
     throws ServletException, IOException {
        final PrintWriter pw = res.getWriter();
       
      
        pw.println("<div id=\"errmsg\" style=\"display:none;background-color: #EFCDC7\">"
        		+ ""
        		+ "</div>"
        		+ "<form name=\"replicateversion\" method=\"post\" id=\"frmVersion\">");
        pw.println("Enter the Base site root Path example (/content/<yourpath>) (If no path given / is selected..ex., /content/geometrixx): ");
        pw.println("<input type=\"text\" name=\"root\" id=\"root\" style=\"width:200px\">");
        pw.println("<br/>");
        pw.println("Enter the Base site dam assets Path example (/content/dam/<yourpath>) (If no path given / is selected..ex., /content/dam/geometrixx): ");
        pw.println("<input type=\"text\" name=\"rootdam\" id=\"rootdam\" style=\"width:200px\">");
        pw.println("<br/>");
        pw.println("Enter the time at which you want the versions(the format should be 2012-04-29,00:00:00");
        pw.println("<input type=\"text\" name=\"datetime\" id=\"datetime\" style=\"width:200px\">");
        pw.println("<br/>");
        pw.println("Enter the agent id");
        pw.println("<input type=\"text\" name=\"agent\" id=\"agent\" style=\"width:200px\">");
        pw.println("<br/>");
       pw.println("<input type=\"button\" value=\"replicate version\" id=\"replicateVersion\" />");
       pw.println("</form>"
       		+ "<div id=\"replicationqueue\"></div>");
       pw.println("<script>");
       pw.println("$(\"#replicateVersion\").click(function(){"
    		   + "var msg=\"Please Wait.......\";"
  				+ "$(\"#replicationqueue\").html(msg);"
       		+ "$.post(\"/system/console"+req.getPathInfo()+"\",$(\"#frmVersion\").serialize(),function(data){"
       				+ "var res=JSON.parse(data);"
       				+ "if(res.status=='error'){"
       				+ "$(\"#errmsg\").html(res.error);"
       				+ "$(\"#errmsg\").css(\"display\",\"block\");"
       				+ "$(\"#replicationqueue\").html('');"
       				+ "}else{"
       				+ "$(\"#errmsg\").html(\"\");"
       				+ "$(\"#errmsg\").css(\"display\",\"none\");"
       				+ "var lnk=\"<a href='/etc/replication/agents.author/\"+res.agent+\".html' target='_new'>View Replication Queue</a><br/><a href='/etc/replication/agents.author/\"+res.agent+\".log.html#end' target='_new'>View Replication Queue log</a>\";"
       				+ "$(\"#replicationqueue\").html(lnk);"
       				+ "}"
       				+ "});"
       		+ "});");
       pw.println("</script>");

    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse res)
     throws ServletException, IOException {
    	
    	String root=getNormalizedPath(req.getParameter("root"));
    	String rootdam=getNormalizedPath(req.getParameter("rootdam"));
      	Date date=getDate(req.getParameter("datetime"));
     	String agent=req.getParameter("agent");
     	SiteVersionMetaData svm=new SiteVersionMetaData(root,rootdam,date,agent);
     	JSONObject obj=null;
     	boolean error=false;
     	try {
			
				obj=validate(svm);
			
			Iterator<Resource> resourceIterator=null;
			if(!obj.has("error")){
		     	if(svm.pageRoot!=null){
		    	 resourceIterator=getResourcesIterator(svm.pageRoot,PAGE);
		    	
					replicateResource(resourceIterator, agent, date);
				
		     	}
		     	if(svm.assetRoot!=null){
		       	resourceIterator=getResourcesIterator(svm.assetRoot,ASSET);
		      
					replicateResource(resourceIterator, agent, date);
				
		     	}
			}
     	}catch (JSONException e1) {
			error=true;
		}catch (RepositoryException e) {
			error=true;
		} catch (ReplicationException e) {
			error=true;
		}
     	if(error){
			if(obj==null)obj=new JSONObject();
			
			try {
				obj.put("error", "System Error.");
				obj.put("status", "error");
			} catch (JSONException e1) {
				
			}
     	}

     	try {
     	if(!obj.has("error")){
     		
				obj.put("status", "replicated");
				obj.put("agent", agent);
			
     	}
     	obj.write(res.getWriter());
     	} catch (JSONException e) {
			
		}

    }
    /**
     * 
     * @param resource
     * @param date
     * @param session
     * @return
     */
 private Version getAppropriateVersion(Resource resource,Date date,Session session){
	 Calendar cal=GregorianCalendar.getInstance();
	 cal.setTime(date);
	   String path = resource.getPath();
	   ArrayList<Version> versions=findAllVersions(path, session);
	   Collections.sort(versions, new Comparator<Version>() {
           public int compare(Version v1, Version v2) {
               try {
				return v2.getCreated().compareTo(v1.getCreated());
			} catch (RepositoryException e) {
				return 0;
			}
           }
       });
	   
	   for(Version v:versions){
		   try {
			if(v.getCreated().compareTo(cal)<1){
				return v;
			}
		} catch (RepositoryException e) {
			
		}
	   }
	   return null;
	   
 }
 /**
  * 
  * @param path
  * @param session
  * @return
  */
 private ArrayList<Version> findAllVersions(String path,Session session){
	 ArrayList<Version> versions = new ArrayList<Version>();
	 try{
		  Node node = (Node) session.getItem(path);
		  if (node.hasNode(NameConstants.NN_CONTENT)) {
			  Node contentNode = node.getNode(NameConstants.NN_CONTENT);
			  if (contentNode.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
				  VersionIterator iter = session.getWorkspace().getVersionManager().getVersionHistory(contentNode.getPath()).getAllVersions();
				  while (iter.hasNext()) {
					   Version v = iter.nextVersion();
					 versions.add(v);
				  }
			  }
		  }
	 }catch(Exception e){
		 
	 }
	 return versions;
}
 /**
  * 
  * @param path
  * @param PAGEORASSET
  * @return
  */
 private Iterator<Resource> getResourcesIterator(String path,int PAGEORASSET){
	 if(PAGEORASSET==PAGE){
	    	return resolver.findResources(path+"//element(*,cq:Page)"	, "xpath");
	    	
	 }else if(PAGEORASSET==ASSET){
		 return resolver.findResources(path+"//element(*,dam:Asset)"	, "xpath");
	 }
	 return null;
 }
 /**
  * 
  * @param resourceIterator
  * @param agent
  * @param date
 * @throws RepositoryException 
 * @throws ReplicationException 
  */
 private void replicateResource(Iterator<Resource> resourceIterator,String agent,Date date) throws RepositoryException, ReplicationException{
	 Session session=resolver.adaptTo(Session.class);
   	Resource resource=null;
   	Version v=null;
   	ReplicationOptions opts = new ReplicationOptions();

   	AgentIdFilter agentFilter=new AgentIdFilter(agent);
   	opts.setFilter(agentFilter);
   	while(resourceIterator.hasNext()){
   		resource=resourceIterator.next();
   		v=getAppropriateVersion(resource, date, session);
   		if(v==null){
   			continue;
   		}
   	
		opts.setRevision(v.getName());
	
		replicator.replicate(session, ReplicationActionType.ACTIVATE, resource.getPath(),opts);
		log.info("replicating  path:"+resource.getPath());
		
	
   	}
 }
 /**
  * 
  * @param path
  * @return
  */
 private String getNormalizedPath(String path){
	 String root=path;
	 if(root==null){
		 return null;
	 }
	   while (root.endsWith("/")) {
           root = root.substring(0, root.length() - 1);
       }
   	
       if (root.length() == 0) {
           root = "/";
       }
       if(!root.startsWith("/jcr:root")){
       	root="/jcr:root"+root;
       }
       return root;
 }
 /**
  * 
  * @param datetime
  * @return
  */
 private Date getDate(String datetime){
		Date date=null;
       	try {
       		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd,hh:mm:ss");
			 date=sdf.parse(datetime);
		} catch (Exception e) {
			
		}
       	return date;
 }
 /**
  * 
  * @param svm
  * @return
  * @throws JSONException
  */
private JSONObject validate(SiteVersionMetaData svm) throws JSONException{
	JSONObject obj=new JSONObject();
	if((svm.pageRoot==null||"".equals(svm.pageRoot))&&svm.assetRoot==null||"".equals(svm.assetRoot)){
		obj.put("error", "both pages root path and assets root path cannot be null");
		return obj;
	}
	if(svm.date==null ){
		obj.put("error", "Enter the time at which you want the versions");
		return obj;
	}
	if(svm.agent==null ||"".equals(svm.agent)){
		obj.put("error", "Enter the agent id");
		return obj;
	}
	return obj;
}
 private static final int PAGE=1;
 private static final int ASSET=0;
}

class SiteVersionMetaData {
	 final String pageRoot;
	 final String assetRoot;
	final Date date;
	final String agent;
	public SiteVersionMetaData(String pageRoot,String assetRoot,Date date,String agent){
		this.pageRoot=pageRoot;
		this.assetRoot=assetRoot;
		this.date=date;
		this.agent=agent;
	}
}