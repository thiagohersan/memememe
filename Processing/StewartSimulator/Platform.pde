class Platform {
  private PVector translation, rotation, initialHeight;
  private PVector[] baseJoint, platformJoint, q, l, A;
  private float[] alpha;
  private float baseRadius, platformRadius, hornLength, legLength;

  // REAL ANGLES
  
  //new angles new small platform, need to invert the servo horns 
  private final float baseAngles[] = {
   314.9, 345.1, 74.9, 105.1, 194.9, 225.1 };

  private final float platformAngles[]  = {
   322.9, 337.1, 82.9, 97.1, 202.9, 217.1};

   private final float beta[] = {
   -8*PI/3, PI/3, 0, -PI, -4*PI/3, -7*PI/3};

  // REAL MEASUREMENTS
  private final float SCALE_INITIAL_HEIGHT = 120; //  250
  private final float SCALE_BASE_RADIUS = 65.43; //140
  private final float SCALE_PLATFORM_RADIUS = 76.35;
  private final float SCALE_HORN_LENGTH = 36;
  private final float SCALE_LEG_LENGTH = 125; // 270

  public Platform(float s) {
    translation = new PVector();
    initialHeight = new PVector(0, 0, s*SCALE_INITIAL_HEIGHT);
    rotation = new PVector();
    baseJoint = new PVector[6];
    platformJoint = new PVector[6];
    alpha = new float[6];
    q = new PVector[6];
    l = new PVector[6];
    A = new PVector[6];
    baseRadius = s*SCALE_BASE_RADIUS;
    platformRadius = s*SCALE_PLATFORM_RADIUS;
    hornLength = s*SCALE_HORN_LENGTH;
    legLength = s*SCALE_LEG_LENGTH;

    for (int i=0; i<6; i++) {
      float mx = baseRadius*cos(radians(baseAngles[i]));
      float my = baseRadius*sin(radians(baseAngles[i]));
      baseJoint[i] = new PVector(mx, my, 0);
    }

    for (int i=0; i<6; i++) {
     float mx = platformRadius*cos(radians(platformAngles[i]));
     float my = platformRadius*sin(radians(platformAngles[i]));

      platformJoint[i] = new PVector(mx, my, 0);
      q[i] = new PVector(0, 0, 0);
      l[i] = new PVector(0, 0, 0);
      A[i] = new PVector(0, 0, 0);
    }
    calcQ();
  }

  public void applyTranslationAndRotation(PVector t, PVector r) {
    rotation.set(r);
    translation.set(t);
    calcQ();
    calcAlpha();
  }

  private void calcQ() {
    for (int i=0; i<6; i++) {
      // rotation
      q[i].x = cos(rotation.z)*cos(rotation.y)*platformJoint[i].x +
        (-sin(rotation.z)*cos(rotation.x)+cos(rotation.z)*sin(rotation.y)*sin(rotation.x))*platformJoint[i].y +
        (sin(rotation.z)*sin(rotation.x)+cos(rotation.z)*sin(rotation.y)*cos(rotation.x))*platformJoint[i].z;

      q[i].y = sin(rotation.z)*cos(rotation.y)*platformJoint[i].x +
        (cos(rotation.z)*cos(rotation.x)+sin(rotation.z)*sin(rotation.y)*sin(rotation.x))*platformJoint[i].y +
        (-cos(rotation.z)*sin(rotation.x)+sin(rotation.z)*sin(rotation.y)*cos(rotation.x))*platformJoint[i].z;

      q[i].z = -sin(rotation.y)*platformJoint[i].x +
        cos(rotation.y)*sin(rotation.x)*platformJoint[i].y +
        cos(rotation.y)*cos(rotation.x)*platformJoint[i].z;

      // translation
      q[i].add(PVector.add(translation, initialHeight));
      l[i] = PVector.sub(q[i], baseJoint[i]);
    }
  }

  private void calcAlpha() {
    for (int i=0; i<6; i++) {
      float L = l[i].magSq()-(legLength*legLength)+(hornLength*hornLength);
      float M = 2*hornLength*(q[i].z-baseJoint[i].z);
      float N = 2*hornLength*(cos(beta[i])*(q[i].x-baseJoint[i].x) + sin(beta[i])*(q[i].y-baseJoint[i].y));
      alpha[i] = asin(L/sqrt(M*M+N*N)) - atan2(N, M);

      A[i].set(hornLength*cos(alpha[i])*cos(beta[i]) + baseJoint[i].x, 
      hornLength*cos(alpha[i])*sin(beta[i]) + baseJoint[i].y, 
      hornLength*sin(alpha[i]) + baseJoint[i].z);

      float xqxb = (q[i].x-baseJoint[i].x);
      float yqyb = (q[i].y-baseJoint[i].y);
      float h0 = sqrt((legLength*legLength)+(hornLength*hornLength)-(xqxb*xqxb)-(yqyb*yqyb)) - q[i].z;

      float L0 = 2*hornLength*hornLength;
      float M0 = 2*hornLength*(h0+q[i].z);
      float a0 = asin(L0/sqrt(M0*M0+N*N)) - atan2(N, M0);

      //println(i+":"+alpha[i]+"  h0:"+h0+"  a0:"+a0);
    }
  }
  
  public float[] getAlpha(){
    return alpha; 
  }

  public void draw() {
    // draw Base
    noStroke();
    fill(128);
    ellipse(0, 0, 2*baseRadius, 2*baseRadius);
    for (int i=0; i<6; i++) {
      pushMatrix();
      translate(baseJoint[i].x, baseJoint[i].y, baseJoint[i].z);
      noStroke();
      fill(0);
      ellipse(0, 0, 5, 5);
      text(String.format("%.2f", degrees(alpha[i])), 5,5,5);
      popMatrix();

      stroke(245);
      line(baseJoint[i].x, baseJoint[i].y, baseJoint[i].z, A[i].x, A[i].y, A[i].z);

      PVector rod = PVector.sub(q[i], A[i]);
      rod.setMag(legLength);
      rod.add(A[i]);

      stroke(100);
      strokeWeight(3);
      line(A[i].x, A[i].y, A[i].z, rod.x, rod.y, rod.z);
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
      line(baseJoint[i].x, baseJoint[i].y, baseJoint[i].z, q[i].x, q[i].y, q[i].z);
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
    ellipse(0, 0, 2*platformRadius, 2*platformRadius);
    popMatrix();
  }
}