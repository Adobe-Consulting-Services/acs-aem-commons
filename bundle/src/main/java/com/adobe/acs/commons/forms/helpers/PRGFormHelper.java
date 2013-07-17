package com.adobe.acs.commons.forms.helpers;

import com.adobe.acs.commons.forms.Form;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface PRGFormHelper extends FormHelper {
    public final static String KEY_FORM_NAME = "n";
    public final static String KEY_FORM = "f";
    public final static String KEY_ERRORS = "e";

    public void sendRedirect(Form form, String path, HttpServletResponse response) throws IOException, JSONException;
    public void sendRedirect(Form form, Page page, HttpServletResponse response) throws IOException, JSONException;
    public void sendRedirect(Form form, Resource resource, HttpServletResponse response) throws IOException, JSONException;
}