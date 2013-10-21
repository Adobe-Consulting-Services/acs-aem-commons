package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushAgentFilter;
import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.adobe.acs.commons.util.OsgiPropertyUtil;
import com.day.cq.replication.*;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(
        label = "ACS AEM Commons - Associated Dispatcher Flush",
        description = "Facilitates the flushing of associated paths based on resources being replicated. This service allows un-related paths to be flushed based on replications to any part of the content tree. Be careful to avoid infinite flush requests.",
        immediate = true,
        metatype = true,
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Service
public class MappedFlushPreProcessorImpl implements Preprocessor {
    private static final Logger log = LoggerFactory.getLogger(MappedFlushPreProcessorImpl.class);

    private static final String OPTION_INHERIT = "INHERIT";
    private static final String OPTION_ACTIVATE = "ACTIVATE";
    private static final String OPTION_DEACTIVATE = "DEACTIVATE";
    private static final String OPTION_DELETE = "DELETE";

    @Reference
    private DispatcherFlusher dispatcherFlusher;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private static final String[] DEFAULT_MAPPED_PATHS = { "/content/dam:/content/mysite/.*", "/etc/designs:/content/mysite.*", "/etc/packages/.*:/" };
    @Property(label = "Flush Rules",
            description = "Pattern to Path associations for flush rules. Format: <pattern-of-replicated-content>:<path-to-flush>",
            cardinality = Integer.MAX_VALUE,
            value = { "/content/dam/.*:/content/mysite/en", "/etc/designs/.*:/content/mysite/en", "/etc/packages/.*:/" })
    private static final String PROP_MAPPED_PATHS = "prop.flush-rules";
    private Map<Pattern, String> map = new HashMap<Pattern, String>();

    private static final String DEFAULT_REPLICATION_ACTION_TYPE_NAME = OPTION_INHERIT;
    @Property(label = "Replication Action Type",
            description = "Force a Replication Action Type to use when issuing the flush commands to the associated paths. If 'Inherit' is selected, the Replication Action Type of the observed Replication Action will be used.",
            options = {
                @PropertyOption(name=OPTION_INHERIT, value="Inherit"),
                @PropertyOption(name=OPTION_ACTIVATE, value="Activate"),
                @PropertyOption(name=OPTION_DEACTIVATE, value="Deactivate"),
                @PropertyOption(name=OPTION_DELETE, value="Delete")
            }
    )
    private static final String PROP_REPLICATION_ACTION_TYPE_NAME = "prop.replication-action-type";

    private ReplicationActionType replicationActionType = null;

    @Override
    public void preprocess(final ReplicationAction replicationAction, final ReplicationOptions replicationOptions) throws ReplicationException {
        final String path = replicationAction.getPath();
        if(StringUtils.isBlank(path) ||
                StringUtils.equals(DispatcherFlushAgentFilter.SERIALIZATION_TYPE, replicationAction.getConfig().getSerializationType())) {
            // Do not trigger associated flushes based on Flush requests or empty paths. This prevents infinite flushing.
            return;
        }

        final ReplicationActionType flushActionType = replicationActionType == null ? replicationAction.getType() : replicationActionType;

        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            for(final Map.Entry<Pattern, String> entry : map.entrySet()) {
                final Pattern pattern = entry.getKey();
                final Matcher m = pattern.matcher(path);

                if(m.matches()) {
                    log.debug("Requesting flush of associated path: {} ~> {}", path, entry.getValue());
                    dispatcherFlusher.flush(resourceResolver, flushActionType, false, entry.getValue());
                }
            }
        } catch (LoginException e) {
            log.error("Error issuing mapped dispatcher flush rules do to repository login exception: {}", e.getMessage());
        } finally {
            if(resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    @Activate
    protected void activate(final Map<String, String> properties) {

        /* Replication Action Type */

        final String replicationActionTypeName = PropertiesUtil.toString(properties.get(PROP_REPLICATION_ACTION_TYPE_NAME), DEFAULT_REPLICATION_ACTION_TYPE_NAME);
        try {
            replicationActionType = ReplicationActionType.valueOf(replicationActionTypeName);
            log.debug("Using replication action type: {}", replicationActionType.name());
        } catch(IllegalArgumentException ex) {
            replicationActionType = null;
            log.debug("Using replication action type: {}", OPTION_INHERIT);
        }

        /* Mapped Paths */

        this.map = new HashMap<Pattern, String>();

        final Map<String, String> tmp = OsgiPropertyUtil.toMap(PropertiesUtil.toStringArray(properties.get(PROP_MAPPED_PATHS), DEFAULT_MAPPED_PATHS), ":");

        for(final Map.Entry<String, String> entry : tmp.entrySet()) {
            final Pattern pattern = Pattern.compile(entry.getKey());
            this.map.put(pattern, entry.getValue());
            log.debug("Adding mapped flush path: {} => {}", pattern.pattern(), entry.getValue());
        }
    }

    @Deactivate
    protected void deactivate(final Map<String, String> properties) {
        this.map = new HashMap<Pattern, String>();
        this.replicationActionType = null;
    }
}
