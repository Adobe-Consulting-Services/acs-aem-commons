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

package com.adobe.acs.commons.workflow.bulk.execution.model;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Model(adaptables = Resource.class)
public class PayloadGroup {
    public static final String PN_NEXT = "next";

    private final Resource resource;

    @Inject
    @Optional
    private String next;

    public PayloadGroup(Resource resource) {
        this.resource = resource;
    }

    /**
     * @return the JCR path of the PayloadGroup resource.
     */
    public String getPath() {
        return this.resource.getPath();
    }

    public String getDereferencedPath() {
        return dereference(this.resource.getPath());
    }

    /**
     * @return the Workspace this payload group belongs to.
     */
    public Workspace getWorkspace() {
        return resource.getParent().adaptTo(Workspace.class);
    }

    /**
     * @return the next payload group to process. null if no more payload groups to process left.
     */
    public PayloadGroup getNextPayloadGroup() {
        if (next == null) {
            return null;
        }

        Resource r = resource.getResourceResolver().getResource(getNext());

        if (r == null) {
            return null;
        }

        return r.adaptTo(PayloadGroup.class);
    }

    public String getNext() {
        return reference(next);
    }

    /**
     * @return list of all the Payloads in the PayloadGroup.
     */
    public List<Payload> getPayloads() {
        List<Payload> payloads = new ArrayList<Payload>();

        for (Resource r : resource.getChildren()) {
            Payload payload = r.adaptTo(Payload.class);
            if (payload != null) {
                payloads.add(payload);
            }
        }

        return payloads;
    }

    /**
     * @return the next payload eligible for processing. null if none exist.
     */
    public Payload getNextPayload() {
        for (Resource r : resource.getChildren()) {
            Payload payload = r.adaptTo(Payload.class);
            if (payload != null && !payload.isOnboarded()) {
                return payload;
            }
        }

        return null;
    }

    /**
     * @return true if this is the last PayloadGroup
     */
    public boolean isLast() {
        return getNextPayloadGroup() == null;
    }

    public static String dereference(String str) {
        if (!StringUtils.startsWith(str, "-")) {
            str = "-" + str;
        }

        return str;
    }

    public static String reference(String str) {
        return StringUtils.removeStart(str, "-");
    }
}
