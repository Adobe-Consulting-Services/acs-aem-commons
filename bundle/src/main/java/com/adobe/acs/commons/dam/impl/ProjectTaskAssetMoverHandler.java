/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

package com.adobe.acs.commons.dam.impl;

import com.day.cq.search.QueryBuilder;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.event.EventConstants;

import java.util.Map;


@Component(
        label = "ACS AEM Commons - Project Task Move Handler",
        description = "Create an OSGi configuration to enable this feature.",
        metatype = true,
        immediate = true,
		configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(
                label = "Event Topics",
                value = {"com/adobe/granite/taskmanagement/event"},
                description = "[Required] Event Topics this event handler will to respond to. Defaults to: com/adobe/granite/taskmanagement/event",
                name = EventConstants.EVENT_TOPIC,
                propertyPrivate = true
        ),

        /* Event filters support LDAP filter syntax and have access to event.getProperty(..) values */
        /* LDAP Query syntax: https://goo.gl/MCX2or */
        @Property(
                label = "Event Filters",
                // Only listen on events associated with nodes that end with /jcr:content
                value = "(&(TaskTypeName=dam:review)(EventType=TASK_COMPLETED))",
                description = "Event Filters used to further restrict this event handler; Uses LDAP expression against event properties. Defaults to: (&(TaskTypeName=dam:review)(EventType=TASK_COMPLETED))",
                name = EventConstants.EVENT_FILTER,
                propertyPrivate = false
        )
})
@Service
public class ProjectTaskAssetMoverHandler extends ReviewTaskAssetMoverHandler {
    
    private static final String DEFAULT_APPROVED_TERM = "approved";
	private String approvedTerm = DEFAULT_APPROVED_TERM;
    @Property(label = "Approved Term",
            description = "Business term for approved asset.",
            value = DEFAULT_APPROVED_TERM)
    private static final String PROP_APPROVED_TERM = "approved.term";
	private static final String DEFAULT_REJECTED_TERM = "rejected";
	private String rejectedTerm = DEFAULT_REJECTED_TERM;
    @Property(label = "Rejected Term",
            description = "Business term for rejected asset.",
            value = DEFAULT_REJECTED_TERM)
    private static final String PROP_REJECTED_TERM = "rejected.term";
    private static final String CONFLICT_RESOLUTION_SKIP = "skip";
    private static final String CONFLICT_RESOLUTION_REPLACE = "replace";
    private static final String CONFLICT_RESOLUTION_NEW_ASSET = "new-asset";
    private static final String CONFLICT_RESOLUTION_NEW_VERSION = "new-version";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private QueryBuilder queryBuilder;

    private static final String DEFAULT_DEFAULT_CONFLICT_RESOLUTION = CONFLICT_RESOLUTION_NEW_VERSION;
    private String defaultConflictResolution = DEFAULT_DEFAULT_CONFLICT_RESOLUTION;
    @Property(label = "Default Conflict Resolution",
            description = "Select default behavior if conflict resolution is not provided at the review task level.",
            options = {
                    @PropertyOption(name = CONFLICT_RESOLUTION_NEW_VERSION, value = "Add as version (new-version)"),
                    @PropertyOption(name = CONFLICT_RESOLUTION_NEW_ASSET, value = "Add as new asset (new-asset)"),
                    @PropertyOption(name = CONFLICT_RESOLUTION_REPLACE, value = "Replace (replace)"),
                    @PropertyOption(name = CONFLICT_RESOLUTION_SKIP, value = "Skip (skip)")
            },
            value = DEFAULT_DEFAULT_CONFLICT_RESOLUTION)
    public static final String PROP_DEFAULT_CONFLICT_RESOLUTION = "conflict-resolution.default";

    private static final String DEFAULT_LAST_MODIFIED_BY = "Project Task";
    private String lastModifiedBy = DEFAULT_LAST_MODIFIED_BY;
    @Property(label = "Last Modified By",
            description = "For Conflict Resolution: Version, the review task event does not track the user that completed the event. Use this property to specify the static name of of the [dam:Asset]/jcr:content@jcr:lastModifiedBy. Default: Review Task",
            value = DEFAULT_LAST_MODIFIED_BY)
    public static final String PROP_LAST_MODIFIED_BY = "conflict-resolution.version.last-modified-by";
    

    @Activate
    protected void activate(Map<String, Object> config) {
        lastModifiedBy = PropertiesUtil.toString(config.get(PROP_LAST_MODIFIED_BY), DEFAULT_LAST_MODIFIED_BY);
        defaultConflictResolution = PropertiesUtil.toString(config.get(PROP_DEFAULT_CONFLICT_RESOLUTION), DEFAULT_DEFAULT_CONFLICT_RESOLUTION);
		approvedTerm = PropertiesUtil.toString(config.get(PROP_APPROVED_TERM), DEFAULT_APPROVED_TERM);
		rejectedTerm = PropertiesUtil.toString(config.get(PROP_REJECTED_TERM), DEFAULT_REJECTED_TERM);
    }


	public String getApprovedTerm() {
		return approvedTerm;
	}


	public String getRejectedTerm() {
		return rejectedTerm;
	}


	public ResourceResolverFactory getResourceResolverFactory() {
		return resourceResolverFactory;
	}


	public Scheduler getScheduler() {
		return scheduler;
	}


	public QueryBuilder getQueryBuilder() {
		return queryBuilder;
	}


	public String getDefaultConflictResolution() {
		return defaultConflictResolution;
	}


	public String getLastModifiedBy() {
		return lastModifiedBy;
	}
	
	
    
}