ArrayList<Node> nodes = new ArrayList<Node>();

HashMap<String, NodeDescription> nodeTypes = new HashMap<String, NodeDescription>() {{
  put("adsr", new NodeDescription(
    new String[] {"gate", "a", "d", "s", "r"},
    new String[] {"out"},
    "adsr"
  ));

  put("sinosc", new NodeDescription(
    new String[] {"freq", "phase", "vol", "feedback", "mult"},
    new String[] {"out"},
    "sinosc"
  ));

  put("freq", new NodeDescription(
    new String[] {},
    new String[] {"out"},
    "freq"
  ));

  put("gate", new NodeDescription(
    new String[] {},
    new String[] {"out"},
    "gate"
  ));

  put("lchan", new NodeDescription(
    new String[] {"in"},
    new String[] {},
    "lchan"
  ));

  put("rchan", new NodeDescription(
    new String[] {"in"},
    new String[] {},
    "rchan"
  ));
}};

Node test = new Node(nodeTypes.get("sinosc"));
float myTextSize = 15;

void setup() {
  size(600, 600);
  test.x = 300;
  test.y = 300;
  textSize(myTextSize);
}

void draw() {
  background(255);
  test.draw();
  ellipseMode(CENTER);
  for(int i = 0; i < test.desc.outputs.length; i++) {
    Location l = test.get_output_loc(i);
    ellipse(l.x, l.y, 10, 10);
  }
}

class NodeDescription {
  public String[] inputs;
  public String[] outputs;
  public String name;

  public NodeDescription(String[] inputs, String[] outputs, String name) {
    this.inputs = inputs;
    this.outputs = outputs;
    this.name = name;
  }
}

class Location {
  public float x;
  public float y;
}

class PortDescription {
  public Node parent;
  public boolean IsInput;
  public int idx;
}

class Node {
  public boolean mouse_snapped = false;
  public NodeDescription desc;
  float x, y;
  float w = 200;
  float h;

  float textH = myTextSize;
  float textPad = 30;

  public Node(NodeDescription desc) {
    this.desc = desc;
    int numRows = max(desc.inputs.length, desc.outputs.length) + 1;
    this.h = numRows * (textH + textPad);
  }

  public Location getPortLoc(boolean isInput, int idx) {
    Location l = new Location();
    if(isInput) {
      l.x = x;
      l.y = y + (idx+1)*(textH + textPad);
    } else {
      l.x = x + w;
      l.y = y + (idx+1)*(textH + textPad);
    }
    return l;
  }

  public PortDescription nearestPortTo(Location l) {
    // TODO
    return null;
  }

  public void draw() {
    if(mouse_snapped) {
      x = mouseX;
      y = mouseY;
    }
    pushMatrix();
    translate(x, y);
    fill(255);
    rect(0, 0, w, h);

    textAlign(CENTER, TOP);
    fill(0);
    text(desc.name, w/2, 0);


    ellipseMode(CENTER);
    // Draw inputs
    textAlign(LEFT, CENTER);
    for (int i = 0; i < desc.inputs.length; i++) {
      float yval = (i+1)*(textH + textPad);
      fill(255);
      ellipse(0, yval, 20, 20);
      fill(0);
      text(desc.inputs[i], 10, yval);
    }

    // Draw outputs
    textAlign(RIGHT, CENTER);
    for (int i = 0; i < desc.outputs.length; i++) {
      float yval = (i+1)*(textH + textPad);
      fill(255);
      ellipse(w, yval, 20, 20);
      fill(0);
      text(desc.outputs[i], w-10, yval);
    }


    popMatrix();
  }
}

class Linkage {
  public Node one;
  public Node two;

  public void draw() {

  }
}
