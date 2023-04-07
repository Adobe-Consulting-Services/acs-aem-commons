package com.adobe.acs.commons.data;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SpreadsheetHeaders {

    private Map<String, Optional<Class>> headerTypes;
    private List<String> headerRow;

    public Map<String, Optional<Class>> getHeaderTypes() {
        return headerTypes;
    }

    public void setHeaderTypes(Map<String, Optional<Class>> headerTypes) {
        this.headerTypes = headerTypes;
    }

    public List<String> getHeaderRow() {
        return headerRow;
    }

    public void setHeaderRow(List<String> headerRow) {
        this.headerRow = headerRow;
    }
}
