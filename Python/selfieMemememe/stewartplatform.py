from math import radians
from time import sleep
from sys import path
from vector3 import Vector3
from stewartplatformmath import StewartPlatformMath

path.append("../ax12")
from ax12 import Ax12

class StewartPlatform:
    SCALE_RADIANS_TO_SERVO_VALUE = 1024.0/radians(300.0)
    SERVO_CENTER_ANGLE_VALUE = 520
    SERVO_MIN_ANGLE_VALUE = 220
    SERVO_MAX_ANGLE_VALUE = 820

    @staticmethod
    def getServoAngleValue(servoNumber, angleRadians):
        angPos = StewartPlatform.SERVO_CENTER_ANGLE_VALUE + int(angleRadians*StewartPlatform.SCALE_RADIANS_TO_SERVO_VALUE)
        angPos = min(StewartPlatform.SERVO_MAX_ANGLE_VALUE, max(StewartPlatform.SERVO_MIN_ANGLE_VALUE, angPos))
        return angPos if servoNumber%2==1 else 1024-angPos

    def __init__(self):
        self.currentValues = [0]*6
        self.targetValues = [0]*6
        self.servos = Ax12()
        self.angles = StewartPlatformMath()
        self.angles.applyTranslationAndRotation()

        for s in range(6):
            self.targetValues[i] = self.angles.alpha[s]
            self.currentValues[i] = self.targetValues[i]
            servoValue = StewartPlatform.getServoAngleValue(s, self.currentValues[i])
            self.servos.moveSpeedRW((s+1), servoValue, 450)
        self.servos.action()
        sleep(4)

