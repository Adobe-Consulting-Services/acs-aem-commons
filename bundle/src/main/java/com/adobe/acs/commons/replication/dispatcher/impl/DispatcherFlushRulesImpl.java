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

package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter.FlushType;
import com.adobe.acs.commons.util.ParameterUtil;
import com.day.cq.replication.AgentManager;
import com.day.cq.replication.Preprocessor;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationOptions;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(
        label = "ACS AEM Commons - Dispatcher Flush Rules",
        description = "Facilitates the flushing of associated paths based on resources being replicated. "
                + "All flushes use the AEM Replication APIs and support queuing on the Replication Agent."
                + "ResourceOnly flushes require Replication Flush Agents with the HTTP Header of "
                + "'CQ-Action-Scope: ResourceOnly'."
                + "Neither rule sets supports chaining; { /a/.*=/b/c -> /b/.*=/d/e }, "
                + "due to dangerous cyclic conditions.",
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
        @Property(
                name = "webconsole.configurationFactory.nameHint",
                value = "Rule: {prop.replication-action-type}, for Hierarchy: [{prop.rules.hierarchical}] or Resources: [{prop.rules.resource-only}]")
})
public class DispatcherFlushRulesImpl implements Preprocessor {
    private static final Logger log = LoggerFactory.getLogger(DispatcherFlushRulesImpl.class);

    private static final String OPTION_INHERIT = "INHERIT";
    private static final String OPTION_ACTIVATE = "ACTIVATE";
    private static final String OPTION_DELETE = "DELETE";


    private static final DispatcherFlushFilter HIERARCHICAL_FILTER =
            new DispatcherFlushRulesFilter(FlushType.Hierarchical);
    private static final DispatcherFlushFilter RESOURCE_ONLY_FILTER =
            new DispatcherFlushRulesFilter(FlushType.ResourceOnly);

    /* Replication Action Type Property */

    private static final String DEFAULT_REPLICATION_ACTION_TYPE_NAME = OPTION_INHERIT;
    @Property(label = "Replication Action Type",
            description = "The Replication Action Type to use when issuing the flush cmd to the associated paths. "
                    + "If 'Inherit' is selected, the Replication Action Type of the observed Replication Action "
                    + "will be used.",
            options = {
                    @PropertyOption(name = OPTION_INHERIT, value = "Inherit"),
                    @PropertyOption(name = OPTION_ACTIVATE, value = "Invalidate Cache"),
                    @PropertyOption(name = OPTION_DELETE, value = "Delete Cache")
            })
    private static final String PROP_REPLICATION_ACTION_TYPE_NAME = "prop.replication-action-type";


    /* Flush Rules */
    private static final String[] DEFAULT_HIERARCHICAL_FLUSH_RULES = {};

    @Property(label = "Flush Rules (Hierarchical)",
            description = "Pattern to Path associations for flush rules."
                    + "Format: <pattern-of-trigger-content>=<path-to-flush>",
            cardinality = Integer.MAX_VALUE,
            value = { })
    private static final String PROP_FLUSH_RULES = "prop.rules.hierarchical";


    /* Flush Rules */
    private static final String[] DEFAULT_RESOURCE_ONLY_FLUSH_RULES = {};

    @Property(label = "Flush Rules (ResourceOnly)",
            description = "Pattern to Path associations for flush rules. "
                    + "Format: <pattern-of-trigger-content>=<path-to-flush>",
            cardinality = Integer.MAX_VALUE,
            value = { })
    private static final String PROP_RESOURCE_ONLY_FLUSH_RULES = "prop.rules.resource-only";

    private static final String SERVICE_NAME = "dispatcher-flush";
    protected static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private DispatcherFlusher dispatcherFlusher;

    @Reference
    private AgentManager agentManager;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private Map<Pattern, String[]> hierarchicalFlushRules = new LinkedHashMap<Pattern, String[]>();
    private Map<Pattern, String[]> resourceOnlyFlushRules = new LinkedHashMap<Pattern, String[]>();
    private ReplicationActionType replicationActionType = null;

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("squid:S3776")
    public final void preprocess(final ReplicationAction replicationAction,
                                 final ReplicationOptions replicationOptions) throws ReplicationException {
        if (!this.accepts(replicationAction, replicationOptions)) {
            return;
        }

        // Path being replicated
        final String path = replicationAction.getPath();

        // Replication action type occurring
        final ReplicationActionType flushActionType =
                replicationActionType == null ? replicationAction.getType() : replicationActionType;

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)){

            // Flush full content hierarchies
            for (final Map.Entry<Pattern, String[]> entry : this.hierarchicalFlushRules.entrySet()) {
                final Pattern pattern = entry.getKey();
                final Matcher m = pattern.matcher(path);

                if (m.matches()) {
                    for (final String value : entry.getValue()) {
                        final String flushPath = m.replaceAll(value);
    
                        log.debug("Requesting hierarchical flush of associated path: {} ~> {}", path,
                                flushPath);
                        dispatcherFlusher.flush(resourceResolver, flushActionType, false,
                                HIERARCHICAL_FILTER,
                                flushPath);
                    }
                }
            }

            // Flush explicit resources using the CQ-Action-Scope ResourceOnly header
            for (final Map.Entry<Pattern, String[]> entry : this.resourceOnlyFlushRules.entrySet()) {
                final Pattern pattern = entry.getKey();
                final Matcher m = pattern.matcher(path);

                if (m.matches()) {
                    for (final String value : entry.getValue()) {
                        final String flushPath = m.replaceAll(value);
    
                        log.debug("Requesting ResourceOnly flush of associated path: {} ~> {}", path, entry.getValue());
                        dispatcherFlusher.flush(resourceResolver, flushActionType, false,
                                RESOURCE_ONLY_FILTER,
                                flushPath);
                    }
                }
            }

        } catch (ReplicationException e) {
            // ReplicationException must be caught here, as otherwise this will prevent the replication at all
            log.error("Error issuing dispatcher flush rules, some downstream replication exception occurred: {}", e.getMessage(), e);
        } catch (LoginException e) {
            log.error("Error issuing dispatcher flush rules due to a repository login exception: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks if this service should react to or ignore this replication action.
     *
     * @param replicationAction The replication action that is initiating this flush request
     * @param replicationOptions The replication options that is initiating this flush request
     * @return true is this service should attempt to flush associated resources for this replication request
     */
    private boolean accepts(final ReplicationAction replicationAction, final ReplicationOptions replicationOptions) {
        if (replicationAction == null || replicationOptions == null)  {
            log.debug("Replication Action or Options are null. Skipping this replication.");
            return false;
        }

        final String path = replicationAction.getPath();

        if (replicationOptions.getFilter() instanceof DispatcherFlushRulesFilter) {
            log.debug("Ignore applying dispatcher flush rules for [ {} ], as it originated from this "
                    + "Service.", path);
            return false;
        } else if ((this.hierarchicalFlushRules == null || this.hierarchicalFlushRules.size() < 1)
                && (this.resourceOnlyFlushRules == null || this.resourceOnlyFlushRules.size() < 1)) {
            log.warn("Ignored due no configured flush rules.");
            return false;
        } else if (StringUtils.isBlank(path)) {
            // Do nothing on blank paths
            log.debug("Replication Action path is blank. Skipping this replication.");
            return false;
        } else if (!ReplicationActionType.ACTIVATE.equals(replicationAction.getType())
                && !ReplicationActionType.DEACTIVATE.equals(replicationAction.getType())
                && !ReplicationActionType.DELETE.equals(replicationAction.getType())) {
            // Ignoring non-modifying ReplicationActionTypes
            return false;
        }

        return true;
    }

    @Activate
    protected final void activate(final Map<String, String> properties) throws Exception {
        /* Replication Action Type */
        this.replicationActionType = this.configureReplicationActionType(
                PropertiesUtil.toString(properties.get(PROP_REPLICATION_ACTION_TYPE_NAME),
                        DEFAULT_REPLICATION_ACTION_TYPE_NAME));

        /* Flush Rules */
        this.hierarchicalFlushRules = this.configureFlushRules(ParameterUtil.toMap(
                PropertiesUtil.toStringArray(properties.get(PROP_FLUSH_RULES),
                        DEFAULT_HIERARCHICAL_FLUSH_RULES), "="));

        log.debug("Hierarchical flush rules: " + this.hierarchicalFlushRules);

        /* ResourceOnly Flush Rules */
        this.resourceOnlyFlushRules = this.configureFlushRules(ParameterUtil.toMap(
                PropertiesUtil.toStringArray(properties.get(PROP_RESOURCE_ONLY_FLUSH_RULES),
                        DEFAULT_RESOURCE_ONLY_FLUSH_RULES), "="));

        log.debug("ResourceOnly flush rules: " + this.resourceOnlyFlushRules);
    }

    /**
     * Create Pattern-based flush rules map.
     *
     * @param configuredRules String based flush rules from OSGi configuration
     * @return returns the configures flush rules
     */
     protected final Map<Pattern, String[]> configureFlushRules(final Map<String, String> configuredRules)
             throws Exception {
        final Map<Pattern, String[]> rules = new LinkedHashMap<Pattern, String[]>();

        for (final Map.Entry<String, String> entry : configuredRules.entrySet()) {
            final Pattern pattern = Pattern.compile(entry.getKey().trim());
            rules.put(pattern, entry.getValue().trim().split("&"));
        }

        return rules;
    }

    /**
     * Derive the ReplicationActionType to be used for Flushes.
     *
     * @param replicationActionTypeName String name of ReplicationActionType to use
     * @return the ReplicationActionType to use, or null if the ReplicationActionType should be inherited from the
     * incoming ReplicationAction
     */
    protected final ReplicationActionType configureReplicationActionType(final String replicationActionTypeName) {
        try {
            final ReplicationActionType repActionType =
                    ReplicationActionType.valueOf(replicationActionTypeName);
            log.debug("Using replication action type: {}", repActionType.name());
            return repActionType;
        } catch (IllegalArgumentException ex) {
            log.warn("Illegal action type configured: {}. Falling back to default: {}", replicationActionTypeName, OPTION_INHERIT);
            return null;
        }
    }

    @Deactivate
    protected final void deactivate(final Map<String, String> properties) {
        this.hierarchicalFlushRules = new HashMap<Pattern, String[]>();
        this.resourceOnlyFlushRules = new HashMap<Pattern, String[]>();
        this.replicationActionType = null;
    }

    /* Implementation Class used to track and prevent cyclic replications */
    protected static final class DispatcherFlushRulesFilter extends DispatcherFlushFilter {
        public DispatcherFlushRulesFilter(final FlushType flushType) {
            super(flushType);
        }
    }
}
