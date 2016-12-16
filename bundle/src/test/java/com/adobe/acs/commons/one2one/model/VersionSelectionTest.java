package com.adobe.acs.commons.one2one.model;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class VersionSelectionTest {

    @Test
    public void shouldWork() throws Exception {

        final String name= "A";
        final Date date = new Date();
        VersionSelection underTest = new VersionSelection(name, date);

        assertThat(underTest.getName(), is(name));
        assertThat(underTest.getDate(), is(date));
    }
}