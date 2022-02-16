use(function () {
    var RedirectFilter = Packages.com.adobe.acs.commons.redirects.filter.RedirectFilter;
    var redirectResource = request.getRequestPathInfo().getSuffixResource();

    var enabled = false;
    var filters = sling.getServices(Packages.javax.servlet.Filter, null);
    for(var i=0; i < filters.length; i++ ){
        if(filters[i] instanceof RedirectFilter){
            enabled = true;
        }
    }

    return {
        disabled: !enabled, // will trigger an alert on manage-redirects.html
        redirectResource: redirectResource
     };
});