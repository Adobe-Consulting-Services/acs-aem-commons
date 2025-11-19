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
package com.adobe.acs.commons.util.datadefinitions.impl;

import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import org.osgi.service.component.annotations.Component;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;

import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang3.StringUtils;

@Component(service = ResourceDefinitionBuilder.class)
public class JcrValidNameDefinitionBuilderImpl implements ResourceDefinitionBuilder {
    public static final String NAME = "TITLE_TO_NODE_NAME";

    @Override
    public final ResourceDefinition convert(final String data) {
        final String name =
                StringUtils.lowerCase(JcrUtil.createValidName(StringUtils.strip(data)));

        final BasicResourceDefinition dataDefinition = new BasicResourceDefinition(name);

        dataDefinition.setTitle(data);

        return dataDefinition;
    }

    @Override
    public boolean accepts(String data) {
        // Default accepts all formats
        return true;
    }
}
