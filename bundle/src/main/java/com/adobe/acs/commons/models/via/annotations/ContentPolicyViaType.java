package com.adobe.acs.commons.models.via.annotations;

import org.apache.sling.models.annotations.ViaProviderType;

public interface ContentPolicyViaType extends ViaProviderType {

    String VIA_COMPONENT = "component";
    String VIA_RESOURCE_PAGE = "resourcePage";
    String VIA_CURRENT_PAGE = "currentPage";
}
