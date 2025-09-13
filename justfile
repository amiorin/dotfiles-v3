# list of all recipes
help:
    @just -f {{ justfile() }} --list --unsorted

test:
    clojure -M:test
