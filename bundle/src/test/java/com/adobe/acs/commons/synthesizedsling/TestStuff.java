package com.adobe.acs.commons.synthesizedsling;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TestStuff {

    protected SlingHttpServletRequest originalRequest;
    protected SlingHttpServletResponse originalResponse;


    public void showOff() throws ServletException, IOException {

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("myFirstProp", "hello");
        props.put("mySecondProp", 1238);

        String htmlResult = SynthesizedUtil.renderSynthesized("my/super-cool/synthesized-stuff/that-doesnt-really-exist",
                "myapp/components/myslingscript", props, originalRequest, originalResponse);

    }
}
