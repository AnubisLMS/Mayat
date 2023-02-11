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

    result = {}

    # Record current time and the raw command
    result["current_datetime"] = str(datetime.now())
    result["command"] = " ".join(sys.argv)

    # Initialization
    result["checkpoints"] = [c.__dict__ for c in config.checkpoints]

    # Start datetime
    start_time = datetime.now()

    checkpoint_results = []
    result["checkpoint_results"] = checkpoint_results
    
    # Check all checkpoints
    for checkpoint in config.checkpoints:
        one_file = {}
        checkpoint_results.append(one_file)
        warnings = []
        one_file["warnings"] = warnings
        
        # Translate all code to ASTs
        subpath = checkpoint.path
        one_file["subpath"] = subpath

        asts = {}
        for dirname in os.listdir(dir):
            path = os.path.join(dir, dirname, subpath)
            if not os.path.exists(path):
                warnings.append(f"{os.path.join(dir, dirname)} doesn't have {subpath}")
                continue

            try:
                ast = AST_class.create(path, **kwargs)
            except ASTGenerationException:
                print(f"{path} cannot be properly parsed")
                continue

            ast.hash()
            asts[path] = ast

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
                        warnings.append(f"{path} doesn't have {name}:{kind}")

                checkpoint_to_asts[(subpath, name, kind)] = local_asts

        # Run matching algorithm
        path_name_kind_result = []
        one_file["path_name_kind_result"] = path_name_kind_result
        for (subpath, name, kind) in checkpoint_to_asts:
            one_result = {}
            path_name_kind_result.append(one_result)
            one_result["name"] = name
            one_result["kind"] = kind
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
            similarity_scores = []
            one_result["entries"] = similarity_scores
            for c in checkers:
                similarity_scores.append({
                    "submission_A": c.path1,
                    "submission_B": c.path2,
                    "similarity": c.similarity
                })

    # Stop datetime
    end_time = datetime.now()

    # Record total time used
    result["execution_time"] = (end_time - start_time).total_seconds()

    return result