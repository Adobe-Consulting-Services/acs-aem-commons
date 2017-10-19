package com.adobe.acs.commons.util.datadefinitions.impl;

import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;

import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

@Component
@Properties({
        @Property(
                name = ResourceDefinitionBuilder.PROP_NAME,
                value = JcrValidNameDefinitionBuilderImpl.NAME,
                propertyPrivate = true
        )
})
@Service
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
