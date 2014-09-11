from getopt import getopt
from sys import argv
from os import listdir, path, makedirs, remove
from re import sub
from subprocess import check_call
from time import sleep
from shutil import rmtree, copytree

DIR_PROCESSING_DATA = "../../Processing/PositiveCollectionTagger/data"
DIR_POS_IMAGES = DIR_PROCESSING_DATA+"/positive-clean"
DIR_POS_IMAGES_CROPPED = DIR_PROCESSING_DATA+"/positive-clean-cropped"
DIR_DATA = "data"
DIR_NEG_IMAGES = DIR_DATA+"/negative-tutorial-haartraining"
DIR_SAMPLE_IMAGES = DIR_DATA+"/positive-clean-cropped-samples"
DIR_HAAR_DATA = DIR_DATA+"/clean-cropped-haardata"

FILE_NEG_COLLECTION = DIR_NEG_IMAGES+"/"+DIR_NEG_IMAGES.split("/")[-1]+".txt"
FILE_SAMPLE_COLLECTION = DIR_SAMPLE_IMAGES+"/"+DIR_SAMPLE_IMAGES.split("/")[-1]+".txt"
FILE_SAMPLE_VEC = DIR_SAMPLE_IMAGES+".vec"

if __name__=="__main__":
    (createSamples, createVec, trainCascade) = (False, False, False)
    opts, args = getopt(argv[1:],"svt",["createSamples","createVec","trainCascade"])
    for opt, arg in opts:
        if(opt in ("--createSamples","-s")):
            createSamples = True
        elif(opt in ("--createVec","-v")):
            createVec = True
        elif(opt in ("--trainCascade","-t")):
            trainCascade = True

    # make sure output dir exists
    if(not path.isdir(DIR_SAMPLE_IMAGES)):
        makedirs(DIR_SAMPLE_IMAGES)

    # get list of negative images.
    #
    # this is equivalent to:
    #     cd data/negative-tutorial-haartraining
    #     ls -l1 *.jpg > negative-tutorial-haartraining.txt
    negImageCollectionFile = open(FILE_NEG_COLLECTION, "w")
    negImageFilenames = [f for f in listdir(DIR_NEG_IMAGES) if path.isfile(path.join(DIR_NEG_IMAGES,f)) and f.lower().endswith("jpg")]
    for f in negImageFilenames:
        negImageCollectionFile.write(f)
        negImageCollectionFile.write("\n")
    negImageCollectionFile.close()

    # get list of positive image files
    posImageFilenames = [f for f in listdir(DIR_POS_IMAGES_CROPPED) if path.isfile(path.join(DIR_POS_IMAGES_CROPPED,f))]

    # createSamples: this creates a bunch of sample images
    #     with images from DIR_POS_IMAGES_CROPPED on top of a negative image,
    #     and its accompanying collection files
    if(createSamples):
        # run the create command for each image
        for f in posImageFilenames:
            check_call(["opencv_createsamples",
                "-img", path.join(DIR_POS_IMAGES_CROPPED,f),
                "-bg", FILE_NEG_COLLECTION,
                "-info", path.join(DIR_SAMPLE_IMAGES,sub("(?i)jpg","txt",f)),
                "-num", "128",
                "-maxxangle", "0.0",
                "-maxyangle", "0.0",
                "-maxzangle", "0.3",
                "-bgcolor", "255",
                "-bgthresh", "8",
                "-w", "48",
                "-h", "48"])
            sleep(1)

    # createVec: this creates a .vec file from the
    #     positive sample images in the data/ directory.
    #     if there are no positive samples in the data/ directory,
    #     then it looks for some in the Processing directory
    if(createVec):
        # get mega list of sample images
        posImageCollectionFilenames = [f for f in listdir(DIR_SAMPLE_IMAGES) if path.isfile(path.join(DIR_SAMPLE_IMAGES,f)) and f.lower().endswith("txt")]

        # if there are no collection files in data/ directory
        #     and no overall collection file has been created,
        #     then, copy clean images and collection file from Processing directory
        if((not posImageCollectionFilenames) and (not path.isfile(FILE_SAMPLE_COLLECTION))):
            rmtree(DIR_SAMPLE_IMAGES)
            copytree(DIR_POS_IMAGES, DIR_SAMPLE_IMAGES)
            posImageCollectionFilenames = [f for f in listdir(DIR_SAMPLE_IMAGES) if path.isfile(path.join(DIR_SAMPLE_IMAGES,f)) and f.lower().endswith("txt")]

        # if no overall collection file has been created,
        #     then, create one from all the collection files in posImageCollectionFilenames
        #
        # this is equivalent to:
        #     cd data/positive-clean-cropped-samples
        #     cat *.txt > positive-clean-cropped-samples.txt
        #     find . -name '*.txt' \! -name positive-clean-cropped-samples.txt -delete
        if(not path.isfile(FILE_SAMPLE_COLLECTION)):
            posImageCollectionFile = open(FILE_SAMPLE_COLLECTION, "w")
            for f in posImageCollectionFilenames:
                lines = open(path.join(DIR_SAMPLE_IMAGES,f), "r")
                for l in lines:
                    posImageCollectionFile.write(l)
                lines.close()
                remove(path.join(DIR_SAMPLE_IMAGES,f))
            posImageCollectionFile.close()

        check_call(["opencv_createsamples",
            "-info", FILE_SAMPLE_COLLECTION,
            "-vec", FILE_SAMPLE_VEC,
            "-bg", FILE_NEG_COLLECTION,
            "-num", str(sum(1 for line in open(FILE_SAMPLE_COLLECTION))),
            "-w", "48",
            "-h", "48"])
        sleep(1)

    # trainCascade: this trains the cascade with the .vec file in FILE_SAMPLE_VEC
    if(trainCascade):
        if(not path.isdir(DIR_HAAR_DATA)):
            makedirs(DIR_HAAR_DATA)

        check_call(["opencv_traincascade",
            "-data", DIR_HAAR_DATA,
            "-vec", FILE_SAMPLE_VEC,
            "-bg", FILE_NEG_COLLECTION,
            "-numPos", str(min(1000, sum(1 for line in open(FILE_SAMPLE_COLLECTION)))),
            "-numNeg", str(min(600, sum(1 for line in open(FILE_NEG_COLLECTION)))),
            "-numStages", "20",
            "-precalcValBufSize", "1024",
            "-precalcIdxBufSize", "1024",
            "-featureType", "HAAR",
            "-w", "48",
            "-h", "48",
            "-minHitRate", "0.995",
            "-maxFalseAlarmRate", "0.5"])
        sleep(1)

    #remove(FILE_NEG_COLLECTION)
    #remove(FILE_SAMPLE_COLLECTION)
    #rmtree(DIR_SAMPLE_IMAGES)
