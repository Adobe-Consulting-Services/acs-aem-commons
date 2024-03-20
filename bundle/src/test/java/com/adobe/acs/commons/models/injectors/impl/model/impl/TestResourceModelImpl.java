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
package com.adobe.acs.commons.models.injectors.impl.model.impl;

import com.adobe.acs.commons.models.injectors.annotation.AemObject;
import com.adobe.acs.commons.models.injectors.impl.model.TestResourceModel;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.xss.XSSAPI;

import javax.jcr.Session;

@Model(adapters = TestResourceModel.class,
        adaptables = {Resource.class, SlingHttpServletRequest.class})
public class TestResourceModelImpl implements TestResourceModel {

    @AemObject
    private Resource resource;
    @AemObject
    private ResourceResolver resourceResolver;
    @AemObject
    @Optional
    private ComponentContext componentContext;
    @AemObject
    private PageManager pageManager;
    @AemObject
    @Optional
    private Page currentPage;
    @AemObject
    @Optional
    private Page resourcePage;
    @AemObject
    @Optional
    private Designer designer;
    @AemObject
    @Optional
    private Design currentDesign;
    @AemObject
    @Optional
    private Design resourceDesign;
    @AemObject
    @Optional
    private Style currentStyle;
    @AemObject
    @Optional
    private Session session;
    @AemObject
    @Optional
    private XSSAPI xssApi;
    @AemObject
    @Optional
    private String namedSomethingElse;

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    @Override
    public ComponentContext getComponentContext() {
        return componentContext;
    }

    @Override
    public PageManager getPageManager() {
        return pageManager;
    }

    @Override
    public Page getCurrentPage() {
        return currentPage;
    }

    @Override
    public Page getResourcePage() {
        return resourcePage;
    }

    @Override
    public Designer getDesigner() {
        return designer;
    }

    @Override
    public Design getCurrentDesign() {
        return currentDesign;
    }

    @Override
    public Design getResourceDesign() {
        return resourceDesign;
    }

    @Override
    public Style getCurrentStyle() {
        return currentStyle;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public XSSAPI getXssApi() {
        return xssApi;
    }
}
