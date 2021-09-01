String compile(ArrayList<Node> nodes, ArrayList<Link> links) {
  HashMap<Port, Integer> allocations = allocateLinks(links);
  println("COMPILATION");
  JSONObject programJson = new JSONObject();
  JSONArray nodesJson = new JSONArray();
  for(Node n: nodes) {
    if(!n.desc.intrinsic) {
      nodesJson.append(getNodeObject(n, allocations));
    }
  }
  programJson.setJSONObject("io", getIntrinsicAssignments(nodes, allocations));
  programJson.setJSONArray("nodes", nodesJson);
  println(programJson.toString());
  return null;
}

JSONObject getIntrinsicAssignments(ArrayList<Node> nodes, HashMap<Port, Integer> allocations) {
  JSONObject assignments = new JSONObject();
  for(Node n: nodes) {
    if(n.desc.intrinsic) {
      // intrinsics should only have a single port
      Integer alloc = allocations.get(n.ports.get(0));
      if(alloc != null) {
        assignments.setInt(n.desc.name, alloc);
      }
    }
  }
  return assignments;
}

JSONObject getNodeObject(Node n, HashMap<Port, Integer> allocations) {
  JSONObject nodeJson = new JSONObject();
  nodeJson.setString("type", n.desc.name);
  nodeJson.setFloat("_x", n.x);
  nodeJson.setFloat("_y", n.y);
  for(Port p: n.ports) {
    JSONObject portJson = new JSONObject();
    Integer alloc = allocations.get(p);
    if(alloc != null) {
      // this port is linked
      portJson.setInt("link", alloc);
      nodeJson.setJSONObject(p.name, portJson);
    } else {
      // port is not linked
      if(p.isInput) {
        portJson.setFloat("const", p.value);
        nodeJson.setJSONObject(p.name, portJson);
      } else {
        // ignore unlinked output ports
      }
    }
  }
  return nodeJson;
}

HashMap<Port, Integer> allocateLinks(ArrayList<Link> links) {
  HashMap<Port, Integer> allocations = new HashMap<Port, Integer>();
  ArrayList<Port> portAlloc = new ArrayList<Port>();
  for(Link l: links) {
    if(!portAlloc.contains(l.outputPort)) {
      portAlloc.add(l.outputPort);
    }
    Integer allocationIndex = Integer.valueOf(portAlloc.indexOf(l.outputPort));
    allocations.put(l.outputPort, allocationIndex);
    allocations.put(l.inputPort, allocationIndex);
  }
  return allocations;
}

class Program {
  public ArrayList<Node> nodes;
  public ArrayList<Link> links;
}

class LinkDef {
  public Port producer;
  public ArrayList<Port> consumers;
  public LinkDef() {
    producer = null;
    consumers = new ArrayList<Port>();
  }
}

Program loadProgram(JSONObject programJson) {
  JSONArray nodesJson = programJson.getJSONArray("nodes");

  ArrayList<Node> nodes = new ArrayList<Node>();
  ArrayList<Link> links = new ArrayList<Link>();
  HashMap<Integer, LinkDef> linkDefs = new HashMap<Integer, LinkDef>();

  initScene(nodes);
  loadIntrinsicNodes(programJson.getJSONObject("io"), linkDefs, nodes);

  for(int i = 0; i < nodesJson.size(); i++) {
    nodes.add(nodeFromJson(nodesJson.getJSONObject(i), linkDefs));
  }

  for(LinkDef linkDef: linkDefs.values()) {
    for(Port consumer: linkDef.consumers) {
      Link l = new Link(linkDef.producer, consumer);
      l.notifyLink();
      links.add(l);
    }
  }

  Program ret = new Program();
  ret.nodes = nodes;
  ret.links = links;
  return ret;
}

void loadIntrinsicNodes(JSONObject ioJson, HashMap<Integer, LinkDef> linkDefs, ArrayList<Node> nodes) {
  for(NodeDescription desc: nodeTypes.values()) {
    if(desc.intrinsic) {
      // TODO check if the name of this intrinsic is in the io object
      // if so, add to linkDefs
      int intrinsicIdx = ioJson.getInt(desc.name, -1);
      if(intrinsicIdx != -1) {
        initId(intrinsicIdx, linkDefs);
        if(desc.intrinsicProducer) {
          linkDefs.get(intrinsicIdx).producer = getNodeByName(desc.name, nodes).ports.get(0);
        } else {
          linkDefs.get(intrinsicIdx).consumers.add(getNodeByName(desc.name, nodes).ports.get(0));
        }
      }
    }
  }
}

Node nodeFromJson(JSONObject json, HashMap<Integer, LinkDef> linkDefs) {
  String type = json.getString("type");
  Node n = new Node(nodeTypes.get(type));
  n.x = json.getFloat("_x");
  n.y = json.getFloat("_y");
  for(String portName: n.desc.inputs) {
    linkPort(n, portName, json, linkDefs);
  }
  for(String portName: n.desc.outputs) {
    linkPort(n, portName, json, linkDefs);
  }
  return n;
}

// Find portName in nodeJson. If it is defined as a const value, set the ports
// value on Node's port list.
// If it is defined as a link, add the port to the linkDefs mapping.
void linkPort(Node n, String portName, JSONObject nodeJson, HashMap<Integer, LinkDef> linkDefs) {
    JSONObject portJson = nodeJson.getJSONObject(portName);
    for(Port p: n.ports) {
      if(p.name.equals(portName)) {
        p.value = portJson.getFloat("const", 0.0);
        Integer linkID = Integer.valueOf(portJson.getInt("link", -1));
        if(linkID != -1) {
          // This port has a link value
          initId(linkID, linkDefs);

          if(p.isInput) {
            //inputs are consumers
            linkDefs.get(linkID).consumers.add(p);
          } else {
            // outputs are producers
            linkDefs.get(linkID).producer = p;
          }
        }
      }
    }
}

void initId(int index, HashMap<Integer, LinkDef> linkDefs) {
  // Create LinkDef if not exist
  Integer indexInt = Integer.valueOf(index);
  if(!linkDefs.containsKey(indexInt)) {
    linkDefs.put(indexInt, new LinkDef());
  }
}

Node getNodeByName(String name, ArrayList<Node> nodes) {
  for(Node n: nodes) {
    if(n.desc.name.equals(name)) {
      return n;
    }
  }
  return null;
}
