;; -*- no-byte-compile: t; -*-

(package! git-auto-commit-mode)
(package! keycast)
(package! f)
(package! s)
(package! ini)
(package! dash)
(package! drag-stuff)
(package! python-black)
(package! interaction-log :recipe (:host github :repo "amiorin/interaction-log.el"))
(package! evil-cleverparens)
(package! expand-region)
(package! git-link)
(package! direnv)
(package! lsp-pyright :pin "dd54b3ae7c22d34faaced7b1a89739063c552b1f")
(package! just-mode :recipe (:host github :repo "leon-barrett/just-mode.el"))
(package! zoxide :recipe (:host github :repo "Vonfry/zoxide.el"))
(package! transient :pin "25b994a565ce8035330b0a3071ee430c0282349e")
(package! org-present :recipe (:host github :repo "rlister/org-present"))
(package! visual-fill-column
  :recipe (:host nil
           :repo "https://codeberg.org/joostkremers/visual-fill-column.git"
           :files ("visual-fill-column.el")))
(package! adoc-mode)
