/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync.impl;

import com.adobe.acs.commons.contentsync.CatalogItem;
import com.adobe.acs.commons.contentsync.UpdateStrategy;
import com.adobe.granite.security.user.util.AuthorizableUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;

import javax.json.stream.JsonGenerator;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;

@Component
public class LastModifiedStrategy implements UpdateStrategy {

    @Override
    public boolean isModified(CatalogItem catalogItem, Resource targetResource) {
        LastModifiedInfo remoteLastModified = getLastModified(catalogItem);
        LastModifiedInfo localLastModified = getLastModified(targetResource);

        return remoteLastModified.getLastModified() > localLastModified.getLastModified();
    }

    @Override
    public String getMessage(CatalogItem catalogItem, Resource targetResource) {
        LastModifiedInfo remoteLastModified = getLastModified(catalogItem);
        LastModifiedInfo localLastModified = getLastModified(targetResource);

        boolean modified = remoteLastModified.getLastModified() > localLastModified.getLastModified();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy, h:mm:ss a");
        StringBuilder msg = new StringBuilder();
        if (targetResource == null) {
            msg.append("resource does not exist");
        } else {
            msg.append(modified ? "resource modified ... " : "replacing ... ");
            if (localLastModified.getLastModified() > 0) {
                msg.append('\n');
                msg.append("\tlocal lastModified: " + dateFormat.format(localLastModified.getLastModified()) + " by " + localLastModified.getLastModifiedBy());
            }
            if (remoteLastModified.getLastModified() > 0) {
                msg.append('\n');
                msg.append("\tremote lastModified: " + dateFormat.format(remoteLastModified.getLastModified()) + " by " + remoteLastModified.getLastModifiedBy());
            }
        }
        return msg.toString();
    }

    @Override
    public boolean accepts(Resource resource) {
        if (
                // don't drill down into jcr:content. The entire content will be grabbed by jcr:content.infinity.jsn
                resource.getPath().contains("/" + JCR_CONTENT)
                // ignore rep:policy, rep:cugPolicy, rep:restrictions and such
                || resource.getPath().contains("/rep:")
        ) {
            return false;
        }
        return true;
    }

    public void writeMetadata(JsonGenerator out, Resource res){
        LastModifiedInfo lastModified = getLastModified(res);

        if(lastModified.getLastModified() > 0L) {
            out.write("lastModified", lastModified.getLastModified());
        }
        if(lastModified.getLastModifiedBy() != null) {
            out.write("lastModifiedBy", lastModified.getLastModifiedBy());
        }

    }


    private LastModifiedInfo getLastModified(CatalogItem item) {
        long lastModified = item.getLong("lastModified");
        String lastModifiedBy = item.getString("lastModifiedBy");
        return new LastModifiedInfo(lastModified, lastModifiedBy);
    }

    private LastModifiedInfo getLastModified(Resource targetResource) {
        long lastModified = 0L;
        String lastModifiedBy = null;
        if (targetResource != null) {
            Resource contentResource = targetResource.getChild(JCR_CONTENT);
            if (contentResource == null) {
                contentResource = targetResource;
            }
            ValueMap vm = contentResource.getValueMap();
            Calendar c = (Calendar) vm.get("cq:lastModified", (Class) Calendar.class);
            if (c == null) {
                c = (Calendar) vm.get("jcr:lastModified", (Class) Calendar.class);
            }
            if (c != null) {
                lastModified = c.getTime().getTime();
            }
            String modifiedBy = (String) vm.get("cq:lastModifiedBy", (Class) String.class);
            if (modifiedBy == null) {
                modifiedBy = (String) vm.get("jcr:lastModifiedBy", (Class) String.class);
            }
            lastModifiedBy = AuthorizableUtil.getFormattedName(targetResource.getResourceResolver(), modifiedBy);
        }
        return new LastModifiedInfo(lastModified, lastModifiedBy);
    }

    private static class LastModifiedInfo {
        private final long lastModified ;
        private final String lastModifiedBy;

        public LastModifiedInfo(long lastModified, String lastModifiedBy) {
            this.lastModified = lastModified;
            this.lastModifiedBy = lastModifiedBy;
        }

        public long getLastModified() {
            return lastModified;
        }

        public String getLastModifiedBy() {
            return lastModifiedBy;
        }
    }
}
