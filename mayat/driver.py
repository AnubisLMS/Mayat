import os
import sys
from datetime import datetime

from mayat.Checker import Checker
from mayat.AST import AST, ASTGenerationException
from mayat.Configurator import Configuration, Checkpoint


def driver(AST_class: AST, dir: str, config: Configuration, threshold: int=5, **kwargs):
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
    # Record current time and the raw command
    print(datetime.now())
    print(" ".join(sys.argv))
    print()

    # Initialization
    print("Things to check:")
    print(config)

    # Start datetime
    start_time = datetime.now()

    for checkpoint in config.checkpoints:
        # Translate all code to ASTs
        subpath = checkpoint.path
        print(subpath)
        print()

        asts = {}
        for dirname in os.listdir(dir):
            path = os.path.join(dir, dirname, subpath)
            if not os.path.exists(path):
                print(f"{os.path.join(dir, dirname)} doesn't have {subpath}")
                continue

            try:
                ast = AST_class.create(path, **kwargs)
            except ASTGenerationException:
                print(f"{path} cannot be properly parsed")
                continue

            ast.hash()
            asts[path] = ast
        print()

        # Find Sub ASTs based on checkpoints
        checkpoint_to_asts = {}
        if len(checkpoint.identifiers) == 0: # Checks the whole file
            checkpoint_to_asts[(subpath, '*', '*')] = asts
        else:                                # Checks partial code
            for name, kind in checkpoint.identifiers:
                # Extract Sub-AST for this specific name and kind
                local_asts = {}
                for path in asts:
                    try:
                        local_asts[path] = asts[path].subtree(kind, name)
                    except:
                        print(f"{path} doesn't have {name}:{kind}")
                print()

                checkpoint_to_asts[(subpath, name, kind)] = local_asts

        # Run matching algorithm
        for (subpath, name, kind) in checkpoint_to_asts:
            print(f"Checking {subpath}: {name}:{kind}")
            print()
            local_asts = checkpoint_to_asts[(subpath, name, kind)]

            checkers = []
            keys = list(local_asts.keys())
            for i in range(len(keys)):
                for j in range(i + 1, len(keys)):
                    path1 = keys[i]
                    path2 = keys[j]
                    checker = Checker(
                        path1,
                        path2,
                        local_asts[path1].preorder(),
                        local_asts[path2].preorder(),
                        threshold=threshold,
                    )

                    checker.check()
                    checkers.append(checker)

            # Print result
            checkers.sort(key=lambda x: -x.similarity)
            for c in checkers:
                print(f"{c.path1} - {c.path2}:\t{c.similarity:%}")
            print()

    # Stop datetime
    end_time = datetime.now()

    # Record total time used
    print(f"{(end_time - start_time).seconds}s")
