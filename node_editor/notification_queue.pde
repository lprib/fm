class Notification {
  public String text;
  public int creationMillis;
  public Notification(String text) {
    this.text = text;
    this.creationMillis = millis();
  }
}

class NotificationQueue {
  ArrayList<Notification> notifications = new ArrayList<Notification>();
  int notificationStayTime = 5000;
  float padding = 10;
  float notificationHeight = myTextSize + padding;

  public void draw() {
    int currentTime = millis();
    ArrayList<Notification> toRemove = new ArrayList<Notification>();
    for(Notification n: notifications) {
      if(currentTime - n.creationMillis >= notificationStayTime) {
        toRemove.add(n);
      }
    }
    notifications.removeAll(toRemove);

    for(int i = 0; i < notifications.size(); i++) {
      fill(notifyBg);
      float x = 20 + i*(notificationHeight + 10);
      rect(20, x, textWidth(notifications.get(i).text) + padding, notificationHeight, 5, 5, 5, 5);
      fill(uiColor);
      textAlign(LEFT, TOP);
      text(notifications.get(i).text, 20 + padding/2, x + padding/2);
    }
  }

  void notify(String text) {
    notifications.add(new Notification(text));
  }
}
