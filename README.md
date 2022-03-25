The AST approach proves to be working well when tested with homework1 of the OS class. I've created a CLI tool called `anubis_pd.py`. To use it you first need to install `clang` library:
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