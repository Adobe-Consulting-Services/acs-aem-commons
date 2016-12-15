package com.adobe.acs.commons.one2one.model;

import java.util.Date;

/**
 * Created by Dominik Foerderreuther <df@adobe.com> on 15/12/16.
 */
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
