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

package com.adobe.acs.commons.util.visitors;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.NameConstants;
import org.apache.commons.lang.ArrayUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ContentVisitor<T extends ResourceRunnable> extends AbstractResourceVisitor {
    private static final Logger log = LoggerFactory.getLogger(ContentVisitor.class);

    private static final String[] DEFAULT_CONTAINER_TYPES = {JcrResourceConstants.NT_SLING_FOLDER, JcrResourceConstants.NT_SLING_ORDERED_FOLDER, JcrConstants.NT_FOLDER};
    private static final String[] DEFAULT_CONTENT_TYPES = {NameConstants.NT_PAGE, DamConstants.NT_DAM_ASSET};

    private final T runnable;
    private final String[] contentTypes;
    private final String[] containerTypes;

    public ContentVisitor(T runnable) {
        this.runnable = runnable;
        this.containerTypes = DEFAULT_CONTAINER_TYPES;
        this.contentTypes = DEFAULT_CONTENT_TYPES;
    }

    public ContentVisitor(T runnable, String[] containerTypes, String[] contentTypes) {
        this.runnable = runnable;
        this.containerTypes = Arrays.copyOf(containerTypes, containerTypes.length);
        this.contentTypes = Arrays.copyOf(contentTypes, contentTypes.length);
    }

    @Override
    public void accept(Resource resource) {
        if (resource != null) {
            final String primaryType = resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class);

            if (ArrayUtils.contains(containerTypes, primaryType) || ArrayUtils.contains(contentTypes, primaryType)) {
                super.accept(resource);
            }
        }
    }

    @Override
    protected void visit(Resource resource) {
        try {
            final String primaryType = resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class);

            if (ArrayUtils.contains(contentTypes, primaryType)) {
                runnable.run(resource);
            }
        } catch (Exception e) {
            log.error("An error occurred while visiting resource [ {} ]", resource.getPath(), e);
        }
    }
}