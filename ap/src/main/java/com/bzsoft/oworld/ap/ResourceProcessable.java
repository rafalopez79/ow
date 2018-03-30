package com.bzsoft.oworld.ap;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(SOURCE)
@Target(TYPE)
public @interface ResourceProcessable {

	public String appLocation() default "app.properties";

	public String className() default "com.bzsoft.oworld.R";

}
