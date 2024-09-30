{
  description = "wayland-java dev setup";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

  outputs =
    { self, nixpkgs }:
    let
      javaVersion = 22;
      overlays = [
        (final: prev: rec {
          jdk = prev."jdk${toString javaVersion}";
          jextract = prev.jextract.overrideAttrs (old: {
            version = "unstable-2024-09-27";
            src = prev.fetchFromGitHub {
              owner = "openjdk";
              repo = "jextract";
              rev = "dd32493be8c8d2553208f87aaaa18870aa7deaca";
              hash = "sha256-7BZBygVX0lPqT+xES8eQw/WC3R1Q9L+p7PnQU1tfIyw=";
            };
            gradleFlags = [
              "-Pllvm_home=${prev.llvmPackages_13.libclang.lib}"
              "-Pjdk22_home=${jdk}"
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
        {
          default = pkgs.mkShell {
            packages = [
              pkgs.jdk22
              pkgs.jextract
              pkgs.gradle
              pkgs.wayland-scanner
              pkgs.python312Packages.pywayland
            ];

            buildInputs = [
              pkgs.wayland
              pkgs.wayland-scanner
              pkgs.wayland-protocols
            ];
            nativeBuildInputs = [ pkgs.pkg-config ];
            inputsFrom = [
              pkgs.wayland
              pkgs.wayland-scanner
              pkgs.wayland-protocols
            ];
            env.LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
              pkgs.wayland
              pkgs.wayland-scanner
              pkgs.wayland-protocols
            ];

          };
        }
      );
    };
}
