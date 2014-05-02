package com.adobe.acs.commons.replication.status.impl;

import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.day.cq.jcrclustersupport.ClusterAware;
import com.day.cq.replication.ReplicationAction;
import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.Packaging;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - JCR Package Replication Status Updater (Event Handler)"
)
@Properties({
        @Property(
                label = "Event Topics",
                value = { ReplicationAction.EVENT_TOPIC },
                description = "[Required] Event Topics this event handler will to respond to.",
                name = EventConstants.EVENT_TOPIC,
                propertyPrivate = true
        ),
        @Property(
                label = "Event Filters",
                value = "(" + ReplicationAction.PROPERTY_TYPE + "=ACTIVATE)",
                name = EventConstants.EVENT_FILTER,
                propertyPrivate = true
        )
})
@Service
public class JcrPackageReplicationStatusEventHandlerImpl implements JobProcessor, EventHandler, ClusterAware {
    private static final Logger log = LoggerFactory.getLogger(ReplicationStatusManagerImpl.class);

    @Reference
    private Packaging packaging;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private ReplicationStatusManager replicationStatusManager;

    private ResourceResolver adminResourceResolver;

    private boolean isMaster = false;

    @Override
    public final void handleEvent(final Event event) {
        final String[] paths = (String[]) event.getProperty("paths");

        if(this.containsJcrPackagePath(paths) && !CollectionUtils.isEmpty(this.getJcrPackages(paths))) {
            JobUtil.processJob(event, this);
        }
    }

    @Override
    public final boolean process(final Event event) {
        final String[] paths = (String[]) event.getProperty("paths");

        log.debug("Processing Replication Status Update for JCR Package: {}", paths);

        final List<JcrPackage> jcrPackages = this.getJcrPackages(paths);

        if(CollectionUtils.isEmpty(jcrPackages)) {
            log.warn("JCR Package is unavailable for Replication Status Update at: {}", paths);
            return true;
        }

        for(final JcrPackage jcrPackage : jcrPackages) {
            try {
                replicationStatusManager.updateReplicationStatus(this.adminResourceResolver,
                        ReplicationStatusManager.Status.ACTIVATED, jcrPackage);
                log.info("Updated Replication Status for JCR Package: {}", jcrPackage.getDefinition().getId());

            } catch (RepositoryException e) {
                log.error("RepositoryException occurred updating replication status for contents of package");
                log.error(e.getMessage());

            } catch (IOException e) {
                log.error("IOException occurred updating replication status for contents of package");
                log.error(e.getMessage());

            }
        }

        return true;
    }

    private boolean containsJcrPackagePath(final String[] paths) {
        for(final String path : paths) {
            if (StringUtils.startsWith(path, "/etc/packages/")
                    && StringUtils.endsWith(path, ".zip")) {
                // At least 1 entry looks like a package
                return true;
            }
        }

        // Nothing looks like a package...
        return false;
    }

    private List<JcrPackage> getJcrPackages(final String[] paths)  {
        final List<JcrPackage> packages = new ArrayList<JcrPackage>();

        for(final String path : paths) {
            final Resource eventResource = this.adminResourceResolver.getResource(path);

            JcrPackage jcrPackage;

            try {
                jcrPackage = packaging.open(eventResource.adaptTo(Node.class), false);
                if(jcrPackage != null) {
                    packages.add(jcrPackage);
                }
            } catch (RepositoryException e) {
                log.warn("Error checking if the path [ {} ] is a JCR Package.", path);
            }

        }
        return packages;
    }

    @Activate
    private void activate(final Map<String, String> properties) throws LoginException {
        log.info("Activating the ACS AEM Commons - JCR Package Replication Status Updater (Event Handler)");

        this.adminResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
    }

    @Deactivate
    private void deactivate(final Map<String, String> properties) {
        if(this.adminResourceResolver != null) {
            this.adminResourceResolver.close();
        }
    }

    @Override
    public void bindRepository(String repositoryId, String clusterId, boolean isMaster) {
        this.isMaster = isMaster;
    }

    @Override
    public void unbindRepository() {
        this.isMaster = false;
    }
}
