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
