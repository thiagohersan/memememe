import java.io.File;

String IMG_DIR = "positive-clean";
ArrayList<String> imgFiles;
int currentFile;
String outputBuffer;
PImage currentImg;
PVector[] selectedRegion;
PVector lastMousePressed;
boolean moveSelectedRegion;

void setup() {
  size(1024, 700);

  // populate
  imgFiles = new ArrayList<String>();
  File f = new File(dataPath(IMG_DIR));
  File[] fs = f.listFiles();
  for (int i=0; i<fs.length; i++) {
    if (fs[i].isFile() && (fs[i].getName().endsWith(".JPG") || fs[i].getName().endsWith(".jpg"))) {
      imgFiles.add(IMG_DIR+"/"+fs[i].getName());
    }
  }

  selectedRegion = new PVector[2];
  selectedRegion[0] = new PVector();
  selectedRegion[1] = new PVector();
  lastMousePressed = new PVector();
  moveSelectedRegion = false;

  currentFile = 0;
  outputBuffer = "";
  currentImg = loadImage(imgFiles.get(currentFile));
  initialGuess();
}

void draw() {
  background(0);
  if (currentFile < imgFiles.size()) {
    image(currentImg, 0, 0);
    stroke(0);
    fill(255, 100, 100, 100);
    rect(selectedRegion[0].x, selectedRegion[0].y, selectedRegion[1].x-selectedRegion[0].x, selectedRegion[1].y-selectedRegion[0].y);
    float aspectRatio = (selectedRegion[1].x-selectedRegion[0].x)/(selectedRegion[1].y-selectedRegion[0].y);
    fill(0);
    textSize(32);
    textAlign(LEFT);
    text(aspectRatio, selectedRegion[1].x+10, selectedRegion[0].y-10);
    noStroke();
    rect(0,0,width,48);
    fill(255);
    text("Press Space Bar To Advance to Next Image", 10, 32);
  }
  else {
    fill(255);
    textAlign(CENTER);
    text("Press 's' To Save Collection File", width/2, height/2);
  }
}

void initialGuess() {
  currentImg.loadPixels();
  selectedRegion[0].set(currentImg.width, currentImg.height);
  selectedRegion[1].set(0, 0);

  for (int y=0; y<currentImg.height; y++) {
    for (int x=0; x<currentImg.width; x++) {
      color c = currentImg.pixels[y*currentImg.width+x];
      if (((c>>16&0xff) < 250) && ((c>>8&0xff) < 250) && ((c>>0&0xff) < 250)) {
        selectedRegion[0].x = min(selectedRegion[0].x, x);
        selectedRegion[0].y = min(selectedRegion[0].y, y);
        selectedRegion[1].x = max(selectedRegion[1].x, x);
        selectedRegion[1].y = max(selectedRegion[1].y, y);
      }
    }
  }
  // make it a square, centered on the found object
  PVector size = PVector.sub(selectedRegion[1], selectedRegion[0]);
  if (size.x > size.y) {
    selectedRegion[0].y -= (size.x - size.y)/2;
    selectedRegion[1].y += (size.x - size.y)/2;
  }
  else {
    selectedRegion[0].x -= (size.y - size.x)/2;
    selectedRegion[1].x += (size.y - size.x)/2;
  }
}

void mousePressed() {
  lastMousePressed.set(mouseX, mouseY);
  moveSelectedRegion = (lastMousePressed.x > selectedRegion[0].x) &&
    (lastMousePressed.x < selectedRegion[1].x) &&
    (lastMousePressed.y > selectedRegion[0].y) &&
    (lastMousePressed.y < selectedRegion[1].y);

  if (!moveSelectedRegion) {
    selectedRegion[0].set(mouseX, mouseY);
  }
}

void mouseDragged() {
  if (!moveSelectedRegion) {
    selectedRegion[1].set(mouseX, mouseY);
  }
  else {
    selectedRegion[0].x += mouseX-pmouseX;
    selectedRegion[0].y += mouseY-pmouseY;
    selectedRegion[1].x += mouseX-pmouseX;
    selectedRegion[1].y += mouseY-pmouseY;
  }
}

void mouseReleased() {
  if (!moveSelectedRegion) {
    selectedRegion[1].set(mouseX, mouseY);
    // make sure selectedRegion[0] is always the top-left
    if (selectedRegion[1].x < selectedRegion[0].x) {
      selectedRegion[1].x = selectedRegion[0].x;
      selectedRegion[0].x = mouseX;
    }
    if (selectedRegion[1].y < selectedRegion[0].y) {
      selectedRegion[1].y = selectedRegion[0].y;
      selectedRegion[0].y = mouseY;
    }
  }
  moveSelectedRegion = false;
}

void keyPressed() {
  if ((key == ' ') && currentFile < imgFiles.size()) {
    outputBuffer += imgFiles.get(currentFile).replace(IMG_DIR+"/","");
    outputBuffer += " 1 ";
    outputBuffer += (int)selectedRegion[0].x+" "+(int)selectedRegion[0].y+" ";
    outputBuffer += (int)(selectedRegion[1].x-selectedRegion[0].x)+" "+(int)(selectedRegion[1].y-selectedRegion[0].y);
    outputBuffer += "\n";
    PImage toSave = createImage((int)(selectedRegion[1].x-selectedRegion[0].x), (int)(selectedRegion[1].x-selectedRegion[0].x), RGB);
    toSave.copy(currentImg, (int)selectedRegion[0].x, (int)selectedRegion[0].y, toSave.width, toSave.height, 0,0, toSave.width, toSave.height);
    toSave.save(dataPath(IMG_DIR+"-cropped/"+imgFiles.get(currentFile).replace(IMG_DIR+"/","")));
    currentImg = loadImage(imgFiles.get((currentFile+1)%imgFiles.size()));
    initialGuess();
    currentFile++;
  }
  if ((key == 's') && (currentFile == imgFiles.size())) {
    // save string to file
    PrintWriter output = createWriter(dataPath(IMG_DIR+"/"+IMG_DIR+".txt"));
    output.print(outputBuffer);
    output.flush();
    output.close();
    exit();
  }
}

