import os
import yaml

class Checkpoint:
    def __init__(self, path=""):
        self.identifiers = []
        self.path = path

    def prepend_path(self, path):
        self.path = os.path.join(path, self.path)

    def add_identifier(self, name, kind):
        self.identifiers.append((name, kind))

    def __str__(self):
        ids = " | ".join([f"{kind} {name}" for (name, kind) in self.identifiers])
        return f"{self.path}" + (f": {ids}" if len(self.identifiers) > 0 else '')

    def __repr__(self):
        return str(self)

class Configuration:
    def __init__(self, filename: str):
        self.raw_config = yaml.load(open(filename, 'r'), Loader=yaml.Loader)
        self.checkpoints = []
        self.gen_paths()

        for cp in self.checkpoints:
            print(cp)

    def gen_paths(self):
        def helper(mapping):
            if isinstance(mapping, dict):
                for directory in mapping:
                    for checkpoint in helper(mapping[directory]):
                        checkpoint.prepend_path(directory)
                        yield checkpoint
            else:
                if isinstance(mapping, str):
                    new_checkpoint = Checkpoint()
                    yield new_checkpoint
                else:
                    new_checkpoint = Checkpoint()
                    for identifier in mapping:
                        kind, name = identifier.split()
                        new_checkpoint.add_identifier(name, kind)

                    yield new_checkpoint

        self.checkpoints = list(helper(self.raw_config))

if __name__ == "__main__":
    config = Configuration("midterm.yaml")
