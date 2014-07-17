import peasy.*;
import controlP5.*;

ControlP5 cp5;
PeasyCam camera;

Platform p;

float posX, posY, posZ, rotX, rotY, rotZ;
float MAX_TRANSLATION = 40;
float MAX_ROTATION = PI/4;

void setup() {
  size(1024, 768, P3D);
  smooth();
  cp5 = new ControlP5(this);
  camera = new PeasyCam(this, 400);

  p = new Platform(200);
  p.applyRotation(new PVector()).applyTranslation(new PVector());

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
}

void draw() {
  background(200);
  p.applyRotation(PVector.mult(new PVector(rotX, rotY, rotZ), MAX_ROTATION))
    .applyTranslation(PVector.mult(new PVector(posX, posY, posZ), MAX_TRANSLATION))
      .draw();

  hint(DISABLE_DEPTH_TEST);
  camera.beginHUD();
  cp5.draw();
  camera.endHUD();
  hint(ENABLE_DEPTH_TEST);
}

