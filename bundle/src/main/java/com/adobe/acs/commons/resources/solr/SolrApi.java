package com.adobe.acs.commons.resources.solr;


import retrofit.http.GET;
import retrofit.http.Query;

import java.util.Map;


/**
 * A Solr API exposed as retrofit interface .. BANG THE STARS IT'S FRIDAY! :-)
 */
public interface SolrApi {

    @GET("/get?wt=json")
    public Map<String, Object> get(@Query("id")String id);

    @GET("/select?wt=json")
    public Map<String, Object> select(@Query("q")String query);

}
