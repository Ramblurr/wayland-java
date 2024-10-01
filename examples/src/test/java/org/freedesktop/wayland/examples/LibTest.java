package org.freedesktop.wayland.examples;

import com.google.common.reflect.ClassPath;
import org.freedesktop.wayland.client.Proxy;
import org.freedesktop.wayland.util.InterfaceMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

class LibTest {

    // Find classes in package that subclass baseClass
    public static List<Class<?>> findSubclasses(String packageName, Class<?> baseClass) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return ClassPath.from(classLoader)
                .getTopLevelClassesRecursive(packageName)
                .stream()
                .map(ClassPath.ClassInfo::load)
                .filter(clazz -> baseClass.isAssignableFrom(clazz) && !clazz.equals(baseClass))
                .collect(Collectors.toList());
    }

    public static String getStaticInterfaceName(Class<?> clazz) throws IllegalAccessException, NoSuchFieldException {
        Field field = clazz.getField("INTERFACE_NAME");
        return (String) field.get(null);
    }

    @Test
    public void test_loading_interfaces() throws IOException, IllegalAccessException, NoSuchFieldException {
        List<Class<?>> proxySubclasses = findSubclasses("org.freedesktop.wayland.client", Proxy.class);
        for (Class<?> proxySubclass : proxySubclasses) {
            var name = getStaticInterfaceName(proxySubclass);
            System.out.println("Loading interface meta for " + name);
            var ifacem = InterfaceMeta.get(proxySubclass);
            Assertions.assertEquals(name, ifacem.getName());
        }

    }


}