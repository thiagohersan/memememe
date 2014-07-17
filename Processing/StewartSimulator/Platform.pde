class Platform {
  private PVector translation, rotation, initialHeight;
  private PVector[] baseJoints, phoneJoints, q, l;
  private float[] beta;
  private float baseRadius, phoneRadius, hornLength, legLength;

  public Platform(float s) {
    translation = new PVector();
    initialHeight = new PVector(0, 0, 1.5*s);
    rotation = new PVector();
    baseJoints = new PVector[6];
    phoneJoints = new PVector[6];
    beta = new float[6];
    q = new PVector[6];
    l = new PVector[6];
    baseRadius = s;
    phoneRadius = 0.2*s;
    hornLength = 0.25*s;
    legLength = 1.75*s;

    for (int i=0; i<6; i++) {
      float mx = baseRadius*cos((PI/3*i)+((i%2==1)?PI/12:-PI/12));
      float my = baseRadius*sin((PI/3*i)+((i%2==1)?PI/12:-PI/12));
      baseJoints[i] = new PVector(mx, my, 0);
      // magik !!
      beta[i] = ((i%2==1)?((i-1)*(-TWO_PI/3)):(i*(-TWO_PI/3)+(PI/3)));
    }

    for (int i=0; i<6; i++) {
      float mx = phoneRadius*cos((PI/3*i)+((i%2==0)?PI/12:-PI/12));
      float my = phoneRadius*sin((PI/3*i)+((i%2==0)?PI/12:-PI/12));
      phoneJoints[i] = new PVector(mx, my, 0);
      q[i] = new PVector(0, 0, 0);
    }
    calcQ();
  }

  public Platform applyTranslation(PVector t) {
    translation.set(t);
    calcQ();
    return this;
  }
  public Platform applyRotation(PVector r) {
    rotation.set(r);
    calcQ();
    return this;
  }

  private void calcQ() {
    for (int i=0; i<6; i++) {
      // rotation
      q[i].x = cos(rotation.z)*cos(rotation.y)*phoneJoints[i].x + 
        (-sin(rotation.z)*cos(rotation.x)+cos(rotation.z)*sin(rotation.y)*sin(rotation.x))*phoneJoints[i].y + 
        (sin(rotation.z)*sin(rotation.x)+cos(rotation.z)*sin(rotation.y)*cos(rotation.x))*phoneJoints[i].z;

      q[i].y = sin(rotation.z)*cos(rotation.y)*phoneJoints[i].x + 
        (cos(rotation.z)*cos(rotation.x)+sin(rotation.z)*sin(rotation.y)*sin(rotation.x))*phoneJoints[i].y + 
        (-cos(rotation.z)*sin(rotation.x)+sin(rotation.z)*sin(rotation.y)*cos(rotation.x))*phoneJoints[i].z;

      q[i].z = -sin(rotation.y)*phoneJoints[i].x + 
        cos(rotation.y)*sin(rotation.x)*phoneJoints[i].y + 
        cos(rotation.y)*cos(rotation.x)*phoneJoints[i].z;

      // translation
      q[i].add(PVector.add(translation, initialHeight));
      l[i] = PVector.sub(q[i], baseJoints[i]);
    }
  }

  private void calcAlpha() {
    for (int i=0; i<6; i++) {
      float L = l[i].magSq()-(legLength*legLength)+(hornLength*hornLength);
      float M = 2*hornLength*(phoneJoints[i].z-baseJoints[i].z);
      float N = 2*hornLength*(cos(beta[i])*(phoneJoints[i].x-baseJoints[i].x) + sin(beta[i])*(phoneJoints[i].y-baseJoints[i].y));
    }
  }

  public void draw() {
    // draw Base
    noStroke();
    fill(128);
    ellipse(0, 0, 2*baseRadius, 2*baseRadius);
    for (int i=0; i<6; i++) {
      pushMatrix();
      translate(baseJoints[i].x, baseJoints[i].y, baseJoints[i].z);
      noStroke();
      fill(0);
      ellipse(0, 0, 5, 5);

      pushMatrix();
      rotateZ(beta[i]);
      stroke(245);
      line(0, 0, hornLength, 0);
      popMatrix();

      popMatrix();
    }

    // draw phone jointss and rods
    for (int i=0; i<6; i++) {
      pushMatrix();
      translate(q[i].x, q[i].y, q[i].z);
      noStroke();
      fill(0);
      ellipse(0, 0, 5, 5);
      popMatrix();

      stroke(100);
      strokeWeight(1);
      line(baseJoints[i].x, baseJoints[i].y, baseJoints[i].z, q[i].x, q[i].y, q[i].z);
    }

    // sanity check
    pushMatrix();
    translate(initialHeight.x, initialHeight.y, initialHeight.z);
    translate(translation.x, translation.y, translation.z);
    rotateZ(rotation.z);
    rotateY(rotation.y);
    rotateX(rotation.x);
    stroke(245);
    noFill();
    ellipse(0, 0, 2*phoneRadius, 2*phoneRadius);
    popMatrix();
  }
}

