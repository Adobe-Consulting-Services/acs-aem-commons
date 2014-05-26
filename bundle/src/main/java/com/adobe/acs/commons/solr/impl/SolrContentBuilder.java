package com.adobe.acs.commons.solr.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.solr.SolrMetadataBuilder;
import com.adobe.acs.commons.solr.SolrMetadataBuilder.ResolvingType;
import com.day.cq.dam.api.Asset;
import com.day.cq.replication.ContentBuilder;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationContent;
import com.day.cq.replication.ReplicationContentFactory;
import com.day.cq.replication.ReplicationException;
import com.day.cq.wcm.api.Page;


/**
 * This contentbuilder builds a JSON payload format, which is suited for
 * updating an external SOLR search engine (tested with Solr 4.7).
 * 
 * It supports activation and deactivation/deletion of content in the solr
 * index.
 * 
 * SolrMetadataBuilders are dynamically registerd and used (lowest ranking come
 * first)
 * 
 */

@Component
@Service
@Properties({
 @Property(name = "name", value = SolrContentBuilder.NAME)
})
@Reference(name = "metadataBuilder", referenceInterface = SolrMetadataBuilder.class, cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class SolrContentBuilder implements ContentBuilder {

    public final static String TITLE = "Solr Content Builder";
    public final static String NAME = "solrcontentbuilder";
    
    private final static String PN_JCR_PRIMARYTYPE = "jcr:PrimaryType";
    private final static String PN_SLING_RESOURCETYPE = "sling:resourceType";

    private static final Logger log = LoggerFactory
	    .getLogger(SolrContentBuilder.class);

    // sorted list of SolrMetaDataBuilderHolders, used for bind/unbind
    private List<MetadataBuilderHolder> metadataBuilderHolders = new ArrayList<MetadataBuilderHolder>();

    // all registered SolrMetadataBuilders
    private SolrMetadataBuilder[] cachedBuilders = new SolrMetadataBuilder[0];

    @Reference
    ResourceResolverFactory rrfac;

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


    // main routines to build JSON

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
     * find the correct SolrMetadataBuilder which is will be used to build the
     * JSON data for this resource
     * 
     * @param resource
     *            the resource
     * @return
     */
    protected SolrMetadataBuilder getBuilderForResource(Resource resource) {

	ResolvingType type;
	String resourcetype;
	if (resource.adaptTo(Page.class) != null) {
	    type = ResolvingType.CQ_PAGE;
	    Page p = resource.adaptTo(Page.class);
	    resourcetype = p.getProperties().get("sling:resourceType", "");
	} else if (resource.adaptTo(Asset.class) != null) {
	    type = ResolvingType.DAM_ASSET;
	    Asset a = resource.adaptTo(Asset.class);
	    resourcetype = a.getMimeType();
	} else {
	    log.info("Cannot serialize resource to JSON: {}",
		    resource.getPath());
	    return null;
	}
	for (SolrMetadataBuilder builder : cachedBuilders) {
	    if (builder.canHandle(resourcetype, type)) {
		return builder;
	    }
	}
	log.error("We have a page or an asset, but no matching SolrMetadataBuilder! Resource = {}", resource.getPath());
	return null;
	
	
    }

    /**
     * Return a matching resource for a node object
     * 
     * @param node
     *            the node
     * @return the node
     * @throws LoginException
     * @throws RepositoryException
     */
    protected Resource getResourceForNode(Node node) throws LoginException,
	    RepositoryException {
	// String user = node.getSession().getUserID();
	String path = node.getPath();
	ResourceResolver adminResolver = null;
	Resource matchingResource = null;

	adminResolver = rrfac.getAdministrativeResourceResolver(null);
	matchingResource = adminResolver.getResource(path);

	return matchingResource;
    }

    /**
     * Helper functions for dynamic registering/unregistering of
     * SolrMetadataBuilders
     * 
     * We want to support ordering of MetadataBuilders, so we need to build some
     * more glue code.
     * 
     * btw: ordering is "lower numbers first".
     * 
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
	log.info("Registered SolrMetadataBuilder "
		+ builder.getClass().getName());
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
	    log.info("Unregistered SolrMetadataBuilder "
		    + builder.getClass().getName());
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
