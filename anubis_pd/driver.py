import os
import sys
from datetime import datetime

from anubis_pd.Checker import Checker


def driver(AST_class, args):
    # Print current time and the raw command
    print(datetime.now())
    print(" ".join(sys.argv))
    print()

    # Start datetime
    start_time = datetime.now()

    # Translate all code to ASTs
    asts = {}
    for dirname in os.listdir(args.dir):
        path = f"{args.dir}/{dirname}/{args.subpath}"
        if not os.path.exists(path):
            print(f"{args.dir}/{dirname} doesn't have {args.subpath}")
            continue

        ast = AST_class.create(path)
        ast.hash()
        asts[path] = ast

    # Run matching algorithm
    keys = list(asts.keys())
    results = []
    for i in range(len(keys)):
        for j in range(i + 1, len(keys)):
            path1 = keys[i]
            path2 = keys[j]
            checker = Checker(
                path1,
                path2,
                asts[path1].preorder(),
                asts[path2].preorder(),
                threshold=args.threshold,
            )

            checker.check()
            results.append(checker)

    # Stop datetime
    end_time = datetime.now()

    # Print total time used
    print()
    print(f"Duration: {(end_time-start_time).seconds}s")
    print()

    # Print similarities in descending order
    results.sort(key=lambda x: -x.similarity)
    for r in results:
        print(f"{r.path1} - {r.path2}:\t{r.similarity:%}")
