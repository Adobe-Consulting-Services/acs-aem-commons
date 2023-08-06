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
import com.day.cq.dam.api.Asset;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;

import javax.json.stream.JsonGenerator;

@Component
public class AssetChecksumStrategy implements UpdateStrategy {
    private static final String DAM_SHA1 = "dam:sha1";

    @Override
    public boolean isModified(CatalogItem catalogItem, Resource targetResource) {
        String remoteChecksum = getChecksum(catalogItem);
        String localChecksum = getChecksum(targetResource);

        return remoteChecksum != null && !remoteChecksum.equals(localChecksum);
    }

    @Override
    public String getMessage(CatalogItem catalogItem, Resource targetResource) {
        String remoteChecksum = getChecksum(catalogItem);
        String localChecksum = getChecksum(targetResource);

        boolean modified = remoteChecksum != null && !remoteChecksum.equals(localChecksum);
        StringBuilder msg = new StringBuilder();
        if (targetResource == null) {
            msg.append("resource does not exist");
        } else {
            msg.append(modified ? "resource modified ... " : "replacing ... ");
            if (localChecksum != null) {
                msg.append('\n');
                msg.append("\tlocal checksum: " + localChecksum);
            }
            if (remoteChecksum != null) {
                msg.append('\n');
                msg.append("\tremote checksum: " + remoteChecksum);
            }
        }
        return msg.toString();
    }

    private String getChecksum(CatalogItem remoteItem) {
        return remoteItem.getString(DAM_SHA1);
    }

    private String getChecksum(Resource targetResource) {
        if(targetResource == null){
            return null;
        }
        Asset asset = targetResource.adaptTo(Asset.class);
        return asset == null ? null : (String)asset.getMetadata(DAM_SHA1);
    }

    public void writeMetadata(JsonGenerator out, Resource res) {
        Asset asset = res.adaptTo(Asset.class);
        if(asset != null){
            out.write(DAM_SHA1, getChecksum(res));
        }
    }

    @Override
    public boolean accepts(Resource resource) {
        return resource.isResourceType("dam:Asset");
    }
}
