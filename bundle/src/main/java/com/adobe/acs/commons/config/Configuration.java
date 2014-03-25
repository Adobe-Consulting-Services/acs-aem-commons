package com.adobe.acs.commons.config;

import java.util.List;
import java.util.Map;

public interface Configuration {
Map<String, String> getRowByKey(String key);
List<Map<String,String>> getRowsByKey(String key);
Map<String, String> getRowByColumnValue(String columnName, String columnValue);
List<Map<String,String>> getRowsByColumnValue(String columnName, String columnValue);
}
