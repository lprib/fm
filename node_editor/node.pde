class Node {
  public boolean mouse_snapped = false;
  public ArrayList<Port> ports = new ArrayList<Port>();
  public String name;
  public boolean deletable;
  float x, y = 0;
  float w = 100;
  float h;

  // y location from top of node to the title underline
  float titleUnderlineYOffset = myTextSize + 5;
  // y location from top of node to the center of the first port
  float portsYOffset = myTextSize + 20;
  // y spacing between each port
  float portSpacing = 25;

  public Node(NodeDescription desc) {
    int numRows = max(desc.inputs.length, desc.outputs.length);
    this.h = portsYOffset + numRows * portSpacing;
    this.name = desc.name;
    this.deletable = desc.deletable;
    initPorts(desc);
  }

  void initPorts(NodeDescription desc) {
    for(int i = 0; i < desc.inputs.length; i++) {
      ports.add(new Port(this, i, true, desc.inputs[i]));
    }
    for(int i = 0; i < desc.outputs.length; i++) {
      ports.add(new Port(this, i, false, desc.outputs[i]));
    }
  }

  // returns a Port if xy is within a port's selection area, else null
  public Port portNearPoint(float x, float y) {
    for(Port p: ports) {
      Location l = p.getAbsoluteLocation();
      if(dist(x, y, l.x, l.y) <= p.ellipseSize) {
        return p;
      }
    }
    return null;
  }

  public boolean isPointInside(float x, float y) {
    return x >= this.x && y >= this.y && x <= this.x + this.w && y <= this.y + this.h;
  }

  public void draw(boolean highlighted, Port highlightedPort) {
    strokeWeight(1);
    stroke(uiColor);
    if (highlighted) {
      fill(highlightColor);
    } else {
      fill(nodeFillColor);
    }

    if(mouse_snapped) {
      x = mouseX - w/2;
      y = mouseY - h/2;
    }

    pushMatrix();
    translate(x, y);
    rect(0, 0, w, h);
    // underline node title
    line(0, titleUnderlineYOffset, w, titleUnderlineYOffset);

    textAlign(CENTER, TOP);
    fill(uiColor);
    text(name, w/2, 0);

    for(Port p: ports) {
      p.draw(highlightedPort == p);
    }

    popMatrix();
  }
}
