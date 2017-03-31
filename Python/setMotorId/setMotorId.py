#! /usr/bin/env python
# -*- coding: utf-8 -*-

from sys import argv, path
from time import sleep

path.append("../ax12")
from ax12 import Ax12

if __name__ == "__main__":
    mServos = Ax12()

    if len(argv) > 1:
        motorId = argv[1]
        for i in range(254):
            sleep(100)
            try:
                mServos.setID(i, motorId)
            except:
                print "error resetting id from "+str(i)+" to "+str(motorId)
            else:
                print "set id to "+str(motorId)
                exit(1)
    else:
        print "ERROR"
        print "usage: python setMotorId motorNumber"
