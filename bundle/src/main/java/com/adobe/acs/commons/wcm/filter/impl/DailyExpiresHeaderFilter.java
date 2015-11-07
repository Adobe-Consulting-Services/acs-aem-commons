package com.adobe.acs.commons.wcm.filter.impl;

import java.util.Calendar;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;


//@formatter:off
@Component(
    label = "ACS AEM Commons - Dispatcher Expires Header - Daily",
    description = "Adds an Expires header to content to enable Dispatcher TTL support.",
    immediate = false,
    metatype = true,
    configurationFactory = true,
    policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
  @Property(label = "Filter Patterns",
      description = "Patterns on which to apply this Expires rule.",
      cardinality = Integer.MAX_VALUE,
      name = AbstractDispatcherCacheHeaderFilter.PROP_FILTER_PATTERN,
      propertyPrivate = false,
      value = { }),
  @Property(label = "Expires Time",
      description = "Time each day at which resources will expire. Must match SimpleDateFormat of 'HH:mm'.",
      name = AbstractExpiresHeaderFilter.PROP_EXPIRES_TIME,
      propertyPrivate = false),
  @Property(
        name = "webconsole.configurationFactory.nameHint",
        value = "Expires Daily at: {expires.time} for Patterns: [{filter.pattern}]")
})
//@formatter:on
public class DailyExpiresHeaderFilter extends AbstractExpiresHeaderFilter {

    @Override
    protected void adjustExpires(Calendar next) {
        if (next.before(Calendar.getInstance())) {
            next.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
}
