package com.adobe.acs.commons.contentsync;

import javax.servlet.ServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class TestUtils {

    public static Map<String, Object> getParameters(ServletRequest request){
        Map<String, Object> params = new HashMap<>();
        for(Enumeration<String> it = request.getParameterNames(); it.hasMoreElements(); ){
            String name = it.nextElement();
            String value = request.getParameter(name);
            params.put(name, value);
        }
        return params;
    }
}
