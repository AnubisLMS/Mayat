from tree_sitter import Language, Parser

Language.build_library(
    "langs/langs.so",
    [
        "langs/tree-sitter-c",
        "langs/tree-sitter-python",
        "langs/tree-sitter-java",
    ]
)
