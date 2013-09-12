package com.adobe.acs.commons.contentfinder.querybuilder.impl.querybuilder;

import com.adobe.acs.commons.contentfinder.querybuilder.impl.ContentFinderHitBuilder;
import com.day.cq.search.Query;
import com.day.cq.search.result.Hit;
import com.day.cq.search.writer.ResultHitWriter;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.Constants;

import javax.jcr.RepositoryException;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - ContentFinder Result Hit Writer",
        description = "QueryBuilder Hit Writer used for creating ContentFinder compatible results",
        factory = "com.day.cq.search.result.ResultHitWriter/cf",
        immediate = false,
        metatype = false
)
@Properties({
        @Property(
                label = "Vendor",
                name = Constants.SERVICE_VENDOR,
                value = "ACS",
                propertyPrivate = true
        )
})
public class ContentFinderResultHitWriter implements ResultHitWriter {
    /**
     * Result hit writer integration
     *
     * @param hit
     * @param jsonWriter
     * @param query
     * @throws javax.jcr.RepositoryException
     * @throws org.apache.sling.commons.json.JSONException
     */
    @Override
    public void write(Hit hit, JSONWriter jsonWriter, Query query) throws RepositoryException, JSONException {
        Map<String, Object> map = ContentFinderHitBuilder.buildGenericResult(hit);

        jsonWriter.object();

        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            jsonWriter.key(entry.getKey()).value(entry.getValue());
        }

        jsonWriter.endObject();
    }
}
