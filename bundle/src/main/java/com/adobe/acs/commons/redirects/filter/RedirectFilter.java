/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.redirects.filter;

import com.adobe.acs.commons.redirects.LocationHeaderAdjuster;
import com.adobe.acs.commons.redirects.models.RedirectMatch;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.engine.EngineConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Hashtable;
import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.sling.engine.EngineConstants.SLING_FILTER_SCOPE;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_ID;

/**
 * A request filter that implements support for virtual redirects.
 */
@Component(service = {Filter.class, RedirectFilterMBean.class, EventHandler.class},
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
        SERVICE_DESCRIPTION + "=A request filter implementing support for virtual redirects",
        SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
        SERVICE_RANKING + ":Integer=10000",
        "jmx.objectname=" + "com.adobe.acs.commons:type=Redirect Manager",
        EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC

})
@Designate(ocd = RedirectFilter.Configuration.class)
public class RedirectFilter extends AnnotatedStandardMBean
        implements Filter, EventHandler, ResourceChangeListener, RedirectFilterMBean {

    public static final String DEFAULT_STORAGE_PATH = "/conf/acs-commons/redirects";
    public static final String ACS_REDIRECTS_RESOURCE_TYPE = "acs-commons/components/utilities/manage-redirects";
    public static final String REDIRECT_RULE_RESOURCE_TYPE = ACS_REDIRECTS_RESOURCE_TYPE + "/redirect-row";


    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String SERVICE_NAME = "redirect-manager";

    @ObjectClassDefinition(name = "ACS Commons Redirect Filter")
    public @interface Configuration {
        @AttributeDefinition(name = "Enable Redirect Filter", description = "Indicates whether the redirect filter is enabled or not.", type = AttributeType.BOOLEAN)
        boolean enabled() default true;

        @AttributeDefinition(name = "Rewrite Location Header", description = "Apply Sling Resource Mappings (/etc/map) to Location header. "
                + "Use if Location header should rewritten using ResourceResolver#map", type = AttributeType.BOOLEAN)
        boolean mapUrls() default true;

        @AttributeDefinition(name = "Request Extensions", description = "List of extensions for which redirection is allowed", type = AttributeType.STRING)
        String[] extensions() default {};

        @AttributeDefinition(name = "Request Paths", description = "List of paths for which redirection is allowed", type = AttributeType.STRING)
        String[] paths() default {"/content"};

        @AttributeDefinition(name = "Preserve Query String", description = "Preserve query string in redirects", type = AttributeType.BOOLEAN)
        boolean preserveQueryString() default true;

        @AttributeDefinition(name = "Storage Path", description = "The path in the repository to store redirect configurations", type = AttributeType.STRING)
        String storagePath() default DEFAULT_STORAGE_PATH;

        @AttributeDefinition(name = "Additional Response Headers", description = "Optional response headers in the name:value format to apply on delivery,"
                + " e.g. Cache-Control: max-age=3600", type = AttributeType.STRING)
        String[] additionalHeaders() default {};
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference(
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.STATIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    private LocationHeaderAdjuster urlAdjuster;

    private ServiceRegistration<?> listenerRegistration;
    private boolean enabled;
    private boolean mapUrls;
    private boolean preserveQueryString;
    private List<Header> onDeliveryHeaders;
    private Collection<String> methods = Arrays.asList("GET", "HEAD");
    private Collection<String> exts;
    private Collection<String> paths;
    private String storagePath;
    private Map<String, RedirectRule> pathRules;
    private Map<Pattern, RedirectRule> patternRules;
    private ExecutorService executor;

    public RedirectFilter() throws NotCompliantMBeanException {
        super(RedirectFilterMBean.class);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no op
    }

    @Activate
    @Modified
    protected final void activate(Configuration config, BundleContext context) {
        enabled = config.enabled();

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(ResourceChangeListener.PATHS, config.storagePath());
        listenerRegistration = context.registerService(ResourceChangeListener.class, this, properties);
        log.debug("Registered {}:{}", SERVICE_ID, listenerRegistration.getReference().getProperty(SERVICE_ID));

        if (enabled) {
            exts = config.extensions() == null ? Collections.emptySet()
                    : Arrays.stream(config.extensions()).filter(ext -> !ext.isEmpty()).collect(Collectors.toSet());
            paths = config.paths() == null ? Collections.emptySet() : Arrays.stream(config.paths()).filter(path -> !path.isEmpty()).collect(Collectors.toSet());
            mapUrls = config.mapUrls();
            storagePath = config.storagePath();
            onDeliveryHeaders = new ArrayList<>();
            for(String kv : config.additionalHeaders()){
                int idx = kv.indexOf(':');
                if(idx == -1 || idx > kv.length() - 1) {
                    log.error("invalid on-delivery header: {}", kv);
                    continue;
                }
                String name = kv.substring(0, idx).trim();
                String value = kv.substring(idx + 1).trim();
                onDeliveryHeaders.add(new BasicHeader(name, value));
            }
            preserveQueryString = config.preserveQueryString();
            log.debug("exts: {}, paths: {}, rewriteUrls: {}, storagePath: {}",
                    exts, paths, mapUrls, storagePath);
            executor = Executors.newSingleThreadExecutor();

            refreshCache();
        }
    }

    @Modified
    protected void modify(BundleContext context, Configuration config) {
        deactivate();
        activate(config, context);
    }


    @Deactivate
    public void deactivate() {
        executor.shutdown();

        if (listenerRegistration != null) {
            log.debug("unregistering ... ");
            listenerRegistration.unregister();
            listenerRegistration = null;
        }
    }

    @Override
    public void handleEvent(Event event) {
        String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        if (path.startsWith(getStoragePath())) {
            log.debug(event.toString());
            // loading redirect configurations can be expensive and needs to run
            // asynchronously,
            // outside of the Sling event processing chain
            executor.submit(() -> refreshCache());
        }
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        boolean changed = changes.stream().anyMatch(e -> e.getPath().startsWith(getStoragePath()));
        if(changed) {
            log.debug(changes.toString());
            executor.submit(() -> refreshCache());
        }
    }

    @Override
    public void refreshCache() {
        Map<String, RedirectRule> pathMatchingRules = new HashMap<>();
        Map<Pattern, RedirectRule> patternMatchingRules = new LinkedHashMap<>();
        long t0 = System.currentTimeMillis();
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME))) {
            Resource storageResource = resolver.getResource(getStoragePath());
            if (storageResource != null) {
                Collection<RedirectRule> rules = getRules(storageResource);
                for (RedirectRule rule : rules) {
                    if (rule.getRegex() != null) {
                        patternMatchingRules.put(rule.getRegex(), rule);
                    } else {
                        pathMatchingRules.put(rule.getSource(), rule);
                    }
                }
            }
        } catch (LoginException e) {
            log.error("Failed to get resolver for {}", SERVICE_NAME, e);
        }
        synchronized (this) {
            this.pathRules = pathMatchingRules;
            this.patternRules = patternMatchingRules;
        }
        log.debug("{} rules loaded in {} ms", pathMatchingRules.size() + patternMatchingRules.size(),
                System.currentTimeMillis() - t0);
    }

    Map<String, RedirectRule> getPathRules() {
        return pathRules;
    }

    Map<Pattern, RedirectRule> getPatternRules() {
        return patternRules;
    }

    /**
     * Read redirect configurations from the repository, i.e.
     *  /conf/acs-commons/redirects --> Collection<RedirectRule>
     *
     * @param resource the parent resource containing redirect configurations
     * @return a list of redirect configurations . Can be empty if no redirects are
     * configured.
     */
    public static Collection<RedirectRule> getRules(Resource resource) {
        Collection<RedirectRule> rules = new ArrayList<>();
        for (Resource res : resource.getChildren()) {
            if(res.isResourceType(REDIRECT_RULE_RESOURCE_TYPE)){
                rules.add(new RedirectRule(res.getValueMap()));
            }
        }
        return rules;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof SlingHttpServletRequest)
                || !(response instanceof SlingHttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

        if (isEnabled() && doesRequestMatch(slingRequest) && handleRedirect(slingRequest, slingResponse)) {
            return;
        }
        chain.doFilter(request, response);
    }

    public boolean handleRedirect(SlingHttpServletRequest slingRequest, SlingHttpServletResponse slingResponse) {
        long t0 = System.currentTimeMillis();
        boolean redirected = false;
        RedirectMatch match = match(slingRequest);
        if (match != null) {
            RedirectRule redirectRule = match.getRule();
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime untilDateTime = redirectRule.getUntilDateTime();
            if (untilDateTime != null && untilDateTime.isBefore(now)) {
                log.info("redirect rule matched, but expired: {}", redirectRule.getUntilDate());
            } else {
                RequestPathInfo pathInfo = slingRequest.getRequestPathInfo();

                String resourcePath = pathInfo.getResourcePath();
                log.info("matched {} to {} in {} ms", resourcePath, redirectRule.toString(),
                        System.currentTimeMillis() - t0);

                String location = redirectRule.evaluate(match.getMatcher());
                if (StringUtils.startsWith(location, "/") && !StringUtils.startsWith(location, "//")) {
                    String ext = pathInfo.getExtension();
                    if (ext != null && !location.endsWith(ext)) {
                        location += "." + ext;
                    }
                    if (mapUrls()) {
                        location = mapUrl(location, slingRequest.getResourceResolver());
                    }
                    if(preserveQueryString) {
                        String queryString = slingRequest.getQueryString();
                        if (queryString != null) {
                            int idx = location.indexOf('?');
                            if (idx == -1) {
                                idx = location.indexOf('#');
                            }
                            if (idx != -1) {
                                location = location.substring(0, idx);
                            }

                            location += "?" + queryString;
                        }
                    }

                    if(urlAdjuster != null){
                        location = urlAdjuster.adjust(slingRequest, location);
                    }
                }

                log.info("Redirecting {} to {}, statusCode: {}",
                        resourcePath, location, redirectRule.getStatusCode());
                slingResponse.setHeader("Location", location);
                for(Header header : onDeliveryHeaders){
                    slingResponse.addHeader(header.getName(), header.getValue());
                }
                slingResponse.setStatus(redirectRule.getStatusCode());
                redirected = true;
            }
        }
        return redirected;
    }

    String mapUrl(String url, ResourceResolver resourceResolver) {
        return resourceResolver.map(url);
    }

    @Override
    public void destroy() {
        // no op
    }

    protected boolean mapUrls() {
        return mapUrls;
    }

    /**
     * @return whether redirection is enabled
     */
    protected boolean isEnabled() {
        return enabled;
    }

    public String getStoragePath() {
        return storagePath;
    }

    protected Collection<String> getExtensions() {
        return exts;
    }

    protected Collection<String> getPaths() {
        return paths;
    }

    protected Collection<String> getMethods() {
        return methods;
    }

    protected List<Header> getOnDeliveryHeaders() {
        return onDeliveryHeaders;
    }

    /**
     * Check whether redirection for the given request is allowed.
     * <ol>
     * <li>On author redirects are disabled in EDIT, PREVIEW and DESIGN WCM Modes.
     * To test on author you need to disable WCM mode and append &wcmmode=disabled
     * to the query string</li>
     * <li>Redirects are supported only for GET and HEAD methods</li>
     * This can be changed in the OSGi configuration</li>
     * <li>If configured, redirects are allowed only for the specified extensions,
     * e.g. only *.html requests will be redirected. Same path with .json extension
     * will <i>not</i> be redirected. This feature is disabled by default.</li>
     * </ol>
     *
     * @param request the request to check
     * @return whether redirection for the given is allowed
     */
    private boolean doesRequestMatch(SlingHttpServletRequest request) {
        WCMMode wcmMode = WCMMode.fromRequest(request);
        if (wcmMode != null && wcmMode != WCMMode.DISABLED) {
            log.trace("Request in author mode: {}, no redirection.", wcmMode);
            return false;
        }

        String method = request.getMethod();
        if (!getMethods().contains(method)) {
            log.trace("Request method [{}] does not match any of {}.", method, methods);
            return false;
        }

        String ext = request.getRequestPathInfo().getExtension();
        if (ext != null && !getExtensions().isEmpty() && !getExtensions().contains(ext)) {
            log.trace("Request extension [{}] does not match any of {}.", ext, exts);
            return false;
        }

        String resourcePath = request.getRequestPathInfo().getResourcePath();
        boolean matches = getPaths().isEmpty() || getPaths().stream().anyMatch(p -> resourcePath.startsWith(p + "/"));
        if (!matches) {
            log.trace("Request path [{}] not within any of {}.", resourcePath, paths);
            return false;
        }
        return true;
    }

    /**
     * @return resource path without extension
     */
    private static String getResourcePath(RequestPathInfo pathInfo) {
        String resourcePath = pathInfo.getResourcePath();
        int sep = resourcePath.indexOf('.');
        if (sep != -1 && !resourcePath.startsWith("/content/dam/")) {
            // strip off extension if present
            resourcePath = resourcePath.substring(0, sep);
        }
        return resourcePath;
    }

    /**
     * Match a path to a redirect configuration.
     * <p>
     * If multiple rules match then the exact rule by path takes precedence over
     * pattern matches, for example, if two rules match then the exact match by path
     * will be used:
     *
     * @param requestPath path to match
     * @return redirect match or <code>null</code>
     */
    RedirectMatch match(String requestPath) {
        RedirectMatch match = null;
        RedirectRule rule = getPathRules().get(requestPath);
        if (rule != null) {
            match = new RedirectMatch(rule, null);
        } else {
            for (Map.Entry<Pattern, RedirectRule> entry : getPatternRules().entrySet()) {
                Matcher m = entry.getKey().matcher(requestPath);
                if (m.matches()) {
                    match = new RedirectMatch(entry.getValue(), m);
                    break;
                }
            }
        }
        return match;

    }

    /**
     * Match a path to a redirect configuration.
     * <p>
     * This method performs two tries: first for the request path, e.g.
     * /content/we.retail/en/page. If the first try didn't match then rewrite the url
     * ( /content/we.retail/en/page -> /en/page ) and try it.
     * </p>
     *
     * @param slingRequest the request to match
     * @return redirect match or <code>null</code>
     */
    RedirectMatch match(SlingHttpServletRequest slingRequest) {
        String resourcePath = getResourcePath(slingRequest.getRequestPathInfo());
        RedirectMatch rule = match(resourcePath);
        if (rule == null) {
            rule = match(mapUrl(resourcePath, slingRequest.getResourceResolver()));
        }
        return rule;
    }

    /**
     * Display cache contents in the MBean
     *
     * @return the redirect configurations in a tabular format for the MBean
     */
    @Override
    public TabularData getRedirectConfigurations() throws OpenDataException {
        String sourceUrl = "Source Url";
        String targetUrl = "Target Url";
        String statusCode = "Status Code";
        String redirectRules = "Redirect Rules";
        CompositeType cacheEntryType = new CompositeType(redirectRules, redirectRules,
                new String[]{sourceUrl, targetUrl, statusCode},
                new String[]{sourceUrl, targetUrl, statusCode},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER});

        TabularDataSupport tabularData = new TabularDataSupport(
                new TabularType(redirectRules, redirectRules, cacheEntryType, new String[]{sourceUrl}));

        Collection<RedirectRule> rules = new ArrayList<>();
        Map<String, RedirectRule> pathMatchingRules = getPathRules();
        if (pathMatchingRules != null) {
            rules.addAll(pathMatchingRules.values());
        }
        Map<Pattern, RedirectRule> patternMatchingRules = getPatternRules();
        if (patternMatchingRules != null) {
            rules.addAll(patternMatchingRules.values());
        }
        for (RedirectRule rule : rules) {
            Map<String, Object> row = new LinkedHashMap<>();

            row.put(sourceUrl, rule.getSource());
            row.put(targetUrl, rule.getTarget());
            row.put(statusCode, rule.getStatusCode());
            tabularData.put(new CompositeDataSupport(cacheEntryType, row));
        }
        return tabularData;
    }

}