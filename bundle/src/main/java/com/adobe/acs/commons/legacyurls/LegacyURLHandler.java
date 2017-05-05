package com.adobe.acs.commons.legacyurls;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface LegacyURLHandler {

    /**
     * Looks for Pages/Assets/Resources with matching legacy URLs to the param requests URI.
     * If 1 or more matches are found, a match will be selected at random and the user will be HTTP Redirected
     * to that page.
     *
     * @param request the Sling Request obj
     * @param response the Sling Response obj
     * @return true is redirect could be found and a
     * @throws IOException
     */
    boolean doRedirect(HttpServletRequest request,
                       HttpServletResponse response) throws IOException;

}
