package com.adobe.acs.commons.replication.packages.automatic;

import java.io.IOException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.hc.api.Result;

import com.adobe.granite.jmx.annotation.Description;
import com.day.cq.replication.ReplicationException;

/**
 * Service interface for Automatic Package Replicator instances
 */
@Description("MBean for managing an Automatic Package Replicator instance")
public interface AutomaticPackageReplicator {

  /**
   * Gets the status of the last execution or null if not executed
   * 
   * @return the last status of the instance
   */
  @Description("Gets the status of the last execution or null if not executed")
  public Result.Status getLastStatus();

  /**
   * Execute the process to build and replicate the package configured for the
   * instance of the Automatic Package Replicator.
   * 
   * @throws RepositoryException  an exception occurs saving the package
   * @throws PackageException     an exception occurs assembling the package
   * @throws IOException          an exception occurs assembling the package
   * @throws ReplicationException an exception occurs replicating the package
   */
  @Description("Replicates the package configured for this Automatic Package Replicator instance")
  public void replicatePackage() throws RepositoryException, PackageException, IOException, ReplicationException;
}
