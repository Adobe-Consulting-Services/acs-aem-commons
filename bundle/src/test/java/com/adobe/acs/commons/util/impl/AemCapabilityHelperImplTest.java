package com.adobe.acs.commons.util.impl;

import com.adobe.acs.commons.util.AemCapabilityHelper;
import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoProvider;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class AemCapabilityHelperImplTest {

    @Mock
    ProductInfoProvider productInfoProvider;

    @Mock
    SlingRepository slingRepository;

    @Mock
    ProductInfo productInfo;

    @Rule
    public final AemContext ctx = new AemContext();

    @Before
    public void setUp() throws Exception {
        ctx.registerService(SlingRepository.class, slingRepository);
    }

    private void setUpAsCloudReady() {
        doReturn(new Version("2020.2.2239.20200214T010959Z")).when(productInfo).getVersion();
        doReturn("2020.2.2239.20200214T010959Z").when(productInfo).getShortVersion();

        doReturn(productInfo).when(productInfoProvider).getProductInfo();

        ctx.registerService(ProductInfoProvider.class, productInfoProvider);
        ctx.registerInjectActivateService(new AemCapabilityHelperImpl());
    }

    private void setUpAsNotCloudReady() {
        doReturn(new Version("6.5.4")).when(productInfo).getVersion();
        doReturn("6.5.4").when(productInfo).getShortVersion();
        doReturn(productInfo).when(productInfoProvider).getProductInfo();

        ctx.registerService(ProductInfoProvider.class, productInfoProvider);
        ctx.registerInjectActivateService(new AemCapabilityHelperImpl());
    }
    
    @Test
    public void isCloudReady_True() {
        setUpAsCloudReady();

        AemCapabilityHelper aemCapabilityHelper = ctx.getService(AemCapabilityHelper.class);

        assertTrue(aemCapabilityHelper.isCloudReady());
    }

    @Test
    public void isCloudReady_False() {
        setUpAsNotCloudReady();

        AemCapabilityHelper aemCapabilityHelper = ctx.getService(AemCapabilityHelper.class);

        assertFalse(aemCapabilityHelper.isCloudReady());
    }

    @Test
    public void referenceFilter_CloudReady_True_Satisfied() {
        setUpAsCloudReady();

        AemCapabilityHelper[] aemCapabilityHelpers = ctx.getServices(AemCapabilityHelper.class, "(cloud-ready=true)");

        assertEquals(1, aemCapabilityHelpers.length);
        assertTrue(aemCapabilityHelpers[0].isCloudReady());
    }

    @Test
    public void referenceFilter_CloudReady_True_Unsatisfied() {
        setUpAsNotCloudReady();

        AemCapabilityHelper[] aemCapabilityHelpers = ctx.getServices(AemCapabilityHelper.class, "(cloud-ready=true)");

        assertEquals(0, aemCapabilityHelpers.length);
    }

    @Test
    public void referenceFilter_CloudReady_False_Satisfied() {
        setUpAsNotCloudReady();

        AemCapabilityHelper[] aemCapabilityHelpers = ctx.getServices(AemCapabilityHelper.class, "(cloud-ready=false)");

        assertEquals(1, aemCapabilityHelpers.length);
        assertFalse(aemCapabilityHelpers[0].isCloudReady());
    }

    @Test
    public void referenceFilter_CloudReady_False_Unsatisfied() {
        setUpAsCloudReady();

        AemCapabilityHelper[] aemCapabilityHelpers = ctx.getServices(AemCapabilityHelper.class, "(cloud-ready=false)");

        assertEquals(0, aemCapabilityHelpers.length);
    }
}