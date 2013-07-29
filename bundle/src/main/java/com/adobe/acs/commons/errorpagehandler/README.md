# Error Page Handler

## Purpose

Provide an author-able means for defining, creating and managing custom Error pages per content tree/site.

## Expected Behavior

### Edit/Design mode

#### 40x Handling
Author is displayed the corresponding Error page

#### 50x Handling
Normal/OOTB 500x/Exception error handling behavior is respected. JSP exceptions are displayed inline w the JSP.

### Preview mode

#### 40x Handling
Author is displayed the corresponding Error page

#### 50x Handling
A custom "Error page" is displayed that includes the Request Progress and Stack Trace.

### Disabled (Publish) mode

The corresponding Error page is displayed.

## Instructions

1. In your base page implementation, add the following `cq:Widget` to the Page Properties dialog
    <errorpages
        jcr:primaryType="cq:Widget"
        path="./apps/acs-commons/components/utilities/errorpagehandler/dialog/errorpages"
        xtype="cqinclude"/>

    - OR create a your own custom pathfield widget -

    <errorpages
        jcr:primaryType="cq:Widget"
        fieldLabel="Error Pages"
        fieldDescription="Error pages for this content tree"
        name="./errorPages"
        xtype="pathfield"/>

2. Create a CQ Page that will act as the default Error page, and also contain all custom variations of error pages.
A common pattern is to create this at the site's root under a node named "errors"
    * Ex. /content/geometrixx/en/us/errors
3. Create any error-specific pages under this default error page created in Step 2.
Note, it is critical that the page NAMES (node names) follow the Sling script handling scheme. The page Titles can be anything.
    * Ex. /content/geometrixx/en/us/errors/404
    * Ex. /content/geometrixx/en/us/errors/500
    * Ex. /content/geometrixx/en/us/errors/Throwable
    * Ex. /content/geometrixx/en/us/errors/CustomAppException
4. Edit the Page Properties of the site's root node, and in the new "Error Pages" dialog input (Step 1) select the default error page (Step 2).
    * Ex. ./errorPages => /content/geometrixx/en/us/errors
5. Further customizations can be made via the OSGi Configuration for the `ACS AEM Commons - Error Page Handler` Configuration




