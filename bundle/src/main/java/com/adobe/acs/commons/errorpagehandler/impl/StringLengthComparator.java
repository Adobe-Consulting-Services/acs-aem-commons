package com.adobe.acs.commons.errorpagehandler.impl;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class StringLengthComparator implements Comparator<String> {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(ErrorPageHandlerImpl.class);

    @Override
    public int compare(String s1, String s2) {
        s1 = StringUtils.stripToEmpty(s1);
        s2 = StringUtils.stripToEmpty(s2);

        if(s1.length() > s2.length()) {
            return -1;
        } else if(s1.length() < s2.length()) {
            return 1;
        } else {
            // Compare alphabeticaly if the same length
            return s1.compareTo(s2);
        }
    }
}
