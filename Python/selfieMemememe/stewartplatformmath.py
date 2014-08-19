from math import radians, pi, cos, sin, asin, atan2, sqrt
from vector3 import Vector3

class StewartPlatformMath:
    # real angles from platform v1.0
    baseAngles = [308.5, 351.5, 68.5, 111.5, 188.5, 231.5 ]
    platformAngles = [286.10, 13.9, 46.1, 133.9, 166.1, 253.9]
    beta = [-8*pi/3, pi/3, 0, -pi, -4*pi/3, -7*pi/3]

    # real measurements from platform v1.0
    SCALE_INITIAL_HEIGHT = 250
    SCALE_BASE_RADIUS = 140
    SCALE_PLATFORM_RADIUS = 32
    SCALE_HORN_LENGTH = 36
    SCALE_LEG_LENGTH = 270

    def __init__(self, scale=1.0):
        self.translation = Vector3()
        self.rotation = Vector3()
        self.initialHeight = Vector3(0, 0, scale*StewartPlatform.SCALE_INITIAL_HEIGHT)
        self.baseJoint = []
        self.platformJoint = []
        self.q = []
        self.l = []
        self.A = []
        self.alpha = []
        self.baseRadius = scale*StewartPlatform.SCALE_BASE_RADIUS
        self.platformRadius = scale*StewartPlatform.SCALE_PLATFORM_RADIUS
        self.hornLength = scale*StewartPlatform.SCALE_HORN_LENGTH
        self.legLength = scale*StewartPlatform.SCALE_LEG_LENGTH;

        for i in range(6):
            mx = self.baseRadius*cos(radians(self.baseAngles[i]))
            my = self.baseRadius*sin(radians(self.baseAngles[i]))
            self.baseJoint.insert(i, Vector3(mx, my))

        for i in range(6):
            mx = self.platformRadius*cos(radians(self.platformAngles[i]))
            my = self.platformRadius*sin(radians(self.platformAngles[i]))
            self.platformJoint.insert(i, Vector3(mx, my))
            self.q.insert(i,Vector3())
            self.l.insert(i,Vector3())
            self.A.insert(i,Vector3())
            self.alpha.insert(i,0)

    def calcQ(self):
        for i in range(6):
            # rotation
            self.q[i].x = (cos(self.rotation.z)*cos(self.rotation.y)*self.platformJoint[i].x +
                (-sin(self.rotation.z)*cos(self.rotation.x)+cos(self.rotation.z)*sin(self.rotation.y)*sin(self.rotation.x))*self.platformJoint[i].y +
                (sin(self.rotation.z)*sin(self.rotation.x)+cos(self.rotation.z)*sin(self.rotation.y)*cos(self.rotation.x))*self.platformJoint[i].z)

            self.q[i].y = (sin(self.rotation.z)*cos(self.rotation.y)*self.platformJoint[i].x +
                (cos(self.rotation.z)*cos(self.rotation.x)+sin(self.rotation.z)*sin(self.rotation.y)*sin(self.rotation.x))*self.platformJoint[i].y +
                (-cos(self.rotation.z)*sin(self.rotation.x)+sin(self.rotation.z)*sin(self.rotation.y)*cos(self.rotation.x))*self.platformJoint[i].z)

            self.q[i].z = (-sin(self.rotation.y)*self.platformJoint[i].x +
                cos(self.rotation.y)*sin(self.rotation.x)*self.platformJoint[i].y +
                cos(self.rotation.y)*cos(self.rotation.x)*self.platformJoint[i].z)

            # translation
            self.q[i] += (self.translation + self.initialHeight)
            self.l[i] = self.q[i] - self.baseJoint[i]

    def calcAlpha(self):
        for i in range(6):
            L = self.l[i].magnitudeSquared()-(self.legLength**2)+(self.hornLength**2)
            M = 2*self.hornLength*(self.q[i].z-self.baseJoint[i].z)
            N = 2*self.hornLength*(cos(self.beta[i])*(self.q[i].x-self.baseJoint[i].x) + sin(self.beta[i])*(self.q[i].y-self.baseJoint[i].y))
            try:
                self.alpha[i] = asin(L/sqrt(M*M+N*N)) - atan2(N,M)
            except ValueError:
                self.alpha[i] = float('NaN')

            self.A[i].x = self.hornLength*cos(self.alpha[i])*cos(self.beta[i]) + self.baseJoint[i].x
            self.A[i].y = self.hornLength*cos(self.alpha[i])*sin(self.beta[i]) + self.baseJoint[i].y
            self.A[i].z = self.hornLength*sin(self.alpha[i]) + self.baseJoint[i].z

    def applyTranslationAndRotation(self, t=Vector3(), r=Vector3()):
        self.rotation = r.copy()
        self.translation = t.copy()
        self.calcQ()
        self.calcAlpha()
