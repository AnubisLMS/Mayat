class Result:
    def __init__(self):
        self.raw_command = ""
        self.current_datetime = None
        self.header_info = []
        self.checkers = []
        self.duration = 0
    
    def __str__(self):
        output = ""
        output += self.raw_command + '\n'
        output += str(self.current_datetime) + '\n\n'
        
        for info in self.header_info:
            output += info + '\n'
        
        output += '\n'
        output += f"{self.duration}s\n\n"

        for c in self.checkers:
            output += f"{c.path1} - {c.path2}:\t{c.similarity:%}\n"
        
        return output
    
    def __repr__(self):
        return str(self)
