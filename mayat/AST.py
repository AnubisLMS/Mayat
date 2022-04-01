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
        child_fingerprints = "".join(c.hash() for c in self.children)
        self.fingerprint = sha256(
            (self.kind.name + child_fingerprints).encode()
        ).hexdigest()

        return self.fingerprint

    def display(self, level=0):
        print("    " * level + str(self))
        for child in self.children:
            child.display(level + 1)

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
    def create(cls, path):
        raise NotImplementedError("create() method not implemented!")
