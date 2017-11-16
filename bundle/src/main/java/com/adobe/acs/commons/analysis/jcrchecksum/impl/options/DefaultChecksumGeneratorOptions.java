/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.analysis.jcrchecksum.impl.options;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.SlingHttpServletRequest;

@ProviderType
public class DefaultChecksumGeneratorOptions extends AbstractChecksumGeneratorOptions {

    public DefaultChecksumGeneratorOptions() {

        this.addIncludedNodeTypes(
                "cq:PageContent",
                "dam:AssetContent",
                "cq:Tag"
        );

        this.addExcludedNodeTypes(
                "rep:ACL",
                "cq:meta"
        );

        this.addExcludedProperties(
                "jcr:mixinTypes",
                "jcr:created",
                "jcr:uuid",
                "jcr:lastModified",
                "jcr:lastModifiedBy",
                "cq:lastModified",
                "cq:lastModifiedBy",
                "cq:lastReplicated",
                "cq:lastReplicatedBy",
                "cq:lastReplicationAction",
                "cq:ReplicationStatus",
                "jcr:versionHistory",
                "jcr:predecessors",
                "jcr:baseVersion"
        );

        this.addSortedProperties(
                "cq:tags"
        );
    }

    public DefaultChecksumGeneratorOptions(SlingHttpServletRequest request) {
        this();
    }

}