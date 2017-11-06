package com.adobe.acs.commons.httpcache.store.jcr.impl.query;

import javax.jcr.NodeIterator;
import javax.jcr.Session;

public class AllEntryNodes
{
    private final Session session;

    public AllEntryNodes(Session session)
    {
        this.session = session;
    }

    public NodeIterator get()
    {
        return null;
    }
}
