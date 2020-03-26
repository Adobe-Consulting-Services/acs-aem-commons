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
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
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
  public static final String SERVICE_USER_NAME = "automatic-package-replication";

  private Queue<ResultLog.Entry> recentReplications = Queues.synchronizedQueue(EvictingQueue.create(10));

  private EventAdmin eventAdmin;

  private Packaging packaging;

  private Replicator replicator;

  private ResourceResolverFactory resourceResolverFactory;

  public AutomaticPackageReplicatorImpl() throws NotCompliantMBeanException {
    super(AutomaticPackageReplicator.class);
  }

  @Override
  public Result.Status getStatus() {
    Iterator<ResultLog.Entry> recent = recentReplications.iterator();
    while (recent.hasNext()) {
      ResultLog.Entry res = recent.next();
      if (res.getStatus() != Result.Status.OK) {
        return res.getStatus();
      }
    }
    return Result.Status.OK;
  }

  public void replicatePackage(String packagePath)
      throws RepositoryException, PackageException, IOException, ReplicationException, LoginException {

    boolean succeeded = false;
    try (ResourceResolver resolver = getResourceResolver()) {

      Session session = resolver.adaptTo(Session.class);
      JcrPackageManager packageManager = packaging.getPackageManager(session);
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
    ResultLog rl = new ResultLog();
    this.recentReplications.stream().forEach(rl::add);
    return rl;
  }

  @Reference
  public void setEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  @Reference
  public void setPackaging(Packaging packaging) {
    this.packaging = packaging;
  }

  @Reference
  public void setReplicator(Replicator replicator) {
    this.replicator = replicator;
  }

  @Reference
  public void setResourceResolverFactory(ResourceResolverFactory factory) {
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