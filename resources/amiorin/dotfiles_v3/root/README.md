# Video
* My endgame 2023. https://www.youtube.com/watch?v=8qOSunEy3jU

# ferris sweep v2
```
+	~	{	}	%		&	@	<	>	|
_	"	(	)	!		?	:	^	$
-	=	[	]	#		*	;	`	\	/
```

# Other apps

``` sh
brew install --cask spaceid
brew install --cask keycastr
brew install --cask raycast
brew install --cask monitorcontrol
brew install --cask wezterm
```

# WezTerm and Zellij prev and next tab
https://github.com/wez/wezterm/issues/4251#issuecomment-1718239499

# Fix WezTerm and global shortcuts
Some shortcuts has to be redefined in `System Settings` and not in the app.
Chrome and Zellij use `shift-cmd-[`, therefore WezTerm has to use `cmd-[`.
Minimizing app has to be remapped to something not used.

```
All Applications
Show Help menu shift-cmd-/
Minimise ctrl-shift-cmd-m
Minimize ctrl-shift-cmd-m
WezTerm
Window->Select Tab->Activate the tab to the left  cmd-[
Window->Select Tab->Activate the tab to the right cmd-]

```

# Accents
https://apple.stackexchange.com/a/332770

# Script
* RSI and small keyboards
* https://peppe.rs/posts/programming_on_34_keys/
* https://precondition.github.io/home-row-mods
* https://keymapdb.com/keymaps/callum_oakley/
* https://karabiner-elements.pqrs.org/
* https://github.com/kmonad/kmonad
* https://www.amazon.com/Koolertron-Portable-Programmable-Mechanical-Backlight/dp/B0C14KGKQW?th=1
* https://monkeytype.com/ 
* https://www.reddit.com/r/mechmarket/comments/15kjtnf/euse_h_dz60_five_degree_case_kyria_rev_14_382_key/
  * 20 gr
* VIAL
* yabai and ferris sweep
* the journey
  * home row mods
  * combo


# My config files
* https://github.com/wez/wezterm
* https://fishshell.com/
* https://github.com/koekeishiya/yabai
* https://github.com/koekeishiya/skhd
* https://github.com/tmux/tmux

# TODO
* Add raycast
* Add applescript to start alacritty with raycast
* Add homerow
* Add MonitorControl
* Add SpaceId
* Add https://github.com/amiorin/happy-yabai

```sh
# editing mode
micromamba env create -f environment.yml --prefix ./.conda
fish_add_path .conda/bin/
pip install pywatchman
watchman-make -p '**' -t install

# check drift
make status

# install changes
make install
```

# Macos settings
![My awesome image](https://github.com/amiorin/dotfiles-v2/blob/8f449d3c2f7a0a65c7a29cb2dfb04471f5098a7b/Keyboard.png)
