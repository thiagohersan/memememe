from time import sleep, time
from sys import exit
from Queue import Queue
from liblo import *
from stewartPlatform import StewartPlatform

class State:
    (WAITING, SEARCHING, LOOKING) = range(3)

class OscServer(ServerThread):
    def __init__(self):
        ServerThread.__init__(self, 8888)
        self.mState = State.SEARCHING

    @make_method('/memememe/stop', '')
    def stop_callback(self, path, args):
        print "%s"%path
        mPlatform.stop()
        self.mState = State.WAITING

    @make_method('/memememe/look', 'ii')
    def look_callback(self, path, args):
        (x,y) = map(lambda v:min(1, max(-1, v)), args)
        print "%s = (%d, %d)"%(path, x,y)
        mLookQueue.put((x,y))
        self.mState = State.LOOKING

    @make_method('/memememe/search', '')
    def search_callback(self, path, args):
        print "%s"%path
        mPlatform.stop()
        mLookQueue = Queue()
        self.mState = State.SEARCHING

    @make_method(None, None)
    def default_callback(self, path, args):
        print "%s"%path

def setup():
    global mServer, mPlatform, mLookQueue

    mLookQueue = Queue()

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
    if(mServer.mState == State.SEARCHING):
        if(mPlatform.isAtTarget()):
            mPlatform.setNextPositionPerlin(translate='xyz')
        mPlatform.update()
    elif(mServer.mState == State.LOOKING):
        if(mPlatform.isAtTarget() and not mLookQueue.empty()):
            (x,y) = mLookQueue.get()
            mPlatform.setNextPositionLook(x=x, y=y)
        mPlatform.update()

def cleanUp():
    print  "Stoping OSCServer"
    mServer.stop()

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
