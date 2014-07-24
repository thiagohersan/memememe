from liblo import *
import sys
import ax12
from time import sleep
from random import random
import math

class MyServer(ServerThread):
    def __init__(self):
        ServerThread.__init__(self, 8888)

    @make_method('/angles', 'ffffff')
    def foo_callback(self, path, args):
        print args
        anglesData.append(args)

def moveMotors():
    print "wrinting on the motors..."
    angles = anglesData[0]

    del anglesData[0]

    anglesInt = []
    for (i,v) in enumerate(angles):
        angRadians = math.degrees(v)
        angInt = int(angRadians*1024/300)
        angPos = 520 + angInt
        angPos = 820 if angPos > 820 else angPos
        angPos = 220 if angPos < 220 else angPos
        anglesInt.append(angPos)
        angPos = angPos if (i+1)%2==0 else 1024-angPos
        try:
            servos.moveSpeedRW(i+1,angPos,1023)
        except:
            pass
        sleep(0.03)
    servos.action()
    sleep(0.02)


def initMotors():
    global servos

    servos = ax12.Ax12()
    p = 800
    for i in range(2):
        for mt in range(1,6+1):
            pp = p if mt%2==0 else 1024-p
            servos.moveSpeedRW(mt,pp,50)
            sleep(0.01)
        servos.action()
        p = 1320 - p
        sleep(3.7)



def setup():
    global server, anglesData

    anglesData = []

    #starting oscSever
    try:
        server = MyServer()
    except ServerError, err:
        print str(err)
        sys.exit()

    print "Starting Server..."
    server.start()

    #inicializing servos
    initMotors()


if __name__=="__main__":
    setup()
    while(1):
        try :
           while 1:
            if(anglesData):
                moveMotors()
            sleep(0.1)

        except KeyboardInterrupt :
            print  "\nStoping OSCServer."
            server.stop()
