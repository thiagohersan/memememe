import peasy.*; //<>//
import controlP5.*;
import oscP5.*;
import netP5.*; //osc library

float MAX_TRANSLATION = 50;
float MAX_ROTATION = PI/2;

ControlP5 cp5;
PeasyCam camera;
Platform mPlatform;

OscP5 oscP5;
NetAddress mOscOut; // address of the pi connected to the motors


float posX=0, posY=0, posZ=0, rotX=0, rotY=0, rotZ=0;
boolean ctlPressed = false;

void setup() {
  size(1024, 768, P3D);
  smooth();
  frameRate(60);
  textSize(20);

  mOscOut = new NetAddress("192.168.0.24", 8888);

  camera = new PeasyCam(this, 666);
  camera.setRotations(-1.0, 0.0, 0.0);
  camera.lookAt(8.0, -50.0, 80.0);

  mPlatform = new Platform(1);
  mPlatform.applyTranslationAndRotation(new PVector(), new PVector());

  cp5 = new ControlP5(this);

  cp5.addSlider("posX")
    .setPosition(20, 20)
    .setSize(180, 40).setRange(-1, 1);
  cp5.addSlider("posY")
    .setPosition(20, 70)
    .setSize(180, 40).setRange(-1, 1);
  cp5.addSlider("posZ")
    .setPosition(20, 120)
    .setSize(180, 40).setRange(-1, 1);

  cp5.addSlider("rotX")
    .setPosition(width-200, 20)
    .setSize(180, 40).setRange(-1, 1);
  cp5.addSlider("rotY")
    .setPosition(width-200, 70)
    .setSize(180, 40).setRange(-1, 1);
  cp5.addSlider("rotZ")
    .setPosition(width-200, 120)
    .setSize(180, 40).setRange(-1, 1);

  cp5.setAutoDraw(false);
  camera.setActive(true);
}

void draw() {
  background(200);
  mPlatform.applyTranslationAndRotation(PVector.mult(new PVector(posX, posY, posZ), MAX_TRANSLATION), 
    PVector.mult(new PVector(rotX, rotY, rotZ), MAX_ROTATION));
  mPlatform.draw();

  hint(DISABLE_DEPTH_TEST);
  camera.beginHUD();
  cp5.draw();
  camera.endHUD();
  hint(ENABLE_DEPTH_TEST);
}

void controlEvent(ControlEvent theEvent) {
  camera.setActive(false);
}
void mouseReleased() {
  camera.setActive(true);
}

long lastTime = 0;

void sendOSC() {
  //after a UI event send a OSC packege
  float[] angles = mPlatform.getAlpha();

  for (float f : angles) {
    if (Float.isNaN(f)) {
      return;
    }
  }

  OscMessage myMessage = new OscMessage("/angles");
  myMessage.add(angles); /* add an int array to the osc message */

  oscP5.flush(myMessage, mOscOut);
  lastTime = millis();
}

void mouseDragged () {
  if (ctlPressed) {
    posX = map(mouseX, 0, width, -1, 1);
    posY = map(mouseY, 0, height, -1, 1);
  }
}


void keyPressed() {
  if (key == ' ') {
    camera.setRotations(-1.0, 0.0, 0.0);
    camera.lookAt(8.0, -50.0, 80.0);
    camera.setDistance(666);
  } else if (keyCode == CONTROL) {
    camera.setActive(false);
    ctlPressed = true;
  }
}

void keyReleased() {
  if (keyCode == CONTROL) {
    camera.setActive(true);
    ctlPressed = false;
  }
}