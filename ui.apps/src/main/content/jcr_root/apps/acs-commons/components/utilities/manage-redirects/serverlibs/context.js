use(function () {
    var RedirectFilter = Packages.com.adobe.acs.commons.redirects.filter.RedirectFilter;
    var ResourceUtil = Packages.org.apache.sling.api.resource.ResourceUtil;

    var redirectHome = "/conf/acs-commons/redirects";
    var enabled = false;
    var filters = sling.getServices(Packages.javax.servlet.Filter, null);
    for(var i=0; i < filters.length; i++ ){
        if(filters[i] instanceof RedirectFilter){
            redirectHome = filters[i].storagePath;
            enabled = true;
        }
    }

    var redirectResource = ResourceUtil.getOrCreateResource(
        resolver, redirectHome,
        {
            "jcr:primaryType": "sling:OrderedFolder"
        },
        "nt:unstructured", true);

    return {
        disabled: !enabled, // will trigger an alert on manage-redirects.html
        redirectResource: redirectResource
     };
});