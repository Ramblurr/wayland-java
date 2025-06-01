package org.freedesktop.wayland.generator.impl;

import org.freedesktop.wayland.generator.api.WaylandCustomProtocol;
import org.freedesktop.wayland.generator.api.WaylandProtocols;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class ProtocolXmlPathResolver {

    private static Path requireNonNullProtocolRoot(@Nullable Path protocolRoot) {
        if (protocolRoot == null)
            throw new NullPointerException("Please provide the wayland.scanner.protocol.root option to the annotation processor to use paths without a pkg-config");
        return protocolRoot;
    }

    public static List<File> resolvePaths(WaylandProtocols protocol, @Nullable Path protocolRoot) {
        Path dataDir;
        if (protocol.pkgConfig().isEmpty()) {
            dataDir = requireNonNullProtocolRoot(protocolRoot).resolve(protocol.path());
        } else {
            var maybeDataDir = pkgConfigDataDir(protocol.pkgConfig());
            if (maybeDataDir.isEmpty())
                return List.of();
            dataDir = Path.of(maybeDataDir.get());
        }

        List<File> result = new LinkedList<>();
        if (protocol.withStable()) {
            result.addAll(resolveNestedPaths(dataDir.resolve("stable")));
        }
        if (protocol.withStaging()) {
            result.addAll(resolveNestedPaths(dataDir.resolve("staging")));
        }
        if (protocol.withUnstable()) {
            result.addAll(resolveNestedPaths(dataDir.resolve("unstable")));
        }
        return result;
    }

    public static List<File> resolveNestedPaths(Path folderPath) {
        var folderFile = folderPath.toFile();
        if (!folderFile.exists() || !folderFile.isDirectory() || !folderFile.canRead())
            return List.of();
        var dirContents = folderPath.toFile().listFiles();
        if (dirContents == null)
            return List.of();
        List<File> results = new LinkedList<>();
        for (var file : dirContents) {
            if (file.isDirectory()) {
                var xmlFiles = file.listFiles((_, name) -> name.endsWith(".xml"));
                if (xmlFiles == null)
                    continue;
                results.addAll(Arrays.stream(xmlFiles).toList());
            }
        }
        return results;
    }

    public static File resolvePath(WaylandCustomProtocol protocol, @Nullable Path protocolRoot) {
        System.out.println(String.format("resolving protocol annotation %s", protocol));
        File path = null;
        if (Objects.equals(protocol.pkgConfig(), "")) {
            path = requireNonNullProtocolRoot(protocolRoot).resolve(protocol.path()).toFile();
        } else {
            var maybePath = resolveFromPkgConfig(protocol.path(), protocol.pkgConfig());
            if (maybePath.isPresent()) {
                path = maybePath.get();
            }
        }

        if (path != null && path.isFile() && path.canRead()) {
            return path;
        }

        throw new RuntimeException(String.format("Cannot locate protocol xml for path=%s pkgConfig=%s, pkgConfigDataDirOutput=", protocol.path(), protocol.pkgConfig(),
                pkgConfigDataDir(protocol.pkgConfig())
        ));
    }

    public static Optional<File> resolvePath(final String potentialPath) {
        var path = new File(potentialPath);
        if (path.exists() && path.canRead()) {
            return Optional.of(path);
        }
        return Optional.empty();
    }

    public static Optional<File> resolvePkgConfig(String potentialPath, String pkgConfigPackage) {
        var path = resolveFromPkgConfig(potentialPath, pkgConfigPackage);
        if (path.isEmpty())
            return Optional.empty();
        if (path.get().exists() && path.get().canRead()) {
            return path;
        }
        return Optional.empty();
    }

    public static Optional<File> resolveFromPkgConfig(String potentialPath, String pkgConfigPackage) {
        var maybePkgConfigDataDir = pkgConfigDataDir(pkgConfigPackage);
        if (maybePkgConfigDataDir.isEmpty())
            return Optional.empty();
        String pkgConfigDataDir = maybePkgConfigDataDir.get();
        var ret = Path.of(pkgConfigDataDir, potentialPath).toFile();
        return Optional.of(ret);
    }

    public static Optional<String> pkgConfigDataDir(String packageName) {
        var process = new ProcessBuilder(
                "pkg-config",
                "--variable=pkgdatadir",
                packageName
        );
        process.redirectErrorStream(true);
        try {
            Process start = process.start();
            InputStream is = start.getInputStream();
            start.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                return Optional.of(line);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
