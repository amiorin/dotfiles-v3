;;; lang/protobuf/config.el -*- lexical-binding: t; -*-

(use-package! protobuf-ts-mode
  :mode "\\.proto\\'"
  :config
  (setq treesit-language-source-alist
        '((proto "https://github.com/treywood/tree-sitter-proto")))
  (treesit-install-language-grammar 'proto)
  (setq proto-ts-mode-indent-offset 2)) ; Set indentation to 2 spaces
