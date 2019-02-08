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
package com.adobe.acs.commons.util.datadefinitions.impl;


import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Properties({
        @Property(
                name = ResourceDefinitionBuilder.PROP_NAME,
                value = TitleAndNodeNameDefinitionBuilderImpl.NAME,
                propertyPrivate = true
        )
})
@Service
public class TitleAndNodeNameDefinitionBuilderImpl implements ResourceDefinitionBuilder {
    public static final String NAME = "TITLE_AND_NODE_NAME";

    private static final Pattern ACCEPT_PATTERN = Pattern.compile(".+\\{\\{(.+)}}$");

    private static final Pattern PATTERN = Pattern.compile("\\{\\{(.+)}}$");

    @Override
    public final ResourceDefinition convert(String data) {
        data = StringUtils.stripToEmpty(data);

        String name;

        final Matcher matcher = PATTERN.matcher(data);

        if (matcher.find() && matcher.groupCount() == 1) {
            name = matcher.group(1);
            name = StringUtils.stripToEmpty(name);
        } else {
            return null;
        }

        String title = PATTERN.matcher(data).replaceAll("");
        title = StringUtils.stripToEmpty(title);

        final BasicResourceDefinition dataDefinition = new BasicResourceDefinition(name);
        dataDefinition.setTitle(title);

        return dataDefinition;
    }

    @Override
    public boolean accepts(String data) {
        final Matcher matcher = ACCEPT_PATTERN.matcher(data);
        return matcher.matches();
    }
}
