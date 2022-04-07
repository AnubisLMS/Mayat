from hashlib import sha256

# Base class for Abstract Syntax Trees
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
        """
        Hash this node by concatenating the kind and its children's fingerprints

        Returns:
            The fingerprint(hash value)
        """
        child_fingerprints = "".join(c.hash() for c in self.children)
        self.fingerprint = sha256(
            (self.kind.name + child_fingerprints).encode()
        ).hexdigest()

        return self.fingerprint

    def display(self, level=0):
        """
        Recursively print out the entire tree structure

        Arguments:
            level: The level of this node(for indentation purpose)
        """
        print("    " * level + str(self))
        for child in self.children:
            child.display(level + 1)

    def __str__(self):
        return f"<{self.name}, {self.pos}, {self.kind.name}, {self.weight}>"

    def __repr__(self):
        return str(self)

    def preorder(self):
        """
        The preorder traversal of this tree

        Returns:
            A list of nodes in preorder
        """
        lst = [self]
        for child in self.children:
            lst += child.preorder()
        return lst
    
    def subtree_preorder(self, kind, name):
        def get_subtree_root(root, kind, name):
            if root.kind == kind and root.name == name:
                return root
            
            for child in self.children:
                child_result = get_subtree_root(child, kind, name)
                if child_result is not None:
                    return child_result
        
        subtree_root = get_subtree_root(self, kind, name)
        if subtree_root is None:
            raise Exception("Cannot find specified kind and identifier name")
        
        return subtree_root.preorder()

    @classmethod
    def create(cls, path, **kwargs):
        """
        Create an AST of the program pointed by the path. This class method is
        meant to be overrided in the child class

        Parameters:
            path: The path to the program
            **kwargs: Additional resources needed to create an AST
        """
        raise NotImplementedError("create() method not implemented!")
