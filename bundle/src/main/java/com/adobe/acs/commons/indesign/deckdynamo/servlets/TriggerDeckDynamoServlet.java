package com.adobe.acs.commons.indesign.deckdynamo.servlets;

import com.adobe.acs.commons.indesign.deckdynamo.exception.DeckDynamoException;
import com.adobe.acs.commons.indesign.deckdynamo.models.DeckDynamoInitiatorPageModel;
import com.adobe.acs.commons.indesign.deckdynamo.services.DeckDynamoService;
import com.adobe.granite.rest.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.post.JSONResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

@Component(
        service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + ServletResolverConstants.DEFAULT_RESOURCE_TYPE,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=" + "json",
                ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + TriggerDeckDynamoServlet.SELECTOR_TRIGGER_DECK_DYNAMO})
public class TriggerDeckDynamoServlet extends SlingAllMethodsServlet {

    public static final String SELECTOR_TRIGGER_DECK_DYNAMO = "triggerDeckDynamo";
    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerDeckDynamoServlet.class);
    private static final long serialVersionUID = 2151877091112583303L;
    private static final String MESSAGE = "message";

    @Reference
    private transient DeckDynamoService deckDynamoService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        // Set response headers.
        response.setContentType(JSONResponse.RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(Constants.DEFAULT_CHARSET);

        JsonObject jsonResponse = new JsonObject();
        ResourceResolver resourceResolver = request.getResourceResolver();

        /* Determine the type of request through selector*/
        if (Arrays.asList(request.getRequestPathInfo().getSelectors()).contains(SELECTOR_TRIGGER_DECK_DYNAMO)) {
            String deckName = request.getParameter("deckTitle");
            String collectionPath = request.getParameter("collectionPath");
            String templatePath = request.getParameter("templatePath");

            String masterAssetPath = request.getParameter("masterAssetPath");
            String destinationPath = request.getParameter("destinationPath");
            String operationMode = request.getParameter("operationMode");
            String queryString = request.getParameter("queryString");

            String tagValues = request.getParameter("tagValues");

            try {
                if (StringUtils.isEmpty(deckName) || StringUtils.isEmpty(operationMode) || StringUtils.isEmpty(templatePath) || StringUtils.isEmpty(destinationPath)) {
                    throw new DeckDynamoException("Supplied deck name OR operation mode OR template path OR destination path is null/empty. Hence exiting the deck generation process.");
                }

                if (StringUtils.equalsIgnoreCase(DeckDynamoInitiatorPageModel.Mode.COLLECTION.toString(), operationMode) && StringUtils.isEmpty(collectionPath)) {
                    throw new DeckDynamoException("Collection path is expected when COLLECTION is operation mode. Hence exiting the deck generation process.");
                }

                if (StringUtils.equalsIgnoreCase(DeckDynamoInitiatorPageModel.Mode.QUERY.toString(), operationMode) && StringUtils.isEmpty(queryString)) {
                    throw new DeckDynamoException("Query string is expected when QUERY is operation mode. Hence exiting the deck generation process.");
                }

                if (StringUtils.equalsIgnoreCase(DeckDynamoInitiatorPageModel.Mode.TAGS.toString(), operationMode) && StringUtils.isEmpty(tagValues)) {
                    throw new DeckDynamoException("Tags string is expected when TAGS is operation mode. Hence exiting the deck generation process.");
                }

                String generatedDeckPath = null;

                List<Resource> assetResourceList = null;
                if (StringUtils.equalsIgnoreCase(DeckDynamoInitiatorPageModel.Mode.COLLECTION.toString(), operationMode)) {
                    assetResourceList = deckDynamoService.fetchAssetListFromCollection(collectionPath, resourceResolver);
                } else if (StringUtils.equalsIgnoreCase(DeckDynamoInitiatorPageModel.Mode.QUERY.toString(), operationMode)) {
                    assetResourceList = deckDynamoService.fetchAssetListFromQuery(queryString, resourceResolver);
                } else if (StringUtils.equalsIgnoreCase(DeckDynamoInitiatorPageModel.Mode.TAGS.toString(), operationMode)) {
                    assetResourceList = deckDynamoService.fetchAssetListFromTags(tagValues, resourceResolver);
                } else {
                    throw new DeckDynamoException("Invalid operation mode supplied. Hence exiting the deck generation process.");
                }


                if (null != assetResourceList && !assetResourceList.isEmpty()) {
                    generatedDeckPath = deckDynamoService.createDeck(deckName, masterAssetPath, assetResourceList, templatePath, destinationPath, resourceResolver);
                } else {
                    throw new DeckDynamoException("Asset resource list cannot be null or empty. Hence exiting the deck generation process.");
                }

                jsonResponse.addProperty(MESSAGE, "Deck created successfully! Template Path = " + generatedDeckPath);
            } catch (DeckDynamoException e) {
                response.setStatus(500);
                jsonResponse.addProperty(MESSAGE, "Deck creation Failed!");
                LOGGER.error("Exception occurred while initiating the deck generation", e);
            }
        }

        out.print(new Gson().toJson(jsonResponse));
    }

    public void setDeckDynamoService(DeckDynamoService deckDynamoService) {
        this.deckDynamoService = deckDynamoService;
    }
}
