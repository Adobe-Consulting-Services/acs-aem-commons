package com.adobe.acs.commons.mcp.impl.processes.cfi;

import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.DataType;
import com.adobe.cq.dam.cfm.FragmentData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.Map;

public class MockFragmentData implements FragmentData {

    Map.Entry<String, String> entry;

    MockFragmentData(Map.Entry<String, String> entry){
        this.entry = entry;
    }

    @NotNull
    @Override
    public DataType getDataType() {
        if(entry.getKey().contains("date")){
            return new MockDataType("calendar");
        }
        return new MockDataType("string");
    }

    @Nullable
    @Override
    public <T> T getValue(Class<T> type) {
        return null;
    }

    @Override
    public boolean isTypeSupported(Class type) {
        return false;
    }

    @Nullable
    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void setValue(@Nullable Object value) throws ContentFragmentException {

    }

    @Nullable
    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public void setContentType(@Nullable String contentType) {

    }
}
