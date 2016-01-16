from time import sleep
from random import random
import math
import OSC
from OSC import OSCServer
import time, threading
import sys
sys.path.append("../")
from ax12 import ax12

server = OSC.ThreadingOSCServer( ("meme00.local", 8888))


def _oscHandler(addr, tags, stuff, source):
    addrTokens = addr.lstrip('/').split('/')
    if (addrTokens[0].lower() == "angles"):
        angles = []
        for (i,v) in enumerate(stuff):
            ang = math.degrees(v)
            angInt = int(ang*1024/300)
            angPos = 520 + angInt
            angPos = 850 if angPos > 850 else angPos
            angPos = 310 if angPos < 310 else angPos
            angles.append(angPos)
            angPos = angPos if (i+1)%2==1 else 1024-angPos
            servos.moveSpeedRW(i+1,angPos,244)
            sleep(0.02)
        print angles
        servos.action()
        sleep(0.02)
    elif (addrTokens[0].lower() == "teste"):
        print stuff

server.addMsgHandler('default', _oscHandler) # adding our function

print "\nStarting OSCServer. Use ctrl-C to quit."



serverThread = threading.Thread( target = server.serve_forever )
serverThread.start()

servos = ax12.Ax12()
MINANGLE = 310
MAXANGLE = 850

p = 500

try :
    #set max min angles in the motors
    for mt in range (1,6+1):
        if(mt%2==1):
            servos.setAngleLimit(mt, MINANGLE, MAXANGLE)
        else:
            servos.setAngleLimit(mt, 1024-MAXANGLE, 1024-MINANGLE)
        #need to sleep between each command
        sleep(0.05)

    for i in range(2):
        for mt in range(1,6+1):
            pp = p if mt%2==1 else 1024-p
            servos.moveSpeedRW(mt,pp,50)
            sleep(0.01)
        servos.action()
        p = 1300 - p
        sleep(3.7)
    while 1:
        sleep(1)

except KeyboardInterrupt :
    print "\nClosing OSCServer."
    server.close()
    print "Waiting for Server-thread to finish"
    serverThread.join() ##!!!
    print "Done"
