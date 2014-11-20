from stewartPlatform import StewartPlatform
from vector3 import Vector3
from Queue import Queue
from time import time, sleep
from sys import exit

mSP = StewartPlatform()
targets = [(Vector3(0,0,-26), Vector3(0.0,0,0)), (Vector3(0,0,26), Vector3(0.0,0,0))]
ti = 0

instructions = Queue()
for i in range(8):
    instructions.put((('near','slow'),{'translate':'xy'}))
for i in range(4):
    instructions.put((('fast','repeat'),{}))

for i in range(8):
    instructions.put((('far','slow'),{'translate':'z'}))
for i in range(4):
    instructions.put((('fast','repeat'),{}))

for i in range(8):
    instructions.put((('far','slow'),{'translate':'xz'}))
for i in range(4):
    instructions.put((('fast','repeat'),{}))

for i in range(8):
    instructions.put((('near','slow'),{'rotate':'xy'}))
for i in range(4):
    instructions.put((('fast','repeat'),{}))

for i in range(8):
    instructions.put((('near','slow'),{'rotate':'y'}))
for i in range(4):
    instructions.put((('fast','repeat'),{}))

'''
for i in range(8):
    instructions.put((('far','slow'),{'translate':'xyz', 'rotate':'xyz'}))
for i in range(4):
    instructions.put((('fast','repeat'),{}))
'''

if __name__=="__main__":
    while(not (instructions.empty() and mSP.isAtTarget())):
        loopStart = time()
        try:
            if(mSP.isAtTarget()):
                #mSP.setTargetAnglesSuccessfully(*targets[ti])
                #ti = (ti+1)%2
                #i = instructions.get()
                #mSP.setNextPositionLinear(*(i[0]),**(i[1]))
                #mSP.setNextPositionPerlin('slow', translate='xyz', rotate='xyz')
                mSP.setNextPositionScan('slow')
            else:
                mSP.update()
            loopTime = time() - loopStart
            #print "%.3f"%(loopTime*1000)
            sleep(max(0.016 - loopTime, 0))
        except KeyboardInterrupt:
            exit()
    mSP = StewartPlatform()
