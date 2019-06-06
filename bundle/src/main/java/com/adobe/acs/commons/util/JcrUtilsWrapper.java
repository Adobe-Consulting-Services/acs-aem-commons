package com.adobe.acs.commons.util;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public interface JcrUtilsWrapper {

	Calendar getLastModified(Node node) throws RepositoryException;

    Node putFile(Node parent, String name, String mime, InputStream data)
            throws RepositoryException;

}
