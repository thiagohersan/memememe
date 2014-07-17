import peasy.*;
PeasyCam camera;

Platform p;

void setup() {
  size(1024, 768, P3D);
  smooth();
  camera = new PeasyCam(this, 0, 0, 0, 400);
  p = new Platform(200);
  p.applyRotation(new PVector(PI/12, PI/6, PI/8))
    .applyTranslation(new PVector(50, 40, 30));
}

void draw() {
  background(200);
  p.draw();
}

