package com.google.code.inject.jaxrs;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for marking injected sub-resources
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface InjectedResource {

}