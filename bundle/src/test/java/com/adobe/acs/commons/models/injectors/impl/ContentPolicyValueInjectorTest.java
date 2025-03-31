package com.adobe.acs.commons.models.injectors.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class ContentPolicyValueInjectorTest {

    @Rule
    private OsgiContext context = new OsgiContext();


    @Test
    public void testDisabledInjector() {
        ContentPolicyValueInjector injector = new ContentPolicyValueInjector();
        Map<String,String> props = new HashMap<>();
        props.put("enabled", "false");
        context.registerInjectActivateService(injector, props);

        try (MockedStatic<InjectorUtils> injectorUtils = Mockito.mockStatic(InjectorUtils.class)) {
            injectorUtils.when(() -> InjectorUtils.getContentPolicy(any()))
                .thenReturn(null);
            injectorUtils.verify(() -> InjectorUtils.getContentPolicy(any()),never());
        }
    }

}
