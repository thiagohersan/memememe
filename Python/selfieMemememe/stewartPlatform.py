from math import radians, isnan
from time import sleep
from sys import path
from vector3 import Vector3
from stewartPlatformMath import StewartPlatformMath

path.append("../ax12")
from ax12 import Ax12

class StewartPlatform:
    SCALE_RADIANS_TO_SERVO_VALUE = 1024.0/radians(300.0)

    SERVO_CENTER_ANGLE_VALUE = 520
    SERVO_MIN_ANGLE_VALUE = 220
    SERVO_MAX_ANGLE_VALUE = 820

    ANGLE_MAX_SPEED = 0.1
    ANGLE_MAX_ACCELERATION = 0.005

    @staticmethod
    def getServoAngleValue(servoNumber, angleRadians):
        angPos = StewartPlatform.SERVO_CENTER_ANGLE_VALUE + int(angleRadians*StewartPlatform.SCALE_RADIANS_TO_SERVO_VALUE)
        angPos = min(StewartPlatform.SERVO_MAX_ANGLE_VALUE, max(StewartPlatform.SERVO_MIN_ANGLE_VALUE, angPos))
        return angPos if servoNumber%2==1 else 1024-angPos

    def __init__(self):
        self.targetAngle = [0]*6
        self.currentAngle = [0]*6
        self.currentSpeed = [0]*6
        self.maxSpeed = [0]*6
        self.servos = Ax12()
        self.angles = StewartPlatformMath()
        self.setTargetAnglesSuccessfully()

        for (i,targetAngle) in enumerate(self.targetAngle):
            self.currentAngle[i] = targetAngle
            servoValue = StewartPlatform.getServoAngleValue(i, self.currentAngle[i])
            self.servos.moveSpeedRW((i+1), servoValue, 450)
        self.servos.action()
        sleep(2)

    def setTargetAnglesSuccessfully(self, translation=Vector3(), rotation=Vector3()):
        self.angles.applyTranslationAndRotation(translation, rotation)
        # check for nans
        for alphaAngle in self.angles.alpha:
            if(isnan(alphaAngle)):
                return False

        # all valid angles
        for (i,alphaAngle) in enumerate(self.angles.alpha):
            self.targetAngle[i] = alphaAngle
            self.currentSpeed[i] = 0
            self.maxSpeed[i] = 0
        return True

    def isAtTarget(self):
        for (i,targetAngle) in enumerate(self.targetAngle):
            if(abs(targetAngle-self.currentAngle[i]) > StewartPlatform.ANGLE_MAX_ACCELERATION):
                return False
        return True

    def update(self):
        for (i,targetAngle) in enumerate(self.targetAngle):
            if(abs(targetAngle - self.currentAngle[i]) <= (self.maxSpeed[i]**2)/(2*StewartPlatform.ANGLE_MAX_ACCELERATION)):
                self.currentSpeed[i] = max(self.currentSpeed[i]-StewartPlatform.ANGLE_MAX_ACCELERATION, 0)
            else:
                self.currentSpeed[i] = min(self.currentSpeed[i]+StewartPlatform.ANGLE_MAX_ACCELERATION, StewartPlatform.ANGLE_MAX_SPEED)
            self.maxSpeed[i] = max(self.maxSpeed[i], self.currentSpeed[i])

            if(targetAngle > self.currentAngle[i]):
                self.currentAngle[i] = min(self.currentAngle[i]+self.currentSpeed[i], targetAngle)
            elif(targetAngle < self.currentAngle[i]):
                self.currentAngle[i] = max(self.currentAngle[i]-self.currentSpeed[i], targetAngle)
            servoValue = StewartPlatform.getServoAngleValue(i, self.currentAngle[i])

            try:
                self.servos.moveSpeedRW((i+1), servoValue, 256)
            except:
                try:
                    self.servos.moveSpeedRW((i+1), servoValue, 256)
                except:
                    print "3rd time. oooops."
            
        self.servos.action()
