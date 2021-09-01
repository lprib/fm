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
