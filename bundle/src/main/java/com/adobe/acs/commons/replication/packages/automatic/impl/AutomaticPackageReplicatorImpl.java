/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.replication.packages.automatic.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;

import com.adobe.acs.commons.replication.packages.automatic.AutomaticPackageReplicator;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of to automatically build and replicate a package.
 */
@Component(service = { AutomaticPackageReplicator.class, DynamicMBean.class }, property = {
    "jmx.objectname=com.adobe.acs.commons.replication:type=Automatic Package Replicator" })
public class AutomaticPackageReplicatorImpl extends AnnotatedStandardMBean implements AutomaticPackageReplicator {

  private static final Logger log = LoggerFactory.getLogger(AutomaticPackageReplicatorImpl.class);

  public static final String OSGI_EVENT_FAILED_TOPIC = "com/adobe/acs/commons/packages/automatic/REPLICATION_FAILED";
  public static final String OSGI_EVENT_PACKAGE_PATH_PARAM = "packagePath";
  public static final String OSGI_EVENT_REPLICATED_TOPIC = "com/adobe/acs/commons/packages/automatic/REPLICATED";
  public static final String SERVICE_USER_NAME = "automatic-package-replicator";

  private Queue<ResultLog.Entry> recentReplications = Queues.synchronizedQueue(EvictingQueue.create(10));

  private ConfigurationAdmin configAdmin;

  private EventAdmin eventAdmin;

  private Packaging packaging;

  private Replicator replicator;

  private ResourceResolverFactory resourceResolverFactory;

  public AutomaticPackageReplicatorImpl() throws NotCompliantMBeanException {
    super(AutomaticPackageReplicator.class);
  }

  public boolean loadConfigurations() throws LoginException {
    boolean succeeded = true;
    try (ResourceResolver resolver = getResourceResolver()) {
      final Resource aprFolder = resolver.getResource("/etc/acs-commons/automatic-package-replication");
      if (aprFolder != null && aprFolder.hasChildren()) {
        for (Resource configResource : aprFolder.getChildren()) {
          if (NameConstants.NT_PAGE.equals(configResource.getResourceType()) && !loadConfiguration(configResource)) {
            succeeded = false;
          }
        }

      }
    }
    log.info("Configurations loaded, {}!", (succeeded ? "all successful" : "failures, check logs"));
    return succeeded;
  }

  @Override
  public Result.Status getStatus() {
    final Iterator<ResultLog.Entry> recent = recentReplications.iterator();
    while (recent.hasNext()) {
      final ResultLog.Entry res = recent.next();
      if (res.getStatus() != Result.Status.OK) {
        return res.getStatus();
      }
    }
    return Result.Status.OK;
  }

  public boolean loadConfiguration(final Resource configResource) {
    log.info("Loading configuration from: {}", configResource.getPath());

    final ValueMap properties = Optional.ofNullable(configResource.getChild(JcrConstants.JCR_CONTENT))
        .map(Resource::getValueMap).orElse(new ValueMapDecorator(Collections.emptyMap()));
    final String packagePath = properties.get(OSGI_EVENT_PACKAGE_PATH_PARAM, String.class);
    if (StringUtils.isBlank(packagePath)) {
      log.info("Package path not set, skipping");
      return false;
    }

    final String trigger = properties.get("trigger", String.class);
    if (StringUtils.isBlank(trigger)) {
      log.info("Trigger not set, skipping");
      return false;
    }

    try {
      final Dictionary<String, Object> configProperties = new Hashtable<>(); // NOSONAR
      Configuration config;
      if ("cron".equals(trigger)) {
        final String cronTrigger = properties.get("cronTrigger", String.class);
        if (StringUtils.isBlank(cronTrigger)) {
          log.info("Cron trigger not set, skipping");
          return false;
        }

        log.info("Creating scheduled Automatic Package Replication configuration with schedule: {}", cronTrigger);
        config = configAdmin.createFactoryConfiguration(ScheduledAutomaticPackageReplicator.class.getName());

        configProperties.put(Scheduler.PROPERTY_SCHEDULER_EXPRESSION, cronTrigger);
        configProperties.put(OSGI_EVENT_PACKAGE_PATH_PARAM, packagePath);
      } else if ("event".equals(trigger)) {
        final String eventTopic = properties.get("eventTopic", String.class);
        if (StringUtils.isBlank(eventTopic)) {
          log.info("Event topic not set, skipping");
          return false;
        }
        final String eventFilter = properties.get("eventFilter", String.class);

        log.info("Creating event-based Automatic Package Replication configuration with topic: {}", eventTopic);
        if (StringUtils.isNotBlank(eventFilter)) {
          config = configAdmin.createFactoryConfiguration(FilteredEventBasedAutomaticPackageReplicator.class.getName());
          configProperties.put(EventConstants.EVENT_FILTER, eventFilter);
        } else {
          config = configAdmin.createFactoryConfiguration(EventBasedAutomaticPackageReplicator.class.getName());
        }
        configProperties.put(EventConstants.EVENT_TOPIC, eventTopic);
        configProperties.put(OSGI_EVENT_PACKAGE_PATH_PARAM, packagePath);
      } else {
        log.info("Invalid trigger {}, skipping", trigger);
        return false;
      }
      config.update(configProperties);
      log.info("Configuration loaded successfully!");
      return true;
    } catch (final IOException e) {
      log.warn("Failed to create configuration", e);
      return false;
    }
  }

  public void replicatePackage(final String packagePath)
      throws RepositoryException, PackageException, IOException, ReplicationException, LoginException {

    boolean succeeded = false;
    try (ResourceResolver resolver = getResourceResolver()) {

      final Session session = resolver.adaptTo(Session.class);
      final JcrPackageManager packageManager = packaging.getPackageManager(session);
      final PackageId packageId = new PackageId(packagePath);

      // check if the package exists
      final JcrPackage jcrPackage = packageManager.open(packageId);
      if (jcrPackage == null || jcrPackage.getNode() == null) {
        log.warn("Package at path {} does not exist", packagePath);
        throw new IllegalArgumentException("Package at path " + packagePath + " does not exist");
      }

      log.debug("Assembling package {}", packagePath);
      packageManager.assemble(jcrPackage, new ErrorLoggingProgressListener());

      log.debug("Replicating package {}", packagePath);
      final Node pkgNode = Optional.ofNullable(jcrPackage.getNode())
          .orElseThrow(() -> new RepositoryException("Failed to get package node"));
      this.replicator.replicate(session, ReplicationActionType.ACTIVATE, pkgNode.getPath());

      log.debug("Package {} replicated successfully!", packagePath);
      fireEvent(OSGI_EVENT_REPLICATED_TOPIC, packagePath);
      succeeded = true;
      this.recentReplications.add(new ResultLog.Entry(Result.Status.OK, packagePath));
    } finally {
      if (!succeeded) {
        fireEvent(OSGI_EVENT_FAILED_TOPIC, packagePath);
        this.recentReplications.add(new ResultLog.Entry(Result.Status.CRITICAL, packagePath));
      }
    }
  }

  private void fireEvent(final String topic, final String packagePath) {
    final Event event = new Event(topic, Collections.singletonMap(OSGI_EVENT_PACKAGE_PATH_PARAM, packagePath));
    this.eventAdmin.postEvent(event);
  }

  private ResourceResolver getResourceResolver() throws LoginException {
    return this.resourceResolverFactory.getServiceResourceResolver(
        Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, AutomaticPackageReplicatorImpl.SERVICE_USER_NAME));
  }

  public ResultLog getResultLog() {
    final ResultLog rl = new ResultLog();
    this.recentReplications.stream().forEach(rl::add);
    return rl;
  }

  @Reference
  public void setConfigAdmin(final ConfigurationAdmin configAdmin) {
    this.configAdmin = configAdmin;
  }

  @Reference
  public void setEventAdmin(final EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  @Reference
  public void setPackaging(final Packaging packaging) {
    this.packaging = packaging;
  }

  @Reference
  public void setReplicator(final Replicator replicator) {
    this.replicator = replicator;
  }

  @Reference
  public void setResourceResolverFactory(final ResourceResolverFactory factory) {
    this.resourceResolverFactory = factory;
  }

  @Override
  public String[] getRecentReplications() {
    return this.recentReplications.stream().map(e -> e.getStatus().toString() + " - " + e.getMessage())
        .toArray(String[]::new);
  }

  @Override
  public void resetRecentReplications() {
    this.recentReplications.clear();
  }

}