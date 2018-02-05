package com.adobe.acs.commons.ondeploy.impl;

/**
 * An exception representing a failed on-deploy script.
 */
public class OnDeployScriptException extends RuntimeException {
    public OnDeployScriptException(Throwable cause) {
        super("On-deploy script failure", cause);
    }
}
