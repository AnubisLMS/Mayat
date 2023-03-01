import os
from tree_sitter import Language, Parser

import mayat
from mayat.AST import AST, ASTGenerationException
from mayat.args import arg_parser
from mayat.driver import driver
from mayat.Result import print_result


LANG_PATH = os.path.join(
    os.path.dirname(os.path.dirname(mayat.__file__)),
    "langs/langs.so"
)

JAVA_LANG = Language(LANG_PATH, 'java')


class Java_AST(AST):
    def __init__(self, parent=None, name=None, pos=None, kind=None):
        AST.__init__(self, parent, name, pos, kind)

    @classmethod
    def create(cls, path):
        def helper(cursor):
            java_ast_node = Java_AST(
                parent=cursor.node.parent,
                name=cursor.current_field_name() or "",
                pos=cursor.node.start_point,
                kind=cursor.node.type
            )

            java_ast_node.weight = 1

            has_more_children = cursor.goto_first_child()
            if not has_more_children:
                return java_ast_node

            while has_more_children:
                child_node = helper(cursor)
                java_ast_node.children.append(child_node)
                java_ast_node.weight += child_node.weight

                has_more_children = cursor.goto_next_sibling()

            cursor.goto_parent()
            return java_ast_node

        with open(path, "rb") as f:
            parser = Parser()
            parser.set_language(JAVA_LANG)

            tree = parser.parse(f.read())
            cursor = tree.walk()
            return helper(cursor)


if __name__ == "__main__":
    java_ast_root = Java_AST.create("/Users/alpacamax/tree_sitter/BinarySearch.java")
    java_ast_root.display()
