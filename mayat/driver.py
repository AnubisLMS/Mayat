import os
import sys
from datetime import datetime

from mayat.Checker import Checker
from mayat.Result import Result


def driver(AST_class, dir, subpath, threshold=5, **kwargs):
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
    result = Result()

    # Record current time and the raw command
    result.current_datetime = datetime.now()
    result.raw_command = " ".join(sys.argv)

    # Start datetime
    start_time = datetime.now()

    # Translate all code to ASTs
    asts = {}
    for dirname in os.listdir(dir):
        path = f"{dir}/{dirname}/{subpath}"
        if not os.path.exists(path):
            result.header_info.append(f"{dir}/{dirname} doesn't have {subpath}")
            continue

        ast = AST_class.create(path, **kwargs)
        ast.hash()
        asts[path] = ast

    # Run matching algorithm
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
            result.checkers.append(checker)

    # Stop datetime
    end_time = datetime.now()

    # Record total time used
    result.duration = (end_time - start_time).seconds
    result.checkers.sort(key=lambda x: -x.similarity)

    return result
