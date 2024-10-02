package org.freedesktop.wayland.generator.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
public @interface WaylandCoreProtocols {
    String path() default "wayland.xml";

    String pkgConfig() default "wayland-scanner";

    boolean excludeDeprecated() default true;

    String sharedPackage() default "shared";

    String clientPackage() default "client";

    String serverPackage() default "server";

    boolean generateClient() default true;

    boolean generateServer() default true;
}
