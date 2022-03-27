# Anubis AntiCheat

This repository contains a plagiarism detection tool developed by [Tian(Maxwell) Yang](https://github.com/AlpacaMax). It works by comparing the Abstract Syntax Trees of students' code solutions. To use it you first need to install clang's python bindings:

```
pip install clang
```

Let's say we need to check all students' `uniq.c` for homework1. The path for each `uniq.c` has the format `homework1/<netid>/uniq.c`. Assume you placed `homework1/` under the same directory as `anubis_pd.py`. You can use:
```
python3 anubis_pd.py -d homework1 -p uniq.c
```
to perform plagiarism detection on all `uniq.c`.

Additionally you can pass two more optional arguments for `anubis_pd.py`:
- `--threshold`: Specify the granularity for the matching algorithm. Default to 5. A smaller value will cause it to check trivial details, which increases the similarity score of two code even though they are not similar. A larger value will cause it to overlook some common cheat tricks such as swapping two function definitions.
- `--libclang`: Specify the path to `libclang`, which is a C API used for analyzing the AST of C code. If `anubis_pd.py` cannot automatically find `libclang` you need to explicitly pass its path.

## Limitations
This tool will never work for assembly code as the code has to be written in a high level programming language that can be converted into an AST. We can potentially figure out a way to automatically reverse engineer assembly code back to C and then convert it to AST. However, there's no guarantee that the reverse-engineered code can be a good representation for its assembly counterpart.
