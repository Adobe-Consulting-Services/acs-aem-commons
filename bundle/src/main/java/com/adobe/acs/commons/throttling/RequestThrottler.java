/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.throttling;

import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows to throttle certain requests, that means limit the amount of requests
 * matching some path criteria per minute.
 * 
 * The primary goal of this implementation is to prevent that some requests are
 * eating up all available CPUs, while others (more important) requests will be
 * impacted by this behavior. This implementation should be activating itself by
 * detecting such situations and start throttling requests. A simple usecase for
 * this can be to limit the number if incoming replication requests and allow
 * more "other" requests be handled.
 * 
 * Requests which are supposed to handled by this implementation must match one
 * of the configured regular expressions in
 * 
 * <pre>
 * filtered_paths
 * </pre>
 * 
 * . Other requests are not considered at all by the throttling implementation.
 * 
 * The throttling algorithm is loosely based on the leaky bucket approach, but
 * it allows to adjust the throttling based on CPU load. This means:
 * <ul>
 * <li>If the CPU usage is smaller than
 * 
 * <pre>
 * start_throttling_percentage
 * </pre>
 * 
 * , than no throttling is active at all. If you set this value to "0",
 * throttling will always be active.</li>
 * <li>If the CPU usage is higher than the configured
 * 
 * <pre>
 * start_throttling_percentage
 * </pre>
 * 
 * , the throttling algorithm is used. It permits at maximum
 * 
 * <pre>
 * max_requests_per_minute
 * </pre>
 * 
 * requests per minute to pass, all other requests will be throttled.</li>
 * </ul>
 * 
 * The number of requests which are permitted to pass the throttling approach,
 * is determined solely by CPU load; this number starts at the configured
 * maximum (
 * 
 * <pre>
 * max_requests_per_minute
 * </pre>
 * 
 * and decreases linearly as the CPU usages increases. At 100% usage the number
 * of permitted requests is 0 (zero) and all requests (matching the expression
 * in
 * 
 * <pre>
 * filtered_paths
 * </pre>
 * 
 * are throttled.
 * 
 * This implementation supports 2 modes of throttling:
 * <ul>
 * <li>rejecting the request with a configurable HTTP statuscode; is should be
 * used in cases when the client is able to handle this case.</li>
 * <li>Or blocking the request unless it can be handled. This is transparent for
 * the client (the request might time out, though!), but it blocks this requests
 * for the complete time, which might lead to a shortage of threads.</li>
 * </ul>
 * 
 * 
 *
 */

@Component(property = { "sling.filter.scope=REQUEST" })
@Designate(ocd = RequestThrottler.Config.class, factory = true)
public class RequestThrottler implements Filter {

    @ObjectClassDefinition(name = "ACS AEM Commons - Request Throttler", description = "Configuration for the ACS AEM Commons Request Throttler")
    public @interface Config {

        @AttributeDefinition(name = "maximum number of requests per minute", description = "The maximum number of requests allowed if the CPU usage exceeds the configured value")
        int max_requests_per_minute() default 60;

        @AttributeDefinition(name = "Start throttling at X percent CPU load", description = "The CPU usage in percent when the throttling starts")
        int start_throttling_percentage() default 70;

        @AttributeDefinition(name = "reject request on throttling", description = "Check if throttled should be rejected and not be further handled")
        boolean reject_on_throttle() default false;

        @AttributeDefinition(name = "HTTP statuscode on reject", description = "the statuscode in case the requests are rejected (e.g. 500")
        int http_status_on_reject() default 503;

        @AttributeDefinition(name = "Filtered paths", description = "The paths (regular expressions) which are considered for this service")
        String[] filtered_paths();

        String webconsole_configurationFactory_nameHint() default "{filtered.paths}";

    }

    private static final Logger LOG = LoggerFactory.getLogger(RequestThrottler.class);

    ThrottlingState state;
    private Config config;

    CpuLoadEstimator loadEstimator;

    List<Pattern> filteredPaths;

    Clock clock;

    @Activate
    @Modified
    protected void activate(Config c) {
        this.config = c;
        ThrottlingConfiguration tc = new ThrottlingConfiguration(c.max_requests_per_minute(),
                c.start_throttling_percentage());
        loadEstimator = new CpuLoadEstimator(tc);
        clock = Clock.systemUTC();
        this.state = new ThrottlingState(clock, loadEstimator);

        // precompile all patterns
        filteredPaths = Arrays.asList(config.filtered_paths()).stream().map(s -> Pattern.compile(s))
                .collect(Collectors.toList());

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        SlingHttpServletRequest req = (SlingHttpServletRequest) request;
        SlingHttpServletResponse res = (SlingHttpServletResponse) response;

        if (needsFiltering(req.getResource().getPath())) {
            doFilterInternal(req, res);
        }

        chain.doFilter(request, response);

    }

    protected void doFilterInternal(SlingHttpServletRequest req, SlingHttpServletResponse res) throws IOException {

        ThrottlingDecision decision = state.evaluateThrottling();
        if (decision.getState().equals(ThrottlingDecision.State.THROTTLE)) {

            if (this.config.reject_on_throttle()) {
                String msg = "Request rejected because of throttling: " + decision.message;
                req.getRequestProgressTracker().log(msg);
                LOG.info(msg);
                res.sendError(config.http_status_on_reject(), decision.message);
            } else {
                String msg = "Throttling request (" + decision.message + ")";
                req.getRequestProgressTracker().log(msg);
                LOG.info(msg);
                delay(decision.delay);

            }

        } else {
            // not throttled
            req.getRequestProgressTracker().log("Request not throttled");
        }
    }

    protected boolean needsFiltering(String path) {

        return filteredPaths.stream().anyMatch(p -> p.matcher(path).matches());

    }

    @SuppressWarnings("CQRules:CWE-676") // use appropriate in this case
    protected void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // obsolete stuff

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // nothing to do

    }

    @Override
    public void destroy() {
        // nothing to do

    }

}
