import javax.swing.JOptionPane;

// text size
float myTextSize = 14;

ArrayList<Node> nodes = new ArrayList<Node>();
ArrayList<Link> links = new ArrayList<Link>();
NotificationQueue notifyQueue = new NotificationQueue();

String currentFilename = "";

// currently highlighted node, can be null if none highlighted
Node highlightedNode = null;

// currently highlighted port, can be null if none highlighted
Port highlightedPort = null;

// If a link has been started, this will be non-null.
Port linkStartedPort = null;

// color of text, outlines, etc.
color bgColor = #102027;
color nodeTitleColor = #ffffff;
color uiColor = #bdbaac;
color outlineColor = #62727b;
color linkColor = color(144, 164, 174, 150);
color nodeFillColor = #37474f;
color intrinsicNodeFillColor = #424242;
color notifyBg = #7f0000;

void setup() {
  size(1400, 800);
  textSize(myTextSize);
  initScene(nodes);
}

void draw() {
  background(bgColor);

  highlightedNode = getHighlightedNode();
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
  stroke(linkColor);
  strokeWeight(2);
  if(linkStartedPort != null) {
    drawBez(linkStartedPort.getAbsoluteLocation(), new Location(mouseX, mouseY));
  }

  notifyQueue.draw();
}

Node getHighlightedNode() {
  Node ret = null;
  for(Node n: nodes) {
    if(n.isPointInside(mouseX, mouseY)) {
      // instead of returning immediately, return the last element of nodes
      // that meets highlight criteria. This is because nodes are drawn in
      // order, so this will return the 'topmost' node that the cursor is over.
      ret = n;
    }
  }
  return ret;
}

Port getHighlightedPort() {
  Port ret = null;
  for(Node n: nodes) {
    Port check = n.portNearPoint(mouseX, mouseY);
    if(check != null) {
      ret = check;
    }
  }
  return ret;
}

void keyPressed() {
  // highlighted actions:
  switch(key) {
    case ' ':
      // snap or unsnap highlightedNode to the mouse
      if(highlightedNode != null && !highlightedNode.desc.intrinsic) {
        highlightedNode.mouse_snapped = !highlightedNode.mouse_snapped;
      }
      break;
    case 'x':
      deleteNodeOrLink();
      break;
    case 's':
      // create sin oscillator
      createNode("sinosc");
      break;
    case 'a':
      // create asdr
      createNode("adsr");
      break;
    case 'm':
      createNode("mixer");
      break;
    case 'c':
      connectOrCreateLink();
      break;
    case 'e':
      editPortValueOrNodeName();
      break;
    case 'E':
      // edit node color
      if(highlightedNode != null && !highlightedNode.desc.intrinsic) {
        highlightedNode.tint = chooseColor(nodeFillColor);
      }
      break;
    case 'p':
      savePatch();
      break;
    case 'l':
      loadPatch();
      break;
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

void deleteNodeOrLink() {
  // delete links to highlighted port if selected, otherwise delete highlighted node
  if(highlightedPort != null) {
    ArrayList<Link> toRemove = new ArrayList<Link>();
    for(Link l: links) {
      if(l.inputPort.equals(highlightedPort) || l.outputPort.equals(highlightedPort)) {
        toRemove.add(l);
        l.notifyUnlink();
      }
    }
    links.removeAll(toRemove);
  } else if(highlightedNode != null && !highlightedNode.desc.intrinsic) {
    // Remove all links that connect to this node
    ArrayList<Link> toRemove = new ArrayList<Link>();
    for(Link l: links) {
      if(
        l.inputPort.parent == highlightedNode ||
        l.outputPort.parent == highlightedNode
      ) {
        toRemove.add(l);
        l.notifyUnlink();
      }
    }
    links.removeAll(toRemove);
    nodes.remove(highlightedNode);
    highlightedNode = null;
  }
}

void connectOrCreateLink() {
  // connect link
  if(linkStartedPort == null) {
    // start a new link if cursor is highlighting a port
    if(highlightedPort != null) {
      linkStartedPort = highlightedPort;
    }
  } else {
    // finish a link if cursor is highlighting a port
    if(highlightedPort != null) {
      createLink(linkStartedPort, highlightedPort);
    }
    linkStartedPort = null;
  }
}

// returns whether link creation was successful
void createLink(Port start, Port end) {
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
        notifyQueue.notify("Attempt to create duplicate link");
        return;
      }
    }

    // If another link already goes in to the specified input port, this link
    // should override and remove it
    Link toRemove = null;
    for(Link other: links) {
      if(other.inputPort == input) {
        toRemove = other;
        other.notifyUnlink();
      }
    }
    links.remove(toRemove);

    linkStartedPort = null;
    l.notifyLink();
    links.add(l);
  } else {
    notifyQueue.notify("Invalid link");
  }
}

void editPortValueOrNodeName() {
  // edit port value
  if(highlightedPort != null) {
    String input = JOptionPane.showInputDialog(frame, "Enter Value");
    if(input != null) {
      try {
        float value = Float.parseFloat(input);
        highlightedPort.value = value;
      } catch(Exception e) {
        // ignore
        notifyQueue.notify("invalid input: " + input);
      }
    }
  } else if(highlightedNode != null && !highlightedNode.desc.intrinsic) {
    // edit node auxillary name
    String newName = JOptionPane.showInputDialog(frame, "Enter name");
    highlightedNode.auxName = newName;
  }
}

void savePatch() {
  String saveFilename = JOptionPane.showInputDialog(frame, "Filename", currentFilename);
  currentFilename = saveFilename;
  JSONObject compiled = compile(nodes, links);
  // TODO input validation
  saveJSONObject(compiled, saveFilename, "indent=4");
  notifyQueue.notify("saved " + saveFilename);
}

void loadPatch() {
  String loadFilename = JOptionPane.showInputDialog(frame, "Filename", currentFilename);
  currentFilename = loadFilename;
  Program p = loadProgram(loadJSONObject(loadFilename));
  nodes = p.nodes;
  links = p.links;
  notifyQueue.notify("loaded " + loadFilename);
}
