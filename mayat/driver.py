import os
import sys
from typing import List
from datetime import datetime

from mayat.Checker import Checker
from mayat.AST import AST, ASTGenerationException, ASTSearchException
from mayat.Configurator import Configuration, Checkpoint


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
        dir: The directory the source code files live in
        subpath: The path to the file relative to each student's directory
        threshold: The granularity of code the algorithm will check
        **kwargs: Additional resources needed
    Return:
        A Result instance containing the result coming out of the algorithm
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
    asts = {}
    for filename in source_filenames:
        try:
            ast = AST_class.create(filename, **kwargs)
        except ASTGenerationException:
            print(f"{filename} cannot be properly parsed")
            continue

        ast.hash()
        asts[filename] = ast

    # Find Sub ASTs based on function name and kind
    if function_name != '*':
        new_asts = {}
        for path in asts:
            try:
                new_asts[path] = asts[path].subtree(function_kind, function_name)
            except ASTSearchException:
                warnings.append(f"{path} doesn't have {function_name}")

        ast = new_asts

    # Run matching algorithm
    checkers = []
    keys = list(asts.keys())
    for i in range(len(keys)):
        for j in range(i + 1, len(keys)):
            path1 = keys[i]
            path2 = keys[j]
            checker = Checker(
                path1,
                path2,
                asts[path1].preorder(),
                asts[path2].preorder(),
                threshold=threshold,
            )

            checker.check()
            checkers.append(checker)

    # Print result
    checker_result = []
    result["result"] = checker_result
    for c in checkers:
        checker_result.append({
            "submission_A": c.path1,
            "submission_B": c.path2,
            "similarity": c.similarity
        })

    # Stop datetime
    end_time = datetime.now()

    # Record total time used
    result["execution_time"] = (end_time - start_time).total_seconds()

    return result