import git
from tree_sitter import Language

git.Repo.clone_from(
    "git@github.com:tree-sitter/tree-sitter-python.git",
    "my_langs/tree-sitter-python"
)
git.Repo.clone_from(
    "git@github.com:tree-sitter/tree-sitter-c.git",
    "my_langs/tree-sitter-c"
)
git.Repo.clone_from(
    "git@github.com:tree-sitter/tree-sitter-java.git",
    "my_langs/tree-sitter-java"
)

Language.build_library(
    "my_langs.so",
    [
        "my_langs/tree-sitter-c",
        "my_langs/tree-sitter-python",
        "my_langs/tree-sitter-java",
    ]
)