{
  description = "wayland-java dev setup";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  outputs =
    { self, nixpkgs }:
    let
      javaVersion = toString 22;
      overlays = [
        (final: prev: rec {
          jdk = prev."jdk${javaVersion}";
          jextract = prev.jextract.overrideAttrs (old: {
            version = "unstable-2024-09-27";
            src = prev.fetchFromGitHub {
              owner = "openjdk";
              repo = "jextract";
              rev = "dd32493be8c8d2553208f87aaaa18870aa7deaca";
              hash = "sha256-7BZBygVX0lPqT+xES8eQw/WC3R1Q9L+p7PnQU1tfIyw=";
            };
            gradleFlags = [
              # as of time of writing jextract requires llvm 13!
              "-Pllvm_home=${prev.llvmPackages_13.libclang.lib}"
              "-Pjdk${javaVersion}_home=${jdk}"
            ];
          });
        })
      ];
      supportedSystems = [
        "x86_64-linux"
        "aarch64-linux"
      ];
      forEachSupportedSystem =
        f:
        nixpkgs.lib.genAttrs supportedSystems (
          system:
          f {
            pkgs = import nixpkgs { inherit overlays system; };
          }
        );
    in
    {
      devShells = forEachSupportedSystem (
        { pkgs }:
        let
          packages = [
            pkgs."jdk${javaVersion}"
            pkgs.jextract
            pkgs.gradle
            pkgs.wayland-scanner
            pkgs.python312Packages.pywayland
          ];
          libraries = [
            pkgs.wayland
            pkgs.wayland-scanner
            pkgs.wayland-protocols
          ];
        in
        {
          default = pkgs.mkShell {
            packages = packages;
            buildInputs = libraries;
            nativeBuildInputs = [ pkgs.pkg-config ];
            inputsFrom = libraries;
            env.LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath libraries;
          };
        }
      );
    };
}
