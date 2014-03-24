package com.adobe.acs.commons.models.injectors;

import org.apache.sling.models.impl.ModelAdapterFactory;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestModelAdapterFactory extends ModelAdapterFactory {

    public TestModelAdapterFactory() {
        super();

        ComponentContext componentCtx = mock(ComponentContext.class);
        BundleContext bundleContext = mock(BundleContext.class);
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);

        activate(componentCtx);
    }

    @Override
    public void bindInjector(Injector injector, Map<String, Object> props) {
        super.bindInjector(injector, props);
    }

}
