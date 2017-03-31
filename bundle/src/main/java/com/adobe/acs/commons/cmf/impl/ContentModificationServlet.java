package com.adobe.acs.commons.cmf.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletOutputStream;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.cmf.ContentModificationProcessor;
import com.adobe.acs.commons.cmf.IdentifiedResources;
import com.adobe.acs.commons.cmf.NoSuchContentModificationStepException;


/**
 * The ContentModificationServlet should be set on a resource with these 2 properties for configuration
 * <ul>
 *   <li><code>path</code> describes the repository path on which to work</li>
 *   <li><code>modificationStep</code> is the name of the ContentModificationtep which is going to be used</li>
 * </ul>
 *
 * When invoked on a GET request, the servlet will display only the resources which are going to be changed.
 * If invoked via POST the change is performed as well.
 * 
 */


@SlingServlet(
        methods = { "GET", "POST" },
        resourceTypes = { "acs-commons/components/cmf" },
        extensions = { "html" }
)
public class ContentModificationServlet extends SlingAllMethodsServlet {
	
	private static Logger log = LoggerFactory.getLogger(ContentModificationServlet.class);
	
	private static final String PATH = "path";
	private static final String MODIFICATION_STEP = "modificationStep";


	@Reference
	ContentModificationProcessor cmp;
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doGet (SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		
		ServletOutputStream out = response.getOutputStream();

		Resource resource = request.adaptTo(Resource.class);
		if (resource == null) {
			response.sendError(500, "No resource");
			log.error ("not invoked on a resource");
			return;
		}
		ValueMap props = resource.adaptTo(ValueMap.class);
		String usedModificationStep = props.get(MODIFICATION_STEP).toString();
		String path = props.get(PATH).toString();
		
		out.print(String.format("<p>Starting %s on path %s</p>", new Object[]{usedModificationStep,path}));
		
		try {
			IdentifiedResources id = cmp.identifyAffectedResources(usedModificationStep, path, request.getResourceResolver());
			Iterator<String> resources = id.getPaths().iterator();
			out.print("Found resources:<ul>");
			while (resources.hasNext()) {
				out.print (String.format("<li>%s</li>", resources.next()));
			}
			out.print("</ul>");			
			
		} catch (NoSuchContentModificationStepException e) {
			out.print ("<p>Did not find modification step called " + usedModificationStep + "</p>");
		}
	}

	
	protected void doPost (SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		
		ServletOutputStream out = response.getOutputStream();

		Resource resource = request.adaptTo(Resource.class);
		if (resource == null) {
			response.sendError(500, "No resource");
			log.error ("not invoked on a resource");
			return;
		}
		ValueMap props = resource.adaptTo(ValueMap.class);
		String usedModificationStep = props.get(MODIFICATION_STEP).toString();
		String path = props.get(PATH).toString();
		
		out.print(String.format("<p>Starting %s on path %s</p>", new Object[]{usedModificationStep,path}));
		
		try {
			IdentifiedResources id = cmp.identifyAffectedResources(usedModificationStep, path, request.getResourceResolver());
			Iterator<String> resources = id.getPaths().iterator();
			out.print("Found resources:<ul>");
			while (resources.hasNext()) {
				out.print (String.format("<li>%s</li>", resources.next()));
			}
			out.print("</ul>");
			
			out.print("<p>Start modification</p>");
			
			cmp.modifyResources(id, request.getResourceResolver());
			
			out.print("<p>Modification completed</p>");
			
			
		} catch (NoSuchContentModificationStepException e) {
			out.print ("<p>Did not find modification step called " + usedModificationStep + "</p>");
		}
	}
	
	
}
