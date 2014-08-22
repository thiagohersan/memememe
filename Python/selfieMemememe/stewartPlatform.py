from math import radians, isnan
from random import uniform, choice
from time import sleep
from sys import path
from vector3 import Vector3
from stewartPlatformMath import StewartPlatformMath

path.append("../ax12")
from ax12 import Ax12

class PlatformPosition:
    def __init__(self, translation=Vector3(), rotation=Vector3()):
        self.translation = translation
        self.rotation = rotation
    def getTranslationAsList(self):
        return [self.translation.x, self.translation.y, self.translation.z]
    def getRotationAsList(self):
        return [self.rotation.x, self.rotation.y, self.rotation.z]

class StewartPlatform:
    SCALE_RADIANS_TO_SERVO_VALUE = 1024.0/radians(300.0)

    SERVO_CENTER_ANGLE_VALUE = 512
    SERVO_MIN_ANGLE_VALUE = 250
    SERVO_MAX_ANGLE_VALUE = 812

    SERVO_SPEED_LIMIT = 0.1
    SERVO_ACCELERATION = 0.03

    MOVE_SHORT_DISTANCE = 10
    MOVE_LONG_DISTANCE = 40
    MOVE_SHORT_ANGLE = 0.3
    MOVE_LONG_ANGLE = 1.0

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
        self.currentSpeedLimit = StewartPlatform.SERVO_SPEED_LIMIT
        self.servos = Ax12()
        self.angles = StewartPlatformMath()
        self.currentPosition = PlatformPosition()
        self.lastPosition = PlatformPosition()

        self.setTargetAnglesSuccessfully()

        for (i,targetAngle) in enumerate(self.targetAngle):
            self.currentAngle[i] = targetAngle
            servoValue = StewartPlatform.getServoAngleValue(i, self.currentAngle[i])
            self.servos.moveSpeedRW((i+1), servoValue, 450)
        self.servos.action()
        sleep(1)

    # ****Very Important*****
    # Possible options/parameters:
    #   'fast' := moves fast (default)
    #   'slow' := moves slow
    #   'repeat' := repeats last movement (ignores all other commmands except 'fast'/'slow')
    #   'long'/'far' := moves large distance (default)
    #   'short'/'near' := moves small distance
    #   'translate' := list of which axis to translate
    #   'rotate' := list of which axis to rotate
    # Examples:
    #   setNextPosition('fast', 'long', translate='xyz', rotate='zy')
    #   setNextPosition(translate='xyz', rotate='zy')
    #   setNextPosition('slow', rotate='zxy')
    #   setNextPosition('slow', 'repeat')
    def setNextPosition(self, *args, **kwargs):
        if('repeat' in args):
            if('slow' in args):
                self.currentSpeedLimit = StewartPlatform.SERVO_SPEED_LIMIT/2
            elif('fast' in args):
                self.currentSpeedLimit = StewartPlatform.SERVO_SPEED_LIMIT
            self.setTargetAnglesSuccessfully(self.lastPosition.translation, self.lastPosition.rotation)
        else:
            if('slow' in args):
                self.currentSpeedLimit = StewartPlatform.SERVO_SPEED_LIMIT/2
            else:
                self.currentSpeedLimit = StewartPlatform.SERVO_SPEED_LIMIT

            # pick new valid position
            translateArg = kwargs.get('translate', '')
            rotateArg = kwargs.get('rotate', '')

            done = False
            deltaDistances = [0]*3
            deltaAngles = [0]*3
            if(('short' in args) or ('near' in args)):
                # randomly move towards -MOVE_SHORT_X or +MOVE_SHORT_X
                deltaDistances = map(lambda x:choice([-1, 1])*x, [StewartPlatform.MOVE_SHORT_DISTANCE]*3)
                deltaAngles = map(lambda x:choice([-1, 1])*x, [StewartPlatform.MOVE_SHORT_ANGLE]*3)
            else:
                # add MOVE_LONG_X towards side with more room
                deltaDistances = map(lambda x:(-1,1)[x<0]*uniform(0.8,1.2)*StewartPlatform.MOVE_LONG_DISTANCE, self.currentPosition.getTranslationAsList())
                deltaAngles = map(lambda x:(-1,1)[x<0]*uniform(0.8,1.2)*StewartPlatform.MOVE_LONG_ANGLE, self.currentPosition.getRotationAsList())

            while not done:
                translation = Vector3(
                    deltaDistances[0] if 'x' in translateArg else 0,
                    deltaDistances[1] if 'y' in translateArg else 0,
                    deltaDistances[2] if 'z' in translateArg else 0) + self.currentPosition.translation
                rotation = Vector3(
                    deltaAngles[0] if 'x' in rotateArg else 0,
                    deltaAngles[1] if 'y' in rotateArg else 0,
                    deltaAngles[2] if 'z' in rotateArg else 0) + self.currentPosition.rotation

                done = self.setTargetAnglesSuccessfully(translation, rotation)
                deltaDistances = map(lambda x:uniform(0.666,0.8)*x, deltaDistances)
                deltaAngles = map(lambda x:uniform(0.666,0.8)*x, deltaAngles)

    def setTargetAnglesSuccessfully(self, translation=Vector3(), rotation=Vector3()):
        alphaAngles = self.angles.applyTranslationAndRotation(translation, rotation)
        # check for nans
        for angle in alphaAngles:
            if(isnan(angle)):
                return False

        # all valid angles
        for (i,angle) in enumerate(alphaAngles):
            self.targetAngle[i] = angle
            self.currentSpeed[i] = StewartPlatform.SERVO_ACCELERATION/10
            self.maxSpeed[i] = 0
        # set positions
        self.lastPosition = self.currentPosition
        self.currentPosition = PlatformPosition(translation, rotation)
        return True

    def isAtTarget(self):
        for (i,targetAngle) in enumerate(self.targetAngle):
            if((abs(targetAngle-self.currentAngle[i]) > self.currentSpeed[i]) and (self.currentSpeed[i] > 0)):
                return False
        return True

    def update(self):
        for (i,targetAngle) in enumerate(self.targetAngle):
            if(abs(targetAngle - self.currentAngle[i]) <= (self.maxSpeed[i]**2)/(2*StewartPlatform.SERVO_ACCELERATION)):
                self.currentSpeed[i] = max(self.currentSpeed[i]-StewartPlatform.SERVO_ACCELERATION, 0)
            else:
                self.currentSpeed[i] = min(self.currentSpeed[i]+StewartPlatform.SERVO_ACCELERATION, self.currentSpeedLimit)
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
