;;; $DOOMDIR/config.el -*- lexical-binding: t; -*-

(setq doom-theme 'doom-one)

(setq display-line-numbers-type t)

(setq org-directory "~/org/")

;; don't prompt before exiting
(setq confirm-kill-emacs nil)
;; don't prompt the first time we start vterm
(setq vterm-always-compile-module t)
;; shortcut to start dired
(map!
 (:map 'override
  :n "-" #'dired-jump))

;; When done with this frame, type SPC q f`?
(setq server-client-instructions nil)

;; No prompt
(map! :leader
      :desc "Delete frame" "q f" #'delete-frame)

;; No prompt when quitting ediff
;; https://emacs.stackexchange.com/questions/9322/how-can-i-quit-ediff-immediately-without-having-to-type-y
(defun disable-y-or-n-p (orig-fun &rest args)
  (cl-letf (((symbol-function 'y-or-n-p) (lambda (prompt) t)))
    (apply orig-fun args)))
(advice-add 'ediff-quit :around #'disable-y-or-n-p)

(after! keycast
  (define-minor-mode keycast-mode
    "Show current command and its key binding in the mode line."
    :global t
    (if keycast-mode
        (add-hook 'pre-command-hook 'keycast--update t)
      (remove-hook 'pre-command-hook 'keycast--update))))
(add-to-list 'global-mode-string '("" keycast-mode-line))
(require 'keycast)

;; Fix fish problems with emacs
(setq shell-file-name (executable-find "bash"))
(setq-default vterm-shell (executable-find "fish"))
(setq-default explicit-shell-file-name (executable-find "fish"))

;; Cider
(setq cider-save-file-on-load t)
(setq cider-ns-refresh-show-log-buffer t)
(setq cider-ns-save-files-on-refresh t)
(setq cider-inspector-pretty-print t)
(map! :map cider-inspector-mode-map
      :n "d" #'cider-inspector-def-current-val
      :n "y" #'cider-inspector-display-analytics
      :n "v" #'cider-inspector-toggle-view-mode
      :n "p" #'cider-inspector-toggle-pretty-print)

;; https://micro.rousette.org.uk/2021/01/03/a-useful-binding.html
(map!
 (:map 'override
  :v "v" #'er/expand-region
  :v "V" #'er/contract-region))

;; no prompt for .dir-locals.el
(setq safe-local-variable-values
      '((cider-ns-refresh-after-fn . "dev/go!")
        (cider-ns-refresh-after-fn . "hello/start!")
        (cider-ns-refresh-before-fn . "hello/stop!")
        (cider-preferred-build-tool . clojure-cli)
        (cider-clojure-cli-aliases . ":dev")
        (cider-clojure-cli-aliases . ":dev:cider")
        (cider-clojure-cli-aliases . ":dev-mode:otel-agent")
        (cider-default-cljs-repl . shadow) (cider-shadow-default-options . ":app")
        (cider-ns-refresh-before-fn . "user/stop!")
        (cider-ns-refresh-after-fn . "user/start!")
        (cider-ns-refresh-after-fn . "dev/start!")
        (cider-ns-refresh-before-fn . "dev/stop!") (gac-automatically-push-p t)
        (gac-silent-message-p nil)
        (cider-preferred-build-tool . clojure-cli)
        (cider-clojure-cli-aliases . ":dev")
        (cider-clojure-cli-aliases . ":dev:cider")
        (cider-clojure-cli-aliases . ":dev-mode:otel-agent")
        (cider-default-cljs-repl . shadow)
        (cider-shadow-default-options . ":app")
        (cider-ns-refresh-before-fn . "user/stop!")
        (cider-ns-refresh-after-fn . "user/start!")
        (cider-ns-refresh-after-fn . "dev/start!")
        (cider-ns-refresh-before-fn . "dev/stop!")
        (gac-automatically-push-p t)
        (gac-silent-message-p nil)))

;; no prompt for lsp
(setq lsp-auto-guess-root t)

;; discover projects
(setq projectile-project-search-path '(("~/code/of" . 2) ("~/code/personal" . 2)))
(setq projectile-auto-discover t)
;; create test files if needed
(setq projectile-create-missing-test-files t)

;; loead direnv mode at startup
(use-package! direnv
  :hook (doom-first-file . direnv-mode)
  :config
  (add-to-list 'warning-suppress-types '(direnv)))

;; dired
(map! :map dired-mode-map
      :n "h" #'dired-up-directory
      :n "l" #'dired-find-file)

;; smartparens and so on
(after! smartparens
  (add-hook 'prog-mode-hook #'smartparens-strict-mode)
  (add-hook 'prog-mode-hook #'evil-cleverparens-mode)
  (setq evil-move-beyond-eol t))

;; open terminal on the right
(defun open-term-on-right (arg)
  (interactive "P")
  (+evil/window-vsplit-and-follow)
  (+vterm/here arg))

;; make easier to find vterm in list buffers
(setq vterm-buffer-name-string "vterm %s")

;; completion with corfu
(after! corfu
  (setq corfu-preview-current nil)
  (setq corfu-quit-at-boundary nil)
  (setq corfu-preselect 'valid)
  (custom-set-faces!
    '(corfu-current :background "#000000")))

;; better "SPC c j"
(map! :leader
      ;;; <leader> c --- code
      (:prefix-map ("c" . "code")
                   (:when (and (modulep! :tools lsp) (not (modulep! :tools lsp +eglot)))
                     (:when (modulep! :completion vertico)
                       :desc "Jump to symbol in current file workspace" "j"   #'consult-lsp-file-symbols
                       :desc "Jump to symbol in current workspace"      "J"   #'consult-lsp-symbols))))

;; swap evil-cp-next-opening with evil-cp-previous-opening
(define-key (current-global-map) [remap evil-cp-next-opening] 'evil-cp-previous-opening)
(define-key (current-global-map) [remap evil-cp-previous-opening] 'evil-cp-next-opening)

;; override evil-cleverparens
(setq evil-cleverparens-use-s-and-S nil)
(defvar evil-cp-additional-movement-keys
  '(("L"   . evil-cp-forward-sexp)
    ("H"   . evil-cp-backward-sexp)
    ("M-H" . evil-cp-beginning-of-defun)
    ("M-h" . (lambda () (interactive) (evil-cp-beginning-of-defun -1)))
    ("M-l" . evil-cp-end-of-defun)
    ("M-L" . (lambda () (interactive) (evil-cp-end-of-defun -1)))
    ("["   . evil-cp-previous-opening)
    ("]"   . evil-cp-next-closing)
    ("{"   . evil-cp-next-opening)
    ("}"   . evil-cp-previous-closing)
    ("("   . evil-cp-backward-up-sexp)
    (")"   . evil-cp-up-sexp)))

;; swap gj with j
(define-key evil-motion-state-map (kbd "j") 'evil-next-visual-line)
(define-key evil-motion-state-map (kbd "k") 'evil-previous-visual-line)
(define-key evil-motion-state-map (kbd "gj") 'evil-next-line)
(define-key evil-motion-state-map (kbd "gk") 'evil-previous-line)

;; python
(setq lsp-pyright-langserver-command "basedpyright")
(after! format
  (set-formatter! 'ruff-format
    '("ruff" "format" "--stdin-filename" "%s" "-")
    :modes '(python-mode python-ts-mode)))

(defun set-git-name
    (value)
  (setenv "GIT_AUTHOR_NAME" value)
  (setenv "GIT_COMMITTER_NAME" value))

(defun set-git-email
    (value)
  (setenv "GIT_AUTHOR_EMAIL" value)
  (setenv "GIT_COMMITTER_EMAIL" value))

(let ((user (getenv "ZELLIJ_SESSION_NAME")))
  (cond
   ((string-prefix-p "vscode" user)
    (progn (set-git-name "Alberto Miorin")
           (set-git-email "32617+amiorin@users.noreply.github.com")
           (setq display-line-numbers-type t)))
   (t
    (progn (set-git-name "Alberto Miorin")
           (set-git-email "32617+amiorin@users.noreply.github.com")
           (setq display-line-numbers-type t)))))

(map! :map 'override "s-s" #'save-buffer)
(map! :map 'override "s-t" #'open-term-on-right)
(map! :map 'override "s-w" #'evil-window-delete)
(define-key key-translation-map (kbd "s-S-[") (kbd "s-{"))
(define-key key-translation-map (kbd "s-S-]") (kbd "s-}"))
(map! :map 'override "s-{" #'evil-window-rotate-downwards)
(map! :map 'override "s-}" #'delete-other-windows)
(map! :map 'override "s-n" #'+evil/window-vsplit-and-follow)
(map! :map 'override "s-h" #'evil-window-left)
(map! :map 'override "s-j" #'evil-window-down)
(map! :map 'override "s-k" #'evil-window-up)
(map! :map 'override "s-l" #'evil-window-right)

;; set the agent
(setenv "SSH_AUTH_SOCK" (concat  "/tmp/" (getenv "ZELLIJ_SESSION_NAME") ".agent"))

;; disable some lsp clients
(setq lsp-disabled-clients '(copilot-ls
                             tfls
                             semgrep-ls))

;; use only one buffer for dired
(setq dired-kill-when-opening-new-dired-buffer t)

;; evil-cp
(define-key key-translation-map (kbd "M-S-[") (kbd "M-{"))
(define-key key-translation-map (kbd "M-S-]") (kbd "M-}"))
(define-key key-translation-map (kbd "M-S-9") (kbd "M-("))
(define-key key-translation-map (kbd "M-S-0") (kbd "M-)"))
(define-key key-translation-map (kbd "M-S-j") (kbd "M-J"))
(define-key key-translation-map (kbd "M-S-s") (kbd "M-S"))
(define-key key-translation-map (kbd "M-S-r") (kbd "M-R"))
(define-key key-translation-map (kbd "M-S-l") (kbd "M-L"))
(define-key key-translation-map (kbd "M-S-h") (kbd "M-H"))

(map!
 (:map 'override
  :n "M-5" #'evil-cp-wrap-next-square     ;; [
  :n "M-]" #'evil-cp-wrap-previous-square ;; ]
  ))

;; zoxide and vterm
(after! vterm
  (setq vterm-clear-scrollback-when-clearing t)
  (setq vterm-eval-cmds '(("find-file" find-file)
                          ("message" message)
                          ("vterm-clear-scrollback" vterm-clear-scrollback)
                          ("dired" dired)
                          ("ediff-files" ediff-files)))
  (add-hook 'find-file-hook (lambda ()
                              (with-current-buffer (current-buffer)
                                (zoxide-add default-directory)))))

;; show the clock
(set-time-zone-rule "/usr/share/zoneinfo/Europe/Berlin")
(setq display-time-24-hr-format t)
(setq display-time-format "%H:%M")
(setq display-time-default-load-average nil)
(setq doom-modeline-time t)
(display-time-mode 1)

;; better tab-name and list files outside a project
(defvar my-update-tabname-timer (current-time))
(defun my-do-update-tabname ()
  (let* ((zellij (getenv "ZELLIJ_SESSION_NAME"))
         (home (getenv "HOME"))
         (user (when zellij
                 (downcase (car (split-string zellij "@")))))
         (root (or (projectile-project-root)
                   default-directory))
         (root (if (string= root "/")
                   "root"
                 root))
         (root (if (string= root (format "%s/code/of/" home))
                   "of"
                 root))
         (root (if (string= root (format "%s/code/personal/" home))
                   "personal"
                 root))
         (root (if (string= root (format "%s/" home))
                   "~"
                 root))
         (root (replace-regexp-in-string (format "%s/" home) "" root))
         (root (replace-regexp-in-string "code/of/" "" root))
         (root (replace-regexp-in-string "code/personal/" "" root))
         (xs (split-string root "/"))
         (xs (seq-filter (lambda (s) (not (string-empty-p s))) xs))
         (_ (seq-setq (xs p-user) (if (member (car xs) '("alberto" "facundo" "valery" "rafael"))
                                      (list (cdr xs) (car xs))
                                    (list xs user))))
         (xs (if (null xs)
                 (list "workspaces" p-user)
               xs))
         (suffix (if (not (string= p-user user))
                     (format " (%s)" p-user)
                   ""))
         (tab-name (concat (string-join xs "/") suffix))
         (cmd (format "zellij action rename-tab \"%s\"" tab-name)))
    (start-process-shell-command "update-tab-name" "*update-tab-name*" cmd)))

(defun my-update-tabname ()
  (interactive)
  (let* ((t1 (current-time))
         (t2 my-update-tabname-timer)
         (delta 0.1))
    (when (> (abs (float-time (time-subtract t1 t2))) delta)
      (setq my-update-tabname-timer (current-time))
      (my-do-update-tabname))))
(add-hook 'doom-switch-buffer-hook #'my-update-tabname)
(add-hook 'doom-switch-window-hook #'my-update-tabname)

;; don't add directories to recentf
(after! recentf
  (remove-hook 'dired-mode-hook #'doom--recentf-add-dired-directory-h))

;; evil-snipe
(setq evil-snipe-scope 'whole-visible)

;; localleader on , instead on SPC-m
(setq doom-localleader-key ",")
(setq doom-localleader-alt-key "M-,")

;; use avy on s
(setq avy-all-windows t)
(map! :map evil-snipe-local-mode-map :nv "s" #'evil-avy-goto-char-timer)

;; use always preview in recentf and CMD-ret for the rest
(after! consult
  (consult-customize
   consult-recent-file
   +default/search-project
   :preview-key 'any)
  (consult-customize
   consult-ripgrep consult-git-grep consult-grep
   consult-bookmark
   consult--source-recent-file consult--source-project-recent-file consult--source-bookmark
   :preview-key "s-<return>")
  (when (modulep! :config default)
    (consult-customize
     +default/search-other-project
     +default/search-project-for-symbol-at-point
     +default/search-cwd +default/search-other-cwd
     +default/search-notes-for-symbol-at-point
     +default/search-emacsd
     :preview-key "s-<return>")))

;; I need to see the clock
(after! vterm
  (remove-hook 'vterm-mode-hook #'hide-mode-line-mode))

;; Better embark bindings
(map! "C-<prior>" #'embark-act
      (:map minibuffer-local-map
       "C-<prior>"          #'embark-act
       "C-<next> C-<prior>" #'embark-export
       "C-<next> s-l"       #'embark-collect
       :desc "Export to writable buffer" "C-<next> s-e" #'+vertico/embark-export-write))

;; magit diff 2 panes instead of 3
(setq magit-ediff-dwim-show-on-hunks t)

;; Hide unneeded UI elements (this can even be done in my/org-present-start!)
(add-to-list 'default-frame-alist '(undecorated-round . t))
(menu-bar-mode 0)
(tool-bar-mode 0)
(scroll-bar-mode 0)

;; Let the desktop background show through
(set-frame-parameter (selected-frame) 'alpha '(100 . 95))
(add-to-list 'default-frame-alist '(alpha . (100 . 95)))

;; Configure fill width
(setq visual-fill-column-width 80
      visual-fill-column-center-text t)

(setq org-present-mode-quit-hook nil)

(defalias 'org-tree-slide-mode 'org-present)

;; Load up doom-palenight for the System Crafters look
;; (load-theme 'doom-palenight t)

;; NOTE: These settings might not be ideal for your machine, tweak them as needed!
(set-face-attribute 'default nil :font "JetBrainsMonoNL Nerd Font" :weight 'extralight :height 160)
(set-face-attribute 'fixed-pitch nil :font "JetBrainsMonoNL Nerd Font" :weight 'extralight :height 190)
(set-face-attribute 'variable-pitch nil :font "Iosevka Aile" :weight 'extralight :height 1.3)

(setq fancy-splash-image "~/.doom.d/doomEmacsDoomOne.svg")

(defun my/org-present-start ()
  (interactive)
  (define-key evil-motion-state-map (kbd "gj") 'evil-next-visual-line)
  (define-key evil-motion-state-map (kbd "gk") 'evil-previous-visual-line)
  (define-key evil-motion-state-map (kbd "j") 'evil-next-line)
  (define-key evil-motion-state-map (kbd "k") 'evil-previous-line)
  (fringe-mode 0)
  (evil-force-normal-state)
  (set-face-attribute 'org-link nil :underline nil)
  (map! :map org-present-mode-keymap
        :n "gg" #'evil-goto-first-line
        :n "G" #'evil-goto-line
        :n "h" #'org-present-prev
        :n "l" #'org-present-next
        :n "r" #'org-present-read-only
        :n "w" #'org-present-read-write)
  (hide-mode-line-mode 1)
  (org-present-hide-cursor)
  (org-present-read-only)
  (display-line-numbers-mode 0)
  (vi-tilde-fringe-mode 0)
  ;; Load org-faces to make sure we can set appropriate faces
  (require 'org-faces)

  ;; Resize Org headings
  (dolist (face '((org-level-1 . 1.2)
                  (org-level-2 . 1.1)
                  (org-level-3 . 1.05)
                  (org-level-4 . 1.0)
                  (org-level-5 . 1.1)
                  (org-level-6 . 1.1)
                  (org-level-7 . 1.1)
                  (org-level-8 . 1.1)))
    (set-face-attribute (car face) nil :font "Iosevka Aile" :weight 'light :height (cdr face)))

  ;; Make the document title a bit bigger
  (set-face-attribute 'org-document-title nil :font "Iosevka Aile" :weight 'bold :height 1.3)

  ;; Make sure certain org faces use the fixed-pitch face when variable-pitch-mode is on
  (set-face-attribute 'org-block nil :foreground nil :inherit 'fixed-pitch)
  (set-face-attribute 'org-table nil :inherit 'fixed-pitch)
  (set-face-attribute 'org-formula nil :inherit 'fixed-pitch)
  (set-face-attribute 'org-code nil :inherit '(shadow fixed-pitch))
  (set-face-attribute 'org-verbatim nil :inherit '(shadow fixed-pitch))
  (set-face-attribute 'org-special-keyword nil :inherit '(font-lock-comment-face fixed-pitch))
  (set-face-attribute 'org-meta-line nil :inherit '(font-lock-comment-face fixed-pitch))
  (set-face-attribute 'org-checkbox nil :inherit 'fixed-pitch)

  (setq-local header-line-format " ")

  ;; Center the presentation and wrap lines
  (visual-fill-column-mode 1)
  (visual-line-mode 1)
  ;; Tweak font sizes
  (setq-local face-remapping-alist '((default (:height 1.5) variable-pitch)
                                     (header-line (:height 4.0) variable-pitch)
                                     (org-document-title (:height 1.75) org-document-title)
                                     (org-code (:height 1.55) org-code)
                                     (org-verbatim (:height 1.70) org-verbatim)
                                     (org-block (:height 1.25) org-block)
                                     (org-block-begin-line (:height 0.7) org-block)))
  ;; Hide emphasis markers on formatted text
  (setq org-hide-emphasis-markers t)
  (font-lock-flush))

(defun my/org-present-end ()
  (interactive)
  (define-key evil-motion-state-map (kbd "j") 'evil-next-visual-line)
  (define-key evil-motion-state-map (kbd "k") 'evil-previous-visual-line)
  (define-key evil-motion-state-map (kbd "gj") 'evil-next-line)
  (define-key evil-motion-state-map (kbd "gk") 'evil-previous-line)
  (fringe-mode 1)
  (org-present-show-cursor)
  (hide-mode-line-mode 0)
  (display-line-numbers-mode 1)
  (vi-tilde-fringe-mode 1)

  (setq-local face-remapping-alist nil)
  ;; Stop centering the document
  (visual-fill-column-mode 0)
  (visual-line-mode 0)
  (setq org-hide-emphasis-markers nil)
  (font-lock-flush))

;; Register hooks with org-present
(add-hook 'org-present-mode-hook 'my/org-present-start)
(add-hook 'org-present-mode-quit-hook 'my/org-present-end)

(defun my/org-present-prepare-slide (buffer-name heading)
  ;; Show only top-level headlines
  (org-overview)

  ;; Unfold the current entry
  (org-show-entry)

  ;; Show only direct subheadings of the slide but don't expand them
  (org-show-children))

(add-hook 'org-present-after-navigate-functions 'my/org-present-prepare-slide)

;; shortcuts like zellij
(map! :map 'override "C-s-t" #'tab-new)
(map! :map 'override "C-s-w" #'tab-close)
(map! :map 'override "s-TAB" #'tab-previous)
(map! :map 'override "C-s-o" #'tab-next)
(map! :map 'override "C-s-n" #'make-frame)

;; Don't prompt when closing a frame
(global-set-key [remap delete-frame] nil)

;; Esc act as quit, but it breaks M-(;)
;; (define-key key-translation-map (kbd "ESC") (kbd "C-g"))

(map! :map 'override "s-[" #'previous-buffer)
(map! :map 'override "s-]" #'next-buffer)

;; Clojure
(add-hook 'clojure-mode-hook #'+format-with-lsp-mode)

;; consult-projectile
(map! :leader
      :desc "Find files in a project"
      "SPC" #'consult-projectile)
