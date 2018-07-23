package com.adobe.acs.commons.adobeio.core.types;

import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;

import com.drew.lang.annotations.NotNull;

public class FilterImpl implements Filter {

    private final Map<String, String> filter = new HashedMap();

    /**
     * Create new Filter
     * @param key Key
     * @param value Value
     */
    public FilterImpl(@NotNull String key, @NotNull String value) {
        filter.put(key, value);
    }

    @Override
    public String getFilter() {

        String result = StringUtils.EMPTY;

        for(Map.Entry<String, String> entry: filter.entrySet()) {
            result = entry.getKey() + "=" + entry.getValue();
        }

        return result;
    }
}
