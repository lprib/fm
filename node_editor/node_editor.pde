HashMap<String, NodeDescription> nodeTypes = new HashMap<String, NodeDescription>() {{
  put("adsr", new NodeDescription(
    new String[] {"gate", "a", "d", "s", "r"},
    new String[] {"out"},
    "adsr",
    true
  ));

  put("sinosc", new NodeDescription(
    new String[] {"freq", "phase", "vol", "feedback", "mult"},
    new String[] {"out"},
    "sinosc",
    true
  ));

  put("freq", new NodeDescription(
    new String[] {},
    new String[] {"out"},
    "freq",
    false
  ));

  put("gate", new NodeDescription(
    new String[] {},
    new String[] {"out"},
    "gate",
    false
  ));

  put("lchan", new NodeDescription(
    new String[] {"in"},
    new String[] {},
    "lchan",
    false
  ));

  put("rchan", new NodeDescription(
    new String[] {"in"},
    new String[] {},
    "rchan",
    false
  ));
}};

ArrayList<Node> nodes = new ArrayList<Node>();
ArrayList<Link> links = new ArrayList<Link>();
// currently highlighted node, can be null if none highlighted
Node highlightedNode = null;

// currently highlighted port, can be null if none highlighted
Port highlightedPort = null;

// If a link has been started, this will be non-null.
Port linkStartedPort = null;

// text size
float myTextSize = 12;


color normalColor = color(0, 0, 0);
color highlightColor = color(100, 100, 255);
color nodeFillColor = color(220, 220, 255);

void setup() {
  size(600, 600);
  textSize(myTextSize);
  initScene();
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
    boolean isHighlighted = (n == highlightedNode);
    n.draw(isHighlighted, highlightedPort);
  }
  // draw links
  for(Link l: links) {
    l.draw();
  }
  
  // draw in-progress links
  stroke(0);
  if(linkStartedPort != null) {
    Location start = linkStartedPort.getAbsoluteLocation();
    line(start.x, start.y, mouseX, mouseY);
  }
}

void initScene() {
  String[] types = {"freq", "gate", "lchan", "rchan"};
  for(int i = 0; i < types.length; i++) {
    Node n = new Node(nodeTypes.get(types[i]));
    n.x = 10;
    n.y = i*50;
    nodes.add(n);
  }
}

Port getHighlightedPort() {
  for(Node n: nodes) {
    Port check = n.portNearPoint(mouseX, mouseY);
    if(check != null) {
      return check;
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
        // Remove all links that connect to this node
        ArrayList<Link> toRemove = new ArrayList<Link>();
        for(Link l: links) {
          if(l.inputPort.parent == highlightedNode || l.outputPort.parent == highlightedNode) {
            toRemove.add(l);
          }
        }
        links.removeAll(toRemove);
        nodes.remove(highlightedNode);
        highlightedNode = null;
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

// returns whether link creation was successful
boolean createLink(Port start, Port end) {
  if(
    // Node cannot link to itsself
    (start.parent != end.parent) &&
    // Output cannot link to output and vice versa
    (start.isInput != end.isInput)
  ) {
    Port output = !start.isInput ? start : end;
    Port input = start.isInput ? start : end;

    Link l = new Link(output, input);
    for(Link other: links) {
      if(other.outputPort == output && other.inputPort == input){
        println("Attempt to create duplicate link");
        return false;
      }
    }

    // If another link already goes in to the specified input port, this link
    // should override and remove it
    Link toRemove = null;
    for(Link other: links) {
      if(other.inputPort == input) {
        toRemove = other;
      }
    }
    links.remove(toRemove);

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
  public boolean deletable;

  public NodeDescription(String[] inputs, String[] outputs, String name, boolean deletable) {
    this.inputs = inputs;
    this.outputs = outputs;
    this.name = name;
  }
}

class Location {
  public float x;
  public float y;

  public Location(float x, float y) {
    this.x = x;
    this.y = y;
  }
}

class Node {
  public boolean mouse_snapped = false;
  public ArrayList<Port> ports = new ArrayList<Port>();
  public String name;
  float x, y = 0;
  float w = 75;
  float h;

  // y location from top of node to the title underline
  float titleUnderlineYOffset = myTextSize + 5;
  // y location from top of node to the center of the first port
  float portsYOffset = myTextSize + 15;
  // y spacing between each port
  float portSpacing = 25;

  public Node(NodeDescription desc) {
    int numRows = max(desc.inputs.length, desc.outputs.length);
    this.h = portsYOffset + numRows * portSpacing;
    this.name = desc.name;
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
    if (highlighted) {
      stroke(highlightColor);
    } else {
      stroke(normalColor);
    }

    if(mouse_snapped) {
      x = mouseX - w/2;
      y = mouseY - h/2;
    }

    pushMatrix();
    translate(x, y);
    fill(nodeFillColor);
    rect(0, 0, w, h);
    // underline node title
    line(0, titleUnderlineYOffset, w, titleUnderlineYOffset);

    textAlign(CENTER, TOP);
    fill(normalColor);
    text(name, w/2, 0);

    for(Port p: ports) {
      p.draw(highlightedPort == p);
    }

    popMatrix();
  }
}

class Port {
  // relative to parent
  public float x, y;
  public Node parent;
  public boolean isInput;
  public int index;
  public String name;

  public float ellipseSize = 20;

  public float nodeTextInset = 10;

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
    stroke(0);
    if(highlighted) {
      fill(highlightColor);
    } else {
      fill(nodeFillColor);
    }

    ellipseMode(CENTER);
    ellipse(x, y, ellipseSize, ellipseSize);

    fill(normalColor);
    if(isInput) {
      textAlign(LEFT, CENTER);
      text(name, x + nodeTextInset, y);
    } else {
      textAlign(RIGHT, CENTER);
      text(name, x - nodeTextInset, y);
    }
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

class Link {
  Port outputPort;
  Port inputPort;

  public Link(Port outputPort, Port inputPort) {
    if(outputPort.isInput || !inputPort.isInput) {
      throw new IllegalArgumentException("start must be an output port, end must be an input port");
    }
    this.outputPort = outputPort;
    this.inputPort = inputPort;
  }

  public void draw() {
    stroke(0);
    Location startLoc = outputPort.getAbsoluteLocation();
    Location endLoc = inputPort.getAbsoluteLocation();
    line(startLoc.x, startLoc.y, endLoc.x, endLoc.y);
  }
}
