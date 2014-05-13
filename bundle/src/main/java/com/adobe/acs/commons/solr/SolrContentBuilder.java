package com.adobe.acs.commons.solr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ContentBuilder;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import com.day.cq.replication.ReplicationException;

@Component
@Service
@Properties({
	@Property(name="name", value=SolrContentBuilder.NAME)
})
public class SolrContentBuilder implements ContentBuilder {

	public final static String TITLE = "Solr Content Builder";
	public final static String NAME = "solrcontentbuilder";

	private static final Logger log = LoggerFactory.getLogger(SolrContentBuilder.class);

	@Override
	public ReplicationContent create(Session session, ReplicationAction action,
			ReplicationContentFactory factory) throws ReplicationException {

		JSONObject json = null;
		try {
			String path = action.getPath();

			if (action.getType() == ReplicationActionType.ACTIVATE) {
				json = buildActivationJson (session, path);
			} else if (action.getType() == ReplicationActionType.DEACTIVATE) {
				json = buildDeactivationJson (path);
			} else if (action.getType() == ReplicationActionType.DELETE) {
				json = buildDeactivationJson (path);
			} else if (action.getType() == ReplicationActionType.TEST) {
				json = buildTestJson (path);
			}
		} catch (JSONException e) {
			log.error ("Cannot build JSON",e);
		}

		if (json != null) {
			
			try {
				File outfile = File.createTempFile("aem","solr");
				//FileOutputStream os = new FileOutputStream (outfile);
				PrintWriter pw = new PrintWriter(outfile);
				json.write (pw);
				pw.close();
				log.info("JSON = " + json.toString());
				return factory.create("text/json", outfile, true);
			} catch (IOException e) {
				log.error("Problems serializing replication payload for solr",e);
			} catch (JSONException e) {
				log.error("Problems serializing JSON payload for solr",e);
			}
			
		}



		return null;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getTitle() {
		return TITLE;
	}


	// helpers

	JSONObject buildActivationJson (Session session, String path) throws JSONException {

		JSONObject content = new JSONObject().put("id", path);
		content.put("name", "irgendwas");
		JSONObject document = new JSONObject().put("doc", content);
		JSONObject root = new JSONObject().put("add", document);

		return root;
	}
	
	JSONObject buildDeactivationJson (String path) throws JSONException {
		
		JSONObject content = new JSONObject().put("id", path);
		JSONObject root = new JSONObject().put("delete",content);
		
		return root;
	}

	JSONObject buildTestJson (String path) throws JSONException {
		
		JSONObject document = new JSONObject().put("id", "test");
		JSONObject root = new JSONObject().put("test", document);
		
		return root;
	}

}
