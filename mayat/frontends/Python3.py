import ast
from typing import List

from mayat.AST import AST, ASTGenerationException
from mayat.args import arg_parser
from mayat.driver import driver
from mayat.Result import print_result


PYTHON3_FUNCTION_KIND = "FunctionDef"


class Python_AST(AST):
    def __init__(self, parent=None, name=None, pos=None, kind=None):
        AST.__init__(self, parent, name, pos, kind)
    
    @classmethod
    def create(cls, path):
        def helper(node, parent=None):
            name = ""
            if isinstance(node, ast.FunctionDef):
                name = node.name
            elif isinstance(node, ast.Name):
                name = node.id
            
            pos = None
            if hasattr(node, 'lineno') and hasattr(node, 'col_offset'):
                (node.lineno, node.col_offset)

            python_ast_node = Python_AST(
                parent=parent,
                name=name,
                pos=pos,
                kind=type(node).__name__
            )

            python_ast_node.weight = 1

            for child in ast.iter_child_nodes(node):
                child_node = helper(child, python_ast_node)
                python_ast_node.children.append(child_node)
                python_ast_node.weight += child_node.weight
            
            return python_ast_node

        with open(path, 'r') as f:
            try:
                prog = ast.parse(f.read())
            except:
                raise ASTGenerationException

            return helper(prog)


def main(
    source_filenames: List[str],
    function_name: str,
    threshold: int
):
    return driver(
        Python_AST,
        source_filenames=source_filenames,
        function_name=function_name,
        function_kind=PYTHON3_FUNCTION_KIND,
        threshold=threshold
    )


if __name__ == "__main__":
    args = arg_parser.parse_args()
    for action in arg_parser._actions:
        if action.dest == "threshold":
            action.default = 30

    result = main(
        source_filenames=args.source_filenames,
        function_name=args.function_name,
        threshold=args.threshold,
    )

    print_result(
        result,
        format=args.output_format,
        list_all=args.list_all
    )