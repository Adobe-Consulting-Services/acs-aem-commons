package com.adobe.acs.commons.reports.internal;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExporterUtilTest {

    @Test
    public void relativizePath() {
        assertEquals("foo/bar", ExporterUtil.relativizePath("foo/bar"));
        assertEquals("./foo/bar", ExporterUtil.relativizePath("./foo/bar"));
        assertEquals("foo/bar", ExporterUtil.relativizePath("/foo/bar"));
    }
}