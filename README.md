# Intro
Use big-config to manage your dotfiles.

``` shell
bb tasks
echo "export DOTFILES_PROFILE=macos" | tee -a .envrc.private

bb install
bb install macos|ubuntu
bb diff
bb diff macos|ubuntu
bb render
bb render all
bb render macos|ubuntu
```

## License

Copyright © 2025 Alberto Miorin

amiorin/dotfiles-v3 is released under the [MIT License](https://opensource.org/licenses/MIT).
