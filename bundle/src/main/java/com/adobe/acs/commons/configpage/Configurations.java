package com.adobe.acs.commons.configpage;

import java.util.List;
import java.util.Map;

public interface Configurations {
    public Map<String,String> lookup(String columnName,String keyWord);
    public List<Map<String,String>> getList(String columnName, String keyWord);
}
