package org.freedesktop.wayland.generator.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
public @interface WaylandProtocols {
    String path() default "";

    String pkgConfig() default "wayland-protocols";

    boolean withStable() default true;

    boolean withStaging() default true;

    boolean withUnstable() default true;

    String sharedPackage() default "shared";

    String clientPackage() default "client";

    String serverPackage() default "server";

    boolean generateClient() default true;

    boolean generateServer() default true;
}
