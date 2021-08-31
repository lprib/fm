// TODO InputPort, OutputPort extends Port?
class Port {
  // relative to parent
  public float x, y;
  public Node parent;
  public boolean isInput;
  public int index;
  public String name;

  public int numLinks = 0;
  // value is only applicable if this port is an input port
  public float value = 0.0;

  // drawing constants
  public final float ellipseSize = 20;
  public final float linkEllipseSize = 8;
  public final float nodeTextInset = 15;

  public Port(Node parent, int index, boolean isInput, String name) {
    this.parent = parent;
    // Place on left if input, else right
    this.x = (isInput ? 0 : parent.w);
    this.y = parent.portsYOffset + index * parent.portSpacing;
    this.isInput = isInput;
    this.index = index;
    this.name = name;
  }

  public Location getAbsoluteLocation() {
    return new Location(parent.x + x, parent.y + y);
  }

  // When this is called, the coordinate system must already be translated to
  // be relative to parent's location
  public void draw(boolean highlighted) {
    stroke(uiColor);
    if(highlighted) {
      fill(highlightColor);
    } else {
      fill(nodeFillColor);
    }

    ellipseMode(CENTER);
    ellipse(x, y, ellipseSize, ellipseSize);



    if(numLinks > 0) {
      fill(linkColor);
      noStroke();
      ellipse(x, y, linkEllipseSize, linkEllipseSize);
    }

    stroke(uiColor);
    fill(uiColor);
    if(isInput) {
      textAlign(LEFT, CENTER);
      text(name, x + nodeTextInset, y);
      // Draw const value if there are no links to this port
      if(numLinks == 0) {
        textAlign(RIGHT, CENTER);
        text(String.format("%.2f", value), x - nodeTextInset, y);
      }
    } else {
      textAlign(RIGHT, CENTER);
      text(name, x - nodeTextInset, y);
    }
  }

  public void addLink() {
    numLinks++;
  }

  public void removeLink() {
    numLinks--;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (obj.getClass() != this.getClass()) { return false; }
    Port other = (Port) obj;
    if(other.parent != parent) { return false; }
    if(other.isInput != isInput) { return false; }
    if(other.index != index) { return false; }
    return true;
  }
}
