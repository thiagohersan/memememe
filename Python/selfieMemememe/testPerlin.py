from math import pi
from time import time
from vector3 import Vector3
from noise import snoise4

NOISE_COMPLEXITY = 0.03 # this is roughly 1/MOVE_LONG_DISTANCE
TIME_SCALE = 1.2
SPEED_SCALE = 100.0
PHASE = 4*pi

location = Vector3()

def update():
  t = time() * TIME_SCALE
  (x,y,z) = (location.x, location.y, location.z)

  # direction
  u = snoise4(x*NOISE_COMPLEXITY + 1*PHASE, y*NOISE_COMPLEXITY + 1*PHASE, z*NOISE_COMPLEXITY + 1*PHASE, t + 1*PHASE)
  v = snoise4(x*NOISE_COMPLEXITY + 2*PHASE, y*NOISE_COMPLEXITY + 2*PHASE, z*NOISE_COMPLEXITY + 2*PHASE, t + 3*PHASE)
  w = snoise4(x*NOISE_COMPLEXITY + 3*PHASE, y*NOISE_COMPLEXITY + 3*PHASE, z*NOISE_COMPLEXITY + 3*PHASE, t + 3*PHASE)

  #magnitude
  speed = min(32, max(8, SPEED_SCALE*(snoise4(u,v,w,t)*0.5+0.5)))
  (location.x, location.y, location.z) = map(lambda loc,dir: dir*speed+loc, (x,y,z), (u,v,w))

if __name__=="__main__":
  while True:
    try:
      loopStart = time()
      update()
      loopTime = time() - loopStart
      print(loopTime*1000*1000)
    except KeyboardInterrupt:
      exit(0)
    except Exception as e:
      exit(0)
