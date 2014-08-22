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
  u = snoise4(x*POSITION_SCALE + 1*PHASE, y*POSITION_SCALE + 1*PHASE, z*POSITION_SCALE + 1*PHASE, t + 1*PHASE)
  v = snoise4(x*POSITION_SCALE + 2*PHASE, y*POSITION_SCALE + 2*PHASE, z*POSITION_SCALE + 2*PHASE, t + 2*PHASE)
  w = snoise4(x*POSITION_SCALE + 3*PHASE, y*POSITION_SCALE + 3*PHASE, z*POSITION_SCALE + 3*PHASE, t + 3*PHASE)

  #magnitude
  speed = min(32, max(8, SPEED_SCALE*(snoise4(u,v,w,t)*0.5+0.5)))

  # result
  deltaDistances = (u*speed, v*speed, w*speed)
  (location.x, location.y, location.z) = (x+u, y+v, z+w)


if __name__=="__main__":
  initTime = time()
  while True:
    try:
      loopStart = time()
      update()
      loopTime = time() - loopStart
      print "%.3f us  ---  %s"%(loopTime*1000*1000, location)
      sleep(max(0.016 - loopTime, 0))
    except KeyboardInterrupt:
      exit(0)
    except Exception as e:
      print str(e)
      exit(0)
