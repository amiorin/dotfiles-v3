#!/bin/bash

# Raycast Script Command Template
#
# Duplicate this file and remove ".template." from the filename to get started.
# See full documentation here: https://github.com/raycast/script-commands
#
# Required parameters:
# @raycast.schemaVersion 1
# @raycast.title Emacs
# @raycast.mode silent
#
# Optional parameters:
# @raycast.icon https://raw.githubusercontent.com/c9rgreen/emacs-macos-icon/refs/heads/main/emacs.iconset/icon_512x512%402x%402x.png
# @raycast.packageName Emacs Plus

ps -e -o comm | grep Emacs && open -a Emacs
emacsclient -a '' -c
