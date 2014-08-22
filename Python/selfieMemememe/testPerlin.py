from math import pi
from time import time, sleep
from vector3 import Vector3
from noise import snoise4

MOVE_LONG_DISTANCE = 32
PHASE = 4*pi
TIME_SCALE = 1.2
POSITION_SCALE = 0.03 # this is roughly 1/MOVE_LONG_DISTANCE
SPEED_SCALE = 20.0

location = Vector3()
initTime = 0

def update():
  global location
  t = (time()-initTime) * TIME_SCALE
  (x,y,z) = (location.x, location.y, location.z)

  # direction
  (u,v,w) = map(lambda p: snoise4(x*POSITION_SCALE + p*PHASE, y*POSITION_SCALE + p*PHASE, z*POSITION_SCALE + p*PHASE, t + 1*PHASE), range(1,4))

  #magnitude
  speed = min(32, max(8, SPEED_SCALE*(snoise4(u,v,w,t)*0.5+0.5)))

  # result
  deltaDistances = map(lambda d: d*speed, (u,v,w))
  location += Vector3(*(deltaDistances))

if __name__=="__main__":
  initTime = time()
  while True:
    try:
      loopStart = time()
      update()
      loopTime = time() - loopStart
      print(loopTime*1000*1000)
      print "%s"%(location)
      sleep(max(0.016 - loopTime, 0))
    except KeyboardInterrupt:
      exit(0)
    except Exception as e:
      print str(e)
      exit(0)
