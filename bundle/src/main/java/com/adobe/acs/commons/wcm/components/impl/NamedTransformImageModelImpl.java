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

package com.adobe.acs.commons.wcm.components.impl;


import com.adobe.acs.commons.wcm.components.NamedTransformImageModel;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.DropTarget;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.foundation.Image;
import com.day.cq.wcm.foundation.Placeholder;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Required;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;


@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {NamedTransformImageModel.class},
        resourceType = {"acs-commons/components/content/named-transform-image"},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class NamedTransformImageModelImpl implements NamedTransformImageModel {

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue
    private String transform;

    @ValueMapValue
    private String linkURL;

    @ScriptVariable
    @Required
    private Page currentPage;

    @ScriptVariable
    @Required
    private Style currentStyle;

    private Image image;

    @PostConstruct
    protected void init() {
        final Resource resource = request.getResource();
        image = new Image(resource);

        if (StringUtils.isNotBlank(transform)) {
            long imageTimestamp;
            try {
                imageTimestamp = image.getLastModified().getTimeInMillis();
            } catch (RepositoryException e) {
                imageTimestamp = -1L;
            }
            final long pageTimestamp = currentPage.getLastModified().getTimeInMillis();
            final long timestamp = imageTimestamp > pageTimestamp ? imageTimestamp : pageTimestamp;

            image.setSrc(resource.getPath() + ".transform/" + transform + "/" + timestamp + "/image." + image.getExtension());
        }

        image.setIsInUITouchMode(Placeholder.isAuthoringUIModeTouch(request));
        image.addCssClass(DropTarget.CSS_CLASS_PREFIX + "image");
        image.loadStyleData(currentStyle);
        image.setSelector(".img"); // use image script
    }

    @Override
    public String getLinkURL() {
        final PageManager pageManager = request.adaptTo(PageManager.class);
        
        if (pageManager.getPage(linkURL) != null && !StringUtils.endsWith(linkURL, ".html")) {
            return linkURL + ".html";
        }

        return linkURL;
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public boolean isReady() {
        return image.hasContent() && StringUtils.isNotBlank(image.getSrc());
    }
}
