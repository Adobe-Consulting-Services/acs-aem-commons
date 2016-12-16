package com.adobe.acs.commons.one2one.model;

import java.util.Date;

public class VersionSelection {
    private final String name;
    private final Date date;

    VersionSelection(String name, Date date) {
        this.name = name;
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public String getName() {
        return name;
    }
}
