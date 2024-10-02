/*
 * Copyright © 2015 Erik De Rijcke
 * Copyright © 2024 Casey Link
 *
 * Licensed under the Apache License,Version2.0(the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 *
 */
package org.freedesktop.wayland.generator.impl;

import org.freedesktop.wayland.util.Fixed;
import org.freedesktop.wayland.util.WlArray;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class StringUtil {

    private static final Set<String> keywords;

    static {
        keywords = new HashSet<>();
        Collections.addAll(keywords,
                "abstract",
                "continue",
                "for",
                "new",
                "switch",
                "assert",
                "default",
                "if",
                "package",
                "synchronized",
                "boolean",
                "do",
                "goto",
                "private",
                "this",
                "break",
                "double",
                "implements",
                "protected",
                "throw",
                "byte",
                "else",
                "import",
                "public",
                "throws",
                "case",
                "enum",
                "instanceof",
                "return",
                "transient",
                "catch",
                "extends",
                "int",
                "short",
                "try",
                "char",
                "final",
                "interface",
                "static",
                "void",
                "class",
                "finally",
                "long",
                "strictfp",
                "volatile",
                "const",
                "float",
                "native",
                "super",
                "while");
    }

    private static final Set<String> PRIMITIVE_TYPES = new HashSet<String>() {{
        add(byte.class.getName());
        add(short.class.getName());
        add(int.class.getName());
        add(long.class.getName());
        add(float.class.getName());
        add(double.class.getName());
        add(char.class.getName());
        add(boolean.class.getName());
    }};

    private StringUtil() {
    }

    public static String getJavaTypeNameEnum(final String sharedPackage,
                                             final Element interfaceNode,
                                             final Element enumNode) {
        return sharedPackage + "." + getSimpleJavaTypeNameEnum(interfaceNode,
                enumNode);
    }

    public static String getSimpleJavaTypeNameEnum(final Element interfaceNode,
                                                   final Element enumNode) {
        return upperCamelName(interfaceNode.getAttribute(InterfaceWriter.ATTRIBUTE_NAME) +
                "_" +
                enumNode.getAttribute(InterfaceWriter.ATTRIBUTE_NAME));
    }

    public static String upperCamelName(String string) {
        Objects.requireNonNull(string);
        if (string.isEmpty()) return string;
        String[] words = string.split("_");
        string = "";
        for (int i = 0; i < words.length; ++i) {
            String firstChar = words[i].substring(0, 1);
            string = string + firstChar.toUpperCase() + words[i].substring(1);
        }
        return string;
    }

    public static String lowerCamelName(String string) {
        Objects.requireNonNull(string);
        if (string.isEmpty()) return string;
        String[] words = string.split("_");
        string = words[0];
        for (int i = 1; i < words.length; ++i) {
            String firstChar = words[i].substring(0, 1);
            string = string + firstChar.toUpperCase() + words[i].substring(1);
        }
        return string;
    }

    public static String getJavaTypeNameResource(final String serverPackage,
                                                 final Element interfaceElement,
                                                 final int version) {
        return serverPackage + "." + getSimpleJavaTypeNameResource(interfaceElement,
                version);
    }

    public static String getSimpleJavaTypeNameResource(final Element interfaceElement,
                                                       final int version) {
        return upperCamelName(interfaceElement.getAttribute(InterfaceWriter.ATTRIBUTE_NAME)) + "Resource" + getVersionAppendix(version);
    }

    public static String getVersionAppendix(final int version) {
        final String versionAppend;
        if (version == 1) {
            versionAppend = "";
        } else {
            versionAppend = "V" + Integer.toString(version);
        }
        return versionAppend;
    }

    public static String getJavaTypeNameRequests(final String serverPackage,
                                                 final Element interfaceElement,
                                                 final int version) {
        return serverPackage + "." + getSimpleJavaTypeNameRequests(interfaceElement,
                version);
    }

    public static String getSimpleJavaTypeNameRequests(final Element interfaceElement,
                                                       final int version) {
        return upperCamelName(interfaceElement.getAttribute(InterfaceWriter.ATTRIBUTE_NAME)) + "Requests" + getVersionAppendix(version);
    }

    public static String getJavaTypeNameProxy(final String clientPackage,
                                              final Element interfaceElement,
                                              final int version) {
        return clientPackage + "." + getSimpleJavaTypeNameProxy(interfaceElement,
                version);
    }

    public static String getSimpleJavaTypeNameProxy(final Element interfaceElement,
                                                    final int version) {
        return upperCamelName(interfaceElement.getAttribute(InterfaceWriter.ATTRIBUTE_NAME)) + "Proxy" + getVersionAppendix(version);
    }

    public static String getJavaTypeNameEvents(final String clientPackage,
                                               final Element interfaceElement,
                                               final int version) {
        return clientPackage + "." + getSimpleJavaTypeNameEvents(interfaceElement,
                version);
    }

    public static String getSimpleJavaTypeNameEvents(final Element interfaceElement,
                                                     final int version) {
        return upperCamelName(interfaceElement.getAttribute(InterfaceWriter.ATTRIBUTE_NAME)) + "Events" + getVersionAppendix(version);
    }

    public static String[] getArgumentForResource(final String serverPackage,
                                                  final Element argElement) {
        final String[] arg = new String[2];
        arg[1] = lowerCamelName(argElement.getAttribute("name"));

        final String type = argElement.getAttribute("type");

        if (type.equals("int")) {
            arg[0] = int.class.getName();
        } else if (type.equals("uint")) {
            arg[0] = int.class.getName();
        } else if (type.equals("fixed")) {
            arg[0] = Fixed.class.getName();
        } else if (type.equals("new_id")) {
            arg[0] = int.class.getName();
        } else if (type.equals("object")) {
            final String interfaceName = argElement.getAttribute("interface");
            if (serverPackage.isEmpty()) {
                arg[0] = upperCamelName(interfaceName) + "Resource";
            } else {
                arg[0] = serverPackage + "." + upperCamelName(interfaceName) + "Resource";
            }
        } else if (type.equals("string")) {
            arg[0] = String.class.getName();
        } else if (type.equals("array")) {
            arg[0] = WlArray.class.getName();
        } else if (type.equals("fd")) {
            arg[0] = int.class.getName();
        } else {
            arg[0] = type;
        }

        return arg;
    }

    public static char toSignatureChar(final Element argElement) {
        final String type = argElement.getAttribute("type");

        if (type.equals("int")) {
            return 'i';
        }
        if (type.equals("uint")) {
            return 'u';
        }
        if (type.equals("fixed")) {
            return 'f';
        }
        if (type.equals("new_id")) {
            return 'n';
        }
        if (type.equals("object")) {
            return 'o';
        }
        if (type.equals("string")) {
            return 's';
        }
        if (type.equals("array")) {
            return 'a';
        }
        if (type.equals("fd")) {
            return 'h';
        } else {
            return '?';
        }
    }

    public static String escapeJavaKeyword(final String literal) {
        if (keywords.contains(literal)) {
            return literal + "_";
        } else {
            return literal;
        }
    }

    public static boolean isPrimitive(final String type) {
        return PRIMITIVE_TYPES.contains(type);
    }

    public static String[] getArgumentForProxy(final String clientPackage,
                                               final Element argElement) {
        final String[] arg = new String[2];
        arg[1] = lowerCamelName(argElement.getAttribute("name"));

        final String type = argElement.getAttribute("type");

        if (type.equals("int")) {
            arg[0] = int.class.getName();
        } else if (type.equals("uint")) {
            arg[0] = int.class.getName();
        } else if (type.equals("fixed")) {
            arg[0] = Fixed.class.getName();
        } else if (type.equals("new_id")) {
            final String interfaceName = argElement.getAttribute("interface");
            if (clientPackage.isEmpty()) {
                arg[0] = upperCamelName(interfaceName) + "Proxy";
            } else {
                arg[0] = clientPackage + "." + upperCamelName(interfaceName) + "Proxy";
            }
        } else if (type.equals("object")) {
            final String interfaceName = argElement.getAttribute("interface");
            if (clientPackage.isEmpty()) {
                arg[0] = upperCamelName(interfaceName) + "Proxy";
            } else {
                arg[0] = clientPackage + "." + upperCamelName(interfaceName) + "Proxy";
            }
        } else if (type.equals("string")) {
            arg[0] = String.class.getName();
        } else if (type.equals("array")) {
            arg[0] = WlArray.class.getName();
        } else if (type.equals("fd")) {
            arg[0] = int.class.getName();
        } else {
            arg[0] = type;
        }

        return arg;
    }

    public static String getDoc(final Element interfaceNode) {
        final NodeList descriptionElements = interfaceNode.getElementsByTagName("description");

        if (descriptionElements.getLength() == 0) {
            return "";
        }

        String doc;
        //only get first
        final Element descriptionElement = (Element) descriptionElements.item(0);
        doc = descriptionElement.getAttribute("summary") + "\n\n";
        doc += descriptionElement.getTextContent();
        doc = doc.replace("\n\n",
                "\n<p>\n");
        return doc;
    }
}
