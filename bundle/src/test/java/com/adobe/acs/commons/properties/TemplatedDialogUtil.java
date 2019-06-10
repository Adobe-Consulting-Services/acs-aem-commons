package com.adobe.acs.commons.properties;

import java.util.HashMap;
import java.util.Map;

import com.adobe.acs.commons.properties.impl.PropertyAggregatorServiceImpl;

import io.wcm.testing.mock.aem.junit.AemContext;

public class TemplatedDialogUtil {

    public static  Map<String, Object> defaultConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("exclude.list", "cq:(.*)");
        map.put("additional.data", "");
        return map;
    }

    public static PropertyAggregatorService defaultService(AemContext context) {
        Map<String, Object> config = defaultConfigMap();
        return context.registerInjectActivateService(new PropertyAggregatorServiceImpl(), config);
    }
}
