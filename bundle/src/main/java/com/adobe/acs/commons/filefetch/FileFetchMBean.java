/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.filefetch;

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

  @Description("Start the file fetcher at the specified index")
  void fetch(int index) throws IOException, ReplicationException;

  @Description("Get all of the registered file fetchers")
  TabularData getFetchers() throws OpenDataException;

}
