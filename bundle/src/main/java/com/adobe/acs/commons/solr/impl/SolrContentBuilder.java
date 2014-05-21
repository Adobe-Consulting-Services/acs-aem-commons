package com.adobe.acs.commons.solr.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.solr.SolrMetadataBuilder;
import com.day.cq.replication.ContentBuilder;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import com.day.cq.replication.ReplicationException;


/**
 * This contentbuilder builds a JSON payload format, which is suited for updating an external
 * SOLR search engine (tested with Solr 4.7).
 * 
 * It supports activation and deactivation/deletion of content in the solr index.
 * 

 */

@Component
@Service
@Properties({
 @Property(name = "name", value = SolrContentBuilder.NAME)
})
@Reference(name = "metadatabuilder", referenceInterface = SolrMetadataBuilder.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class SolrContentBuilder implements ContentBuilder {

    public final static String TITLE = "Solr Content Builder";
    public final static String NAME = "solrcontentbuilder";

    private static final Logger log = LoggerFactory
	    .getLogger(SolrContentBuilder.class);

    // sorted list of SolrMetaDataBuilderHolders, used for bind/unbind
    private List<MetadataBuilderHolder> metadataBuilderHolders = new ArrayList<MetadataBuilderHolder>();

    // all registered SolrMetadataBuilders
    private SolrMetadataBuilder[] cachedBuilders = new SolrMetadataBuilder[0];

    @Override
    public ReplicationContent create(Session session, ReplicationAction action,
	    ReplicationContentFactory factory) throws ReplicationException {

	JSONObject json = null;
	try {
	    String path = action.getPath();

	    if (action.getType() == ReplicationActionType.ACTIVATE) {
		json = buildActivationJson(session, path);
	    } else if (action.getType() == ReplicationActionType.DEACTIVATE) {
		json = buildDeactivationJson(path);
	    } else if (action.getType() == ReplicationActionType.DELETE) {
		json = buildDeactivationJson(path);
	    } else if (action.getType() == ReplicationActionType.TEST) {
		json = buildTestJson(path);
	    }
	} catch (JSONException e) {
	    log.error("Cannot build JSON", e);
	}

	if (json != null) {

	    try {
		File outfile = File.createTempFile("aem", "solr");
		PrintWriter pw = new PrintWriter(outfile);
		json.write(pw);
		pw.close();
		return factory.create("text/json", outfile, true);
	    } catch (IOException e) {
		log.error("Problems serializing replication payload for solr",
			e);
	    } catch (JSONException e) {
		log.error("Problems serializing JSON payload for solr", e);
	    }

	}
	return ReplicationContent.VOID;
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

    /**
     * Build the JSON structure for adding/updating a solr document in the index
     * 
     * @param session
     * @param path
     * @return
     * @throws JSONException
     */
    JSONObject buildActivationJson(Session session, String path)
	    throws JSONException {

	JSONObject content = new JSONObject().put("id", path);
	content.put("name", "irgendwas");
	JSONObject document = new JSONObject().put("doc", content);
	JSONObject root = new JSONObject().put("add", document);

	return root;
    }

    /**
     * Build a JSON structure for deleting a solr document from the index
     * 
     * @param path
     * @return
     * @throws JSONException
     */
    JSONObject buildDeactivationJson(String path) throws JSONException {

	JSONObject content = new JSONObject().put("id", path);
	JSONObject root = new JSONObject().put("delete", content);

	return root;
    }

    JSONObject buildTestJson(String path) throws JSONException {

	JSONObject document = new JSONObject().put("id", "test");
	JSONObject root = new JSONObject().put("test", document);

	return root;
    }

    /**
     * Helper functions for dynamic registering/unregistering of
     * SolrMetadataBuilders
     */

    protected void bindMetadataBuilder(final SolrMetadataBuilder builder,
	    final Map<String, Object> properties) {
	final MetadataBuilderHolder mbh = new MetadataBuilderHolder();
	mbh.builder = builder;
	mbh.ranking = OsgiUtil.toInteger(
		properties.get(Constants.SERVICE_RANKING), 0);

	synchronized (this.metadataBuilderHolders) {
	    int index = 0;
	    while (index < this.metadataBuilderHolders.size()
		    && mbh.ranking < this.metadataBuilderHolders.get(index).ranking) {
		index++;
	    }
	    if (index == this.metadataBuilderHolders.size()) {
		this.metadataBuilderHolders.add(mbh);
	    } else {
		this.metadataBuilderHolders.add(index, mbh);
	    }
	    this.updateCache();
	}
    }

    protected void unbindMetadataBuilder(final SolrMetadataBuilder builder,
	    final Map<String, Object> properties) {
	synchronized (this.metadataBuilderHolders) {
	    final Iterator<MetadataBuilderHolder> i = this.metadataBuilderHolders
		    .iterator();
	    while (i.hasNext()) {
		final MetadataBuilderHolder current = i.next();
		if (current.builder == builder) {
		    i.remove();
		}
	    }
	    this.updateCache();
	}
    }

    private void updateCache() {
	final SolrMetadataBuilder[] localCache = new SolrMetadataBuilder[this.metadataBuilderHolders
		.size()];
	int index = 0;
	for (final MetadataBuilderHolder current : this.metadataBuilderHolders) {
	    localCache[index] = current.builder;
	    index++;
	}
	this.cachedBuilders = localCache;
    }

    private static final class MetadataBuilderHolder {
	public SolrMetadataBuilder builder;
	public int ranking;
    }

}
