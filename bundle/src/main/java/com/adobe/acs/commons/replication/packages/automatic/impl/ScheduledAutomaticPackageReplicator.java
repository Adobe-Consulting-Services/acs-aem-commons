/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 - Adobe
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

import javax.jcr.RepositoryException;

import com.adobe.acs.commons.replication.packages.automatic.AutomaticPackageReplicator;
import com.adobe.acs.commons.replication.packages.automatic.impl.ScheduledAutomaticPackageReplicator.ScheduledAutomaticPackageReplicatorConfig;
import com.day.cq.replication.ReplicationException;

import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.resource.LoginException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { Runnable.class })
@Designate(ocd = ScheduledAutomaticPackageReplicatorConfig.class, factory = true)
public class ScheduledAutomaticPackageReplicator implements Runnable {

  @ObjectClassDefinition(name = "	ACS AEM Commons - Scheduled Automatic Package Replicator")
  public @interface ScheduledAutomaticPackageReplicatorConfig {

    @AttributeDefinition(name = "Package Path")
    String packagePath();

    @AttributeDefinition(name = "Scheduler Expression")
    String scheduler_expression();

    String webconsole_configurationFactory_nameHint() default "Scheduled Automatic Package Replicator <b>{scheduler.expression}->{packagePath}</b>";

  }

  private static final Logger log = LoggerFactory.getLogger(ScheduledAutomaticPackageReplicator.class);

  private ScheduledAutomaticPackageReplicatorConfig config;

  private AutomaticPackageReplicator automaticPackageReplicator;

  @Activate
  public void activate(ScheduledAutomaticPackageReplicatorConfig config) {
    this.config = config;
  }

  @Override
  public void run() {
    try {
      automaticPackageReplicator.replicatePackage(config.packagePath());
    } catch (RepositoryException | PackageException | IOException | ReplicationException | LoginException e) {
      log.error("Failed to replicate package {}", config.packagePath(), e);
    }
  }

  @Reference
  public void setAutomaticPackageReplicator(AutomaticPackageReplicator automaticPackageReplicator) {
    this.automaticPackageReplicator = automaticPackageReplicator;
  }

}
