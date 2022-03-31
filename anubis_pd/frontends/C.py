import sys
import clang.cindex

from anubis_pd.AST import AST
from anubis_pd.args import arg_parser
from anubis_pd.driver import driver


class C_AST(AST):
    def __init__(self, parent=None, name=None, pos=None, kind=None):
        AST.__init__(self, parent, name, pos, kind)

    @classmethod
    def create(cls, path):
        def helper(node, parent=None):
            c_ast_node = C_AST(
                parent=parent,
                name=node.spelling,
                pos=(node.location.line, node.location.column),
                kind=node.kind,
            )

            c_ast_node.weight = 1
            for child in node.get_children():
                child_node = helper(child, c_ast_node)
                c_ast_node.children.append(child_node)
                c_ast_node.weight += child_node.weight

            return c_ast_node

        prog = index.parse(path)

        return helper(prog.cursor)


arg_parser.add_argument(
    "--libclang",
    dest="libclang_path",
    help="The path to libclang, a C API used for analyzing the AST of C code",
)


def main():
    global index

    args = arg_parser.parse_args()
    # /Library/Developer/CommandLineTools/usr/lib
    if args.libclang_path is not None:
        clang.cindex.Config.set_library_path(args.libclang_path)
        index = clang.cindex.Index.create()
    else:
        try:
            index = clang.cindex.Index.create()
        except clang.cindex.LibclangError:
            print(
                "Cannot find libclang. Please specify its path using --libclang argument"
            )
            sys.exit()

    result = driver(C_AST, dir=args.dir, subpath=args.subpath, threshold=args.threshold)
    print(result)


if __name__ == "__main__":
    main()
