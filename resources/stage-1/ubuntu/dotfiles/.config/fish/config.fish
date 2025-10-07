if status is-interactive
    set -gx DIRENV_LOG_FORMAT ""
    SHELL=fish devbox global shellenv --recompute | source

{% if profile = "macos" %}
    /opt/homebrew/bin/brew shellenv | source
{% endif %}

    starship init fish | source
    zoxide init fish | source
    direnv hook fish | source
    atuin init fish | source

{% if profile = "macos" %}
    # https://www.packetmischief.ca/2016/09/06/ssh-agent-on-os-x/
    set -gx SSH_AUTH_SOCK (launchctl getenv SSH_AUTH_SOCK)

    # brew
    set -gx HOMEBREW_NO_AUTO_UPDATE true

    # github credentials
    if test -f (dirname (realpath (status --current-filename)))/config.private.fish
       source (dirname (realpath (status --current-filename)))/config.private.fish
    end
{% endif %}

    #asdf
    if test -z $ASDF_DATA_DIR
        set _asdf_shims "$HOME/.asdf/shims"
    else
        set _asdf_shims "$ASDF_DATA_DIR/shims"
    end

    # Do not use fish_add_path (added in Fish 3.2) because it
    # potentially changes the order of items in PATH
    if not contains $_asdf_shims $PATH
        set -gx --prepend PATH $_asdf_shims
    end
    set --erase _asdf_shims

    # agent setup
    if SSH_AUTH_SOCK=/tmp/$ZELLIJ_SESSION_NAME.agent ssh-add -l > /dev/null 2>&1
        set -gx SSH_AUTH_SOCK /tmp/$ZELLIJ_SESSION_NAME.agent
    end

    function register-cmd
        set -l CMD $argv[1]
        set -l TARGET_DIR ~/.config/fish/completions
        set -l TARGET $TARGET_DIR/$CMD.fish
        mkdir -p $TARGET_DIR
        if not test -e $TARGET
            register-python-argcomplete --shell fish $CMD > ~/.config/fish/completions/$CMD.fish
        end
    end

    # ansible
    # register-cmd ansible
    # register-cmd ansible-playbook

    # multi-account github
    if test -n "$GITHUB_TOKEN"
        git config --global url."https://$GITHUB_TOKEN:x-oauth-basic@github.com/".insteadOf "https://github.com/"
    end

    if test -n "$GITHUB_TOKEN_ALPHA"
        git config --global url."https://$GITHUB_TOKEN_ALPHA:x-oauth-basic@github.com/".insteadOf "https://alpha@github.com/"
    end

    if test -n "$GITHUB_TOKEN_BETA"
        git config --global url."https://$AMIORIN_TOKEN_BETA:x-oauth-basic@github.com/".insteadOf "https://beta@github.com/"
    end

    if test -n "$GITHUB_TOKEN_GAMMA"
        git config --global url."https://$FACUNDO_TOKEN_GAMMA:x-oauth-basic@github.com/".insteadOf "https://gamma@github.com/"
    end

    fish_vi_key_bindings
    # cursor style like vim
    set fish_vi_force_cursor
    set fish_cursor_default block
    set fish_cursor_insert line
    set fish_cursor_replace_one underscore
    set fish_cursor_replace underscore
    set fish_cursor_external line
    set fish_cursor_visual block

    if test "$INSIDE_EMACS" = vterm
        set -gx EDITOR emacsclient
    else
        set -gx EDITOR "emacsclient -a '' -t"
    end
    alias emacs=$EDITOR
    alias e=$EDITOR

{% if profile = "macos" %}
    alias ze="zellij attach --create AMIORIN@silicon"
{% endif %}

    set -g fish_greeting
    set -gx COLORTERM truecolor

    # https://eza.rocks
    alias ls=eza
    alias ll="ls -l --smart-group --icons --group-directories-first"
    alias l="ll -a"
    alias rt="ls -l -r -a --smart-group --sort=time"
    alias u="cd .."
    alias k=kubectl
    alias o=overmind
    alias j=just

    # misc
    set -gx POETRY_VIRTUALENVS_IN_PROJECT true
    set -gx TZ 'Europe/Berlin'

{% if profile = "ubuntu" %}
    set -gx LOCALE_ARCHIVE /usr/lib/locale/locale-archive
{% endif %}

end
