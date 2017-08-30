package com.adobe.acs.commons.util.datadefinitions.impl;

import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Properties({
        @Property(
                name = ResourceDefinitionBuilder.PROP_NAME,
                value = LocalizedTitleDefinitionBuilderImpl.NAME,
                propertyPrivate = true
        )
})
@Service
public class LocalizedTitleDefinitionBuilderImpl implements ResourceDefinitionBuilder {
    public static final String NAME = "LOCALIZED_TITLE";

    private static final Pattern ACCEPT_PATTERN = Pattern.compile("^\\s*([a-zA-Z_-]+\\[[^]]+\\]\\s+)+\\{\\{(.+)}}\\s*$");

    private static final Pattern NODE_NAME_PATTERN = Pattern.compile("\\{\\{(.+)}}\\s?$");

    private static final Pattern LOCALIZED_TITLES_PATTERN = Pattern.compile("([a-zA-Z_-]+)\\[([^]]+)\\]");

    private static final String DEFAULT_LOCALE_KEY = "default";

    @Override
    public final ResourceDefinition convert(String data) {
        data = StringUtils.stripToEmpty(data);

        Map<String, String> localizedTitles = new TreeMap<String, String>();

        String name;
        String title = "";

        final Matcher matcher = NODE_NAME_PATTERN.matcher(data);

        if (matcher.find() && matcher.groupCount() == 1) {
            name = StringUtils.stripToEmpty(matcher.group(1));
        } else {
            return null;
        }

        final String rawLocalizedTitles = StringUtils.stripToEmpty(NODE_NAME_PATTERN.matcher(data).replaceAll(""));
        final Matcher localeMatch = LOCALIZED_TITLES_PATTERN.matcher(rawLocalizedTitles);

        String firstLocale = null;
        while (localeMatch.find()) {
            final String locale = StringUtils.stripToEmpty(localeMatch.group(1));
            final String localeTitle = StringUtils.stripToEmpty(localeMatch.group(2));

            if (firstLocale == null) {
                firstLocale = locale;
            }

            if (DEFAULT_LOCALE_KEY.equals(locale)) {
                title = localeTitle;
            } else {
                localizedTitles.put(locale, localeTitle);
            }
        }

        // It no default title was found, fall back to the first locale listed

        if (StringUtils.isEmpty(title) && firstLocale != null) {
            title = localizedTitles.get(firstLocale);
        }

        if (StringUtils.isEmpty(title)) {
            return null;
        }

        final BasicResourceDefinition tagData = new BasicResourceDefinition(name);
        tagData.setTitle(title);
        tagData.setLocalizedTitles(localizedTitles);
        return tagData;
    }

    @Override
    public boolean accepts(String data) {
        final Matcher matcher = ACCEPT_PATTERN.matcher(data);
        return matcher.matches();
    }
}
