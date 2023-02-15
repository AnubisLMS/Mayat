import sys
import clang.cindex
from typing import List

from mayat.AST import AST
from mayat.args import arg_parser
from mayat.driver import driver
from mayat.Result import print_result


C_FUNCTION_KIND = "FUNCTION_DECL"


class C_AST(AST):
    def __init__(self, parent=None, name=None, pos=None, kind=None):
        AST.__init__(self, parent, name, pos, kind)

    @classmethod
    def create(cls, path, **kwargs):
        def helper(node, parent=None):
            c_ast_node = C_AST(
                parent=parent,
                name=node.spelling,
                pos=(node.location.line, node.location.column),
                kind=node.kind.name,
            )

            c_ast_node.weight = 1
            for child in node.get_children():
                child_node = helper(child, c_ast_node)
                c_ast_node.children.append(child_node)
                c_ast_node.weight += child_node.weight

            return c_ast_node

        index = dict(kwargs)["index"]
        prog = index.parse(path)

        return helper(prog.cursor)


arg_parser.add_argument(
    "--libclang",
    dest="libclang_path",
    help="The path to libclang, a C API used for analyzing the AST of C code",
)


def main(
    source_filenames: List[str],
    function_name: str,
    threshold: int,
    libclang_path: str=None
):
    if libclang_path is not None:
        clang.cindex.Config.set_library_path(libclang_path)
        index = clang.cindex.Index.create()
    else:
        try:
            index = clang.cindex.Index.create()
        except clang.cindex.LibclangError:
            print(
                "Cannot find libclang. Please specify its path using --libclang argument"
            )
            sys.exit()

    return driver(
        C_AST,
        source_filenames=source_filenames,
        function_name=function_name,
        function_kind=C_FUNCTION_KIND,
        threshold=threshold,
        index=index
    )


if __name__ == "__main__":
    args = arg_parser.parse_args()

    result = main(
        source_filenames=args.source_filenames,
        function_name=args.function_name,
        threshold=args.threshold,
        libclang_path=args.libclang_path
    )

    print_result(
        result,
        format=args.output_format,
        list_all=args.list_all
    )
