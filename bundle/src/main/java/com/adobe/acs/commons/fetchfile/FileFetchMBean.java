package com.adobe.acs.commons.fetchfile;

import java.io.IOException;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import com.adobe.granite.jmx.annotation.Description;
import com.day.cq.replication.ReplicationException;

import aQute.bnd.annotation.ProviderType;

@ProviderType
@Description("MBean for managing the FileFetch jobs")
public interface FileFetchMBean {

  @Description("Whether or not all jobs succeeded")
  boolean allSucceeded();

  @Description("Start the job at the specified index")
  void fetch(int index) throws IOException, ReplicationException;
  

  @Description("Get all of the registered jobs")
  TabularData getJobs() throws OpenDataException;

}
