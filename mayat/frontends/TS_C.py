import os
from tree_sitter import Language, Parser
from typing import List

import mayat
from mayat.AST import AST
from mayat.args import arg_parser
from mayat.driver import driver
from mayat.Result import serialize_result


LANG_PATH = os.path.join(
    os.path.dirname(mayat.__file__),
    "langs.so"
)

if not os.path.exists(LANG_PATH):
    raise Exception("Run mayat.install_langs to install tree-sitter parsers!")

C_LANG = Language(LANG_PATH, 'c')
C_FUNCTION_KIND = "function_definition"
C_FUNCTION_DECLARATION_KIND = "function_declarator"
C_IDENTIFIER_KIND = "identifier"
C_INLINE_COMMENT = "comment"

C_IGNORE_KINDS = {
    C_INLINE_COMMENT
}


class C_AST(AST):
    def __init__(self, parent=None, name=None, text=None, start_pos=None, end_pos=None, kind=None):
        AST.__init__(self, parent, name, text, start_pos, end_pos, kind)

    @classmethod
    def create(cls, path):
        def helper(cursor, parent=None):
            c_ast_node = C_AST(
                parent=parent,
                name="",
                text=cursor.node.text,
                start_pos=cursor.node.start_point,
                end_pos=cursor.node.end_point,
                kind=cursor.node.type,
            )

            if cursor.node.type == C_FUNCTION_KIND:
                for node in cursor.node.children:
                    if node.type == C_FUNCTION_DECLARATION_KIND:
                        for sub_node in node.children:
                            if sub_node.type == C_IDENTIFIER_KIND:
                                c_ast_node.name = sub_node.text

            c_ast_node.weight = 1

            has_more_children = cursor.goto_first_child()
            if not has_more_children:
                return c_ast_node

            while has_more_children:
                if cursor.node.type in C_IGNORE_KINDS:
                    has_more_children = cursor.goto_next_sibling()
                    continue

                child_node = helper(cursor, c_ast_node)
                c_ast_node.children.append(child_node)
                c_ast_node.weight += child_node.weight

                has_more_children = cursor.goto_next_sibling()

            cursor.goto_parent()
            return c_ast_node

        with open(path, "rb") as f:
            parser = Parser()
            parser.set_language(C_LANG)

            tree = parser.parse(f.read())
            cursor = tree.walk()
            return helper(cursor)


def main(
    source_filenames: List[str],
    function_name: str,
    threshold: int,
):
    return driver(
        C_AST,
        source_filenames=source_filenames,
        function_name=function_name,
        function_kind=C_FUNCTION_KIND,
        threshold=threshold
    )


if __name__ == "__main__":
    args = arg_parser.parse_args()

    result = main(
        source_filenames=args.source_filenames,
        function_name=args.function_name,
        threshold=args.threshold,
    )

    print(serialize_result(
        result,
        format=args.output_format,
        list_all=args.list_all
    ))
