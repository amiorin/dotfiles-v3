{ pkgs, lib, config, inputs, ... }:

{
  languages.clojure.enable = true;
  languages.ansible.enable = true;
  languages.opentofu.enable = true;
  packages = [
    pkgs.jet
    pkgs.hcl2json
  ];
}
