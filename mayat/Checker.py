class Checker:
    def __init__(self, path1, path2, arrLL1, arrLL2, threshold=5):
        self.path1 = path1
        self.path2 = path2
        self.arrLL1 = arrLL1
        self.arrLL2 = arrLL2
        self.threshold = threshold
        self.similarity = 0

    # The optimized version
    def check(self):
        """
        Check the similarities of two ASTs
        """
        arrLL1_set = {(node.weight, node.fingerprint) for node in self.arrLL1}

        # The overlapped (node.weight, node.fingerprint) along with its occurrence
        overlaps = {}
        for node in self.arrLL2:
            key = (node.weight, node.fingerprint)
            if key in arrLL1_set:
                if key not in overlaps:
                    overlaps[key] = 1
                else:
                    overlaps[key] += 1

        num_of_same_nodes = 0
        arrLL = self.arrLL1 if len(self.arrLL1) < len(self.arrLL2) else self.arrLL2

        for node in arrLL:
            key = (node.weight, node.fingerprint)
            if node.weight < self.threshold:
                continue

            if key in overlaps:
                if overlaps[key] == 0:
                    continue
                overlaps[key] -= 1

            if (
                node.parent is not None
                and (node.parent.weight, node.parent.fingerprint) in overlaps
            ):
                continue

            if key in overlaps:
                num_of_same_nodes += node.weight

        self.similarity = num_of_same_nodes / len(arrLL)
