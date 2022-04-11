package com.adobe.acs.commons.mcp.impl.processes.cfi;

import com.adobe.cq.dam.cfm.DataType;
import org.jetbrains.annotations.NotNull;

public class MockDataType implements DataType {

    String type;

    MockDataType(String type){
        this.type = type;
    }

    @NotNull
    @Override
    public String getTypeString() {
        return type;
    }

    @Override
    public boolean isMultiValue() {
        return false;
    }
}
