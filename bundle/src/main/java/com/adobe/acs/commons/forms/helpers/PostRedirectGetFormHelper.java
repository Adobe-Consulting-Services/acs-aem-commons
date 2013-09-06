package com.adobe.acs.commons.forms.helpers;

import com.adobe.acs.commons.forms.Form;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;

import java.io.IOException;

public interface PostRedirectGetFormHelper extends FormHelper {
    public final static String KEY_FORM_NAME = "n";
    public final static String KEY_FORM = "f";
    public final static String KEY_ERRORS = "e";
    public final static String KEY_PREFIX_FORM_NAME = "f_";
    public final static String QUERY_PARAM_FORM_SELECTOR = "f_selector";

    /**
     * Issues a 302 redirect with the form serialized into a JSON object that can be
     * read out by the PostRedirectGetFormHelper on the "other side".
     *
     * Allows 302 redirect to target the specified path.
     *
     * @param form
     * @param path
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    public void sendRedirect(Form form, String path, SlingHttpServletResponse response) throws IOException, JSONException;

    /**
     * Issues a 302 redirect with the form serialized into a JSON object that can be
     * read out by the PostRedirectGetFormHelper on the "other side".
     *
     * Allows 302 redirect to target the specified CQ Page.
     *
     * @param form
     * @param page
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    public void sendRedirect(Form form, Page page, SlingHttpServletResponse response) throws IOException, JSONException;

    /**
     /**
     * Issues a 302 redirect with the form serialized into a JSON object that can be
     * read out by the PostRedirectGetFormHelper on the "other side".
     *
     * Allows 302 redirect to target the specified resource with provided .html extension.
     *
     * @param form
     * @param resource
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    public void sendRedirect(Form form, Resource resource, SlingHttpServletResponse response) throws IOException, JSONException;


    /**
     * Same as:
     *
     *      sendRedirect(Form form, String path, SlingHttpServletResponse response)
     *
     * but adds the Form selector query parameter to redirect request.
     *
     * @param form
     * @param path
     * @param formSelector
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    public void sendRedirect(Form form, String path, String formSelector, SlingHttpServletResponse response) throws IOException, JSONException;

    /**
     * Same as:
     *
     *      sendRedirect(Form form, Page page, SlingHttpServletResponse response)
     *
     * but adds the Form selector query parameter to redirect request.
     *
     * @param form
     * @param page
     * @param formSelector
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    public void sendRedirect(Form form, Page page, String formSelector, SlingHttpServletResponse response) throws IOException, JSONException;

    /**
     * Same as:
     *
     *      sendRedirect(Form form, Resource resource, SlingHttpServletResponse response)
     *
     * but adds the Form selector query parameter to redirect request.
     *
     * @param form
     * @param resource
     * @param formSelector
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    public void sendRedirect(Form form, Resource resource, String formSelector, SlingHttpServletResponse response) throws IOException, JSONException;
}