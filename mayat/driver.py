import os
import sys
from typing import List
from datetime import datetime
from tqdm import tqdm

from mayat.Checker import Checker
from mayat.AST import AST, ASTGenerationException, ASTSearchException


def driver(
    AST_class: AST,
    source_filenames: List[str],
    function_name: str,
    function_kind: str,
    threshold: int,
    **kwargs
):
    """
    A driver function to run the plagiarism detection algorithm over a set of
    students' code

    Arguments:
        AST_class: The AST class of the programming language students' code
                   written in
        source_filenames: The list of filenames to check
        function_name: The specific function name to check
        function_kind: The name of the kind for function in the AST we check
        threshold: The granularity of code the algorithm will check
        **kwargs: Additional resources needed
    Return:
        A dictionary containing the result coming out of the algorithm
    """

    result = {}

    # Record current time and the raw command
    result["current_datetime"] = str(datetime.now())

    # Start datetime
    start_time = datetime.now()

    warnings = []
    result["function"] = function_name
    result["warnings"] = warnings

    # Translate all code to ASTs
    print("Parsing files...", file=sys.stderr)
    asts = {}
    for filename in tqdm(source_filenames):
        try:
            ast = AST_class.create(filename, **kwargs)
        except ASTGenerationException:
            warnings.append(f"{filename} cannot be properly parsed")
            continue
        except FileNotFoundError:
            warnings.append(f"{filename} not found")
            continue

        ast.hash_non_recursive()
        asts[os.path.abspath(filename)] = ast

    # Find Sub ASTs based on function name and kind
    function_name = function_name.encode()
    if function_name != b'*':
        print("Filtering partial code...", file=sys.stderr)
        new_asts = {}
        for path in tqdm(asts):
            try:
                sub_ast = asts[path].subtree(function_kind, function_name)
                new_asts[path] = sub_ast
            except ASTSearchException:
                warnings.append(f"{path} doesn't have {function_name}")

        asts = new_asts

    # Run similarity checking algorithm
    checkers = []
    keys = list(asts.keys())

    print("Running plagiarism detection algorithm...", file=sys.stderr)
    with tqdm(total=(1 + len(keys) - 1)*(len(keys)-1)//2) as pbar:
        for i in range(len(keys)):
            for j in range(i + 1, len(keys)):
                path1 = keys[i]
                path2 = keys[j]
                checker = Checker(
                    path1,
                    path2,
                    asts[path1],
                    asts[path2],
                    threshold=threshold,
                )

                checker.check_v2()
                checkers.append(checker)
                pbar.update(1)

    # Collect result
    checker_result = []
    result["result"] = checker_result
    for c in checkers:
        checker_result.append({
            "submission_A": c.path1,
            "submission_B": c.path2,
            "similarity": c.similarity,
            "overlapping_ranges": c.overlapping_ranges,
        })

    # Stop datetime
    end_time = datetime.now()

    # Record total time used
    result["execution_time"] = (end_time - start_time).total_seconds()

    return result
