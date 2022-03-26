from hashlib import sha256
import clang.cindex
import os
import argparse
from datetime import datetime
import sys
import textwrap

# Base class for AST classes for other programming languages
class AST:
    def __init__(self, parent=None, name=None, pos=None, kind=None):
        self.parent = parent
        self.name = name
        self.pos = pos
        self.kind = kind
        self.children = []
        self.fingerprint = None
        self.weight = 0

    def hash(self):
        child_fingerprints = ''.join(c.hash() for c in self.children)
        self.fingerprint = sha256(
            (self.kind.name + child_fingerprints).encode()
        ).hexdigest()

        return self.fingerprint

    def display(self, level=0):
        print("    "*level + str(self))
        for child in self.children:
            child.display(level+1)

    def __str__(self):
        return f"<{self.name}, {self.pos}, {self.kind.name}, {self.weight}>"

    def __repr__(self):
        return str(self)

    def preorder(self):
        lst = [self]
        for child in self.children:
            lst += child.preorder()
        return lst
    
    @classmethod
    def create(cls, node, parent=None):
        NotImplemented

class C_AST(AST):
    def __init__(self, parent=None, name=None, pos=None, kind=None):
        AST.__init__(self, parent, name, pos, kind)
    
    @classmethod
    def create(cls, node, parent=None):
        c_ast_node = C_AST(
            parent=parent,
            name=node.spelling,
            pos=(node.location.line, node.location.column),
            kind=node.kind
        )

        c_ast_node.weight = 1
        for child in node.get_children():
            child_node = C_AST.create(child, c_ast_node)
            c_ast_node.children.append(child_node)
            c_ast_node.weight += child_node.weight

        return c_ast_node

class Checker:
    def __init__(self, path1, path2, arrLL1, arrLL2, threshold=5):
        self.path1 = path1
        self.path2 = path2
        self.arrLL1 = arrLL1
        self.arrLL2 = arrLL2
        self.threshold = threshold
        self.similarity = 0

    # The optimized version
    def check(self):
        arrLL1_set = {(node.weight, node.fingerprint) for node in self.arrLL1}

        # The overlaped (node.weight, node.fingerprint) along with its occurance
        overlaps = {}
        for node in self.arrLL2:
            key = (node.weight, node.fingerprint)
            if key in arrLL1_set:
                if key not in overlaps:
                    overlaps[key] = 1
                else:
                    overlaps[key] += 1

        num_of_same_nodes = 0
        arrLL = self.arrLL1 if len(self.arrLL1) < len(self.arrLL2) else self.arrLL2

        for node in arrLL:
            key = (node.weight, node.fingerprint)
            if node.weight < self.threshold: continue

            if key in overlaps:
                if (overlaps[key] == 0): continue
                overlaps[key] -= 1

            if node.parent is not None\
                and (node.parent.weight, node.parent.fingerprint) in overlaps:
                continue

            if key in overlaps:
                num_of_same_nodes += node.weight

        self.similarity = num_of_same_nodes / len(arrLL)

def main():
    parser = argparse.ArgumentParser(
        formatter_class=argparse.RawDescriptionHelpFormatter,
        description="Anubis AntiCheat",
        epilog=textwrap.dedent('''
        Explain:
            The script will find all code under the path:
                <DIR>/<any dirname>/<SUBPATH>

            For example, `python3 anubis_pd.py -d /home/homework -p dir1/prog.c` will match:
                /home/homework/<any dirname>/dir1/prog.c
        ''')
    )
    parser.add_argument(
        '-d',
        dest="dir",
        help="The main directory storing the code",
        required=True,
    )
    parser.add_argument(
        '-p',
        dest="subpath",
        help="The path relative to the directories under the main directory to the code itself",
        required=True,
    )
    parser.add_argument(
        '--threshold',
        type=int,
        default=5,
        help="The threshold value controlling the granularity of the matching. Default 5",
    )
    parser.add_argument(
        '--libclang',
        dest="libclang_path",
        help="The path to libclang, a C API used for analyzing the AST of C code"
    )

    args = parser.parse_args()

    # /Library/Developer/CommandLineTools/usr/lib
    if args.libclang_path is not None:
        clang.cindex.Config.set_library_path(args.libclang_path)
        index = clang.cindex.Index.create()
    else:
        try:
            index = clang.cindex.Index.create()
        except clang.cindex.LibclangError:
            print("Cannot find libclang. Please specify its path using --libclang argument")
            sys.exit()

    print(datetime.now())
    print(' '.join(sys.argv))
    print()

    start_time = datetime.now()

    asts = {}
    for dirname in os.listdir(args.dir):
        path = f"{args.dir}/{dirname}/{args.subpath}"
        if (not os.path.exists(path)):
            print(f"{args.dir}/{dirname} doesn't have {args.subpath}")
            continue

        prog = index.parse(path)
        c_ast = C_AST.create(prog.cursor)
        c_ast.hash()
        asts[path] = c_ast

    keys = list(asts.keys())
    results = []
    for i in range(len(keys)):
        for j in range(i+1, len(keys)):
            path1 = keys[i]
            path2 = keys[j]
            checker = Checker(
                path1,
                path2,
                asts[path1].preorder(),
                asts[path2].preorder(),
                threshold=args.threshold
            )

            checker.check()

            results.append(checker)

    end_time = datetime.now()

    print()
    print(f"Duration: {(end_time-start_time).seconds}s")
    print()

    results.sort(key=lambda x: -x.similarity)
    for r in results:
        print(f"{r.path1} - {r.path2}:\t{r.similarity:%}")

if (__name__ == "__main__"):
    main()