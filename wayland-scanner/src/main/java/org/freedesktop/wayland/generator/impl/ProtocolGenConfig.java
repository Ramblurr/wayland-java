package org.freedesktop.wayland.generator.impl;

import org.freedesktop.wayland.generator.api.WaylandCoreProtocols;
import org.freedesktop.wayland.generator.api.WaylandCustomProtocol;
import org.freedesktop.wayland.generator.api.WaylandProtocols;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import java.io.File;

public record ProtocolGenConfig(String sharedPackage, String clientPackage, String serverPackage,
                                boolean generateClient, boolean generateServer, boolean validateDtd,
                                Element element, PackageElement packageElement,
                                File protocolFile,
                                WaylandCustomProtocol customProtocol
) {
    public ProtocolGenConfig(WaylandCustomProtocol protocol,
                             Element element, PackageElement packageElement,
                             File protocolFile) {
        this(protocol.sharedPackage(), protocol.clientPackage(), protocol.serverPackage(), protocol.generateClient(), protocol.generateServer(), protocol.dtd(),
                element, packageElement, protocolFile, protocol);
    }

    public ProtocolGenConfig(WaylandCoreProtocols protocol,
                             Element element, PackageElement packageElement,
                             File protocolFile) {
        this(protocol.sharedPackage(), protocol.clientPackage(), protocol.serverPackage(), protocol.generateClient(), protocol.generateServer(), false,
                element, packageElement,
                protocolFile, null);
    }

    public ProtocolGenConfig(WaylandProtocols protocol,

                             Element element, PackageElement packageElement,
                             File protocolFile) {
        this(protocol.sharedPackage(), protocol.clientPackage(), protocol.serverPackage(), protocol.generateClient(), protocol.generateServer(), false,
                element, packageElement,
                protocolFile, null);
    }

    @Override
    public String toString() {
        return "ProtocolGenConfig{" +
                "sharedPackage='" + sharedPackage + '\'' +
                ", clientPackage='" + clientPackage + '\'' +
                ", serverPackage='" + serverPackage + '\'' +
                ", generateClient=" + generateClient +
                ", generateServer=" + generateServer +
                ", validateDtd=" + validateDtd +
                ", element=" + element +
                ", packageElement=" + packageElement +
                ", protocolFile=" + protocolFile +
                ", customProtocol=" + customProtocol +
                '}';
    }
}
