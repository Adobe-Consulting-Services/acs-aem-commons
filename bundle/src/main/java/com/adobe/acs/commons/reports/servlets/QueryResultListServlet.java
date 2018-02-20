package com.adobe.acs.commons.reports.servlets;

import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

import static javax.jcr.query.Query.JCR_SQL2;

@Component(service = Servlet.class, property = {
        Constants.SERVICE_DESCRIPTION + "= Service to get drop down list",
        "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        "sling.servlet.paths=" + "/bin/queryresultlist"})
public class QueryResultListServlet extends SlingSafeMethodsServlet {

    final String DROPDOWNQUERY = "dropDownQuery";

    //Create an ArrayList to hold data
    List<Resource> fakeResourceList = new ArrayList<Resource>();

    @Reference
    private QueryHelper queryHelper;

    private final static Logger logger = LoggerFactory.getLogger(QueryResultListServlet.class);

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {

        ResourceResolver resolver = request.getResourceResolver();

        ValueMap vm = null;
        try {
            String queryStatement = request.getResource().getValueMap().get(DROPDOWNQUERY, String.class);
            request.setAttribute(DataSource.class.getName(), EmptyDataSource.instance());
            if (queryStatement != null && !queryStatement.isEmpty()) {
                List<Resource> resourceList = queryHelper.findResources(resolver, JCR_SQL2, queryStatement, StringUtils.EMPTY);
                StringBuilder selectOptionsHTML = new StringBuilder();
                List<String> selectQueryColumns = getSelectColumns(queryStatement);
                List<String> distinctOptionValues = new ArrayList<>();
                for (Resource resource : resourceList) {
                    ValueMap valueMap = resource.getValueMap();
                    Set<Map.Entry<String, Object>> entrySet = valueMap.entrySet();
                    Iterator<Map.Entry<String, Object>> entryIterator = entrySet.iterator();
                    while (entryIterator.hasNext()) {
                        Map.Entry<String, Object> next = entryIterator.next();
                        vm = new ValueMapDecorator(new HashMap<String, Object>());
                        String key = next.getKey();
                        Object value = next.getValue();
                        if (value instanceof String && (selectQueryColumns.size() == 0 || selectQueryColumns.contains(key.toLowerCase()))) {
                            //Either it has to be a select * query or the query parameter has to contain the current key
                            //We are doing this only string and string array
                            if (!distinctOptionValues.contains(value.toString())) {
                                vm.put("value", value.toString());
                                vm.put("text", value.toString());
                                distinctOptionValues.add(value.toString());
                                fakeResourceList.add(new ValueMapResource(resolver, new ResourceMetadata(), "nt:unstructured", vm));
                            }
                        }
                    }//End of while parsing the property
                }
                //Create a DataSource that is used to populate the drop-down control
                DataSource ds = new SimpleDataSource(fakeResourceList.iterator());
                request.setAttribute(DataSource.class.getName(), ds);
            }
        } catch (Exception e) {
            logger.error("The exception is", e.getMessage(), e);
        }
    }

    //Mehtod for selecting the property for showing in the dropdown.
    public List<String> getSelectColumns(String jcrQueryString) {
        List<String> selectQueryColumns = new ArrayList<>();
        String querySelectParameters = jcrQueryString.toLowerCase();
        querySelectParameters = StringUtils.substringAfter(querySelectParameters, "select");
        querySelectParameters = StringUtils.substringBefore(querySelectParameters, " from");
        querySelectParameters = querySelectParameters.replaceAll("\\[", "").replaceAll("\\]", "");
        querySelectParameters = querySelectParameters.replaceAll("distinct", "");
        querySelectParameters = querySelectParameters.trim().toLowerCase();
        querySelectParameters = querySelectParameters.replaceAll("\r", "").replaceAll("\n", "");

        String[] arr = querySelectParameters.split(",");
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].indexOf(".") >= 0) {
                arr[i] = StringUtils.substringAfter(arr[i], ".");
            }
            selectQueryColumns.add(arr[i].toLowerCase().trim());
        }
        return selectQueryColumns;
    }
}
