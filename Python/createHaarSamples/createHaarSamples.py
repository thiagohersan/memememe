from os import listdir, path, makedirs
from re import sub
from subprocess import call
from time import sleep
from random import random

PATH_POS_IMAGES = "../../Processing/PositiveCollectionTagger/data/positive-clean-00_cropped"
PATH_NEG_IMAGES = "data/negative-tutorial-haartraining"
PATH_SAMPLE_IMAGES = "data/"+PATH_POS_IMAGES.split('/')[-1]+"_samples"

if __name__=="__main__":
    # make sure output dir exists
    if(not path.isdir(PATH_SAMPLE_IMAGES)):
        makedirs(PATH_SAMPLE_IMAGES)

    # get list of negative images
    negImageFilenames = [f for f in listdir(PATH_NEG_IMAGES) if path.isfile(path.join(PATH_NEG_IMAGES,f)) and f.lower().endswith("jpg")]
    negImageCollectionFile = open(PATH_NEG_IMAGES+".txt", "w")
    for f in negImageFilenames:
        negImageCollectionFile.write(PATH_NEG_IMAGES.split('/')[-1]+"/"+f)
        negImageCollectionFile.write("\n")

    # get list of images
    posImageFilenames = [f for f in listdir(PATH_POS_IMAGES) if path.isfile(path.join(PATH_POS_IMAGES,f))]

    # run the create command for each image
    for f in posImageFilenames:
        cmd = "opencv_createsamples"
        cmd += " -img "+path.join(PATH_POS_IMAGES,f)
        cmd += " -bg "+PATH_NEG_IMAGES+".txt"
        cmd += " -info "+path.join(PATH_SAMPLE_IMAGES,sub("(?i)jpg","txt",f))
        cmd += " -num 10 -maxxangle 0.0 -maxyangle 0.0 -maxzangle 0.4 -bgcolor 255 -bgthresh 8 -w 48 -h 48"
        print cmd

        '''
        opencv_createsamples -img positive-clean-00_cropped/IMG_1620.JPG -bg negative-tutorial-haartraining.txt -info 1620_foo.txt -num 10 
        -maxxangle 0.0 -maxyangle 0.0 -maxzangle 0.4 -bgcolor 255 -bgthresh 8 -w 48 -h 48
        '''
