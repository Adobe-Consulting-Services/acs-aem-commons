package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.models.injectors.annotation.SharedValueMapValue;
import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import java.util.Collection;
import java.util.List;

@Model(adaptables = {Resource.class, SlingHttpServletRequest.class})
public class SharedValueMapValueTestModel {
    @SlingObject
    private Resource resource;

    @OSGiService
    private PageRootProvider prp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.STRING_PROP, type = SharedComponentProperties.ValueTypes.GLOBAL)
    private String globalStringProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.STRING_PROP, type = SharedComponentProperties.ValueTypes.SHARED)
    private String sharedStringProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.STRING_PROP, type = SharedComponentProperties.ValueTypes.MERGED)
    private String mergedStringProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String stringProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String stringProp2;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String stringProp3;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Long longProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.LONG_PROP_STR)
    private Long longPropFromString;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private boolean boolPropTrue;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private boolean boolPropFalse;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.BOOL_PROP_TRUE_STR)
    private boolean boolPropTrueFromString;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.BOOL_PROP_FALSE_STR)
    private boolean boolPropFalseFromString;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String[] stringArrayProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.STRING_ARRAY_PROP)
    private List<String> stringListProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.STRING_ARRAY_PROP)
    private Collection<String> stringCollectionProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Long[] longArrayProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.LONG_ARRAY_PROP)
    private List<Long> longListProp;

    @SharedValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL, name = SharedValueMapValueInjectorTest.LONG_ARRAY_PROP)
    private Collection<Long> longCollectionProp;

    public String getHomePath() {
        return prp.getRootPagePath(resource.getPath());
    }

    public String getGlobalStringProp() {
        return globalStringProp;
    }

    public String getSharedStringProp() {
        return sharedStringProp;
    }

    public String getMergedStringProp() {
        return mergedStringProp;
    }

    public String getStringProp() {
        return stringProp;
    }

    public String getStringProp2() {
        return stringProp2;
    }

    public String getStringProp3() {
        return stringProp3;
    }

    public Long getLongProp() {
        return longProp;
    }

    public Long getLongPropFromString() {
        return longPropFromString;
    }

    public boolean isBoolPropTrue() {
        return boolPropTrue;
    }

    public boolean isBoolPropFalse() {
        return boolPropFalse;
    }

    public boolean isBoolPropTrueFromString() {
        return boolPropTrueFromString;
    }

    public boolean isBoolPropFalseFromString() {
        return boolPropFalseFromString;
    }


    public String[] getStringArrayProp() {
        return stringArrayProp;
    }

    public List<String> getStringListProp() {
        return stringListProp;
    }

    public Collection<String> getStringCollectionProp() {
        return stringCollectionProp;
    }

    public Long[] getLongArrayProp() {
        return longArrayProp;
    }

    public List<Long> getLongListProp() {
        return longListProp;
    }

    public Collection<Long> getLongCollectionProp() {
        return longCollectionProp;
    }

}