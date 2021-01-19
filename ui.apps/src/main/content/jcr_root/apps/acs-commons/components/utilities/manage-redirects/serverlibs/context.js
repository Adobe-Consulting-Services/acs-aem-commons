use(function () {
    var RedirectFilter = Packages.com.adobe.acs.commons.redirects.filter.RedirectFilter;
    var ResourceUtil = Packages.org.apache.sling.api.resource.ResourceUtil;

    var redirectHome;
    var filters = sling.getServices(Packages.javax.servlet.Filter, null);
    for(var i=0; i < filters.length; i++ ){
        if(filters[i] instanceof RedirectFilter){
            redirectHome = filters[i].storagePath;
        }
    }

    var redirectResource = ResourceUtil.getOrCreateResource(
        resolver, redirectHome,
        {
            "jcr:primaryType": "sling:OrderedFolder"
        },
        "nt:unstructured", true);

    return {
        redirectResource: redirectResource
     };
});