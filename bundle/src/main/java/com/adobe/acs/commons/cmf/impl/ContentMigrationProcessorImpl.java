package com.adobe.acs.commons.cmf.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.cmf.ContentMigrationProcessor;
import com.adobe.acs.commons.cmf.ContentMigrationStep;
import com.adobe.acs.commons.cmf.IdentifiedResources;
import com.adobe.acs.commons.cmf.NoSuchContentMigrationStepException;

@Component
@Service
@Properties({
    @Property(name="felix.webconsole.label", value="cmf"),
    @Property(name="felix.webconsole.title", value="Content Migration Framework"),
})
public class ContentMigrationProcessorImpl extends HttpServlet implements ContentMigrationProcessor {

	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
			policy=ReferencePolicy.DYNAMIC, 
			referenceInterface=com.adobe.acs.commons.cmf.ContentMigrationStep.class)
	HashMap<String, ContentMigrationStep> registeredSteps = new HashMap<String,ContentMigrationStep>();
	
	private static final Logger log = LoggerFactory.getLogger(ContentMigrationProcessorImpl.class);
	
	

	
	protected void bindContentMigrationStep (ContentMigrationStep step, Map properties) {
		synchronized (registeredSteps) {
			String name = (String) properties.get(ContentMigrationStep.STEP_NAME);
			registeredSteps.put (name, step);
			log.info("registered content migration step '{}'", name);
		}
	}
	
	protected void unbindContentMigrationStep (ContentMigrationStep step, Map properties) {
		synchronized (registeredSteps) {
			String name = (String) properties.get(ContentMigrationStep.STEP_NAME);
			registeredSteps.remove(name);
			log.info("unregistered content migration step '{}'", name);
		}
	}
	
	protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
		PrintWriter pw = res.getWriter();
		pw.println("<div class='statline'>Registered Content Migration Steps:</div>");
		pw.println("<ul>");
		for (String step: registeredSteps.keySet()) {
			pw.println("<li>" + step + "</li>");
		}
		pw.println("</ul>");
	}


	@Override
	public Set<String> listAvailableMigrationSteps() {
		return registeredSteps.keySet();
	}


	@Override
	public IdentifiedResources identifyAffectedResources(String name, String path,
			ResourceResolver resolver) throws NoSuchContentMigrationStepException {
		
		ContentMigrationStep cms = registeredSteps.get(name);
		if (cms == null) {
			String msg = String.format("ContentMigrationStep %s not found", name);
			throw new NoSuchContentMigrationStepException(msg);
		}
		
		Resource rootResource = resolver.getResource(path);
		
		List <Resource> r=  cms.identifyResources(rootResource);
		
		List <String> result = new ArrayList<String> (r.size());
		Iterator<Resource> iter = r.iterator();
		while (iter.hasNext()) {
			Resource res = iter.next();
			result.add(res.getPath());
		}
		
		log.info("ContentMigrationStep {} identified {} resources below {} for migration", new Object[]{name,result.size(), path});
		
		return new IdentifiedResources (result, name);
		
		
	}

	@Override
	public void migrateResources(IdentifiedResources resources,
			ResourceResolver resolver)
			throws NoSuchContentMigrationStepException, PersistenceException {
		
		ContentMigrationStep cms = registeredSteps.get(resources.getContentMigrationStep());
		if (cms == null) {
			String msg = String.format("ContentMigrationStep %s not found", resources.getContentMigrationStep());
			throw new NoSuchContentMigrationStepException(msg);
		}
		
		Iterator<String> iter = resources.getResources().iterator();
		while (iter.hasNext()) {
			String path = iter.next();
			Resource toMigrate = resolver.getResource(path);
			cms.migrate(toMigrate);
		}
		
		
	}
	
}
