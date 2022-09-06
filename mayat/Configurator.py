import yaml

class Configuration:
    def __init__(self, filename: str):
        self.raw_config = yaml.load(open(filename, 'r'), Loader=yaml.Loader)
        self.paths = dict()
        self.gen_paths()
        print(self.paths)

    def gen_paths(self):
        def helper(mapping):
            if isinstance(mapping, dict):
                for directory in mapping:
                    for sub_dir in helper(mapping[directory]):
                        yield f"/{directory}{sub_dir}"
            else:
                yield f" {mapping}"

        for point in helper(self.raw_config):
            path, node = point.split(' ', 1)
            if path in self.paths:
                self.paths[path].append(node)
            else:
                self.paths[path] = [node]

if __name__ == "__main__":
    config = Configuration("midterm.yaml")
