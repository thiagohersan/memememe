from time import sleep, time
from sys import exit
from Queue import Queue
from liblo import *
from stewartPlatform import StewartPlatform

class State:
    (WAITING, SEARCHING, LOOKING, SCANNING) = range(4)

class OscServer(ServerThread):
    def __init__(self):
        ServerThread.__init__(self, 8888)

    @make_method('/memememe/stop', '')
    def stop_callback(self, path, args):
        global mState
        print "%s"%path
        mPlatform.stop()
        mState = State.WAITING

    @make_method('/memememe/look', 'ii')
    def look_callback(self, path, args):
        global mState
        (x,y) = map(lambda v:min(1, max(-1, v)), args)
        print "%s = (%d, %d)"%(path, x,y)
        mLookQueue.put((x,y))
        mState = State.LOOKING

    @make_method('/memememe/reset', '')
    def reset_callback(self, path, args):
        global mState
        print "%s"%path
        mPlatform.stop()
        mPlatform.setNextPositionLinear('slow', 'far', translate='xyz', rotate='xyz')
        mLookQueue = Queue()
        mState = State.SEARCHING

    @make_method('/memememe/search', '')
    def search_callback(self, path, args):
        global mState, mLastHeartbeat
        print "%s"%path
        mPlatform.stop()
        mLastHeartbeat = time()
        mLookQueue = Queue()
        mState = State.SEARCHING

    @make_method('/memememe/scan', '')
    def scan_callback(self, path, args):
        global mState, mLastHeartbeat
        print "%s"%path
        mPlatform.stop()
        mLastHeartbeat = time()
        mLookQueue = Queue()
        mState = State.SCANNING

    @make_method(None, None)
    def default_callback(self, path, args):
        print "%s"%path

def setup():
    global mServer, mState, mPlatform, mLookQueue, mLastLook, mLastHeartbeat

    mLookQueue = Queue()
    mLastLook = time()
    mLastHeartbeat = time()
    mState = State.WAITING

    try:
        mServer = OscServer()
        mServer.start()
    except ServerError as e:
        print str(e)
        exit(0)
    try:
        mPlatform = StewartPlatform()
    except Exception as e:
        print str(e)
        exit(0)

def loop():
    global mLastLook, mLastHeartbeat
    if(mState == State.SEARCHING):
        if(time()-mLastHeartbeat > 5):
          mPlatform.stop()
        else:
          if(mPlatform.isAtTarget()):
              mPlatform.setNextPositionPerlin('slow', translate='xyz', rotate='xyz')
          mPlatform.update()
    elif(mState == State.SCANNING):
        if(time()-mLastHeartbeat > 5):
          mPlatform.stop()
        else:
          if(mPlatform.isAtTarget() and (time()-mLastLook > 0.2)):
              mPlatform.setNextPositionScan('slow')
              mLastLook = time()
          mPlatform.update()
    elif(mState == State.LOOKING):
        if(mPlatform.isAtTarget() and not mLookQueue.empty() and (time()-mLastLook > 0.5)):
            (x,y) = mLookQueue.get()
            mPlatform.setNextPositionLook(x=x, y=y)
            mLastLook = time()
        mPlatform.update()

def cleanUp():
    print  "Stoping OSCServer"
    mServer.stop()
    mServer.free()

if __name__=="__main__":
    setup()

    while True:
        try:
            loopStart = time()
            loop()
            loopTime = time() - loopStart
            sleep(max(0.016 - loopTime, 0))
        except KeyboardInterrupt:
            cleanUp()
            exit(0)
        except Exception as e:
            print "loop caught: "+str(e)
            cleanUp()
            setup()
