float complexity = 0.006; //2.0; // field complexity
float timeScale = 0.5;//1.2;  // variation speed
float speedScale = 64;//128; // "mass"
float phase = PI;   // separate u-noise from v-noise

PVector location, previousLocation;
float radius = 1;
boolean isRunning = false;

void setup() {
  size(512, 512, P3D);
  location = new PVector(0, 0, 0);
  previousLocation = new PVector(0, 0, 0);
  smooth();
  background(255);
}

void update() {
  float t = (millis()/1000.0) * timeScale;

  float x = location.x;
  float y = location.y;
  float z = location.z;

  // direction
  float u = noise(y*complexity + 1*phase, z*complexity + 1*phase, t + 1*phase);
  float v = noise(x*complexity + 2*phase, z*complexity + 2*phase, t + 2*phase);
  float w = noise(x*complexity + 3*phase, y*complexity + 3*phase, t + 3*phase);

  // magnitude
  float speed = constrain(noise(t, u, v) * speedScale, 16, 128);

  x += lerp(-speed, speed, u);
  y += lerp(-speed, speed, v);
  z += lerp(-speed, speed, w);

  previousLocation.set(location.x, location.y, location.z);

  location.set(
  constrain(x, -width/2+radius, width/2-radius), 
  constrain(y, -height/2+radius, height/2-radius), 
  constrain(z, -height/2+radius, height/2-radius)
    );
}

void draw() { 
  update();

  if (isRunning) {
    pushMatrix();
    translate(0, 0, -height);
    translate(width/2, height/2, height/2);
    stroke(0, 64);
    strokeWeight(3);
    fill(200);
    line(location.x, location.y, location.z, previousLocation.x, previousLocation.y, previousLocation.z);
    popMatrix();
  }
}

void mousePressed() {
  isRunning = !isRunning;
  setup();
}

