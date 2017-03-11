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
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import com.adobe.acs.commons.cmf.ContentMigrationProcessor;
import com.adobe.acs.commons.cmf.IdentifiedResources;
import com.adobe.acs.commons.cmf.NoSuchContentMigrationStepException;

@SlingServlet(
        methods = { "GET" },
        resourceTypes = { "acs-commons/components/cmf/list" },
        selectors = { "list" },
        extensions = { "html" }
)
public class ContentMigrationServlet extends SlingAllMethodsServlet {
	
	private static final String PATH = "path";


	private static final String MIGRATION_STEP = "migrationStep";


	@Reference
	ContentMigrationProcessor cmp;
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doGet (SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		
		ServletOutputStream out = response.getOutputStream();
		
		String usedMigrationStep = request.getParameter(MIGRATION_STEP);
		String path = request.getParameter(PATH);
		
		out.print(String.format("<p>Starting %s on path %s</p>", new Object[]{usedMigrationStep,path}));
		
		try {
			IdentifiedResources id = cmp.identifyAffectedResources(usedMigrationStep, path, request.getResourceResolver());
			Iterator<String> resources = id.getResources().iterator();
			out.print("Found resources:<ul>");
			while (resources.hasNext()) {
				out.print (String.format("<li>%s</li>", resources.next()));
			}
			out.print("</ul>");
			
			out.print("<p>Start migration</p>");
			
			cmp.migrateResources(id, request.getResourceResolver());
			
			out.print("<p>Migration completed</p>");
			
			
		} catch (NoSuchContentMigrationStepException e) {
			out.print ("<p>Did not find migration step called " + usedMigrationStep + "</p>");
		}
		
		
		
		
	}

}
