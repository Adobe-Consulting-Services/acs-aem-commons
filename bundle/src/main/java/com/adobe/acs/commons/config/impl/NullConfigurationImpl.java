package com.adobe.acs.commons.config.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.adobe.acs.commons.config.Configuration;

public class NullConfigurationImpl implements Configuration {

    @Override
    public Map<String, String> getRowByKey(String key) {
        
        return Collections.emptyMap();
    }

    @Override
    public List<Map<String, String>> getRowsByKey(String key) {
       
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getRowByColumnValue(String columnName,
            String columnValue) {
        
        return Collections.emptyMap();
    }

    @Override
    public List<Map<String, String>> getRowsByColumnValue(String columnName,
            String columnValue) {
        
        return Collections.emptyList();
    }

}
