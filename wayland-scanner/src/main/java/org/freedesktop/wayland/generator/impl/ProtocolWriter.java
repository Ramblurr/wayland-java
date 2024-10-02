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

import org.freedesktop.wayland.generator.api.WaylandCustomProtocol;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.processing.Filer;
import javax.lang.model.element.PackageElement;
import java.io.IOException;

public class ProtocolWriter {

    public static final String ELEMENT_COPYRIGHT = "copyright";

    private static final String ELEMENT_PROTOCOL_ATTRIBUTE_NAME = "name";

    public void write(final PackageElement packageElement,
                      final Filer filer,
                      final ProtocolGenConfig config,
                      final Element protocolElement) throws IOException {
        //TODO do something with name?
        final String name = protocolElement.getAttribute(ELEMENT_PROTOCOL_ATTRIBUTE_NAME);

        String copyright = "";
        final NodeList copyrightElements = protocolElement.getElementsByTagName(ELEMENT_COPYRIGHT);
        for (int i = 0; i < copyrightElements.getLength(); i++) {
            final Element copyrightElement = (Element) copyrightElements.item(i);
            copyright += "\n" + copyrightElement.getTextContent();
        }

        final NodeList interfaceElements = protocolElement.getElementsByTagName(InterfaceWriter.ELEMENT);
        for (int i = 0; i < interfaceElements.getLength(); i++) {
            final Element interfaceElement = (Element) interfaceElements.item(i);
            new InterfaceWriter().write(packageElement,
                    filer,
                    config,
                    copyright,
                    interfaceElement);
        }
    }
}
