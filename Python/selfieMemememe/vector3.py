from copy import deepcopy

class Vector3:
    def __init__(self, x=0.0, y=0.0, z=0.0):
        self.x = x
        self.y = y
        self.z = z

    def __str__(self):
        return "[ %.3f, %.3f, %.3f ]"%(self.x, self.y, self.z)
    def __repr__(self):
        return self.__str__()

    def __add__(self, other):
        return Vector3(self.x+other.x, self.y+other.y, self.z+other.z)

    def __sub__(self, other):
        return Vector3(self.x-other.x, self.y-other.y, self.z-other.z)

    def __eq__(self, other):
        return (abs(self.x-other.x)+abs(self.y-other.y)+abs(self.z-other.z)) < 1e-9

    def copy(self):
        return deepcopy(self)

    def magnitudeSquared(self):
        return (self.x*self.x + self.y*self.y + self.z*self.z)

    def constrain(self, lo, hi):
        self.x = min(hi, max(lo, self.x))
        self.y = min(hi, max(lo, self.y))
        self.z = min(hi, max(lo, self.z))
