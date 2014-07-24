import ax12
from time import sleep
from random import random
import math
import OSC
from OSC import OSCServer
import time, threading

server = OSC.ThreadingOSCServer( ("meme00.local", 8888))


def _oscHandler(addr, tags, stuff, source):
    addrTokens = addr.lstrip('/').split('/')
    if (addrTokens[0].lower() == "angles"):
        angles = []
        for (i,v) in enumerate(stuff):
            ang = math.degrees(v)
            angInt = int(ang*1024/300 )
            angPos = 520 + angInt
            angPos = 820 if angPos > 820 else angPos
            angPos = 220 if angPos < 220 else angPos
            angles.append(angPos)
            angPos = angPos if (i+1)%2==0 else 1024-angPos
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
p = 500

try :
    for i in range(2):
        for mt in range(1,6+1):
            pp = p if mt%2==0 else 1024-p
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
