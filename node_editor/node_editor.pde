import javax.swing.JOptionPane;

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

// color of text, outlines, etc.
color uiColor = color(0, 0, 0);
color linkColor = color(0, 100, 200);
color nodeFillColor = color(200, 200, 255);
color intrinsicNodeFillColor = color(210, 250, 255);
color highlightColor = color(230, 230, 255);

void setup() {
  size(1200, 600);
  textSize(myTextSize);
  initScene(nodes);
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
  stroke(linkColor);
  if(linkStartedPort != null) {
    drawBez(linkStartedPort.getAbsoluteLocation(), new Location(mouseX, mouseY));
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
  switch(key) {
    case ' ':
      // move highlighted node
      if(highlightedNode != null && !highlightedNode.desc.intrinsic) {
        highlightedNode.mouse_snapped = !highlightedNode.mouse_snapped;
      }
      break;
    case 'x':
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
    case 'c':
      // connect link
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
            println("failed to create link");
          } else {
            println("created link");
          }
        }
        linkStartedPort = null;
      }
      break;
    case 'e':
      // edit node value
      if(highlightedPort != null) {
        String input = JOptionPane.showInputDialog(frame, "Enter Value");
        try {
          float value = Float.parseFloat(input);
          highlightedPort.value = value;
        } catch(Exception e) {
          // ignore
          println("invalid input: " + input);
        }
      }
      break;
    case 'p':
      compile(nodes, links);
      break;
    case 'l':
      String filename = JOptionPane.showInputDialog(frame, "Filename");
      Program p = loadProgram(loadJSONObject(filename));
      nodes = p.nodes;
      links = p.links;
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
        other.notifyUnlink();
      }
    }
    links.remove(toRemove);

    linkStartedPort = null;
    l.notifyLink();
    links.add(l);
    return true;
  }
  return false;
}
