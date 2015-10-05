package com.adobe.acs.commons.resources.solr;


import com.adobe.acs.commons.resources.GenericRestResource;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.RestAdapter;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component(metatype = true, label = "ACS AEM Commons - Solr Resource Provider",
        description = "Resource Provider that integrates your super duper remote Solr REST API with Sling's resource tree")
@Service
@Properties({
        @Property(name = ResourceProvider.ROOTS, value = SolrResourceProvider.ROOT_PATH),
        @Property(name = ResourceProvider.OWNS_ROOTS, boolValue = true),
        @Property(name = QueriableResourceProvider.LANGUAGES, value = {SolrResourceProvider.QUERY_LANGUAGE_SOLR})
})
public class SolrResourceProvider implements ResourceProvider, QueriableResourceProvider, ModifyingResourceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SolrResourceProvider.class);

    protected static final String ROOT_PATH = "/mnt/solr";
    protected static final String RESOURCE_TYPE = "com/adobe/acs/commons/solr-resource";
    protected static final String SOLR_URL = "http://192.168.0.214:8984/solr/procato_products";

    public static final String QUERY_LANGUAGE_SOLR = "solrQuery";

    protected SolrApi solrApi;

    protected void checkRetrofit() {
        if (solrApi == null) {
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(SOLR_URL)
//                    .setLog(Slf4jLog.INSTANCE)
                    .setLogLevel(RestAdapter.LogLevel.FULL)
                    .build();

            solrApi = restAdapter.create(SolrApi.class);
        }
    }

    /**
     * Check for special cases where the root resource is requested or path contains .json extension or some other
     * selectors and that stuff
     *
     * @param path Input
     * @return <code>True</code>, if the sanity check is passed
     */
    private static boolean sanityCheck(String path) {
        return !(StringUtils.isBlank(path) || StringUtils.equals(path, ROOT_PATH) ||
                StringUtils.endsWith(path, ".json") || StringUtils.contains(path, ".tidy") ||
                StringUtils.contains(".infinity", path));
    }

    /**
     * Map the input path to the "real" Solr path
     *
     * @param path Example: "/mnt/rest-resources/my-shit"
     * @return Example: "/my-shit"
     */
    private static String toSolrPath(String path) {
        return StringUtils.substring(path, ROOT_PATH.length());
    }

    /**
     * Map the input path to the "virtual" resource path
     *
     * @param path Example: "/my-shit"
     * @return Example: "/mnt/rest-resources/my-shit"
     */
    private static String toResourcePath(String path) {
        return ROOT_PATH + (StringUtils.startsWith(path, "/") ? path : "/" + path);
    }

    @Override
    public Resource getResource(ResourceResolver resourceResolver, HttpServletRequest httpServletRequest, String path) {
        return getResource(resourceResolver, path);
    }

    @Override
    public Resource getResource(ResourceResolver resourceResolver, String path) {
        LOG.debug("getResource() ... path={}", path);

        if (sanityCheck(path)) {
            LOG.debug("getting resource for path={}", path);
            String restPath = toSolrPath(path);
            checkRetrofit();

            Map<String, Object> thing = solrApi.get(restPath);

            return new GenericRestResource(resourceResolver, path, RESOURCE_TYPE,
                    new ValueMapDecorator((Map<String, Object>) thing.get("doc")));
        }

        return null;
    }

    @Override
    public Iterator<Resource> findResources(ResourceResolver resourceResolver, String query, String language) {
        LOG.debug("findResources() ... query={}", query);

        if (QUERY_LANGUAGE_SOLR.equals(language)) {
            checkRetrofit();

            Map<String, Object> response = solrApi.select(query);
            List<Map<String, Object>> documents = (List<Map<String, Object>>) ((Map<String, Object>) response.get("response")).get("docs");

            List<Resource> resourceList = new ArrayList<Resource>(documents.size());
            for (Map<String, Object> document : documents) {
                Resource solrResource = new GenericRestResource(resourceResolver, toResourcePath((String) document.get("id")),
                        RESOURCE_TYPE, new ValueMapDecorator(document));

                resourceList.add(solrResource);
            }

            return resourceList.iterator();
        }

        return null;
    }

    @Override
    public Iterator<ValueMap> queryResources(ResourceResolver resourceResolver, String query, String language) {
        LOG.debug("queryResources() ... not yet implemented ... the cool shit of solr ... if there's love ion this LIVE and it is..");

        return null;
    }


    @Override
    public Iterator<Resource> listChildren(Resource resource) {
        LOG.debug("listChildren() ... not yet implemented ... monday left me broken");

        return null;
    }

    @Override
    public Resource create(ResourceResolver resourceResolver, String path, Map<String, Object> stringObjectMap) throws PersistenceException {
        LOG.debug("create() ... not yet implemented ... tuesday i was thru with hopin");

        return null;
    }

    @Override
    public void delete(ResourceResolver resourceResolver, String path) throws PersistenceException {
        LOG.debug("delete() ... not yet implemented ... wednesday my empty arms were open");

    }

    @Override
    public void revert(ResourceResolver resourceResolver) {
        LOG.debug("revert() ... not yet implemented ... thursday waiting for love, waiting for love");

    }

    @Override
    public void commit(ResourceResolver resourceResolver) throws PersistenceException {
        LOG.debug("commit() ... not yet implemented ... thank the stars it's friday");

    }

    @Override
    public boolean hasChanges(ResourceResolver resourceResolver) {
        LOG.debug("hasChanges() ... not yet implemented ... WE'RE BURNING LIKE A FIRE FONE WILD ON SATURDAY");

        return false;
    }

}
