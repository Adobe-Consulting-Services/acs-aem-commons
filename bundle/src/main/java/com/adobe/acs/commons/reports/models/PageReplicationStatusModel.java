/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.reports.models;

import java.util.Calendar;

import javax.jcr.Session;

import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;

/**
 * Model for getting the replication status for content.
 */
@Model(adaptables = Resource.class)
public class PageReplicationStatusModel implements ReportCellCSVExporter {

  private static final Logger log = LoggerFactory.getLogger(PageReplicationStatusModel.class);

  public enum Status {
    ACTIVATED, DEACTIVATED, IN_PROGRESS, MODIFIED, NOT_ACTIVATED
  }

  @OSGiService
  private Replicator replicator;

  @OSGiService
  private ReplicationStatusManager replicationStatusManager;

  @Self
  private Resource resource;

  private Calendar getLastModified(ResourceResolver resourceResolver, String pageContentPath) {
    Resource pageContent = resourceResolver.getResource(pageContentPath);
    Calendar lastModified = null;
    if (pageContent != null) {
      lastModified = pageContent.getValueMap().get(NameConstants.PN_PAGE_LAST_MOD, Calendar.class);
    }
    return lastModified;
  }

  private String getReplicationStatus(Resource targetResource) {
    final Resource replicationResource = replicationStatusManager.getReplicationStatusResource(targetResource.getPath(), targetResource.getResourceResolver());

    log.debug("Getting replication status for {}", replicationResource.getPath());

    ReplicationStatus status = replicationResource.adaptTo(ReplicationStatus.class);

    Status rStatus = Status.NOT_ACTIVATED;
    if (status != null) {
      if (status.isDeactivated()) {
        rStatus = Status.DEACTIVATED;
      } else if (status.isPending()) {
        rStatus = Status.IN_PROGRESS;
      } else if (status.isActivated()) {
        Calendar lastModified = getLastModified(targetResource.getResourceResolver(), replicationResource.getPath());
        if (lastModified != null && status.getLastPublished() != null
                && lastModified.after(status.getLastPublished())) {
          rStatus = Status.MODIFIED;
        } else {
          rStatus = Status.ACTIVATED;
        }
      }
    }

    log.debug("Retrieved replication status {}", rStatus);
    return rStatus.toString();

  }

  public String getReplicationStatus() {
    return getReplicationStatus(resource);
  }

  @Override
  public String getValue(Object result) {
    if (result instanceof Resource) {
      return getReplicationStatus((Resource) result);
    } else {
      return "UNKNOWN";
    }
  }
}
