import yaml

class Configuration:
    def __init__(self, filename: str):
        self.raw_config = yaml.load(open(filename, 'r'), Loader=yaml.Loader)
        print(self.raw_config)
