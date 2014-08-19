from sys import path
from math import radians
from vector3 import Vector3
from stewartplatformmath import StewartPlatformMath

path.append("../ax12")
from ax12 import Ax12

class StewartPlatform:
    SCALE_RADIANS_TO_SERVO_VALUE = 1024.0/radians(300.0)
    SERVO_CENTER_ANGLE_VALUE = 520
    SERVO_MIN_ANGLE_VALUE = 220
    SERVO_MAX_ANGLE_VALUE = 820

    class PlatformPosition:
        def __init__(self):
            self.translation = Vector3()
            self.rotation = Vector3()

    def __init__(self):
        self.currentPosition = StewartPlatform.PlatformPosition()
        self.targetPosition = StewartPlatform.PlatformPosition()
        self.servos = Ax12()
        self.angles = StewartPlatformMath()
        self.angles.applyTranslationAndRotation(self.currentPosition.translation, self.currentPosition.rotation)

        for s in range(6):
            servoValue = StewartPlatform.getServoAngleValue(s, self.angles.alpha[s])
            self.servos.moveSpeedRW((s+1), servoValue, 450)
            #sleep(0.01)
        servos.action()
        sleep(4)

    def getServoAngleValue(servoNumber, angleRadians):
        angPos = StewartPlatform.SERVO_CENTER_ANGLE_VALUE + int(angleRadians*StewartPlatform.SCALE_RADIANS_TO_SERVO_VALUE)
        angPos = min(StewartPlatform.SERVO_MAX_ANGLE_VALUE, max(StewartPlatform.SERVO_MIN_ANGLE_VALUE, angPos))
        return angPos if servoNumber%2==1 else 1024-angPos
