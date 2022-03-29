from anubis_pd.AST import AST


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
