from math import radians, pi, cos, sin, asin, atan2, sqrt
from vector3 import Vector3

class StewartPlatformMath:
    # real angles from platform v1.0
    baseAngles = [308.5, 351.5, 68.5, 111.5, 188.5, 231.5 ]
    platformAngles = [286.10, 13.9, 46.1, 133.9, 166.1, 253.9]
    beta = [-8*pi/3, pi/3, 0, -pi, -4*pi/3, -7*pi/3]

    # real measurements from platform v1.0
    SCALE_INITIAL_HEIGHT = 239
    SCALE_BASE_RADIUS = 140
    SCALE_PLATFORM_RADIUS = 32
    SCALE_HORN_LENGTH = 36
    SCALE_LEG_LENGTH = 270

    def __init__(self, scale=1.0):
        self.translation = Vector3()
        self.rotation = Vector3()
        self.initialHeight = Vector3(0, 0, scale*StewartPlatformMath.SCALE_INITIAL_HEIGHT)
        self.baseJoint = []
        self.platformJoint = []
        self.q = []
        self.l = []
        self.alpha = []
        self.baseRadius = scale*StewartPlatformMath.SCALE_BASE_RADIUS
        self.platformRadius = scale*StewartPlatformMath.SCALE_PLATFORM_RADIUS
        self.hornLength = scale*StewartPlatformMath.SCALE_HORN_LENGTH
        self.legLength = scale*StewartPlatformMath.SCALE_LEG_LENGTH;

        for angle in self.baseAngles:
            mx = self.baseRadius*cos(radians(angle))
            my = self.baseRadius*sin(radians(angle))
            self.baseJoint.append(Vector3(mx, my))

        for angle in self.platformAngles:
            mx = self.platformRadius*cos(radians(angle))
            my = self.platformRadius*sin(radians(angle))
            self.platformJoint.append(Vector3(mx, my))

        self.q = [Vector3()]*len(self.platformAngles)
        self.l = [Vector3()]*len(self.platformAngles)
        self.alpha = [0]*len(self.beta)

    def calcQ(self):
        for (i,joint) in enumerate(self.platformJoint):
            # rotation
            self.q[i].x = (cos(self.rotation.z)*cos(self.rotation.y)*joint.x +
                (-sin(self.rotation.z)*cos(self.rotation.x)+cos(self.rotation.z)*sin(self.rotation.y)*sin(self.rotation.x))*joint.y +
                (sin(self.rotation.z)*sin(self.rotation.x)+cos(self.rotation.z)*sin(self.rotation.y)*cos(self.rotation.x))*joint.z)

            self.q[i].y = (sin(self.rotation.z)*cos(self.rotation.y)*joint.x +
                (cos(self.rotation.z)*cos(self.rotation.x)+sin(self.rotation.z)*sin(self.rotation.y)*sin(self.rotation.x))*joint.y +
                (-cos(self.rotation.z)*sin(self.rotation.x)+sin(self.rotation.z)*sin(self.rotation.y)*cos(self.rotation.x))*joint.z)

            self.q[i].z = (-sin(self.rotation.y)*self.platformJoint[i].x +
                cos(self.rotation.y)*sin(self.rotation.x)*joint.y +
                cos(self.rotation.y)*cos(self.rotation.x)*joint.z)

            # translation
            self.q[i] += (self.translation + self.initialHeight)
            self.l[i] = self.q[i] - self.baseJoint[i]

    def calcAlpha(self):
        for (i,joint) in enumerate(self.baseJoint):
            L = self.l[i].magnitudeSquared()-(self.legLength**2)+(self.hornLength**2)
            M = 2*self.hornLength*(self.q[i].z-joint.z)
            N = 2*self.hornLength*(cos(self.beta[i])*(self.q[i].x-joint.x) + sin(self.beta[i])*(self.q[i].y-joint.y))
            try:
                self.alpha[i] = asin(L/sqrt(M*M+N*N)) - atan2(N,M)
            except ValueError:
                self.alpha[i] = float('NaN')

    def applyTranslationAndRotation(self, t=Vector3(), r=Vector3()):
        self.rotation = r.copy()
        self.translation = t.copy()
        self.calcQ()
        self.calcAlpha()
        return list(self.alpha)
