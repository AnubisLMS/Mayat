import os
import git
import shutil
from tree_sitter import Language

import mayat

MAYAT_DIR = os.path.dirname(mayat.__file__)
LANGS = [
    "tree-sitter-c",
    "tree-sitter-java",
    "tree-sitter-python",
]

for lang in LANGS:
    if os.path.exists(os.path.join(MAYAT_DIR, "langs", lang)):
        continue

    print(f"Downloading {lang}")
    git.Repo.clone_from(
        f"git@github.com:tree-sitter/{lang}.git",
        os.path.join(MAYAT_DIR, "langs", lang)
    )

print("Building lang.so")
Language.build_library(
    os.path.join(MAYAT_DIR, "langs.so"),
    [os.path.join(MAYAT_DIR, "langs", lang) for lang in LANGS]
)

shutil.rmtree(os.path.join(MAYAT_DIR, "langs"))