package com.adobe.acs.commons.errorpagehandler.impl;

import com.adobe.acs.commons.errorpagehandler.ErrorPageHandlerService;
import com.day.cq.commons.PathInfo;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.eval.JcrPropertyPredicateEvaluator;
import com.day.cq.search.eval.NodenamePredicateEvaluator;
import com.day.cq.search.eval.TypePredicateEvaluator;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.engine.auth.Authenticator;
import org.apache.sling.engine.auth.NoAuthenticationHandlerException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;

@Component(label = "ACS AEM Commons - Error Page Handler",
            description = "Error Page Handling module which facilitates the resolution of errors against authorable pages for discrete content trees.",
            immediate = false,
            metatype = true)
@Properties({
    @Property(
        name = "service.vendor",
        value = "ACS")
})
@Service
public class ErrorPageHandlerImpl implements ErrorPageHandlerService {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(ErrorPageHandlerImpl.class);

    private static final String USER_AGENT = "User-Agent";
    private static final String MOZILLA = "Mozilla";
    private static final String OPERA = "Opera";
    public static final String DEFAULT_ERROR_PAGE_NAME = "errors";
    public static final String ERROR_PAGE_PROPERTY = "errorPages";

    /* Enable/Disable */
    private static final boolean DEFAULT_ENABLED = true;
    private boolean enabled = DEFAULT_ENABLED;
    @Property(label = "Enable",
    description = "Enables/Disables the error handler. [Required]",
    boolValue = DEFAULT_ENABLED)
    private static final String PROP_ENABLED = "prop.enabled";

    /* Error Page Extension */
    private static final String DEFAULT_ERROR_PAGE_EXTENSION = "html";
    private String errorPageExtension = DEFAULT_ERROR_PAGE_EXTENSION;
    @Property(label = "Error page extension",
    description = "Examples: html, htm, xml, json. [Optional] [Default: html]",
    value = DEFAULT_ERROR_PAGE_EXTENSION)
    private static final String PROP_ERROR_PAGE_EXTENSION = "prop.error-page.extension";

    /* Fallback Error Code Extension */
    private static final String DEFAULT_FALLBACK_ERROR_NAME = "500";
    private String fallbackErrorName = DEFAULT_FALLBACK_ERROR_NAME;
    @Property(label = "Fallback error page name",
    description = "Error page name (not path) to use if a valid Error Code/Error Servlet Name cannot be retrieved from the Request. [Required] [Default: 500]",
    value = DEFAULT_FALLBACK_ERROR_NAME)
    private static final String PROP_FALLBACK_ERROR_NAME = "prop.error-page.fallback-name";

    /* System Error Page Path */
    private static final String DEFAULT_SYSTEM_ERROR_PAGE_PATH_DEFAULT = "";
    private String systemErrorPagePath = DEFAULT_SYSTEM_ERROR_PAGE_PATH_DEFAULT;
    @Property(label = "System error page",
    description = "Absolute path to system Error page resource to serve if no other more appropriate error pages can be found. Does not include extension. [Optional... but highly recommended]",
    value = DEFAULT_SYSTEM_ERROR_PAGE_PATH_DEFAULT)
    private static final String PROP_ERROR_PAGE_PATH = "prop.error-page.system-path";

    /* Search Paths */
    private static final String[] DEFAULT_SEARCH_PATHS = {};
    @Property(label = "Error page paths",
    description = "List of valid inclusive content trees under which error pages may reside, along with the name of the the default error page for the content tree. This is a fallback/less powerful option to adding the ./errorPages property to CQ Page property dialogs. Example: /content/geometrixx/en:errors [Optional]",
    cardinality = Integer.MAX_VALUE)
    private static final String PROP_SEARCH_PATHS = "prop.paths";

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private Authenticator authenticator;

    private SortedMap<String, String> pathMap = new TreeMap<String, String>();

    /**
     * Find the full path to the most appropriate Error Page
     *
     * @param request
     * @param errorResource
     * @return
     */
    @Override
    public String findErrorPage(SlingHttpServletRequest request, Resource errorResource) {
        if (!isEnabled()) { return null; }

        Resource page = null;
        final ResourceResolver resourceResolver = errorResource.getResourceResolver();

        // Get error page name to look for based on the error code/name
        final String pageName = getErrorPageName(request);

        // Try to find the closest real parent for the requested resource
        final Resource parent = findFirstRealParentOrSelf(errorResource);

        final SortedMap<String, String> errorPagesMap = getErrorPagesMap(resourceResolver);

        if (!errorPagesMap.isEmpty()) {
            // Get the best-matching Errors Path for this particular Request
            final String errorsPath = this.getErrorPagesPath(parent, errorPagesMap);

            if(StringUtils.isNotBlank(errorsPath)) {
                // Search for CQ Page for specific servlet named Page (404, 500, Throwable, etc.)
                SearchResult result = executeQuery(resourceResolver, pageName);
                List<String> errorPaths = filterResults(errorsPath, result);

                // Return the first existing match
                for (String errorPath : errorPaths) {
                    page = getResource(resourceResolver, errorPath);
                    if(page != null) { break; }
                }

                // No error-specific page could be found, use the "default" error page
                // for the Root content path
                if(page == null && StringUtils.isNotBlank(errorsPath)) {
                    page = resourceResolver.resolve(errorsPath);
                }
            }
        }

        if (page == null || ResourceUtil.isNonExistingResource(page)) {
            // If no error page could be found
            if (this.hasSystemErrorPage()) {
                final String errorPage = applyExtension(this.getSystemErrorPagePath());
                log.debug("Using default error page: {}", errorPage);
                return StringUtils.stripToNull(errorPage);
            }
        } else {
            final String errorPage = applyExtension(page.getPath());
            log.debug("Using resolved error page: {}", errorPage);
            return StringUtils.stripToNull(errorPage);
        }

        return null;
    }


    /**
     * Create the query for finding candidate cq:Pages
     *
     * @param resourceResolver
     * @param pageNames
     * @return
     */
    private SearchResult executeQuery(ResourceResolver resourceResolver, String... pageNames) {
        final Session session = resourceResolver.adaptTo(Session.class);
        final Map<String, String> map = new HashMap<String, String>();
        if(pageNames == null) { pageNames = new String[]{}; }

        // Construct query builder query
        map.put(TypePredicateEvaluator.TYPE, "cq:Page");

        if(pageNames.length == 1) {
            map.put(NodenamePredicateEvaluator.NODENAME, escapeNodeName(pageNames[0]));
        } else if(pageNames.length > 1) {
            map.put("group.p.or", "true");
            for(int i = 0; i < pageNames.length; i++) {
                map.put("group." + String.valueOf(i) + "_" + NodenamePredicateEvaluator.NODENAME, escapeNodeName(pageNames[i]));
            }
        }

        final Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
        return query.getResult();
    }

    /**
     * Gets the resource object for the provided path.
     *
     * Performs checks to ensure resource exists and is accessible to user.
     *
     * @param resourceResolver
     * @param path
     * @return
     */
    private Resource getResource(ResourceResolver resourceResolver, String path) {
        // Double check that the resource exists and return it as a match
        final Resource resource = resourceResolver.getResource(path);

        if(resource != null && !ResourceUtil.isNonExistingResource(resource)) {
            return resource;
        }

        return null;
    }

    /**
     * Filter query results
     *
     * @param rootPath
     * @param result
     * @return list of resource paths of candidate error pages
     */
    private List<String> filterResults(String rootPath, SearchResult result) {
        final List<Node> nodes = IteratorUtils.toList(result.getNodes());
        final List<String> resultPaths = new ArrayList<String>();
        if(StringUtils.isBlank(rootPath)) { return resultPaths; }

        // Filter results by the searchResource path; All valid results' paths should begin
        // with searchResource.getPath()
        for(Node node : nodes) {
            if(node == null) { continue; }
            try {
                // Make sure all query results under or equals to the current Search Resource
                if(StringUtils.equals(node.getPath(), rootPath) ||
                    StringUtils.startsWith(node.getPath(), rootPath.concat("/"))) {
                    resultPaths.add(node.getPath());
                }
            } catch(RepositoryException ex) {
                log.warn("Could not get path for node. {}", ex.getMessage());
                // continue
            }
        }

        return resultPaths;
    }

    /** HTTP Request Data Retrieval Methods **/

    /**
     * Get Error Status Code from Request
     *
     * @param request
     * @return
     */
    public int getStatusCode(SlingHttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(ErrorPageHandlerService.STATUS_CODE);

        if (statusCode != null) {
            return statusCode;
        } else {
            return ErrorPageHandlerService.DEFAULT_STATUS_CODE;
        }
    }

    /**
     *
     *
     * @param request
     * @return
     */
    public String getErrorPageName(SlingHttpServletRequest request) {
        // Get status code from request
        // Set the servlet name ot find to statusCode; update later if needed
        String servletName = String.valueOf(getStatusCode(request));

        if(StringUtils.isBlank(servletName)) { servletName = this.fallbackErrorName; }

        final String servletPath = (String) request.getAttribute(ErrorPageHandlerService.SERVLET_NAME);
        if(StringUtils.isBlank(servletPath)) { return servletName; }

        try {
            final PathInfo pathInfo = new PathInfo(servletPath);
            final String[] parts = StringUtils.split(pathInfo.getResourcePath(), '/');
            if (parts.length > 0) {
                servletName = parts[parts.length - 1];
            }
        } catch(IllegalArgumentException ex) {
            // Use status code
        }

        return StringUtils.lowerCase(servletName);
    }


    private SortedMap<String, String> getErrorPagesMap(ResourceResolver resourceResolver) {
        final Session session = resourceResolver.adaptTo(Session.class);
        Map<String, String> map = new HashMap<String, String>();
        SortedMap<String, String> authoredMap =  new TreeMap<String, String>(new StringLengthComparator());

        // Construct query builder query
        map.put(TypePredicateEvaluator.TYPE, "cq:Page");
        map.put(JcrPropertyPredicateEvaluator.PROPERTY, JcrConstants.JCR_CONTENT + "/" + ERROR_PAGE_PROPERTY);
        map.put(JcrPropertyPredicateEvaluator.PROPERTY + "." + JcrPropertyPredicateEvaluator.OPERATION, JcrPropertyPredicateEvaluator.OP_EXISTS);
        map.put("p.limit", "0");

        final Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);

        for(final Hit hit : query.getResult().getHits()) {
            try {
                final Resource contentResource = hit.getResource().getChild(JcrConstants.JCR_CONTENT);
                final ValueMap properties = contentResource.adaptTo(ValueMap.class);
                final String errorPagePath = properties.get(ERROR_PAGE_PROPERTY, String.class);

                if(StringUtils.isBlank(errorPagePath)) { continue; }

                final Resource errorPageResource = resourceResolver.resolve(errorPagePath);
                if(errorPageResource != null && !ResourceUtil.isNonExistingResource(errorPageResource)) {
                    authoredMap.put(hit.getPath(), errorPagePath);
                }
            } catch (RepositoryException ex) {
                log.error("Could not resolve hit to a valid resource");
            }
        }

        return mergeMaps(authoredMap, this.pathMap);
    }

    /** OSGi Component Property Getters/Setters **/

    /**
     *
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks if the System Error Page has been configured
     *
     * @return
     */
    public boolean hasSystemErrorPage() {
        return StringUtils.isNotBlank(this.getSystemErrorPagePath());
    }

    /**
     * Get the configured System Error Page Path
     * @return
     */
    public String getSystemErrorPagePath() {
        return StringUtils.strip(this.systemErrorPagePath);
    }

    /**
     * Get configured error page extension
     *
     * @return
     */
    public String getErrorPageExtension() {
        return StringUtils.stripToEmpty(this.errorPageExtension);
    }

    /**
     * Get the sorted Search Paths
     *
     * @return
     */
    private List<String> getRootPaths(Map<String, String> errorPagesMap) {
        return Arrays.asList(errorPagesMap.keySet().toArray(new String[errorPagesMap.size()]));
    }

    /**
     * Gets the Error Pages Path for the provided content root path
     *
     * @param rootPath
     * @param errorPagesMap
     * @return
     */
    public String getErrorPagesPath(String rootPath, Map<String, String> errorPagesMap) {
        if(errorPagesMap.containsKey(rootPath)) {
            return errorPagesMap.get(rootPath);
        } else {
            return null;
        }
    }

    /**
     * Find the Error page search path that best contains the provided resource
     *
     * @param resource
     * @return
     */
    private String getErrorPagesPath(Resource resource, SortedMap<String, String> errorPagesMap) {
        // Path to evaluate against Root paths
        final String path = resource.getPath();
        final ResourceResolver resourceResolver = resource.getResourceResolver();

        for(final String rootPath : this.getRootPaths(errorPagesMap)) {
            if(StringUtils.equals(path, rootPath) ||
                    StringUtils.startsWith(path, rootPath.concat("/"))) {

                final String errorPagePath = getErrorPagesPath(rootPath, errorPagesMap);

                Resource errorPageResource = getResource(resourceResolver, errorPagePath);
                if(errorPageResource != null && !ResourceUtil.isNonExistingResource(errorPageResource)) {
                    return errorPageResource.getPath();
                }
            }
        }
        return null;
    }

    /**
     * Given the Request path, find the first Real Parent of the Request (even if the resource doesnt exist)
     *
     * @param resource
     * @return
     */
    private Resource findFirstRealParentOrSelf(Resource resource) {
        if(resource == null) {
            return null;
        } else if(resource != null && !ResourceUtil.isNonExistingResource(resource)) {
            return resource;
        }

        final Resource parent = resource.getParent();
        if (parent != null) { return parent; }

        final ResourceResolver resourceResolver = resource.getResourceResolver();
        final String path = resource.getPath();
        final PathInfo pathInfo = new PathInfo(path);
        String[] parts = StringUtils.split(pathInfo.getResourcePath(), '/');

        for (int i = parts.length - 1; i >= 0; i--) {
            String[] tmpArray = (String[]) ArrayUtils.subarray(parts, 0, i);
            String tmpStr = "/".concat(StringUtils.join(tmpArray, '/'));

            final Resource tmpResource = resourceResolver.getResource(tmpStr);

            if (tmpResource != null) {
                return tmpResource;
            }
        }

        return null;
    }

    /**
     * Add extension as configured via OSGi Component Property
     *
     * Defaults to .html
     *
     * @param path
     * @return
     */
    private String applyExtension(String path) {
        if (path == null) {
            return null;
        }

        String ext = getErrorPageExtension();
        if (StringUtils.isBlank(ext)) {
            return path;
        }

        return StringUtils.stripToEmpty(path).concat(".").concat(ext);
    }


    /**
     * Escapes JCR node names for search; Especially important for nodes that start with numbers
     *
     * @param name
     * @return
     */
    private String escapeNodeName(String name) {
        name = StringUtils.stripToNull(name);
        if (name == null) {
            return "";
        }
        return name;
    }


    /** Script Support Methods **/

    /**
     * Determins if the request has been authenticated or is Anonymous
     *
     * @param request
     * @return
     */
    protected boolean isAnonymousRequest(SlingHttpServletRequest request) {
        return (request.getAuthType() == null || request.getRemoteUser() == null);
    }

    /**
     * Determines if the request originated from a Browser
     *
     * @param request
     * @return
     */
    protected boolean isBrowserRequest(SlingHttpServletRequest request) {
        final String userAgent = request.getHeader(USER_AGENT);
        return !StringUtils.isBlank(userAgent) && (StringUtils.contains(userAgent, MOZILLA) || StringUtils.contains(userAgent, OPERA));
    }

    /**
     * Determines if the Request is to Author
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAuthorModeRequest(SlingHttpServletRequest request) {
        final WCMMode mode = WCMMode.fromRequest(request);
        return (mode != null && !WCMMode.DISABLED.equals(mode));
    }

    /**
     * Determines is the Request is to Author in Preview mode
     *
     * @param request
     * @return true if Request is to an Author in Preview
     */
    @Override
    public boolean isAuthorPreviewModeRequest(SlingHttpServletRequest request) {
        final WCMMode mode = WCMMode.fromRequest(request);
        return WCMMode.PREVIEW.equals(mode);
    }

    /**
     * Attempts to invoke a valid Sling Authentication Handler for the request
     *
     * @param request
     * @param response
     */
    protected void authenticateRequest(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        if (authenticator == null) {
            log.warn("Cannot login: Missing Authenticator service");
            return;
        }

        try {
            authenticator.login(request, response);
        } catch (NoAuthenticationHandlerException ex) {
            log.warn("Cannot login: No Authentication Handler is willing to authenticate");
        }
    }

    /**
     * Determine and handle 404 Requests
     *
     * @param request
     * @param response
     */
    @Override
    public void doHandle404(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        if(!isAuthorModeRequest(request)) {
            return;
        } else if (getStatusCode(request) != SlingHttpServletResponse.SC_NOT_FOUND) {
            return;
        }

        if (isAnonymousRequest(request) && isBrowserRequest(request)) {
            authenticateRequest(request, response);
        }
    }

    /**
     * Returns the Exception Message (Stacktrace) from the Request
     *
     * @param request
     * @return
     */
    @Override
    public String getException(SlingHttpServletRequest request) {
        StringWriter stringWriter = new StringWriter();
        if (request.getAttribute(SlingConstants.ERROR_EXCEPTION) instanceof Throwable) {
            Throwable throwable = (Throwable) request.getAttribute(SlingConstants.ERROR_EXCEPTION);

            if (throwable == null) {
                return "";
            }

            if (throwable instanceof ServletException) {
                ServletException se = (ServletException) throwable;
                while (se.getRootCause() != null) {
                    throwable = se.getRootCause();
                    if (throwable instanceof ServletException) {
                        se = (ServletException) throwable;
                    } else {
                        break;
                    }
                }
            }

            throwable.printStackTrace(new PrintWriter(stringWriter, true));
        }

        return stringWriter.toString();
    }

    /**
     * Returns a String representation of the RequestProgress trace
     *
     * @param request
     * @return
     */
    @Override
    public String getRequestProgress(SlingHttpServletRequest request) {
        StringWriter stringWriter = new StringWriter();
        if (request != null) {
            RequestProgressTracker tracker = request.getRequestProgressTracker();
            tracker.dump(new PrintWriter(stringWriter, true));
        }
        return stringWriter.toString();
    }

    /**
     * Reset response attributes to support printing out a new page (rather than one that potentially errored out).
     * This includes clearing clientlib inclusion state, and resetting the response.
     *
     * If the response is committed, and it hasnt been closed by code, check the response AND jsp buffer sizes and ensure they are large enough to NOT force a buffer flush.
     * @param request
     * @param response
     * @param statusCode
     */
    @Override
    public void resetRequestAndResponse(SlingHttpServletRequest request, SlingHttpServletResponse response, int statusCode) {
        // Clear client libraries
        request.setAttribute(com.day.cq.widget.HtmlLibraryManager.class.getName() + ".included",
                new HashSet<String>());
        // Clear the response
        response.reset();
        response.setStatus(statusCode);
    }

    /**
     * Merge two Maps together. In the event of any key collisions the Master map wins
     *
     * Any blank value keys are dropped from the final Map
     *
     * Map is sorted by value (String) length
     *
     * @param master
     * @param slave
     * @return
     */
    private SortedMap<String, String> mergeMaps(SortedMap<String, String> master, SortedMap<String, String> slave) {
        SortedMap<String, String>map = new TreeMap<String, String>(new StringLengthComparator());

        for(final String key : master.keySet()) {
            if(StringUtils.isNotBlank(master.get(key))) {
                map.put(key, master.get(key));
            }
        }

        for(final String key : slave.keySet()) {
            if(master.containsKey(key)) { continue; }
            if(StringUtils.isNotBlank(slave.get(key))) {
                map.put(key, slave.get(key));
            }
        }

        return map;
    }

    /**
     * Util for parsing Service properties in the form &gt;value&lt;&gt;separator&lt;&gt;value&lt;
     *
     * @param value
     * @param separator
     * @return
     */
    private SimpleEntry toSimpleEntry(String value, String separator) {
        String[] tmp = StringUtils.split(value, separator);

        if (tmp == null) {
            return null;
        }

        if (tmp.length == 2) {
            return new SimpleEntry(tmp[0], tmp[1]);
        } else {
            return null;
        }
    }


    /** OSGi Component Methods **/

    @Activate
    protected void activate(ComponentContext componentContext) {
        configure(componentContext);
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        enabled = false;
    }

    private void configure(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();

        this.enabled = PropertiesUtil.toBoolean(properties.get(PROP_ENABLED), DEFAULT_ENABLED);

        this.systemErrorPagePath = PropertiesUtil.toString(properties.get(PROP_ERROR_PAGE_PATH), DEFAULT_SYSTEM_ERROR_PAGE_PATH_DEFAULT);

        this.errorPageExtension = PropertiesUtil.toString(properties.get(PROP_ERROR_PAGE_EXTENSION), DEFAULT_ERROR_PAGE_EXTENSION);

        this.fallbackErrorName = PropertiesUtil.toString(properties.get(PROP_FALLBACK_ERROR_NAME), DEFAULT_FALLBACK_ERROR_NAME);

        this.pathMap = configurePathMap(PropertiesUtil.toStringArray(properties.get(PROP_SEARCH_PATHS), DEFAULT_SEARCH_PATHS));
    }

    /**
     * Covert OSGi Property storing Root content paths:Error page paths into a SortMap
     *
     * @param paths
     * @return
     */
    private SortedMap<String, String> configurePathMap(String[] paths) {
        SortedMap<String, String> sortedMap = new TreeMap<String, String>(new StringLengthComparator());

        for (String path : paths) {
            if(StringUtils.isBlank(path)) { continue; }

            final SimpleEntry tmp = toSimpleEntry(path, ":");

            if(tmp == null) { continue; }

            String key = StringUtils.strip((String) tmp.getKey());
            String val = StringUtils.strip((String) tmp.getValue());

            // Only accept absolute paths
            if(StringUtils.isBlank(key) || !StringUtils.startsWith(key, "/")) { continue; }

            // Validate page name value
            if(StringUtils.isBlank(val)) {
                val = key + "/" + DEFAULT_ERROR_PAGE_NAME;
            } else if(StringUtils.equals(val, ".")) {
                val = key;
            } else if(!StringUtils.startsWith(val, "/")) {
                val = key + "/" + val;
            }

            sortedMap.put(key, val);
        }

        return sortedMap;
    }

}