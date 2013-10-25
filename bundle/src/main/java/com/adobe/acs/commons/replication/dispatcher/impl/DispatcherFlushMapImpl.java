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

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.adobe.acs.commons.util.OsgiPropertyUtil;
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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(
        label = "ACS AEM Commons - Dispatcher Flush Map",
        description = "Facilitates the flushing of associated paths based on resources being replicated. "
                + "Be careful to avoid infinite flush requests.",
        immediate = false,
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE)
@Service
public class DispatcherFlushMapImpl implements Preprocessor {
    private static final Logger log = LoggerFactory.getLogger(DispatcherFlushMapImpl.class);

    private static final String OPTION_INHERIT = "INHERIT";
    private static final String OPTION_ACTIVATE = "ACTIVATE";
    private static final String OPTION_DEACTIVATE = "DEACTIVATE";
    private static final String OPTION_DELETE = "DELETE";

       /* Flush Rules */

    private static final String[] DEFAULT_FLUSH_RULES = {"/content/dam:/content/mysite/.*",
            "/etc/designs:/content/mysite.*",
            "/etc/packages/.*:/"
    };
    @Property(label = "Flush Rules",
            description = "Pattern to Path associations for flush rules. "
                    + "Format: <pattern-of-trigger-content>:<path-to-flush>",
            cardinality = Integer.MAX_VALUE,
            value = { "/content/dam/.*:/content/mysite/en",
                    "/etc/designs/.*:/content/mysite/en",
                    "/etc/packages/.*:/"
            })
    private static final String PROP_FLUSH_RULES = "prop.flush-rules";

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

    @Reference
    private DispatcherFlusher dispatcherFlusher;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private Map<Pattern, String> flushRules = new LinkedHashMap<Pattern, String>();
    private ReplicationActionType replicationActionType = null;

    @Override
    public final void preprocess(final ReplicationAction replicationAction,
                                 final ReplicationOptions replicationOptions) throws ReplicationException {
        if (!this.accepts(replicationAction)) {
            return;
        }

        final String path = replicationAction.getPath();
        final ReplicationActionType flushActionType =
                replicationActionType == null ? replicationAction.getType() : replicationActionType;

        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            for (final Map.Entry<Pattern, String> entry : flushRules.entrySet()) {
                final Pattern pattern = entry.getKey();
                final Matcher m = pattern.matcher(path);

                if (m.matches()) {
                    log.debug("Requesting flush of associated path: {} ~> {}", path, entry.getValue());
                    dispatcherFlusher.flush(resourceResolver, flushActionType, false, entry.getValue());
                }
            }
        } catch (LoginException e) {
            log.error("Error issuing  dispatcher flush rules do to repository login exception: {}", e.getMessage());
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /**
     * Checks if this service should react to or ignore this replication action.
     *
     * @param replicationAction The replication action that is initiating this flush request
     * @return true is this service should attempt to flush associated resources for this replication request
     */
    private boolean accepts(final ReplicationAction replicationAction) {
        if (replicationAction == null) {
            log.debug("Replication Action is null. Skipping this replication.");
            return false;
        }

        final String path = replicationAction.getPath();

        if (this.flushRules == null || this.flushRules.size() < 1) {
            log.error("Ignored due to dangerous (cyclic) flush rule configuration or no flush rules.");
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

    /**
     * Note: This detection is PER OSGi Configuration. This will NOT detect cycles between configurations.
     *
     * @return true is a cyclic rule path has been detected for this OSGi configuration
     */
    private boolean hasCyclicFlushRules(final Map<Pattern, String> flushRules) {
        for (final Map.Entry<Pattern, String> entry : flushRules.entrySet()) {
            List<Map.Entry<Pattern, String>> list = new ArrayList<Map.Entry<Pattern, String>>();

            list.add(entry);
            if (this.findCyclicFlushRules(flushRules, entry, list)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Recursive "working" function that performs the cyclic detection.
     * <p/>
     * Note: This detection is PER OSGi Configuration. This will NOT detect cycles between configurations.
     *
     * @param parent the previous entry in the flush rule "stack"
     * @param list   the "stack" of previous flush rule entries
     * @return true if a cyclic rule path has been detected in this OSGi configuration
     */
    private boolean findCyclicFlushRules(final Map<Pattern, String> flushRules,
                                         final Map.Entry<Pattern, String> parent,
                                         List<Map.Entry<Pattern, String>> list) {
        for (final Map.Entry<Pattern, String> child : flushRules.entrySet()) {
            final Pattern pattern = child.getKey();
            final Matcher m = pattern.matcher(parent.getValue());

            if (m.matches()) {
                if (list.contains(child)) {
                    log.error("Cyclic flush rules detected: {} ~> {}", parent.getValue(), child.getKey().toString());
                    return true;
                } else {
                    // Some other rule will flush based for a parent flush
                    list.add(child);
                    return this.findCyclicFlushRules(flushRules, child, list);
                }
            }
        }

        return false;
    }

    @Activate
    protected final void activate(final Map<String, String> properties) {

        /* Replication Action Type */
        this.replicationActionType = this.configureReplicationActionType(
                PropertiesUtil.toString(properties.get(PROP_REPLICATION_ACTION_TYPE_NAME),
                        DEFAULT_REPLICATION_ACTION_TYPE_NAME));

        /* Flush Rules */
        this.flushRules = this.configureFlushRules(OsgiPropertyUtil.toMap(
                PropertiesUtil.toStringArray(properties.get(PROP_FLUSH_RULES), DEFAULT_FLUSH_RULES), ":"));
    }

    /**
     * Create Pattern-based flush rules map
     *
     * @param configuredRules String based flush rules from OSGi configuration
     * @return returns the configures flush rules, or an empty map if the rules are cyclic
     */
    protected Map<Pattern, String> configureFlushRules(final Map<String, String> configuredRules) {
        final Map<Pattern, String> flushRules = new LinkedHashMap<Pattern, String>();

        for (final Map.Entry<String, String> entry : configuredRules.entrySet()) {
            final Pattern pattern = Pattern.compile(entry.getKey());
            flushRules.put(pattern, entry.getValue());
            log.debug("Adding flush rule: {} => {}", pattern.pattern(), entry.getValue());
        }

        if (this.hasCyclicFlushRules(flushRules)) {
            log.error("Configuration defines cyclic flush rules. Disabling this configuration for safety!");
            return new LinkedHashMap<Pattern, String>();
        } else {
            return flushRules;
        }
    }

    /**
     * Derive the ReplicationActionType scheme to be used for Flushes
     * @param replicationActionTypeName String name of ReplicationActionType to use
     * @return the ReplicationActionType to use, or null if the ReplicationActionType should be inherited from the
     * incoming ReplicationAction
     */
    protected ReplicationActionType configureReplicationActionType(final String replicationActionTypeName) {
        try {
            final ReplicationActionType replicationActionType = ReplicationActionType.valueOf(replicationActionTypeName);
            log.debug("Using replication action type: {}", replicationActionType.name());
            return replicationActionType;
        } catch (IllegalArgumentException ex) {
            log.debug("Using replication action type: {}", OPTION_INHERIT);
            return null;
        }
    }

    @Deactivate
    protected final void deactivate(final Map<String, String> properties) {
        this.flushRules = new HashMap<Pattern, String>();
        this.replicationActionType = null;
    }
}