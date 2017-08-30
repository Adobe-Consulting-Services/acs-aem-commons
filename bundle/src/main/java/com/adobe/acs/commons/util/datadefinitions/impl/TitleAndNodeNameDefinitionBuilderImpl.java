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
