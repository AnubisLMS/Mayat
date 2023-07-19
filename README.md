# Mayat

**Mayat** is a code similarity detection tool developed by [Tian(Maxwell) Yang](https://github.com/AlpacaMax). It works by comparing the Abstract Syntax Trees of students' code solutions and generate a similarity score for each pair of students' code.

## Usage
To use the C frontend you first need to install clang's python bindings:

```
pip install clang
```

Let's say we need to check all students' `uniq.c` for homework1. The path for each `uniq.c` has the format `homework1/<netid>/user/uniq.c`. All we need to do is run:
```
python -m mayat.frontends.C homework1/*/user/uniq.c
```

If we only want to check the `main` function, we can do:
```
python -m mayat.frontends.C homework1/*/user/uniq.c -f main
```

   Additionally, we can pass two more optional arguments for `C.py`:
   - `--threshold`: Specify the granularity for the matching algorithm. Default to 5. A smaller value will cause it to check trivial details, which increases the similarity score of two code even though they are not similar. A larger value will cause it to overlook some common cheat tricks such as swapping two function definitions.
   - `--libclang`: Specify the path to `libclang`, which is a C API used for analyzing the AST of C code. If Mayat cannot automatically find `libclang` you need to explicitly pass its path.

## Implement a new PL's frontend
We implement a new programming language's frontend by using classes and functions defined in `mayat`. They are:
- `mayat.AST.AST`: The base class for Abstract Syntax Tree. For a new PL you should inherit this and implement the `AST.create(path)` class method, which takes the path of a program as a parameter and returns the AST representation of that program.
- `mayat.args.arg_parser`: A `argparse.ArgumentParser` object. You need to use this object to retrieve command arguments. You can add new arguments if needed.
- `mayat.driver.driver`: The driver function that takes the inherited AST class and the parsed arguments as parameters and run the plagiarism detection algorithm.

An example of this can be find in `mayat/frontends/C.py`, which is a C frontend for this tool.

## Testing
To run the test suite, simply `cd` into `tests` directory and run `test.py`
```
cd tests
python test.py -v
```

## Limitations
This tool will never work for assembly code as the code has to be written in a high level programming language that can be converted into an AST. We can potentially figure out a way to automatically reverse engineer assembly code back to C and then convert it to AST. However, there's no guarantee that the reverse-engineered code can be a good representation for its assembly counterpart.
