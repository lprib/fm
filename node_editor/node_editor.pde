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

ArrayList<Node> nodes = new ArrayList<Node>();
ArrayList<Link> links = new ArrayList<Link>();
// currently highlighted node, can be null if none highlighted
Node highlightedNode = null;

// currently highlighted port, can be null if none highlighted
PortDescription highlightedPort = null;

// If a link has been started, this will be non-null.
// TODO draw link in progress
PortDescription linkStartedPort = null;

// text size
float myTextSize = 12;

void setup() {
  size(600, 600);
  textSize(myTextSize);
}

void draw() {
  background(255);

  // Check if the cursor is over a node
  highlightedNode = null;
  for(Node n: nodes) {
    if(n.isPointInside(mouseX, mouseY)) {
      highlightedNode = n;
    }
  }

  // check if the cursor is over a port
  highlightedPort = getHighlightedPort();

  // draw nodes
  for(Node n: nodes) {
    boolean hlNode = (n == highlightedNode);
    n.draw(hlNode, highlightedPort);
  }
  // draw links
  for(Link l: links) {
    l.draw();
  }
  
  // draw in-progress links
  if(linkStartedPort != null) {
    // TODO extract this out to a method of PortDescription
    Location start = linkStartedPort.parent.getPortLoc(linkStartedPort.isInput, linkStartedPort.idx);
    line(start.x, start.y, mouseX, mouseY);
  }
}

PortDescription getHighlightedPort() {
  for(Node n: nodes) {
    // Check all input ports for node
    for(int i = 0; i < n.desc.inputs.length; i++) {
      Location l = n.getPortLoc(true, i);
      if(dist(l.x, l.y, mouseX, mouseY) <= 20) {
        return new PortDescription(n, true, i);
      }
    }
    // Check all output ports for node
    for(int i = 0; i < n.desc.outputs.length; i++) {
      Location l = n.getPortLoc(false, i);
      if(dist(l.x, l.y, mouseX, mouseY) <= 20) {
        return new PortDescription(n, false, i);
      }
    }
  }
  return null;
}

void keyPressed() {
  // highlighted actions:
  if(highlightedNode != null) {
    switch(key) {
      case ' ':
        highlightedNode.mouse_snapped = !highlightedNode.mouse_snapped;
        break;
      case '\b':
        nodes.remove(highlightedNode);
        highlightedNode = null;
        //TODO remove all links that connect to this node
    }
  }
  // Other actions
  switch(key) {
    case 'o':
      createNode("sinosc");
      break;
    case 'a':
      createNode("adsr");
      break;
    case 'l':
      if(linkStartedPort == null) {
        // start a new link if cursor is highlighting a port
        if(highlightedPort != null) {
          linkStartedPort = highlightedPort;
        }
      } else {
        // finish a link if cursor is highlighting a port
        if(highlightedPort != null) {
          boolean success = createLink(linkStartedPort, highlightedPort);
          if(!success) {
            linkStartedPort = null;
            println("failed to create link");
          } else {
            println("created link");
          }
        }
      }
  }
}

void createNode(String type) {
  if(highlightedNode != null) {
    highlightedNode.mouse_snapped = false;
  }
  Node newNode = new Node(nodeTypes.get(type));
  newNode.mouse_snapped = true;
  nodes.add(newNode);
}

boolean createLink(PortDescription start, PortDescription end) {
  if(
    // Node cannot link to itsself
    (start.parent != end.parent) &&
    // Output cannot link to output and vice versa
    (start.isInput != end.isInput)
  ) {

    // TODO check if a link already goes in to the input port, since inputs can
    // only have a single driver
    Link l = new Link(linkStartedPort, highlightedPort);
    for(Link other: links) {
      if(
        (l.start.equals(other.start) && l.end.equals(other.end)) ||
        (l.end.equals(other.start) && l.end.equals(other.start))
      ) {
        println("Attempt to create duplicate link");
        return false;
      }
    }

    linkStartedPort = null;
    links.add(l);
    return true;
  }
  return false;
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
  public boolean isInput;
  public int idx;

  public PortDescription(Node parent, boolean isInput, int idx) {
    this.parent = parent;
    this.isInput = isInput;
    this.idx = idx;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (obj.getClass() != this.getClass()) { return false; }
    PortDescription other = (PortDescription) obj;
    if(other.parent != parent) { return false; }
    if(other.isInput != isInput) { return false; }
    if(other.idx != idx) { return false; }
    return true;
  }
}

class Node {
  public boolean mouse_snapped = false;
  public NodeDescription desc;
  float x, y;
  float w = 75;
  float h;

  float textH = myTextSize;
  float textPad = 10;

  public Node(NodeDescription desc) {
    this.desc = desc;
    int numRows = max(desc.inputs.length, desc.outputs.length) + 1;
    this.h = numRows * (textH + textPad);
  }

  // find xy of given port
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

  // find nearest port on this node of type input/output to the location xy
  public int nearestPortIdxTo(boolean isInput, float x, float y) {
    int nearestIdx = 0;
    float minDistance = 10000000;

    for(int i = 0; i < (isInput ? desc.inputs.length : desc.outputs.length); i++) {
      Location l = getPortLoc(isInput, i);
      float d = dist(l.x, l.y, x, y);
      if(d < minDistance) {
        nearestIdx = i;
        minDistance = d;
      }
    }
    return nearestIdx;
  }

  public boolean isPointInside(float x, float y) {
    return x >= this.x && y >= this.y && x <= this.x + this.w && y <= this.y + this.h;
  }

  public void draw(boolean highlighted, PortDescription highlightPort) {

    strokeWeight(2);
    if (highlighted) {
      stroke(0, 0, 255);
    } else {
      stroke(0);
    }

    if(mouse_snapped) {
      x = mouseX - w/2;
      y = mouseY - h/2;
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
      if(
          highlightPort != null &&
          highlightPort.parent == this &&
          highlightPort.isInput &&
          highlightPort.idx == i
      ) {
        fill(0, 0, 255);
        ellipse(0, yval, 10, 10);
      }
      fill(0);
      text(desc.inputs[i], 10, yval);
    }

    // Draw outputs
    textAlign(RIGHT, CENTER);
    for (int i = 0; i < desc.outputs.length; i++) {
      float yval = (i+1)*(textH + textPad);
      fill(255);
      ellipse(w, yval, 20, 20);
      if(
          highlightPort != null &&
          highlightPort.parent == this &&
          !highlightPort.isInput &&
          highlightPort.idx == i
      ) {
        fill(0, 0, 255);
        ellipse(w, yval, 10, 10);
      }
      fill(0);
      text(desc.outputs[i], w-10, yval);
    }


    popMatrix();
  }
}

class Link {
  PortDescription start;
  PortDescription end;

  public Link(PortDescription start, PortDescription end) {
    this.start = start;
    this.end = end;
  }

  public void draw() {
    Location startLoc = start.parent.getPortLoc(start.isInput, start.idx);
    Location endLoc = end.parent.getPortLoc(end.isInput, end.idx);
    stroke(0);
    line(startLoc.x, startLoc.y, endLoc.x, endLoc.y);
  }
}
