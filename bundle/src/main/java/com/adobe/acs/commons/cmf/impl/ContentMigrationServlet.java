package com.adobe.acs.commons.cmf.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletOutputStream;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import com.adobe.acs.commons.cmf.ContentMigrationProcessor;

@SlingServlet(
        methods = { "GET" },
        resourceTypes = { "acs-commons/components/cmf/list" },
        selectors = { "list" },
        extensions = { "html" }
)
public class ContentMigrationServlet extends SlingAllMethodsServlet {
	
	@Reference
	ContentMigrationProcessor cmp;
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected void doGet (SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		
		ServletOutputStream out = response.getOutputStream();
		Set<String> registeredProcessors = cmp.listAvailableMigrationSteps();
		
		out.print("List of registered processors (" + registeredProcessors.size() + ")");
		Iterator<String> iter = registeredProcessors.iterator();
		
		while (iter.hasNext()) {
			String s = iter.next();
			out.println(s + ",");
		}
		
		
		
		
	}

}
