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
    stroke(linkColor);
    strokeWeight(3);
    drawBez(outputPort.getAbsoluteLocation(), inputPort.getAbsoluteLocation());
  }

  public void notifyLink() {
    outputPort.addLink();
    inputPort.addLink();
  }

  public void notifyUnlink() {
    outputPort.removeLink();
    inputPort.removeLink();
  }
}
