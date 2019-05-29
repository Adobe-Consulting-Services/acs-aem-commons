package com.adobe.acs.commons.models.injectors.impl.model;

import java.util.Collection;
import java.util.List;

public interface TestSharedValueMapValueModel {
    String getGlobalStringProp();

    String getSharedStringProp();

    String getMergedStringProp();

    String getStringProp();

    String getStringProp2();

    String getStringProp3();

    Long getLongProp();

    Long getLongPropFromString();

    boolean isBoolPropTrue();

    boolean isBoolPropFalse();

    boolean isBoolPropTrueFromString();

    boolean isBoolPropFalseFromString();

    String[] getStringArrayProp();

    List<String> getStringListProp();

    Collection<String> getStringCollectionProp();

    Long[] getLongArrayProp();

    List<Long> getLongListProp();

    Collection<Long> getLongCollectionProp();

    Long[] getLongArrayPropFromNonArray();

    List<Long> getLongListPropFromNonArray();
}
