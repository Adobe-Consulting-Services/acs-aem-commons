/*
 * ***********************************************************************
 * HS2 SOLUTIONS CONFIDENTIAL
 * ___________________
 *
 * Copyright 2019 Bounteous
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property
 * of HS2 Solutions and its suppliers, if any. The intellectual and
 * technical concepts contained herein are proprietary to HS2 Solutions
 * and its suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from HS2 Solutions.
 * ***********************************************************************
 */
package com.adobe.acs.commons.models.injectors.annotation;

import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject a child (or list of child) resources using a request as the adaptable.
 *
 * This is used to overcome limitations in the `@ChildResource` injector in that it only injects
 * a `Resource`, losing the context of the `SlingHttpServletRequest`.  For any sling models that
 * require a `SlingHttpServletRequest`, the loss of the `SlingHttpServletRequest` makes the
 * `@ChildResource` injector an insufficient solution.
 *
 * See https://issues.apache.org/jira/browse/SLING-8279 for more context on the issue that this
 * via provider solves.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@InjectAnnotation
@Source("child-requests")
public @interface ChildRequest {
    String name() default "";

    /** @deprecated */
    @Deprecated
    boolean optional() default false;

    InjectionStrategy injectionStrategy() default InjectionStrategy.DEFAULT;

    String via() default "";
}
