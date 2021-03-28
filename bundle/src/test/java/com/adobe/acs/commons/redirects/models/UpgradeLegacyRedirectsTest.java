package com.adobe.acs.commons.redirects.models;

import com.adobe.acs.commons.redirects.filter.RedirectFilter;
import com.adobe.acs.commons.redirects.filter.RedirectFilterMBean;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpgradeLegacyRedirectsTest {
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Before
    public void setUp(){
        context.addModelsForClasses(UpgradeLegacyRedirects.class);
        RedirectFilterMBean mbean = mock(RedirectFilterMBean.class);
        when(mbean.getBucket()).thenReturn("settings");
        when(mbean.getConfigName()).thenReturn("redirects");
        context.registerService(RedirectFilterMBean.class, mbean);

    }

    @Test
    public void ignoreUpgrade(){

        UpgradeLegacyRedirects model =
                context.getService(ModelFactory.class).createModel(context.request(), UpgradeLegacyRedirects.class);
        assertFalse(model.isMoved());
    }

    @Test
    public void upgrade(){
        ResourceBuilder rb = context.build().
                resource("/conf/global/settings/redirects").
                resource("/conf/acs-commons/redirects");
        rb.resource("/conf/acs-commons/redirects/redirect-1",
                "sling:resourceType", REDIRECT_RULE_RESOURCE_TYPE,
                RedirectRule.SOURCE_PROPERTY_NAME, "/1",
                RedirectRule.TARGET_PROPERTY_NAME, "/2",
                RedirectRule.STATUS_CODE_PROPERTY_NAME, "301");

        UpgradeLegacyRedirects model =
                context.getService(ModelFactory.class).createModel(context.request(), UpgradeLegacyRedirects.class);
        assertTrue(model.isMoved());
        assertNull("/conf/acs-commons/redirects/redirect-1 should be moved to \"/conf/global/settings/redirects/redirect-1\"",
                context.resourceResolver().getResource("/conf/acs-commons/redirects/redirect-1"));
        assertNotNull("/conf/acs-commons/redirects/redirect-1 should be moved to \"/conf/global/settings/redirects/redirect-1\"",
                context.resourceResolver().getResource("/conf/global/settings/redirects/redirect-1"));
        assertTrue(context.resourceResolver().getResource("/conf/acs-commons/redirects").getValueMap().get("moved", false));
    }
}
