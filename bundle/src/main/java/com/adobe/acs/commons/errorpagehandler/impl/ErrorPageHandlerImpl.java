/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.errorpagehandler.impl;

import com.adobe.acs.commons.errorpagehandler.cache.impl.ErrorPageCache;
import com.adobe.acs.commons.errorpagehandler.cache.impl.ErrorPageCacheImpl;
import com.adobe.acs.commons.errorpagehandler.ErrorPageHandlerService;
import com.adobe.acs.commons.wcm.ComponentHelper;
import com.day.cq.commons.PathInfo;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.QueryBuilder;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.commons.auth.Authenticator;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Component(
        label = "ACS AEM Commons - Error Page Handler",
        description = "Error Page Handling module which facilitates the resolution of errors "
                + "against author-able "
                + "pages for discrete content trees.",
        immediate = false, metatype = true)
@Service
public final class ErrorPageHandlerImpl implements ErrorPageHandlerService {

    private static final Logger log = LoggerFactory.getLogger(ErrorPageHandlerImpl.class);

    public static final String DEFAULT_ERROR_PAGE_NAME = "errors";

    public static final String ERROR_PAGE_PROPERTY = "errorPages";

    /* Enable/Disable */
    private static final boolean DEFAULT_ENABLED = true;

    private boolean enabled = DEFAULT_ENABLED;

    @Property(label = "Enable", description = "Enables/Disables the error handler. [Required]",
            boolValue = DEFAULT_ENABLED)
    private static final String PROP_ENABLED = "enabled";

    /* Error Page Extension */
    private static final String DEFAULT_ERROR_PAGE_EXTENSION = "html";

    private String errorPageExtension = DEFAULT_ERROR_PAGE_EXTENSION;

    @Property(label = "Error page extension",
            description = "Examples: html, htm, xml, json. [Optional] [Default: html]",
            value = DEFAULT_ERROR_PAGE_EXTENSION)
    private static final String PROP_ERROR_PAGE_EXTENSION = "error-page.extension";

    /* Fallback Error Code Extension */
    private static final String DEFAULT_FALLBACK_ERROR_NAME = "500";

    private String fallbackErrorName = DEFAULT_FALLBACK_ERROR_NAME;

    @Property(
            label = "Fallback error page name",
            description = "Error page name (not path) to use if a valid Error Code/Error Servlet Name cannot be "
                    + "retrieved from the Request. [Required] [Default: 500]",
            value = DEFAULT_FALLBACK_ERROR_NAME)
    private static final String PROP_FALLBACK_ERROR_NAME = "error-page.fallback-name";

    /* System Error Page Path */
    private static final String DEFAULT_SYSTEM_ERROR_PAGE_PATH_DEFAULT = "";

    private String systemErrorPagePath = DEFAULT_SYSTEM_ERROR_PAGE_PATH_DEFAULT;

    @Property(
            label = "System error page",
            description = "Absolute path to system Error page resource to serve if no other more appropriate "
                    + "error pages can be found. Does not include extension. [Optional... but highly recommended]",
            value = DEFAULT_SYSTEM_ERROR_PAGE_PATH_DEFAULT)
    private static final String PROP_ERROR_PAGE_PATH = "error-page.system-path";

    /* Search Paths */
    private static final String[] DEFAULT_SEARCH_PATHS = {};

    @Property(
            label = "Error page paths",
            description = "List of inclusive content trees under which error pages may reside, "
                    + "along with the name of the the default error page for the content tree. This is a "
                    + "fallback/less powerful option to adding the ./errorPages property to CQ Page property dialogs."
                    + " Example: /content/geometrixx/en:errors [Optional]",
            cardinality = Integer.MAX_VALUE)
    private static final String PROP_SEARCH_PATHS = "paths";

    private static final int DEFAULT_TTL = 60 * 5; // 5 minutes

    private static final boolean DEFAULT_SERVE_AUTHENTICATED_FROM_CACHE = false;

    @Property(label = "Serve authenticated from cache",
            description = "Serve authenticated requests from the error page cache. [ Default: false ]",
            boolValue = DEFAULT_SERVE_AUTHENTICATED_FROM_CACHE)
    private static final String PROP_SERVE_AUTHENTICATED_FROM_CACHE = "cache.serve-authenticated";
    private static final String LEGACY_PROP_SERVE_AUTHENTICATED_FROM_CACHE = "serve-authenticated-from-cache";

    @Property(label = "TTL (in seconds)",
            description = "TTL for each cache entry in seconds. [ Default: 300 ]",
            intValue = DEFAULT_TTL)
    private static final String PROP_TTL = "cache.ttl";
    private static final String LEGACY_PROP_TTL = "ttl";

    /* Enable/Disables error images */
    private static final boolean DEFAULT_ERROR_IMAGES_ENABLED = false;

    private boolean errorImagesEnabled = DEFAULT_ERROR_IMAGES_ENABLED;

    @Property(label = "Enable placeholder images", description = "Enable image error handling  [ Default: false ]",
            boolValue = DEFAULT_ERROR_IMAGES_ENABLED)
    private static final String PROP_ERROR_IMAGES_ENABLED = "error-images.enabled";

    /* Relative placeholder image path */
    private static final String DEFAULT_ERROR_IMAGE_PATH = ".img.png";

    private String errorImagePath = DEFAULT_ERROR_IMAGE_PATH;

    @Property(label = "Error image path/selector",
            description = "Accepts a selectors.extension (ex. `.img.png`) absolute, or relative path. "
                    + "If an extension or relative path, this value is applied to the resolved error page."
                    + " Note: This concatenated path must resolve to a nt:file else a 200 response will be sent."
                    + " [ Optional ] [ Default: .img.png ]",
            value = DEFAULT_ERROR_IMAGE_PATH)
    private static final String PROP_ERROR_IMAGE_PATH = "error-images.path";

    /* Error image extensions to handle */
    private static final String[] DEFAULT_ERROR_IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif"};

    private String[] errorImageExtensions = DEFAULT_ERROR_IMAGE_EXTENSIONS;

    @Property(
            label = "Error image extensions",
            description = "List of valid image extensions (no proceeding .) to handle. "
                    + "Example: 'png' "
                    + "[ Optional ] [ Default: png, jpeg, jpeg, gif ]",
            cardinality = Integer.MAX_VALUE,
            value = { "png", "jpeg", "jpg", "gif" })
    private static final String PROP_ERROR_IMAGE_EXTENSIONS = "error-images.extensions";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private Authenticator authenticator;

    @Reference
    private ComponentHelper componentHelper;

    private ErrorPageCache cache;

    private SortedMap<String, String> pathMap = new TreeMap<String, String>();

    private ServiceRegistration cacheRegistration;

    /**
     * Find the JCR full path to the most appropriate Error Page.
     *
     * @param request
     * @param errorResource
     * @return
     */
    public String findErrorPage(SlingHttpServletRequest request, Resource errorResource) {
        if (!isEnabled()) {
            return null;
        }

        Resource page = null;
        final ResourceResolver resourceResolver = errorResource.getResourceResolver();
        final String errorResourcePath = errorResource.getPath();

        final boolean isError = this.getStatusCode(request) >= SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR;

        // Get error page name to look for based on the error code/name
        final String pageName = getErrorPageName(request);
        String errorsPath = null;

        // Try to find the closest real parent for the requested resource
        final Resource parent = findFirstRealParentOrSelf(errorResource);
        if (parent != null) {
            // Get content resource of the page
            final Resource parentContentResource = parent.getChild("jcr:content");

            if (parentContentResource != null) {
                final InheritanceValueMap pageProperties = new HierarchyNodeInheritanceValueMap(parentContentResource);
                errorsPath = pageProperties.getInherited(ERROR_PAGE_PROPERTY, String.class);

                // could not find inherited property
                if (errorsPath == null) {
                    for (final Map.Entry<String, String> mapPage : pathMap.entrySet()) {
                        if (errorResourcePath.startsWith(mapPage.getKey())) {
                            errorsPath = mapPage.getValue();
                            break;
                        }
                    }
                }
            }
        }

        if (StringUtils.isNotBlank(errorsPath)) {
            log.debug("Best matching errors path for request is: {}", errorsPath);


            String errorPath = errorsPath + "/" + pageName;
            page = getResource(resourceResolver, errorPath);

            // No error-specific page could be found, use the "default" error page
            // for the Root content path
            if (page == null && StringUtils.isNotBlank(errorsPath)) {
                page = resourceResolver.resolve(errorsPath);
            }
        }

        String errorPagePath = null;
        if (page == null || ResourceUtil.isNonExistingResource(page)) {
            // If no error page could be found
            if (this.hasSystemErrorPage()) {
                errorPagePath = this.getSystemErrorPagePath();
            }
        } else {
            errorPagePath = page.getPath();
        }

        if (errorImagesEnabled && this.isImageRequest(request)) {

            if (StringUtils.startsWith(this.errorImagePath, "/")) {
                // Absolute path
                return this.errorImagePath;
            } else if (StringUtils.isNotBlank(errorPagePath)) {
                // Selector or Relative path; compute path based off found error page

                if (StringUtils.startsWith(this.errorImagePath, ".")) {
                    final String selectorErrorImagePath = errorPagePath + this.errorImagePath;
                    log.debug("Using selector-based error image: {}", selectorErrorImagePath);
                    return selectorErrorImagePath;
                } else {
                    final String relativeErrorImagePath = errorPagePath + "/"
                            + StringUtils.removeStart(this.errorImagePath, "/");
                    log.debug("Using relative path-based error image: {}", relativeErrorImagePath);
                    return relativeErrorImagePath;
                }
            } else {
                log.warn("Error image path found, but no error page could be found so relative path cannot "
                        + "be applied: {}", this.errorImagePath);
            }
        } else if (StringUtils.isNotBlank(errorPagePath)) {
            errorPagePath = StringUtils.stripToNull(applyExtension(errorPagePath));
            log.debug("Using resolved error page: {}", errorPagePath);
            return errorPagePath;
        } else {
            log.warn("ACS AEM Commons Error Page Handler is enabled but mis-configured. A valid error image"
                    + " handler nor a valid error page could be found.");
        }
        return null;
    }

    /**
     * Gets the resource object for the provided path.
     * <p/>
     * Performs checks to ensure resource exists and is accessible to user.
     *
     * @param resourceResolver
     * @param path
     * @return
     */
    private Resource getResource(ResourceResolver resourceResolver, String path) {
        // Double check that the resource exists and return it as a match
        final Resource resource = resourceResolver.getResource(path);

        if (resource != null && !ResourceUtil.isNonExistingResource(resource)) {
            return resource;
        }

        return null;
    }

    /** HTTP Request Data Retrieval Methods **/

    /**
     * Get Error Status Code from Request or Default (500) if no status code can be found.
     *
     * @param request
     * @return
     */
    public int getStatusCode(SlingHttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(SlingConstants.ERROR_STATUS);

        if (statusCode != null) {
            return statusCode;
        } else {
            return ErrorPageHandlerService.DEFAULT_STATUS_CODE;
        }
    }

    /**
     * Get the Error Page's name (all lowercase) that should be used to render the page for this error.
     * <p/>
     * This looks at the Status code delivered via by Sling into the error page content
     *
     * @param request
     * @return
     */
    public String getErrorPageName(SlingHttpServletRequest request) {
        // Get status code from request
        // Set the servlet name ot find to statusCode; update later if needed
        String servletName = String.valueOf(getStatusCode(request));

        // Only support Status codes as error exception lookup scheme is too complex/expensive at this time.
        // Using the 500 response code/default error page should suffice for all errors pages generated from exceptions.

        /*
        final Object tmp = request.getAttribute(SlingConstants.ERROR_EXCEPTION_TYPE);

        if(tmp != null && tmp instanceof Class) {
            final Class clazz = (Class) tmp;

            final String exceptionName = clazz.getSimpleName();
            log.debug("Servlet path used to derived exception name: {} ", exceptionName);

            if(StringUtils.isNotBlank(exceptionName)) {
                servletName = exceptionName;
            }
        }

        if(StringUtils.isBlank(servletName)) { servletName = this.fallbackErrorName; }
        */

        servletName = StringUtils.lowerCase(servletName);

        log.debug("Error page name to (try to) use: {} ", servletName);

        return servletName;
    }

    /** OSGi Component Property Getters/Setters **/

    /**
     * Determines if this Service is "enabled". If it has been configured to be "Disabled" the Service still exists
     * however it should not be used.
     * This OSGi Property toggle allows error page handler to be toggled on an off without via OSGi means without
     * throwing Null pointers, etc.
     *
     * @return true is the Service should be considered enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks if the System Error Page has been configured.
     *
     * @return
     */
    public boolean hasSystemErrorPage() {
        return StringUtils.isNotBlank(this.getSystemErrorPagePath());
    }

    /**
     * Get the configured System Error Page Path.
     *
     * @return
     */
    public String getSystemErrorPagePath() {
        return StringUtils.strip(this.systemErrorPagePath);
    }

    /**
     * Gets the Error Pages Path for the provided content root path.
     *
     * @param rootPath
     * @param errorPagesMap
     * @return
     */
    public String getErrorPagesPath(String rootPath, Map<String, String> errorPagesMap) {
        if (errorPagesMap.containsKey(rootPath)) {
            return errorPagesMap.get(rootPath);
        } else {
            return null;
        }
    }

    /**
     * Check if this is an image request.
     *
     * @param request the current {@link SlingHttpServletRequest}
     * @return true if this request should deliver an image.
     */
    private boolean isImageRequest(final SlingHttpServletRequest request) {
        if (StringUtils.isBlank(errorImagePath)) {
            log.warn("ACS AEM Commons error page handler enabled to handle error images, "
                    + "but no error image path was provided.");
            return false;
        }

        final String extension = StringUtils.stripToEmpty(StringUtils.lowerCase(
                request.getRequestPathInfo().getExtension()));

        return ArrayUtils.contains(errorImageExtensions, extension);
    }

    /**
     * Given the Request path, find the first Real Parent of the Request (even if the resource doesnt exist).
     *
     * @param resource
     * @return
     */
    private Resource findFirstRealParentOrSelf(Resource resource) {
        if (resource == null) {
            return null;
        } else if (!ResourceUtil.isNonExistingResource(resource)) {
            return resource;
        }

        final Resource parent = resource.getParent();
        if (parent != null) {
            return parent;
        }

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
     * Add extension as configured via OSGi Component Property.
     * <p/>
     * Defaults to .html
     *
     * @param path
     * @return
     */
    private String applyExtension(String path) {
        if (path == null) {
            return null;
        }

        if (StringUtils.isBlank(errorPageExtension)) {
            return path;
        }

        return StringUtils.stripToEmpty(path).concat(".").concat(errorPageExtension);
    }

    /** Script Support Methods **/

    /**
     * Determines if the request has been authenticated or is Anonymous.
     *
     * @param request
     * @return
     */
    protected boolean isAnonymousRequest(SlingHttpServletRequest request) {
        return (request.getAuthType() == null || request.getRemoteUser() == null);
    }

    /**
     * Attempts to invoke a valid Sling Authentication Handler for the request.
     *
     * @param request
     * @param response
     */
    protected void authenticateRequest(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        if (authenticator == null) {
            log.warn("Cannot login: Missing Authenticator service");
            return;
        }

        authenticator.login(request, response);
    }

    /**
     * Determine is the request is a 404 and if so handles the request appropriately base on some CQ idiosyncrasies.
     * <p/>
     * Mainly forces an authentication request in Authoring modes (!WCMMode.DISABLED)
     *
     * @param request
     * @param response
     */
    @Override
    public void doHandle404(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        if (componentHelper.isDisabledMode(request)) {
            return;
        } else if (getStatusCode(request) != SlingHttpServletResponse.SC_NOT_FOUND) {
            return;
        }

        if (isAnonymousRequest(request) && AuthUtil.isBrowserRequest(request)) {
            authenticateRequest(request, response);
        }
    }

    /**
     * Returns the Exception Message (Stacktrace) from the Request.
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
     * Returns a String representation of the RequestProgress trace.
     *
     * @param request
     * @return
     */
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
     * <p/>
     * If the response is committed, and it hasnt been closed by code, check the response AND jsp buffer sizes and
     * ensure they are large enough to NOT force a buffer flush.
     *
     * @param request
     * @param response
     * @param statusCode
     */
    public void resetRequestAndResponse(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                        int statusCode) {
        // Clear client libraries

        // Replace with proper API call is HtmlLibraryManager provides one in the future;
        // Currently this is our only option.
        request.setAttribute(com.day.cq.widget.HtmlLibraryManager.class.getName() + ".included",
                new HashSet<String>());
        // Clear the response
        response.reset();
        response.setContentType("text/html");
        response.setStatus(statusCode);
    }

    /**
     * Util for parsing Service properties in the form &gt;value&lt;&gt;separator&lt;&gt;value&lt;.
     *
     * @param value
     * @param separator
     * @return
     */
    private SimpleEntry<String, String> toSimpleEntry(String value, String separator) {
        String[] tmp = StringUtils.split(value, separator);

        if (tmp == null) {
            return null;
        }

        if (tmp.length == 2) {
            return new SimpleEntry<String, String>(tmp[0], tmp[1]);
        } else {
            return null;
        }
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        configure(componentContext);
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        enabled = false;
        if (cacheRegistration != null) {
            cacheRegistration.unregister();
            cacheRegistration = null;
        }
    }

    private void configure(ComponentContext componentContext) {
        Dictionary<?, ?> config = componentContext.getProperties();
        final String legacyPrefix = "prop.";

        this.enabled = PropertiesUtil.toBoolean(config.get(PROP_ENABLED),
                PropertiesUtil.toBoolean(config.get(legacyPrefix + PROP_ENABLED),
                        DEFAULT_ENABLED));

        /** Error Pages **/

        this.systemErrorPagePath = PropertiesUtil.toString(config.get(PROP_ERROR_PAGE_PATH),
                PropertiesUtil.toString(config.get(legacyPrefix + PROP_ERROR_PAGE_PATH),
                        DEFAULT_SYSTEM_ERROR_PAGE_PATH_DEFAULT));

        this.errorPageExtension = PropertiesUtil.toString(config.get(PROP_ERROR_PAGE_EXTENSION),
                PropertiesUtil.toString(config.get(legacyPrefix + PROP_ERROR_PAGE_EXTENSION),
                        DEFAULT_ERROR_PAGE_EXTENSION));

        this.fallbackErrorName = PropertiesUtil.toString(config.get(PROP_FALLBACK_ERROR_NAME),
                PropertiesUtil.toString(config.get(legacyPrefix + PROP_FALLBACK_ERROR_NAME),
                        DEFAULT_FALLBACK_ERROR_NAME));

        this.pathMap = configurePathMap(PropertiesUtil.toStringArray(config.get(PROP_SEARCH_PATHS),
                PropertiesUtil.toStringArray(config.get(legacyPrefix + PROP_SEARCH_PATHS),
                        DEFAULT_SEARCH_PATHS)));

        /** Error Page Cache **/

        int ttl = PropertiesUtil.toInteger(config.get(PROP_TTL),
                PropertiesUtil.toInteger(LEGACY_PROP_TTL, DEFAULT_TTL));

        boolean serveAuthenticatedFromCache = PropertiesUtil.toBoolean(config.get(PROP_SERVE_AUTHENTICATED_FROM_CACHE),
                PropertiesUtil.toBoolean(LEGACY_PROP_SERVE_AUTHENTICATED_FROM_CACHE,
                        DEFAULT_SERVE_AUTHENTICATED_FROM_CACHE));
        try {
            cache = new ErrorPageCacheImpl(ttl, serveAuthenticatedFromCache);

            Dictionary<String, Object> serviceProps = new Hashtable<String, Object>();
            serviceProps.put("jmx.objectname", "com.adobe.acs.commons:type=ErrorPageHandlerCache");

            cacheRegistration = componentContext.getBundleContext().registerService(DynamicMBean.class.getName(),
                    cache, serviceProps);
        } catch (NotCompliantMBeanException e) {
            log.error("Unable to create cache", e);
        }

        /** Error Images **/

        this.errorImagesEnabled = PropertiesUtil.toBoolean(config.get(PROP_ERROR_IMAGES_ENABLED),
                DEFAULT_ERROR_IMAGES_ENABLED);

        this.errorImagePath = PropertiesUtil.toString(config.get(PROP_ERROR_IMAGE_PATH),
                DEFAULT_ERROR_IMAGE_PATH);

        // Absolute path
        if (StringUtils.startsWith(this.errorImagePath, "/")) {
            ResourceResolver adminResourceResolver = null;
            try {
                adminResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                final Resource resource = adminResourceResolver.resolve(this.errorImagePath);

                if (resource != null && resource.isResourceType(JcrConstants.NT_FILE)) {
                    final PathInfo pathInfo = new PathInfo(this.errorImagePath);

                    if (!StringUtils.equals("img", pathInfo.getSelectorString())
                            || StringUtils.isBlank(pathInfo.getExtension())) {

                        log.warn("Absolute Error Image Path paths to nt:files should have '.img.XXX' "
                                + "selector.extension");
                    }
                }
            } catch (LoginException e) {
                log.error("Could not get admin resource resolver to inspect validity of absolute errorImagePath");
            } finally {
                if (adminResourceResolver != null) {
                    adminResourceResolver.close();
                }
            }
        }

        this.errorImageExtensions = PropertiesUtil.toStringArray(config.get(PROP_ERROR_IMAGE_EXTENSIONS),
                DEFAULT_ERROR_IMAGE_EXTENSIONS);

        for (int i = 0; i < errorImageExtensions.length; i++) {
            this.errorImageExtensions[i] = StringUtils.lowerCase(errorImageExtensions[i], Locale.ENGLISH);
        }

        log.debug("Enabled: {}", this.enabled);

        log.debug("System Error Page Path: {}", this.systemErrorPagePath);
        log.debug("Error Page Extension: {}", this.errorPageExtension);
        log.debug("Fallback Error Page Name: {}", this.fallbackErrorName);

        log.debug("Cache - TTL: {}", ttl);
        log.debug("Cache - Serve Authenticated: {}", serveAuthenticatedFromCache);

        log.debug("Error Images - Enabled: {}", this.errorImagesEnabled);
        log.debug("Error Images - Path: {}", this.errorImagePath);
        log.debug("Error Images - Extensions: {}", Arrays.toString(this.errorImageExtensions));
    }

    /**
     * Convert OSGi Property storing Root content paths:Error page paths into a SortMap.
     *
     * @param paths
     * @return
     */
    private SortedMap<String, String> configurePathMap(String[] paths) {
        SortedMap<String, String> sortedMap = new TreeMap<String, String>(new StringLengthComparator());

        for (String path : paths) {
            if (StringUtils.isBlank(path)) {
                continue;
            }

            final SimpleEntry<String, String> tmp = toSimpleEntry(path, ":");

            if (tmp == null) {
                continue;
            }

            String key = StringUtils.strip((String) tmp.getKey());
            String val = StringUtils.strip((String) tmp.getValue());

            // Only accept absolute paths
            if (StringUtils.isBlank(key) || !StringUtils.startsWith(key, "/")) {
                continue;
            }

            // Validate page name value
            if (StringUtils.isBlank(val)) {
                val = key + "/" + DEFAULT_ERROR_PAGE_NAME;
            } else if (StringUtils.equals(val, ".")) {
                val = key;
            } else if (!StringUtils.startsWith(val, "/")) {
                val = key + "/" + val;
            }

            sortedMap.put(key, val);
        }

        return sortedMap;
    }

    public void includeUsingGET(final SlingHttpServletRequest request, final SlingHttpServletResponse response,
                                final String path) {
        if (cache == null
                || errorImagesEnabled && this.isImageRequest(request)) {
            final RequestDispatcher dispatcher = request.getRequestDispatcher(path);

            if (dispatcher != null) {
                try {
                    dispatcher.include(new GetRequest(request), response);
                } catch (Exception e) {
                    log.debug("Exception swallowed while including error page", e);
                }
            }
        } else {
            final String responseData = cache.get(path, new GetRequest(request), response);
            try {
                response.getWriter().write(responseData);
            } catch (Exception e) {
                log.info("Exception swallowed while including error page", e);
            }
        }
    }

    /**
     * Forces request to behave as a GET Request.
     */
    private static class GetRequest extends SlingHttpServletRequestWrapper {

        public GetRequest(SlingHttpServletRequest wrappedRequest) {
            super(wrappedRequest);
        }

        @Override
        public String getMethod() {
            return "GET";
        }
    }

}
