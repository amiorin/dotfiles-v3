{ pkgs, lib, config, inputs, ... }:

{
  packages = [ pkgs.git pkgs.babashka ];
  languages.ansible.enable = true;
  languages.clojure.enable = true;
}
