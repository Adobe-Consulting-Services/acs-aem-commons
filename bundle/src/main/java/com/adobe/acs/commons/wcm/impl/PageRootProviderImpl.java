package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.wcm.PageRootProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(
    label = "ACS AEM Commons - Page Root Provider",
    description = "Service to fetch the site root page (i.e. home page) for a given resource.",
    policy = ConfigurationPolicy.REQUIRE,
    metatype = true
)
@Service
public class PageRootProviderImpl implements PageRootProvider {
    private static final Logger log = LoggerFactory.getLogger(PageRootProviderImpl.class.getName());
    private static final String DEFAULT_PAGE_ROOT_PATH = "/content/";

    @Property(label = "page root path", description = "Page root path (allows regex pattern)",
            value = DEFAULT_PAGE_ROOT_PATH)
    private static final String PAGE_ROOT_PATH = "page.root.path";

    private Pattern pageRootPattern = null;

    @Activate
    protected void activate(Map<String, Object> props) {
        String pageRootPath = PropertiesUtil.toString(props.get(PAGE_ROOT_PATH), DEFAULT_PAGE_ROOT_PATH);
        pageRootPattern = Pattern.compile("^(" + pageRootPath + ")(|/.+)$");
    }

    @Override
    public Page getRootPage(Resource resource) {
        String resourcePath = resource.getPath();
        Matcher matcher = pageRootPattern.matcher(resourcePath);
        if (matcher.matches()) {
            ResourceResolver resolver = resource.getResourceResolver();
            PageManager pageManager = resolver.adaptTo(PageManager.class);
            String pagePath = matcher.group(1);

            Page rootPage = pageManager.getPage(pagePath);
            if (rootPage == null) {
                log.debug("Page not found at {}", pagePath);
            } else if (!rootPage.isValid()) {
                log.debug("Page invalid at {}", pagePath);
            } else {
                log.debug("Page root found at {}", pagePath);
                return rootPage;
            }
        } else {
            log.debug("Resource path does not include the configured page root path");
        }
        return null;
    }
}
