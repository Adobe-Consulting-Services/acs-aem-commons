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
package com.adobe.acs.commons.filefetch.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.adobe.acs.commons.filefetch.FileFetcher;
import com.adobe.acs.commons.filefetch.FileFetchMBean;
import com.adobe.granite.jmx.annotation.AnnotatedStandardMBean;
import com.day.cq.replication.ReplicationException;

/**
 * Implementation of the FileFetch MBean
 */
@Component(service = { DynamicMBean.class, FileFetchMBean.class }, property = {
    "jmx.objectname=com.adobe.acs.commons.filefetch:type=FileFetch" })
public class FileFetchMBeanImpl extends AnnotatedStandardMBean implements FileFetchMBean {

  private static final String PN_INDEX = "Index";
  private static final String PN_REMOTE_URL = "Remote URL";
  private static final String PN_DAM_PATH = "DAM Path";
  private static final String PN_LAST_EXCEPTION = "Last Exception";
  private static final String PN_LAST_JOB_SUCCEEDED = "Last Job Succeeded";
  private static final String PN_LAST_MODIFIED = "Last Modified";

  @Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.AT_LEAST_ONE)
  private List<FileFetcher> fetchers;

  public FileFetchMBeanImpl() throws NotCompliantMBeanException {
    super(FileFetchMBean.class);
  }

  @Override
  public boolean allSucceeded() {
    return fetchers.stream().allMatch(FileFetcher::isLastJobSucceeded);
  }

  @Override
  public void fetch(int index) throws IOException, ReplicationException {
    fetchers.get(index).updateFile();
  }

  @Override
  public TabularData getFetchers() throws OpenDataException {
    CompositeType compositeType = new CompositeType("File Fetch", "File Fetch Instance",
        new String[] { PN_INDEX, PN_LAST_JOB_SUCCEEDED, PN_LAST_EXCEPTION, PN_REMOTE_URL, PN_DAM_PATH,
            PN_LAST_MODIFIED },
        new String[] { PN_INDEX, PN_LAST_JOB_SUCCEEDED, PN_LAST_EXCEPTION, PN_REMOTE_URL, PN_DAM_PATH,
            PN_LAST_MODIFIED },
        new OpenType[] { SimpleType.INTEGER, SimpleType.BOOLEAN, SimpleType.STRING, SimpleType.STRING,
            SimpleType.STRING, SimpleType.STRING });
    TabularDataSupport tabularData = new TabularDataSupport(
        new TabularType("File Fetch", "File Fetch Instance", compositeType, new String[] { PN_INDEX }));
    for (int i = 0; i < fetchers.size(); i++) {
      Map<String, Object> data = new HashMap<>();

      FileFetcher fetch = fetchers.get(i);
      data.put(PN_INDEX, i);
      data.put(PN_LAST_JOB_SUCCEEDED, fetch.isLastJobSucceeded());
      data.put(PN_LAST_EXCEPTION, fetch.getLastException());
      data.put(PN_LAST_MODIFIED, fetch.getLastModified());
      data.put(PN_REMOTE_URL, fetch.getConfig().remoteUrl());
      data.put(PN_DAM_PATH, fetch.getConfig().damPath());
      CompositeData cd = new CompositeDataSupport(compositeType, data);
      tabularData.put(cd);
    }
    return tabularData;
  }

  public void setFetchers(List<FileFetcher> fetchers) {
    this.fetchers = fetchers;
  }

}
