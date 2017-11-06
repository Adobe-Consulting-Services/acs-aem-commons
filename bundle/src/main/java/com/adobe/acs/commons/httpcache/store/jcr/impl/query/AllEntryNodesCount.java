package com.adobe.acs.commons.httpcache.store.jcr.impl.query;

import javax.jcr.Session;

public class AllEntryNodesCount
{
    private final Session session;

    public AllEntryNodesCount(Session session)
    {
        this.session = session;
    }

    public long get(){
        return 0;
    }
}
