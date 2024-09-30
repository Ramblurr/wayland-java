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
import org.freedesktop.wayland.server.Client;
import org.freedesktop.wayland.server.Resource;
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

import static org.freedesktop.wayland.generator.impl.StringUtil.*;

public class ResourceWriter {

    private static final String ELEMENT_REQUEST = "request";
    private static final String ELEMENT_EVENT = "event";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ELEMENT_REQUEST_ATTRIBUTE_VERSION = "version";
    private static final String ELEMENT_ARG = "arg";
    private static final String ATTRIBUTE_SINCE = "since";
    private static final String ATTRIBUTE_ALLOW_NULL = "allow-null";
    private static final String ATTRIBUTE_INTERFACE = "interface";


    public void write(final Writer writer,
                      final String serverPackage,
                      final String copyright,
                      final Element interfaceNode) throws IOException {
        final JavaWriter javaWriter = new JavaWriter(writer);

        //prepare annotations
        final NodeList requestNodes = interfaceNode.getElementsByTagName(ELEMENT_REQUEST);
        final Object[] methods = new Object[requestNodes.getLength()];

        for (int i = 0; i < requestNodes.getLength(); i++) {
            final Element requestElement = (Element) requestNodes.item(i);
            methods[i] = constructMessage(serverPackage,
                    requestElement);
        }

        final NodeList eventNodes = interfaceNode.getElementsByTagName(ELEMENT_EVENT);
        final Object[] events = new Object[eventNodes.getLength()];

        for (int i = 0; i < eventNodes.getLength(); i++) {
            final Element eventElement = (Element) eventNodes.item(i);
            events[i] = constructMessage(serverPackage,
                    eventElement);
        }
        //imports
        javaWriter.emitPackage(serverPackage)
                .emitImports(Arguments.class,
                        Interface.class,
                        Message.class,
                        Nullable.class,
                        Nonnull.class)
                .emitSingleLineComment(copyright.replace("\n",
                        "\n//"));
        //class javadoc
        javaWriter.emitJavadoc(getDoc(interfaceNode));
        //class annotation
        javaWriter.emitAnnotation(Interface.class.getSimpleName(),
                new HashMap<String, Object>() {{
                    put("name",
                            JavaWriter.stringLiteral(interfaceNode.getAttribute(ATTRIBUTE_NAME)));
                    put("version",
                            interfaceNode.getAttribute(ELEMENT_REQUEST_ATTRIBUTE_VERSION));
                    put("methods",
                            methods);
                    put("events",
                            events);
                }});
        //class definition
        final String typeNameResource = getJavaTypeNameResource(serverPackage,
                interfaceNode,
                1);
        javaWriter.beginType(typeNameResource,
                "class",
                EnumSet.of(Modifier.PUBLIC),
                JavaWriter.type(Resource.class,
                        getJavaTypeNameRequests(serverPackage,
                                interfaceNode,
                                1)));
        //interface name
        javaWriter.emitEmptyLine()
                .emitField("String",
                        "INTERFACE_NAME",
                        EnumSet.of(Modifier.PUBLIC,
                                Modifier.STATIC,
                                Modifier.FINAL),
                        JavaWriter.stringLiteral(interfaceNode.getAttribute(ATTRIBUTE_NAME)));
        //constructors
        javaWriter.emitEmptyLine()
                .beginConstructor(EnumSet.of(Modifier.PUBLIC),
                        Client.class.getName(),
                        "client",
                        int.class.getName(),
                        "version",
                        int.class.getName(),
                        "id",
                        getJavaTypeNameRequests(serverPackage,
                                interfaceNode,
                                1),
                        "implementation")
                .emitStatement("super(client, version, id, implementation)")
                .endConstructor()
                .emitEmptyLine()
                .beginConstructor(EnumSet.of(Modifier.PUBLIC),
                        MemorySegment.class.getName(),
                        "pointer")
                .emitStatement("super(pointer)")
                .endConstructor()
                .emitEmptyLine();

        //methods
        for (int i = 0; i < eventNodes.getLength(); i++) {
            final Element eventElement = (Element) eventNodes.item(i);
            String javaDoc = getDoc(eventElement);

            //construct java method arg types & names
            final String eventName = eventElement.getAttribute(ATTRIBUTE_NAME);
            final NodeList argElements = eventElement.getElementsByTagName(ELEMENT_ARG);
            final String sinceAttr = eventElement.getAttribute(ATTRIBUTE_SINCE);
            final String since = sinceAttr.isEmpty() ? "1" : sinceAttr;
            final String[] args = new String[argElements.getLength() * 2];
            for (int j = 0; j < argElements.getLength(); j++) {
                final Element argElement = (Element) argElements.item(j);
                final boolean allowNull = Boolean.valueOf(argElement.getAttribute(ATTRIBUTE_ALLOW_NULL));
                final String[] argumentForResource = getArgumentForResource(serverPackage,
                        argElement);
                final int k = j * 2;
                String argumentType = argumentForResource[0];
                if (!StringUtil.isPrimitive(argumentType)) {
                    argumentType = allowNull ?
                            "@" + javaWriter.compressType(Nullable.class.getSimpleName()) + " " + argumentType :
                            "@" + javaWriter.compressType(Nonnull.class.getSimpleName()) + " " + argumentType;
                }
                args[k] = argumentType;
                args[k + 1] = StringUtil.escapeJavaKeyword(argumentForResource[1]);

                javaDoc += "\n@param " + args[k + 1] + " " + argElement.getAttribute("summary");
            }
            //method javadoc
            javaWriter.emitEmptyLine()
                    .emitJavadoc(javaDoc);
            //actual method
            javaWriter.beginMethod("void",
                    lowerCamelName(eventName),
                    EnumSet.of(Modifier.PUBLIC),
                    args);

            if (Integer.parseInt(since) > 1) {
                javaWriter.emitEmptyLine()
                        .beginControlFlow("if (getVersion() < %s)",
                                since)
                        .emitStatement("throw new UnsupportedOperationException("
                                        + "\"This object is version \"+getVersion()+\" while version %s is required for this operation.\")",
                                since)
                        .endControlFlow();
            }

            final String[] argValues = new String[argElements.getLength()];
            if (argValues.length > 0) {
                for (int j = 0; j < argValues.length; j++) {
                    final int k = (j * 2) + 1;
                    argValues[j] = ".set(" + j + ", " + args[k] + ")";
                }
                String arguments = "";
                for (final String argValue : argValues) {
                    arguments += argValue;
                }
                javaWriter.emitStatement("postEvent(%d,Arguments.create(%d)%s)",
                        i,
                        argValues.length,
                        arguments);
            } else {
                javaWriter.emitStatement("postEvent(%d)",
                        i);
            }
            javaWriter.endMethod();
        }
        //end class
        javaWriter.endType();
    }

    private String constructMessage(final String serverPackage,
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
            types[i] = getArgumentForResource(serverPackage,
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
