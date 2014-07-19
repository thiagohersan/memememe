import peasy.*;
import controlP5.*;

float MAX_TRANSLATION = 60;
float MAX_ROTATION = PI/6;

ControlP5 cp5;
PeasyCam camera;
Platform p;

float posX=0, posY=0, posZ=0, rotX=0, rotY=0, rotZ=0;

void setup() {
  size(1024, 768, P3D);
  smooth();
  textSize(20);

  camera = new PeasyCam(this, 666);
  camera.setRotations(-1.0, 0.0, 0.0);
  camera.lookAt(8.0, -50.0, 80.0);

  p = new Platform(1);
  p.applyTranslationAndRotation(new PVector(), new PVector());

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
  p.applyTranslationAndRotation(PVector.mult(new PVector(posX, posY, posZ), MAX_TRANSLATION),
                                PVector.mult(new PVector(rotX, rotY, rotZ), MAX_ROTATION));
  p.draw();

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

void keyPressed() {
  if (key == ' ') {
    camera.setRotations(-1.0, 0.0, 0.0);
    camera.lookAt(8.0, -50.0, 80.0);
    camera.setDistance(666);
  }
}

