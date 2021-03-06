import javax.swing.JColorChooser;
import java.awt.Color;

class Location {
  public float x;
  public float y;

  public Location(float x, float y) {
    this.x = x;
    this.y = y;
  }
}

void drawBez(Location s, Location e) {
    // flip so start coords are always to the left of end coords
    if(s.x > e.x) {
      Location temp = s;
      s = e;
      e = temp;
    }
    // float controlInset = min((e.x - s.x) / 1.2, 100);
    float controlInset = (e.x - s.x) / 2.0;
    float control1x = s.x + controlInset;
    float control2x = e.x - controlInset;
    noFill();
    bezier(s.x, s.y, control1x, s.y, control2x, e.y, e.x, e.y);
}

void initScene(ArrayList<Node> nodes) {
  String[] types = {"freq", "gate", "lchan", "rchan"};
  for(int i = 0; i < types.length; i++) {
    NodeDescription desc = nodeTypes.get(types[i]);
    Node n = new Node(desc);
    // Place on left of screen if producer, right of screen if consumer
    if(desc.intrinsicProducer) {
      n.x = 10;
    } else {
      n.x = width - n.w - 10;
    }
    n.y = i*80 + 20;
    nodes.add(n);
  }
}

color chooseColor(color defaultValue) {
  Color awtColor = JColorChooser.showDialog(frame, "Node Color", Color.white);
  if(awtColor != null) {
    return color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
  } else {
    return defaultValue;
  }
}
