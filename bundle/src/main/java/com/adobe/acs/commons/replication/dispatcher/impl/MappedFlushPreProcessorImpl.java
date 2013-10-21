package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.adobe.acs.commons.util.OsgiPropertyUtil;
import com.day.cq.replication.*;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.PropertyOption;
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
        immediate = true,
        metatype = true
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

    private static final String[] DEFAULT_MAPPED_PATHS = { "/content/dam:/content/mysite", "/etc/designs:/content/mysite", "/etc/packages:/" };
    @Property(label = "Flush Path Mapping",
            description = "Example: /content/dam:/content/geometrixx",
            cardinality = Integer.MAX_VALUE,
            value = { "/content/dam:/content/mysite", "/etc/designs:/content/mysite", "/etc/packages:/" })
    private static final String PROP_MAPPED_PATHS = "prop.mapped-paths";
    private Map<Pattern, String> map = new HashMap<Pattern, String>();

    private static final String DEFAULT_REPLICATION_ACTION_TYPE_NAME = OPTION_INHERIT;
    @Property(label = "Replication Action Type",
            description = "",
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
        if(StringUtils.isBlank(path)) { return; }

        final ReplicationActionType flushActionType = replicationActionType == null ? replicationAction.getType() : replicationActionType;

        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            for(final Map.Entry<Pattern, String> entry : map.entrySet()) {
                final Pattern pattern = entry.getKey();
                final Matcher m = pattern.matcher(path);

                if(m.matches()) {
                    log.debug("Requesting flush of mapped path: {} ~> {}", path, entry.getValue());
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
            log.debug("Using replication action type: {}", replicationActionType.getName());
        } catch(IllegalArgumentException ex) {
            replicationActionType = null;
            log.debug("Using replication action type: {}", "Inherit");
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
