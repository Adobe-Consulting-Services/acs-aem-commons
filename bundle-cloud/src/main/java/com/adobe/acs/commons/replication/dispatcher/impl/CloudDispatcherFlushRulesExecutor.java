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
package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlushRules;
import com.adobe.acs.commons.util.RequireAem;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationOptions;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.ImportPostProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component(immediate = true)
@ServiceRanking(5)
public class CloudDispatcherFlushRulesExecutor implements ImportPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(CloudDispatcherFlushRulesExecutor.class);

    @Reference(target = "(distribution=cloud-ready)")
    private RequireAem requireAem;

    @Reference
    private volatile List<DispatcherFlushRules> dispatcherFlushRules;


    @Override
    public void process(Map<String, Object> props) {
        ReplicationActionType actionType = getReplicationActionType(props);
        List<String> distributionPaths = (List<String>) props.get("distribution.paths");
        if (actionType == null || actionType.equals(ReplicationActionType.TEST) || distributionPaths == null || distributionPaths.isEmpty()) {
            log.debug("Skipping processing because the distribution paths are empty");
            return;
        }
        try {
            ReplicationAction action = new ReplicationAction(actionType, distributionPaths.toArray(new String[0]), 0L, "", null);
            ReplicationOptions opts = new ReplicationOptions();
            for (DispatcherFlushRules dispatcherFlushRule : dispatcherFlushRules) {
                dispatcherFlushRule.preprocess(action, opts);
            }
            if (log.isInfoEnabled()) {
                log.info("Invalidated resources [{}]", abbreviate(distributionPaths));
            }
        } catch (Exception ex) {
            log.warn("Could not invalidate request ", ex);
        }
    }


    private ReplicationActionType getReplicationActionType(Map<String, Object> props) {
        Object requestTypeObject = props.get("distribution.type");
        DistributionRequestType requestType = (requestTypeObject instanceof DistributionRequestType) ? (DistributionRequestType) requestTypeObject : DistributionRequestType.fromName((String) requestTypeObject);
        if (DistributionRequestType.ADD.equals(requestType))
            return ReplicationActionType.ACTIVATE;
        if (DistributionRequestType.DELETE.equals(requestType))
            return ReplicationActionType.DEACTIVATE;
        if (DistributionRequestType.TEST.equals(requestType))
            return ReplicationActionType.TEST;
        log.debug("Distribution request type {} not supported", requestType);
        return null;
    }

    protected static String abbreviate(List<String> list) {
        if (list == null)
            return null;
        Iterator<String> iter = list.iterator();
        StringBuilder abbr = new StringBuilder();
        abbr.append("[");
        if (iter.hasNext()) {
            abbr.append(iter.next());
        }
        if (iter.hasNext()) {
            abbr.append(", ... ");
            abbr.append(list.size() - 1);
            abbr.append(" more");
        }
        abbr.append("]");
        return abbr.toString();
    }

}
