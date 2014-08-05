import java.io.File;

String IMG_DIR = "positive-clean-00";
ArrayList<String> imgFiles;
int currentFile;
String outputBuffer;
PImage currentImg;

void setup(){
  size(1024,683);
  
  // populate
  imgFiles = new ArrayList<String>();
  File f = new File(dataPath(IMG_DIR));
  File[] fs = f.listFiles();
  for(int i=0; i<fs.length; i++){
    if(fs[i].isFile() && (fs[i].getName().endsWith(".JPG") || fs[i].getName().endsWith(".jpg"))){
      imgFiles.add(IMG_DIR+"/"+fs[i].getName());
    }
  }

  currentFile = 0;
  outputBuffer = "";
  currentImg = loadImage(imgFiles.get(currentFile));
}

void draw(){
  background(0);
  if(currentFile < imgFiles.size()){
    image(currentImg,0,0);
  }
  else{
    // some text about saving
  }
}

void keyPressed(){
  if((key == ' ') && currentFile < imgFiles.size()){
    outputBuffer += imgFiles.get(currentFile);
    outputBuffer += " 1 ";
    outputBuffer += "x y w h";
    outputBuffer += "\n";
    currentImg = loadImage(imgFiles.get((currentFile+1)%imgFiles.size()));
    currentFile++;
  }
  if((key == 's') && (currentFile == imgFiles.size())){
    // save string to file
  }
}

