#! /usr/bin/env python
# -*- coding: utf-8 -*-

from pytumblr import TumblrRestClient
from xml.dom import minidom
from os.path import join
import time
import os

if __name__ == "__main__":
    DATA_DIR = "data"
    TAGS = ["#selfie", "selfie"];

    keyDoc = minidom.parse('keys.xml')
    keys = {}
    for i in keyDoc.getElementsByTagName('string'):
        keys[i.attributes['name'].value] = i.childNodes[0].data
    
    mTumblr = TumblrRestClient(keys['consumer_key'],
                               keys['consumer_secret'],
                               keys['oauth_token'],
                               keys['oauth_token_secret'])

    for directory in [d for d in os.listdir(DATA_DIR) if os.path.isdir(join(DATA_DIR, d))]:
        for filename in [f for f in os.listdir(join(DATA_DIR, directory)) if f.startswith("memememeselfie") and f.endswith(".jpg")]:
            fullPath = join(DATA_DIR, directory, filename)
            print "POSTING %s"%fullPath
            mTumblr.create_photo(directory, state="published", tags=TAGS, data=fullPath)
            time.sleep(1)
            os.remove(fullPath)
