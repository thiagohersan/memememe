from os import listdir, path, makedirs, remove
from re import sub
from subprocess import check_call
from time import sleep
from shutil import rmtree

PATH_POS_IMAGES = "../../Processing/PositiveCollectionTagger/data/positive-clean-00_cropped"
PATH_NEG_IMAGES = "data/negative-tutorial-haartraining"
PATH_SAMPLE_IMAGES = "data/"+PATH_POS_IMAGES.split('/')[-1]+"_samples"

if __name__=="__main__":
    # make sure output dir exists
    if(not path.isdir(PATH_SAMPLE_IMAGES)):
        makedirs(PATH_SAMPLE_IMAGES)

    # get list of negative images
    negImageCollectionFilename = PATH_NEG_IMAGES+".txt"
    negImageCollectionFile = open(negImageCollectionFilename, "w")
    negImageFilenames = [f for f in listdir(PATH_NEG_IMAGES) if path.isfile(path.join(PATH_NEG_IMAGES,f)) and f.lower().endswith("jpg")]
    for f in negImageFilenames:
        negImageCollectionFile.write(PATH_NEG_IMAGES.split('/')[-1]+"/"+f)
        negImageCollectionFile.write("\n")
    negImageCollectionFile.close()

    # get list of positive image files
    posImageFilenames = [f for f in listdir(PATH_POS_IMAGES) if path.isfile(path.join(PATH_POS_IMAGES,f))]
    # run the create command for each image
    for f in posImageFilenames:
        check_call(["opencv_createsamples",
            "-img", path.join(PATH_POS_IMAGES,f),
            "-bg", negImageCollectionFilename,
            "-info", path.join(PATH_SAMPLE_IMAGES,sub("(?i)jpg","txt",f)),
            "-num", "30",
            "-maxxangle", "0.0",
            "-maxyangle", "0.0",
            "-maxzangle", "0.4",
            "-bgcolor", "255",
            "-bgthresh", "8",
            "-w", "48",
            "-h", "48"])
        sleep(1)

    # get mega list of sample images
    posImageCollectionFilename = PATH_SAMPLE_IMAGES+".txt"
    posImageCollectionFile = open(posImageCollectionFilename, "w")
    posImageCollectionFilenames = [f for f in listdir(PATH_SAMPLE_IMAGES) if path.isfile(path.join(PATH_SAMPLE_IMAGES,f)) and f.lower().endswith("txt")]

    for f in posImageCollectionFilenames:
        lines = open(path.join(PATH_SAMPLE_IMAGES,f), "r")
        for l in lines:
            posImageCollectionFile.write(PATH_SAMPLE_IMAGES.split('/')[-1]+"/"+l)
        lines.close()
        remove(path.join(PATH_SAMPLE_IMAGES,f))
    posImageCollectionFile.close()

    check_call(["opencv_createsamples",
        "-info", posImageCollectionFilename,
        "-vec", sub("(?i)txt","vec",posImageCollectionFilename),
        "-num", str(sum(1 for line in open(posImageCollectionFilename))),
        "-w", "48",
        "-h", "48"])
    sleep(1)

    remove(negImageCollectionFilename)
    remove(posImageCollectionFilename)
    rmtree(PATH_SAMPLE_IMAGES)
