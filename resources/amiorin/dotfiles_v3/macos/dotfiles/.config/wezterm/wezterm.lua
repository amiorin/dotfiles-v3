-- https://hackernoon.com/get-the-most-out-of-your-terminal-a-comprehensive-guide-to-wezterm-configuration
-- Pull in the wezterm API
local wezterm = require 'wezterm'
-- local mux = wezterm.mux
local act = wezterm.action

-- This table will hold the configuration.
local config = {}

-- In newer versions of wezterm, use the config_builder which will
-- help provide clearer error messages
if wezterm.config_builder then
  config = wezterm.config_builder()
end

-- This is where you actually apply your config choices
config.window_padding = {
  left = 0,
  right = 0,
  top = 0,
  bottom = 0,
}

config.hide_tab_bar_if_only_one_tab = true

config.enable_kitty_keyboard = true

-- For example, changing the color scheme:
--config.color_scheme = 'Batman'
-- Spawn a fish shell in login mode
config.default_prog = { '/opt/homebrew/bin/fish', '-l' }
config.font = wezterm.font 'JetBrainsMonoNL Nerd Font Mono'
config.font_size = 16
config.harfbuzz_features = { 'calt=0', 'clig=0', 'liga=0' }
config.disable_default_key_bindings = true
config.keys = {
  { key = 'w', mods = 'ALT|SHIFT', action = act.CloseCurrentPane { confirm = false },},
  { key = 'n', mods = 'ALT|SHIFT', action = act.SpawnWindow },
  { key = 't', mods = 'ALT|SHIFT', action = act.SpawnTab 'CurrentPaneDomain' },
  { key = 'i', mods = 'ALT|SHIFT', action = act.ActivateTabRelative(-1) },
  { key = 'o', mods = 'ALT|SHIFT', action = act.ActivateTabRelative(1) },
  { key = 'v', mods = 'CMD', action = act.PasteFrom 'Clipboard' },
}
config.key_tables = {
}
config.adjust_window_size_when_changing_font_size = false
config.window_decorations = "RESIZE"
config.color_scheme = 'Afterglow'
config.window_close_confirmation = 'NeverPrompt'
config.command_palette_font_size = 16
config.quit_when_all_windows_are_closed = false

wezterm.on('mux-is-process-stateful', function(proc)
  _ = proc
  return nil
end)

wezterm.on('format-tab-title', function(tab)
  local pane = tab.active_pane
  local title = pane.title
  if pane.domain_name then
    title = title .. ' - (' .. pane.domain_name .. ')'
  end
  return title
end)

wezterm.on('update-status', function(window, pane)
  _ = pane
  window:set_right_status(window:active_workspace() .. '  ')
end)

-- and finally, return the configuration to wezterm
return config
