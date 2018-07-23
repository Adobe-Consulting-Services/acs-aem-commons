package com.adobe.acs.commons.adobeio.core.types;

@SuppressWarnings("WeakerAccess")
public class ActionImpl implements Action {

    private String action;

    @Override
    public void setValue(String actionType) {
        this.action = actionType;
    }

    @Override
    public String getValue() {
        return action;
    }
}
