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
package com.adobe.acs.commons.redirects.models;

import com.adobe.acs.commons.redirects.filter.RedirectFilterMBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.models.RedirectRule.UNTIL_DATE_PROPERTY_NAME;

/**
 * In v5.0.4  redirects were stored under /conf/acs-commons/redirects.
 * In 5.0.5+ the default path to store redirect was changed to /conf/global/settings/redirects to be
 * compatible with Sling context aware configurations.
 *
 * This class is invoked from /apps/acs-commons/content/redirect-manager/redirects.html to
 * move redirect rules from /conf/acs-commons/redirects to /conf/global/settings/redirects
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class UpgradeLegacyRedirects {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String REDIRECTS_HOME_5_0_4 = "/conf/acs-commons/redirects";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    private boolean moved;


    @SlingObject
    private SlingHttpServletRequest request;
    @OSGiService
    private RedirectFilterMBean redirectFilter;

    @PostConstruct
    protected void init() {
        ResourceResolver resolver = request.getResourceResolver();
        Resource legacyHome = resolver.getResource(REDIRECTS_HOME_5_0_4);
        if (legacyHome == null) {
            return;
        }
        moved = legacyHome.getValueMap().get("moved", false);
        if (moved) {
            // already converted to /conf/global
            return;
        }

        String globalPath = "/conf/global/" + redirectFilter.getBucket() + "/" + redirectFilter.getConfigName();
        Resource globalHome = resolver.getResource(globalPath);
        if (globalHome == null) {
            return;
        }
        try {
            int numMoved = 0;
            for (Resource ch : legacyHome.getChildren()) {
                if (ch.isResourceType(REDIRECT_RULE_RESOURCE_TYPE)) {
                    String nodeName = ResourceUtil.createUniqueChildName(globalHome, ch.getName());
                    Map<String, Object> props = new HashMap<>(ch.getValueMap());
                    // convert untilDate to Calendar
                    Object untilDate = props.get(UNTIL_DATE_PROPERTY_NAME);
                    if(untilDate instanceof String && !StringUtils.isEmpty((String)untilDate)){
                        String str = (String)untilDate;
                        Calendar c = toCalendar(str);
                        if(c != null) {
                            props.put(UNTIL_DATE_PROPERTY_NAME, c);
                        }
                    }
                    Resource r = resolver.create(globalHome, nodeName, props);
                    resolver.delete(ch);
                    log.debug("moved {} to {}", ch.getPath(), r.getPath());
                    numMoved++;
                }
            }
            if (numMoved > 0) {
                moved = true;
                // this flag will trigger an alert on the page
                legacyHome.adaptTo(ModifiableValueMap.class).put("moved", true);
                resolver.commit();
            }
        } catch (Exception e){
            log.error("failed to move {} to {}", REDIRECTS_HOME_5_0_4, globalPath,  e);
            resolver.revert();
        }
    }

    public boolean isMoved() {
        return moved;
    }

    public static Calendar toCalendar(String dateStr){
        Calendar calendar = null;
        if(!StringUtils.isEmpty(dateStr)) {
            try {
                LocalDate ld = DATE_FORMATTER.parse(dateStr).query(LocalDate::from);
                if (ld != null) {
                    ZonedDateTime zdt = ld.atStartOfDay().plusDays(1).minusSeconds(1).atZone(ZoneId.systemDefault());
                    calendar = GregorianCalendar.from(zdt);
                }
            } catch (DateTimeParseException e) {
                // not fatal. log and continue
                log.error("Invalid UntilDateTime {}", dateStr, e);
            }
        }
        return calendar;
    }

}
