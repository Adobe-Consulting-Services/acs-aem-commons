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
package com.adobe.acs.commons.replication.packages.automatic;

import java.io.IOException;

import javax.jcr.RepositoryException;

import com.adobe.granite.jmx.annotation.Description;
import com.day.cq.replication.ReplicationException;

import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.hc.api.Result;

/**
 * A service for automatically building and replicating packages
 */
@Description("MBean for managing the Automatic Package Replicator")
public interface AutomaticPackageReplicator {

  /**
   * Gets the recent replications as an array
   * 
   * @return the recent replications
   */
  @Description("Gets the recent replications as an array")
  public String[] getRecentReplications();

  /**
   * Gets the status from the recent replications
   * 
   * @return the status
   */
  @Description("Gets the status from the recent replications")
  public Result.Status getStatus();

  /**
   * Replicates the package at the specified path
   * 
   * @param packagePath the path to the package
   * 
   * @throws RepositoryException  an exception occurs saving the package
   * @throws PackageException     an exception occurs assembling the package
   * @throws IOException          an exception occurs assembling the package
   * @throws ReplicationException an exception occurs replicating the package
   * @throws LoginException       the service user is not configured
   */
  @Description("Replicates the package at the specified path")
  public void replicatePackage(String packagePath)
      throws RepositoryException, PackageException, IOException, ReplicationException, LoginException;


  /**
   * Resets the recent replications
   */
  @Description("Resets the recent replications")
  public void resetRecentReplications();
}
