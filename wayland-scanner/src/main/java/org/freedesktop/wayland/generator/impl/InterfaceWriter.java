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


import org.freedesktop.wayland.generator.api.Protocol;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.processing.Filer;
import javax.lang.model.element.PackageElement;
import java.io.IOException;
import java.io.Writer;

import static org.freedesktop.wayland.generator.impl.StringUtil.getJavaTypeNameEnum;
import static org.freedesktop.wayland.generator.impl.StringUtil.getJavaTypeNameResource;

public class InterfaceWriter {

    public static final String ELEMENT = "interface";

    public static final String ATTRIBUTE_NAME = "name";


    public void write(final PackageElement packageElement,
                      final Filer filer,
                      final Protocol protocol,
                      final String copyright,
                      final Element interfaceElement) throws IOException {
        final String packageRoot = packageElement.getQualifiedName()
                .toString();
        writeShared(packageElement,
                filer,
                packageRoot + "." + protocol.sharedPackage(),
                copyright,
                interfaceElement);
        if (protocol.generateServer()) {
            writeServer(packageElement,
                    filer,
                    packageRoot + "." + protocol.serverPackage(),
                    copyright,
                    interfaceElement);
        }
        if (protocol.generateClient()) {
            writeClient(packageElement,
                    filer,
                    packageRoot + "." + protocol.clientPackage(),
                    copyright,
                    interfaceElement);
        }
    }

    private void writeShared(final PackageElement packageElement,
                             final Filer filer,
                             final String sharedPackage,
                             final String copyright,
                             final Element interfaceElement) throws IOException {
        final NodeList enums = interfaceElement.getElementsByTagName("enum");
        for (int i = 0; i < enums.getLength(); i++) {
            final Element enumElement = (Element) enums.item(i);
            writeEnum(filer.createSourceFile(getJavaTypeNameEnum(sharedPackage,
                                            interfaceElement,
                                            enumElement),
                                    packageElement)
                            .openWriter(),
                    sharedPackage,
                    copyright,
                    interfaceElement,
                    enumElement);
        }
    }

    private void writeServer(final PackageElement packageElement,
                             final Filer filer,
                             final String serverPackage,
                             final String copyright,
                             final Element interfaceNode) throws IOException {
        writeRequests(filer,
                serverPackage,
                copyright,
                interfaceNode);

        writeResource(filer.createSourceFile(getJavaTypeNameResource(serverPackage,
                                        interfaceNode,
                                        1),
                                packageElement)
                        .openWriter(),
                serverPackage,
                copyright,
                interfaceNode);
    }

    private void writeClient(final PackageElement packageElement,
                             final Filer filer,
                             final String clientPackage,
                             final String copyright,
                             final Element interfaceXMLNode) throws IOException {
        writeProxy(filer.createSourceFile(StringUtil.getJavaTypeNameProxy(clientPackage,
                                        interfaceXMLNode,
                                        1),
                                packageElement)
                        .openWriter(),
                clientPackage,
                copyright,
                interfaceXMLNode);

        writeEvents(filer,
                clientPackage,
                copyright,
                interfaceXMLNode);
    }

    private void writeEnum(final Writer writer,
                           final String sharedPackage,
                           final String copyright,
                           final Element interfaceElement,
                           final Element enumElement) throws IOException {
        new EnumWriter().write(writer,
                sharedPackage,
                copyright,
                interfaceElement,
                enumElement);
        writer.close();
    }

    private void writeRequests(final Filer filer,
                               final String serverPackage,
                               final String copyright,
                               final Element interfaceNode) throws IOException {
        new RequestsWriter().write(filer,
                serverPackage,
                copyright,
                interfaceNode);
    }

    private void writeResource(final Writer writer,
                               final String serverPackage,
                               final String copyright,
                               final Element interfaceNode) throws IOException {
        new ResourceWriter().write(writer,
                serverPackage,
                copyright,
                interfaceNode);
        writer.close();
    }

    private void writeProxy(final Writer writer,
                            final String clientPackage,
                            final String copyright,
                            final Element interfaceXMLNode) throws IOException {
        new ProxyWriter().write(writer,
                clientPackage,
                copyright,
                interfaceXMLNode);
        writer.close();
    }

    private void writeEvents(final Filer filer,
                             final String clientPackage,
                             final String copyright,
                             final Element interfaceXMLNode) throws IOException {
        new EventsWriter().write(filer,
                clientPackage,
                copyright,
                interfaceXMLNode);
    }
}
