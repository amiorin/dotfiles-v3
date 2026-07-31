if status is-interactive
    set -gx DIRENV_LOG_FORMAT ""

{%- if profile = "macos" %}
    /opt/homebrew/bin/brew shellenv | source
{%- endif %}
    starship init fish | source
    zoxide init fish | source
    direnv hook fish | source
    # workaround https://github.com/atuinsh/atuin/issues/2940
    atuin init fish | sed "s/-k up/up/g" | source

{%- if profile = "macos" %}
    # https://www.packetmischief.ca/2016/09/06/ssh-agent-on-os-x/
    set -gx SSH_AUTH_SOCK (launchctl getenv SSH_AUTH_SOCK)

    # brew
    set -gx HOMEBREW_NO_AUTO_UPDATE true

    # github credentials
    set -gx GITHUB_TOKEN {{ "GITHUB_TOKEN" | lookup-env }}

    # docker ssh
    set -gx DOCKER_HOST ssh://walter
{%- endif %}

    #asdf
    if test -z $ASDF_DATA_DIR
        set _asdf_shims "$HOME/.asdf/shims"
    else
        set _asdf_shims "$ASDF_DATA_DIR/shims"
    end

    # Always move the asdf shims to the FRONT of PATH so they win over
    # system stubs like /usr/bin/java. A plain "prepend only if absent"
    # guard is not enough: the inherited PATH often already contains the
    # shims dir near the end (launchd/path_helper ordering), which would
    # leave /usr/bin ahead of it. So drop any existing occurrence first,
    # then prepend.
    set -gx PATH (string match --invert -- $_asdf_shims $PATH)
    set -gx --prepend PATH $_asdf_shims
    set --erase _asdf_shims

    # agent setup
    if SSH_AUTH_SOCK=/tmp/$ZELLIJ_SESSION_NAME.agent ssh-add -l > /dev/null 2>&1
        set -gx SSH_AUTH_SOCK /tmp/$ZELLIJ_SESSION_NAME.agent
    end

    # pnpm setup
{%- if profile = "macos" %}
    set -gx PNPM_HOME "/Users/amiorin/Library/pnpm"
{%- elif profile = "ubuntu" %}
    set -gx PNPM_HOME "$HOME/.local/share/pnpm"
{%- endif %}
    if not string match -q -- $PNPM_HOME $PATH
        set -gx PATH "$PNPM_HOME/bin" $PATH
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

    alias e="emacsclient -a '' -t"
    alias ne="emacs --init-directory ~/.config/neoemacs -nw"
    alias zne="zellij --layout emacs"
    alias za="zellij attach"

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

{%- if profile = "ubuntu" %}
    set -gx LOCALE_ARCHIVE /usr/lib/locale/locale-archive
{%- endif %}
end
