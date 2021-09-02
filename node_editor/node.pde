HashMap<String, NodeDescription> nodeTypes = new HashMap<String, NodeDescription>() {{
  put("adsr", new NodeDescription(
    new String[] {"gate", "a", "d", "s", "r"},
    new float[] {0, 0, 0, 1, 0},
    new String[] {"out"},
    "adsr",
    false,
    false
  ));

  put("sinosc", new NodeDescription(
    new String[] {"freq", "phase", "vol", "feedback", "mult"},
    new float[] {0, 0, 1, 0, 1},
    new String[] {"out"},
    "sinosc",
    false,
    false
  ));

  put("mixer", new NodeDescription(
    new String[] {"in1", "in2", "in3", "mix1", "mix2", "mix3"},
    new float[] {0, 0, 0, 1, 1, 0},
    new String[] {"out"},
    "mixer",
    false,
    false
  ));

  put("freq", new NodeDescription(
    new String[] {},
    new float[] {},
    new String[] {"out"},
    "freq",
    true,
    true
  ));

  put("gate", new NodeDescription(
    new String[] {},
    new float[] {},
    new String[] {"out"},
    "gate",
    true,
    true
  ));

  put("lchan", new NodeDescription(
    new String[] {"in"},
    new float[] {0},
    new String[] {},
    "lchan",
    true,
    false
  ));

  put("rchan", new NodeDescription(
    new String[] {"in"},
    new float[] {0},
    new String[] {},
    "rchan",
    true,
    false
  ));
}};

class NodeDescription {
  public String[] inputs;
  public float[] defaultInputValues;
  public String[] outputs;
  public String name;
  public boolean intrinsic;

  // If this node type is an intrinsic node, determines whether it is a signal
  // producer or consumer
  public boolean intrinsicProducer;

  // specialIndex can be null if none
  public NodeDescription(String[] inputs, float[] defaultInputValues, String[] outputs, String name, boolean intrinsic, boolean intrinsicProducer) {
    this.inputs = inputs;
    this.defaultInputValues = defaultInputValues;
    this.outputs = outputs;
    this.name = name;
    this.intrinsic = intrinsic;
    this.intrinsicProducer = intrinsicProducer;
  }
}

class Node {
  public boolean mouse_snapped = false;
  public ArrayList<Port> ports = new ArrayList<Port>();
  public NodeDescription desc;
  // auxillary name: user defined for organization
  public String auxName;
  public color mainColor;
  public color tint;
  float x, y = 0;
  float w = 125;
  float h;

  // y location from top of node to the title underline
  float titleUnderlineYOffset = myTextSize + 5;
  // y location from top of node to the center of the first port
  float portsYOffset = myTextSize + 20;
  // y spacing between each port
  float portSpacing = 15;

  public Node(NodeDescription desc) {
    int numRows = max(desc.inputs.length, desc.outputs.length);
    this.h = portsYOffset + numRows * portSpacing;
    this.desc = desc;
    this.auxName = desc.name;
    this.mainColor = desc.intrinsic ? intrinsicNodeFillColor : nodeFillColor;
    // default to no tint (ie. tint same as main color)
    this.tint = mainColor;
    initPorts();
  }

  void initPorts() {
    for(int i = 0; i < desc.inputs.length; i++) {
      ports.add(new Port(this, desc.defaultInputValues[i], i, true, desc.inputs[i]));
    }
    for(int i = 0; i < desc.outputs.length; i++) {
      ports.add(new Port(this, 0, i, false, desc.outputs[i]));
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
    stroke(outlineColor);
    fill(mainColor);

    if(mouse_snapped) {
      x = mouseX - w/2;
      y = mouseY - h/2;
    }

    pushMatrix();
    translate(x, y);
    
    rect(0, 0, w, h, 5, 5, 5, 5);
    int tintAlpha = 40;
    fill(tint & 0xFFFFFF + (tintAlpha << 24));
    rect(0, 0, w, h, 5, 5, 5, 5);

    // overlay low alpha white for highlighting
    if(highlighted) {
      noStroke();
      fill(255, 20);
      rect(0, 0, w, h, 5, 5, 5, 5);
      stroke(outlineColor);
    }
    // underline node title
    line(0, titleUnderlineYOffset, w, titleUnderlineYOffset);

    textAlign(CENTER, TOP);
    fill(uiColor);
    text(auxName + ": " + desc.name, w/2, 0);

    for(Port p: ports) {
      p.draw(highlightedPort == p);
    }

    popMatrix();
  }
}
