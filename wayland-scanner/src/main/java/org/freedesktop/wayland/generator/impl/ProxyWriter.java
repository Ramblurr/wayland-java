//Copyright 2015 Erik De Rijcke
//
//Licensed under the Apache License,Version2.0(the"License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,software
//distributed under the License is distributed on an"AS IS"BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
package org.freedesktop.wayland.generator.impl;

import com.squareup.javawriter.JavaWriter;
import org.freedesktop.wayland.client.Display;
import org.freedesktop.wayland.client.Proxy;
import org.freedesktop.wayland.util.Arguments;
import org.freedesktop.wayland.util.Interface;
import org.freedesktop.wayland.util.Message;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.foreign.MemorySegment;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.freedesktop.wayland.generator.impl.StringUtil.*;

public class ProxyWriter {

    private static final String ELEMENT_REQUEST = "request";
    private static final String ELEMENT_EVENT = "event";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_VERSION = "version";
    private static final String ATTRIBUTE_INTERFACE = "interface";
    private static final String ATTRIBUTE_SINCE = "since";
    private static final String ELEMENT_ARG = "arg";
    private static final String ATTRIBUTE_ALLOW_NULL = "allow-null";

    public void write(final Writer writer,
                      final String clientPackage,
                      final String copyright,
                      final Element interfaceNode) throws IOException {
        final JavaWriter javaWriter = new JavaWriter(writer);
        final String interfaceName = interfaceNode.getAttribute(ATTRIBUTE_NAME);
        //prepare annotations
        final String versionAttr = interfaceNode.getAttribute(ATTRIBUTE_VERSION);
        final NodeList requestNodes = interfaceNode.getElementsByTagName(ELEMENT_REQUEST);
        final Object[] methods = new Object[requestNodes.getLength()];

        for (int i = 0; i < requestNodes.getLength(); i++) {
            final Element requestElement = (Element) requestNodes.item(i);
            methods[i] = constructMessage(clientPackage,
                    requestElement);
        }

        final NodeList eventNodes = interfaceNode.getElementsByTagName(ELEMENT_EVENT);
        final Object[] events = new Object[eventNodes.getLength()];

        for (int i = 0; i < eventNodes.getLength(); i++) {
            final Element eventElement = (Element) eventNodes.item(i);
            events[i] = constructMessage(clientPackage,
                    eventElement);
        }

        //imports
        javaWriter.emitPackage(clientPackage)
                .emitImports(Arguments.class,
                        Interface.class,
                        Message.class,
                        Nullable.class,
                        Nonnull.class)
                .emitSingleLineComment(copyright.replace("\n",
                        "\n//"));
        //class javadoc
        String typeDoc = getDoc(interfaceNode);
        if (interfaceName.equals("wl_display")) {
            typeDoc += "\n<p>\n@see " + javaWriter.compressType(Display.class.getName());
        }
        javaWriter.emitJavadoc(typeDoc);
        //class annotation
        javaWriter.emitAnnotation(Interface.class.getSimpleName(),
                new HashMap<String, Object>() {{
                    put("name",
                            JavaWriter.stringLiteral(interfaceName));
                    put("version",
                            versionAttr);
                    put("methods",
                            methods);
                    put("events",
                            events);
                }});
        final String extendsType;
        //special case for wl_display proxy
        if (interfaceName.equals("wl_display")) {
            extendsType = Display.class.getName();
        } else {
            extendsType = JavaWriter.type(Proxy.class,
                    getJavaTypeNameEvents(clientPackage,
                            interfaceNode,
                            1));
        }

        //class definition
        javaWriter.beginType(getJavaTypeNameProxy(clientPackage,
                        interfaceNode,
                        1),
                "class",
                EnumSet.of(Modifier.PUBLIC),
                extendsType);
        //interface name
        javaWriter.emitEmptyLine()
                .emitField("String",
                        "INTERFACE_NAME",
                        EnumSet.of(Modifier.PUBLIC,
                                Modifier.STATIC,
                                Modifier.FINAL),
                        JavaWriter.stringLiteral(interfaceName));

        //constructor
        //special case for wl_display proxy
        if (interfaceName.equals("wl_display")) {
            javaWriter.emitEmptyLine()
                    .beginConstructor(EnumSet.of(Modifier.PUBLIC),
                            MemorySegment.class.getName(),
                            "pointer")
                    .emitStatement("super(pointer)")
                    .endConstructor();
        } else {
            javaWriter.emitEmptyLine()
                    .beginConstructor(EnumSet.of(Modifier.PUBLIC),
                            MemorySegment.class.getName(),
                            "pointer",
                            getJavaTypeNameEvents(clientPackage,
                                    interfaceNode,
                                    1),
                            "implementation",
                            int.class.getName(),
                            "version")
                    .emitStatement("super(pointer, implementation, version)")
                    .endConstructor()
                    .emitEmptyLine()
                    .beginConstructor(EnumSet.of(Modifier.PUBLIC),
                            MemorySegment.class.getName(),
                            "pointer")
                    .emitStatement("super(pointer)")
                    .endConstructor();
        }
        //methods
        for (int i = 0; i < requestNodes.getLength(); i++) {
            final Element requestElement = (Element) requestNodes.item(i);

            //construct java method return, arg types & names
            final String requestName = requestElement.getAttribute(ATTRIBUTE_NAME);
            final NodeList argElements = requestElement.getElementsByTagName(ELEMENT_ARG);
            final String sinceAttr = requestElement.getAttribute(ATTRIBUTE_SINCE);
            final String since = sinceAttr.isEmpty() ? "1" : sinceAttr;

            final LinkedList<String> args = new LinkedList<>();
            String returnType = "void";
            String implementationType;

            String javaDoc = getDoc(requestElement);

            for (int j = 0; j < argElements.getLength(); j++) {
                final Element argElement = (Element) argElements.item(j);
                final String[] argumentForProxy = getArgumentForProxy(clientPackage,
                        argElement);
                final String type = argElement.getAttribute("type");
                if (type.equals("new_id")) {
                    returnType = argumentForProxy[0];
                    final String argInterfaceName = argElement.getAttribute(ATTRIBUTE_INTERFACE);
                    if (argInterfaceName.isEmpty()) {
                        returnType = "<J,T extends " + returnType + "<J>> T";
                        implementationType = "J";
                        args.add("Class<T>");
                        args.add("proxyType");
                        args.add(int.class.getName());
                        args.add("version");

                        javaDoc += "\n@param proxyType The type of proxy to create. Must be a subclass of Proxy.";
                        javaDoc += "\n@param version The protocol version to use. Must not be higher than what the supplied implementation can support.";
                    } else {
                        implementationType = clientPackage + "." + upperCamelName(argInterfaceName) + "Events";
                    }
                    args.add(implementationType);
                    args.add("implementation");

                    javaDoc += "\n@param implementation A protocol event listener for the newly created proxy.";
                } else {
                    final boolean allowNull = Boolean.valueOf(argElement.getAttribute(ATTRIBUTE_ALLOW_NULL));
                    String argumentType = argumentForProxy[0];
                    if (!StringUtil.isPrimitive(argumentType)) {
                        argumentType = allowNull ?
                                "@" + javaWriter.compressType(Nullable.class.getSimpleName()) + " "
                                        + argumentType :
                                "@" + javaWriter.compressType(Nonnull.class.getSimpleName()) + " "
                                        + argumentType;
                    }
                    args.add(argumentType);
                    args.add(StringUtil.escapeJavaKeyword(argumentForProxy[1]));

                    javaDoc += "\n@param " + args.getLast() + " " + argElement.getAttribute("summary");
                }
            }
            //method javadoc
            javaWriter.emitEmptyLine()
                    .emitJavadoc(javaDoc);
            //actual method
            final String[] argParams = args.toArray(new String[args.size()]);
            javaWriter.beginMethod(returnType,
                    lowerCamelName(requestName),
                    EnumSet.of(Modifier.PUBLIC),
                    argParams);

            final List<String> argValues = new LinkedList<>();
            int argIndex = 0;
            for (int j = 0; j < argParams.length / 2; j++) {
                final int k = (j * 2) + 1;
                String argParam = argParams[k];
                if (argParam.equals("implementation")) {
                    //new_id expects a NULL argument
                    argParam = "0";
                }
                if (argParam.equals("proxyType")) {
                    argParam = "proxyType.getAnnotation(Interface.class).name()";
                }
                argValues.add(".set(" + argIndex + ", " + argParam + ")");
                argIndex++;
            }

            //runtime version check
            if (Integer.parseInt(since) > 1) {
                javaWriter.beginControlFlow("if (getVersion() < %s)",
                                since)
                        .emitStatement("throw new UnsupportedOperationException("
                                        + "\"This object is version \"+getVersion()+\" while version %s is required for this operation.\")",
                                since)
                        .endControlFlow();
            }

            if (argValues.size() > 0) {
                String arguments = "";
                for (final String argValue : argValues) {
                    arguments += argValue;
                }
                if (returnType.equals("void")) {
                    javaWriter.emitStatement("marshal(%d, Arguments.create(%d)%s)",
                            i,
                            argValues.size(),
                            arguments);
                } else {
                    final boolean specialConstructor = returnType.startsWith("<J,T extends ");
                    returnType = specialConstructor ? "proxyType" : returnType + ".class";
                    final String version = specialConstructor ? "version" : "getVersion()";
                    javaWriter.emitStatement("return marshalConstructor(%d, implementation, %s, %s, Arguments.create(%d)%s)",
                            i,
                            version,
                            returnType,
                            argValues.size(),
                            arguments);
                }
            } else {
                if (returnType.equals("void")) {
                    javaWriter.emitStatement("marshal(%d)",
                            i);
                } else {
                    //normally this situation should not happen as new_id always adds a NULL argument
                    throw new IllegalStateException("Got zero argument marshal constructor invocation.");
                }
            }
            javaWriter.endMethod();
        }

        //special case for wl_display proxy
        if (interfaceName.equals("wl_display")) {
            javaWriter.emitEmptyLine()
                    .emitJavadoc("Connect to a Wayland display\n<p>\n"
                            + "Connect to the Wayland display named 'name'. If 'name' is 'null',\n"
                            + "its value will be replaced with the WAYLAND_DISPLAY environment\n"
                            + "variable if it is set, otherwise display \"wayland-0\" will be used.\n"
                            + "@param name Name of the Wayland display to connect to\n"
                            + "@return A {@code WlDisplayProxy} object or null on failure."
                    )
                    .beginMethod(getJavaTypeNameProxy(clientPackage,
                                    interfaceNode,
                                    1),
                            "connect",
                            EnumSet.of(Modifier.PUBLIC,
                                    Modifier.STATIC),
                            "String",
                            "name")
                    .emitStatement(
                            "return new %s(Display._connect(name))",
                            getSimpleJavaTypeNameProxy(interfaceNode, 1))
                    .endMethod()
                    .emitEmptyLine()
                    .emitJavadoc("Connect to Wayland display on an already open fd\n<p>\n"
                            + "The {@code WlDisplayProxy} takes ownership of the fd and will close it when the\n"
                            + "display is destroyed.  The fd will also be closed in case of\n"
                            + "failure.\n"
                            + "@param fd The fd to use for the connection\n"
                            + "@return A {@code WlDisplayProxy} object or null on failure.")
                    .beginMethod(getJavaTypeNameProxy(clientPackage,
                                    interfaceNode,
                                    1),
                            "connectToFd",
                            EnumSet.of(Modifier.PUBLIC,
                                    Modifier.STATIC),
                            int.class.getName(),
                            "fd")
                    .emitStatement(
                            "return new %s(Display._connect(fd))",
                            getSimpleJavaTypeNameProxy(interfaceNode,
                                    1))
                    .endMethod();
            javaWriter.emitEmptyLine()
                    .beginMethod(Void.class.getName(),
                            "getImplementation",
                            EnumSet.of(Modifier.PUBLIC))
                    .emitStatement("throw new UnsupportedOperationException(\"Implementation provided by native library\")")
                    .endMethod();
        }
        //end class
        javaWriter.endType();
    }


    private String constructMessage(final String clientPackage,
                                    final Element requestElement) throws IOException {
        final String requestName = requestElement.getAttribute(ATTRIBUTE_NAME);
        final String since = requestElement.getAttribute(ATTRIBUTE_SINCE);
        final NodeList argElements = requestElement.getElementsByTagName(ELEMENT_ARG);

        final StringBuffer signatureBuilder = new StringBuffer(since);
        final String[] types = new String[argElements.getLength()];

        for (int i = 0; i < argElements.getLength(); i++) {
            final Element arg = (Element) argElements.item(i);

            final boolean allowNull = Boolean.valueOf(arg.getAttribute(ATTRIBUTE_ALLOW_NULL));
            if (allowNull) {
                signatureBuilder.append('?');
            }
            final char signatureArg = toSignatureChar(arg);
            if (signatureArg == 'n' && arg.getAttribute(ATTRIBUTE_INTERFACE)
                    .isEmpty()) {
                signatureBuilder.append("su");
            }
            signatureBuilder.append(signatureArg);
            types[i] = getArgumentForProxy(clientPackage,
                    arg)[0] + ".class";
        }

        final StringWriter stringWriter = new StringWriter();
        final JavaWriter messageWriter = new JavaWriter(stringWriter);
        messageWriter.emitPackage("");
        messageWriter.emitAnnotation(Message.class.getSimpleName(),
                new HashMap<String, Object>() {{
                    put("signature",
                            JavaWriter.stringLiteral(signatureBuilder.toString()));
                    put("name",
                            JavaWriter.stringLiteral(requestName));
                    put("types",
                            types);
                    put("functionName",
                            JavaWriter.stringLiteral(StringUtil.lowerCamelName(requestName)));
                }});
        messageWriter.close();

        return stringWriter.toString();
    }
}
