if status is-interactive
    set -gx SHELL /usr/bin/fish
    devbox global shellenv --recompute | source

    # workaround to direnv
    test -e .envrc; and touch .envrc

    # start lorri
    if not socat -u OPEN:/dev/null UNIX-CONNECT:$HOME/.cache/lorri/daemon.socket > /dev/null 2>&1
        nohup lorri daemon < /dev/null > /dev/null 2>&1 &
        disown
    end

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

    starship init fish | source
    zoxide init fish | source
    direnv hook fish | source
    atuin init fish | source

    fish_vi_key_bindings
    # cursor style like vim
    set fish_vi_force_cursor
    set fish_cursor_default block
    set fish_cursor_insert line
    set fish_cursor_replace_one underscore
    set fish_cursor_replace underscore
    set fish_cursor_external line
    set fish_cursor_visual block

    if test "$INSIDE_EMACS" = "vterm"
        set -gx EDITOR "emacsclient -s $ZELLIJ_SESSION_NAME"
    else
        set -gx EDITOR happy-emacs
    end
    alias emacs=$EDITOR
    alias e=$EDITOR

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
    set -gx LOCALE_ARCHIVE /usr/lib/locale/locale-archive
    test -e /var/run/docker.sock; and sudo chmod 777 /var/run/docker.sock
end
