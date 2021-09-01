class Location {
  public float x;
  public float y;

  public Location(float x, float y) {
    this.x = x;
    this.y = y;
  }
}

void drawBez(Location s, Location e) {
    // flip so start coords are always to the left of end coords
    if(s.x > e.x) {
      Location temp = s;
      s = e;
      e = temp;
    }
    float controlInset = (e.x - s.x) / 2;
    float control1x = s.x + controlInset;
    float control2x = e.x - controlInset;
    noFill();
    bezier(s.x, s.y, control1x, s.y, control2x, e.y, e.x, e.y);
}
