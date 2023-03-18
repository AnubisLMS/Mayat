class Checker:
    def __init__(self, path1, path2, arrLL1, arrLL2, threshold=5):
        self.path1 = path1
        self.path2 = path2
        self.arrLL1 = arrLL1
        self.arrLL2 = arrLL2
        self.threshold = threshold
        self.similarity = 0
        self.overlapping_ranges = []

    # The optimized version
    def check(self):
        """
        Check the similarities of two ASTs
        """
        arrLL1_dict = {(node.weight, node.fingerprint):node for node in self.arrLL1}

        # The overlapped (node.weight, node.fingerprint) along with the ranges it occur
        overlaps_in_arrLL2 = {}
        for node in self.arrLL2:
            key = (node.weight, node.fingerprint)
            if key in arrLL1_dict:
                if key not in overlaps_in_arrLL2:
                    overlaps_in_arrLL2[key] = [[node], 1]
                else:
                    overlaps_in_arrLL2[key][0].append(node)
                    overlaps_in_arrLL2[key][1] += 1
        
        for key in overlaps_in_arrLL2:
            overlaps_in_arrLL2[key][0] = iter(overlaps_in_arrLL2[key][0])

        num_of_same_nodes = 0

        for node in self.arrLL1:
            key = (node.weight, node.fingerprint)
            if node.weight < self.threshold:
                continue

            if key in overlaps_in_arrLL2:
                if overlaps_in_arrLL2[key][1] == 0:
                    continue
                overlaps_in_arrLL2[key][1] -= 1

            if (
                node.parent is not None
                and (node.parent.weight, node.parent.fingerprint) in overlaps_in_arrLL2
            ):
                continue

            if key in overlaps_in_arrLL2:
                num_of_same_nodes += node.weight
                node_in_arrLL2 = next(overlaps_in_arrLL2[key][0])
                self.overlapping_ranges.append((
                    node.start_pos,
                    node.end_pos,
                    node_in_arrLL2.start_pos,
                    node_in_arrLL2.end_pos
                ))

        self.similarity = num_of_same_nodes / min(len(self.arrLL1), len(self.arrLL2))
