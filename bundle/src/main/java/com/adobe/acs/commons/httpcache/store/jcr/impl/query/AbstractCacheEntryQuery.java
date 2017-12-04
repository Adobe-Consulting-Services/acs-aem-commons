package com.adobe.acs.commons.httpcache.store.jcr.impl.query;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractCacheEntryQuery
{

    private final Session session;
    private final String cacheRootPath;
    private QueryResult queryResult;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCacheEntryQuery.class);


    public AbstractCacheEntryQuery(Session session, String cacheRootPath){
        this.session = session;
        this.cacheRootPath = cacheRootPath;

        try {
            final Query query = this.createQuery();
            queryResult = query.execute();
        } catch (RepositoryException e) {
            LOGGER.error("error executing query", e);
            LOGGER.trace("SQL: {}", createQueryStatement());
        }
    }

    protected Session getSession()
    {
        return session;
    }

    protected Query createQuery() throws RepositoryException
    {
        return session.getWorkspace().getQueryManager().createQuery(createQueryStatement(), getQueryLanguage());
    }

    protected String getQueryLanguage()
    {
        return Query.JCR_SQL2;
    }

    protected abstract String createQueryStatement();

    protected String getCacheRootPath()
    {
        return cacheRootPath;
    }

    protected QueryResult getQueryResult()
    {
        return queryResult;
    }
}
