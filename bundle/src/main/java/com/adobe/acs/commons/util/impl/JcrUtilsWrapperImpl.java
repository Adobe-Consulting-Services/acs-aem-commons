package com.adobe.acs.commons.util.impl;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.JcrUtils;

import com.adobe.acs.commons.util.JcrUtilsWrapper;

public final class JcrUtilsWrapperImpl implements JcrUtilsWrapper {

    public Calendar getLastModified(final Node node) throws RepositoryException {
        return JcrUtils.getLastModified(node);
    }

    public Node putFile(final Node parent, final String name, final String mime, final InputStream data)
            throws RepositoryException {
        return JcrUtils.putFile(parent, name, mime, data, Calendar.getInstance());
    }

}
