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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;

import static org.freedesktop.wayland.generator.impl.StringUtil.*;

public class EventsWriter {

    private static final String ELEMENT_EVENT = "event";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_VERSION = "version";
    private static final String ATTRIBUTE_SINCE = "since";
    private static final String ELEMENT_ARG = "arg";
    private static final String ATTRIBUTE_ALLOW_NULL = "allow-null";


    public void write(final Filer filer,
                      final String clientPackage,
                      final String copyright,
                      final Element interfaceNode) throws IOException {
        final int maxVersion = Integer.parseInt(interfaceNode.getAttribute(ATTRIBUTE_VERSION));
        for (int version = 1; version <= maxVersion; version++) {
            writeVersion(filer,
                    clientPackage,
                    copyright,
                    interfaceNode,
                    version);
        }
    }

    private void writeVersion(final Filer filer,
                              final String clientPackage,
                              final String copyright,
                              final Element interfaceNode,
                              final int version) throws IOException {


        final Writer writer = filer.createSourceFile(getJavaTypeNameEvents(clientPackage,
                        interfaceNode,
                        version))
                .openWriter();
        final JavaWriter javaWriter = new JavaWriter(writer);

        //imports
        javaWriter.emitPackage(clientPackage)
                .emitImports(Nullable.class,
                        Nonnull.class)
                .emitSingleLineComment(copyright.replace("\n",
                        "\n//"));
        //class javadoc
        javaWriter.emitJavadoc(getDoc(interfaceNode));
        //begin type
        final String extendsType;
        if (version > 1) {
            extendsType = getJavaTypeNameEvents(clientPackage,
                    interfaceNode,
                    version - 1);
        } else {
            extendsType = null;
        }

        javaWriter.beginType(getJavaTypeNameEvents(clientPackage,
                        interfaceNode,
                        version),
                "interface",
                EnumSet.of(Modifier.PUBLIC),
                extendsType);
        //version constant
        javaWriter.emitField(int.class.getName(),
                "VERSION",
                EnumSet.noneOf(Modifier.class),
                Integer.toString(version));

        //methods
        final NodeList eventNodes = interfaceNode.getElementsByTagName(ELEMENT_EVENT);
        for (int i = 0; i < eventNodes.getLength(); i++) {
            final Element requestElement = (Element) eventNodes.item(i);

            final String sinceAttr = requestElement.getAttribute(ATTRIBUTE_SINCE);
            final int since = Integer.parseInt(sinceAttr.isEmpty() ? "1" : sinceAttr);
            if (since > version) {
                continue;
            }

            final String requestName = requestElement.getAttribute(ATTRIBUTE_NAME);
            final NodeList argElements = requestElement.getElementsByTagName(ELEMENT_ARG);
            //construct java method arg types & names
            final String[] args = new String[((argElements.getLength() + 1) * 2)];
            args[0] = getJavaTypeNameProxy(clientPackage,
                    interfaceNode,
                    1);
            args[1] = "emitter";

            String javaDoc = getDoc(requestElement);
            javaDoc += "\n@param " + args[1] + " The protocol object that emitted the event.";

            for (int j = 0; j < argElements.getLength(); j++) {
                final Element argElement = (Element) argElements.item(j);
                final boolean allowNull = Boolean.valueOf(argElement.getAttribute(ATTRIBUTE_ALLOW_NULL));
                final String[] argumentForProxy = getArgumentForProxy(clientPackage,
                        argElement);
                final int k = (j + 1) * 2;
                String argumentType = argumentForProxy[0];
                if (!StringUtil.isPrimitive(argumentType)) {
                    argumentType = allowNull ?
                            "@" + javaWriter.compressType(Nullable.class.getSimpleName()) + " " + argumentType :
                            "@" + javaWriter.compressType(Nonnull.class.getSimpleName()) + " " + argumentType;
                }
                args[k] = argumentType;
                args[k + 1] = StringUtil.escapeJavaKeyword(argumentForProxy[1]);

                javaDoc += "\n@param " + args[k + 1] + " " + argElement.getAttribute("summary");
            }
            //method javadoc
            javaWriter.emitEmptyLine()
                    .emitJavadoc(javaDoc);
            //actual method
            javaWriter.beginMethod("void",
                    lowerCamelName(requestName),
                    EnumSet.of(Modifier.PUBLIC),
                    args);
            javaWriter.endMethod();
        }

        //end type
        javaWriter.endType();
        javaWriter.close();
    }
}
