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
package com.adobe.acs.commons.replication.packages.automatic;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.management.NotCompliantMBeanException;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.sling.hc.api.Result;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replication.packages.automatic.impl.ErrorLoggingProgressListener;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

/**
 * Base implementation to automatically build and replicate a package.
 */
public abstract class AbstractAutomaticPackageReplicator extends AnnotatedStandardMBean
    implements AutomaticPackageReplicator {
  private static final Logger log = LoggerFactory.getLogger(AbstractAutomaticPackageReplicator.class);

  public static final String OSGI_EVENT_FAILED_TOPIC = "com/adobe/acs/commons/packages/automatic/REPLICATION_FAILED";
  public static final String OSGI_EVENT_PACKAGE_PATH_PARAM = "packagePath";
  public static final String OSGI_EVENT_REPLICATED_TOPIC = "com/adobe/acs/commons/packages/automatic/REPLICATED";
  public static final String SERVICE_USER_NAME = "automatic-package-replication";
  private Result.Status lastStatus;

  public AbstractAutomaticPackageReplicator() throws NotCompliantMBeanException {
    super(AutomaticPackageReplicator.class);
  }

  @Override
  public Result.Status getLastStatus() {
    return lastStatus;

  }

  protected void doReplicatePackage(JcrPackageManager packageManager)
      throws RepositoryException, PackageException, IOException, ReplicationException {

    boolean succeeded = false;
    String packagePath = getPackagePath();
    try {

      PackageId packageId = new PackageId(packagePath);

      // check if the package exists
      JcrPackage jcrPackage = packageManager.open(packageId);
      if (jcrPackage == null || jcrPackage.getNode() == null) {
        log.warn("Package at path {} does not exist", packagePath);
        throw new IllegalArgumentException("Package at path " + packagePath + " does not exist");
      }

      log.debug("Assembling package {}", packagePath);
      packageManager.assemble(jcrPackage, new ErrorLoggingProgressListener());

      log.debug("Replicating package {}", packagePath);
      Node pkgNode = Optional.ofNullable(jcrPackage.getNode())
          .orElseThrow(() -> new RepositoryException("Failed to get package node"));
      getReplicator().replicate(packageManager.getPackageRoot().getSession(), ReplicationActionType.ACTIVATE,
          pkgNode.getPath());

      log.debug("Package {} replicated successfully!", packagePath);
      fireEvent(OSGI_EVENT_REPLICATED_TOPIC, packagePath);
      succeeded = true;
      lastStatus = Result.Status.OK;
    } finally {
      if (!succeeded) {
        fireEvent(OSGI_EVENT_FAILED_TOPIC, packagePath);
        lastStatus = Result.Status.CRITICAL;
      }
    }
  }

  private void fireEvent(String topic, String packagePath) {
    final Event event = new Event(topic, Collections.singletonMap(OSGI_EVENT_PACKAGE_PATH_PARAM, packagePath));
    getEventAdmin().postEvent(event);
  }

  /**
   * Get the event admin to send events after the job is complete
   * 
   * @return the event admin
   */
  public abstract EventAdmin getEventAdmin();

  /**
   * The path of the package configured for the instance of the
   * AutomaticPackageReplicator.
   * 
   * @return the package path
   */
  public abstract String getPackagePath();

  /**
   * Get the replicator service to replicate the package
   * 
   * @return the replicator instance
   */
  public abstract Replicator getReplicator();

}
