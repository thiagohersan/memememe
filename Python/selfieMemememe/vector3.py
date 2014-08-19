from time import sleep
from math import sqrt

class Vector3:
    def __init__(self, x_=0.0, y_=0.0, z_=0.0):
        self.x = x_
        self.y = y_
        self.z = z_

    def __str__(self):
        return "[ "+str(self.x)+", "+str(self.y)+", "+str(self.z)+" ]"

    def __add__(self, other):
        return Vector3(self.x+other.x, self.y+other.y, self.z+other.z)

    def __sub__(self, other):
        return Vector3(self.x-other.x, self.y-other.y, self.z-other.z)

    def set(self, other):
        self.x = other.x
        self.y = other.y
        self.z = other.z

    def add(self, other):
        self.x += other.x
        self.y += other.y
        self.z += other.z

    def sub(self, other):
        self.x -= other.x
        self.y -= other.y
        self.z -= other.z

    def magnitudeSquared(self):
        return (self.x*self.x + self.y*self.y + self.z*self.z)
    def magnitude(self):
        return sqrt(self.x*self.x + self.y*self.y + self.z*self.z)
