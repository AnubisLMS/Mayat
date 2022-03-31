import os
import sys
from datetime import datetime

from anubis_pd.Checker import Checker
from anubis_pd.Result import Result


def driver(AST_class, dir, subpath, threshold=5):
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

        ast = AST_class.create(path)
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
