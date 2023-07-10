package spaghettiserver;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatConnection extends Thread implements TagListener {

    final int STARTED = 0, OK = 1;

    int state = STARTED;

    ChatSession cs;

    public String sessionname = "";

    public String sessionpassword = "";

    public String roomid = "";

    public String name = "";

    public String password = "";

    String fn = "";

    String data = "";

    boolean owner = false;

    boolean maydraw = true;

    DataOutputStream dout;

    BufferedReader din;

    boolean running = true;

    boolean inPut = false;

    XMLTagParser xmlparser;

    Server a;

    Socket s;

    ChatConnection(Server a, Socket s, DataOutputStream dout, DataInputStream din) {
        this.a = a;
        this.din = new BufferedReader(new InputStreamReader(din));
        this.dout = dout;
        this.s = s;
        xmlparser = new XMLTagParser();
        xmlparser.attach(this);
    }

    boolean connected = true;

    public void send(String l) {
        if (connected) {
            System.out.println("SEND: " + l);
            try {
                dout.writeBytes(l + "\n");
                dout.flush();
            } catch (Exception e) {
                e.printStackTrace();
                this.a.csm.getSession(this.name).removeConnection(this);
            }
        }
    }

    public void run() {
        System.out.println("Incoming parser started.");
        while (running) {
            try {
                char c = (char) s.getInputStream().read();
                int i1 = (int) c;
                if (i1 == 65535) {
                    running = false;
                    killConnection();
                }
                xmlparser.addChar(c);
            } catch (Exception e) {
                e.printStackTrace();
                killConnection();
            }
        }
    }

    public void error(String error) {
        try {
            send(error);
        } catch (Exception e) {
        }
    }

    public void killConnection() {
        try {
            System.out.println("Current roomid: " + roomid);
            if (a != null) {
                if (a.csm != null) {
                    if (a.csm.getSession(roomid) != null) {
                        a.csm.getSession(roomid).removeConnection(this);
                    } else {
                        System.out.println("C");
                    }
                } else {
                    System.out.println("B");
                }
            } else {
                System.out.println("A");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("killing connection: " + this.name + "/" + s.getInetAddress());
            running = false;
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String sketch;

    boolean sketchflag = false;

    int depth = 0;

    public void tagStart(Element e) {
        depth++;
        System.out.println("RECV: " + e.toString());
    }

    public void tagStop(Element e) {
        depth--;
        if (depth < 2) {
            System.out.println("RECV: " + e.toString());
            switch(state) {
                case STARTED:
                    parseHandshake(e);
                    break;
                case OK:
                    parse(e);
                    break;
            }
        }
    }

    /**
	 * actually parses the handshake 
	 * @param e
	 */
    void parseHandshake(Element e) {
        if (e.name.equals("handshake")) {
            String login = e.getElement("login").getText();
            this.name = login;
            String password = e.getElement("password").getText();
            if (!a.dblayer.loginOK(login, password)) {
                this.send("<handshake code='rejected'/> ");
                killConnection();
            } else {
                this.send("<handshake code='ok'/> ");
                if (login.equals("admin")) {
                    this.send("<showadminframe/>");
                }
                state = OK;
            }
        }
    }

    /**
	 * actually parses everything during an established connection 
	 * @param e
	 */
    void parse(Element e) {
        String tagname = e.name;
        if (tagname.equals("privatejoin")) {
            String roomid = e.getAttr("roomid");
            String password = "";
            String who = e.getElement("username").getText();
            this.a.csm.getSession(roomid).addConnection(this.a.csm.getSession("mainhall").getUser(who));
            if (e.getElement("password") != null) {
                password = e.getElement("password").getText();
            }
            if (a.csm.getSession(roomid) != null) {
                if (a.csm.getSession(roomid).password.equals(password)) {
                    a.csm.getSession(roomid).addConnection(this);
                    this.roomid = roomid;
                } else {
                }
            }
            this.roomid = roomid;
        }
        if (tagname.equals("join")) {
            String roomid = e.getElement("roomid").getText();
            String password = "";
            if (e.getElement("password") != null) {
                password = e.getElement("password").getText();
            }
            if (a.csm.getSession(roomid) != null) {
                if (a.csm.getSession(roomid).password.equals(password)) {
                    if (a.csm.getSession(roomid).priv) {
                        a.csm.getSession(roomid).askOwnerForPermissionToJoin(this.name);
                    } else {
                        a.csm.getSession(roomid).addConnection(this);
                        this.roomid = roomid;
                    }
                } else {
                }
            } else {
                this.owner = true;
                a.csm.createSession(roomid, password);
                a.csm.getSession(roomid).addConnection(this);
                Element priv = e.getElement("private");
                if (priv != null) {
                    a.csm.getSession(roomid).setPrivate(true);
                }
                System.out.println("Setting owner.");
                this.roomid = roomid;
            }
        } else if (tagname.equals("leave")) {
            ChatSession cs = this.a.csm.getSession(e.getElement("roomid").getText());
            cs.removeConnection(this);
        } else if (tagname.equals("message")) {
            String type = e.getAttr("type");
            String to = e.getAttr("roomid");
            if (type.equals("conference")) {
                if (a.csm.getSession(to) != null) {
                    e.attributes.put("from", this.name);
                    a.csm.getSession(to).dispatch(e.toString());
                }
                Element e2 = e.getElement("owner");
                if (e2 != null) {
                    Element e21 = e2.getElement("code");
                    Element e3 = e2.getElement("username");
                    if (e21 != null & e3 != null) {
                        a.csm.getSession(to).changeUserRights(this, e3.getText(), e21.getText());
                    }
                }
            } else {
                e.addAttr("from", this.name);
                System.out.println("Private message recieved. ");
                Vector v = this.a.csm.sessions;
                for (int i = 0; i < v.size(); i++) {
                    ChatSession cs = (ChatSession) v.elementAt(i);
                    Vector v2 = cs.cons;
                    for (int j = 0; j < v2.size(); j++) {
                        ChatConnection c = (ChatConnection) v2.elementAt(j);
                        if (c.name.equals(e.getAttr("to"))) {
                            c.send(e.toString());
                            System.out.println("Private message sent.");
                            return;
                        }
                    }
                }
            }
        } else if (tagname.equals("administration")) {
            if (e.getElement("newuser") != null) {
                Element e1 = e.getElement("newuser");
                String username = e1.getElement("username").getText();
                String password = e1.getElement("password").getText();
                if (a.dblayer.userExists(username)) {
                } else {
                    a.dblayer.createUser(username, password);
                }
            }
        } else if (tagname.equals("get")) {
            System.out.println("Parsing get.");
            Element e1 = e.getElement("conferencelist");
            if (e1 != null) {
                this.send(this.a.csm.getChannelList());
                System.out.println("Transmitting room list.");
            }
            e1 = e.getElement("filelist");
            if (e1 != null) {
                this.pushFolderContent();
            }
        } else if (tagname.equals("whiteboard") & maydraw) {
            String to = e.getAttr("roomid");
            if (a.csm.getSession(to) != null) {
                e.attributes.put("from", this.name);
                a.csm.getSession(to).dispatchExcept(e.toString(), this);
            }
        } else if (tagname.equals("fileput")) {
            fn = e.getElement("name").getText();
            data = e.getElement("content").getText();
            a.dblayer.saveFile(fn, data);
            this.pushFolderContent();
        } else if (tagname.equals("kick")) {
            String username = e.getText();
            ChatSession cs = a.csm.getSession(this.roomid);
            if (cs != null) cs.kickUser(username); else System.out.println("chat session not found.");
        } else if (tagname.equals("title")) {
            ChatSession cs = a.csm.getSession(this.roomid);
            if (cs != null) cs.setTitle(e.getText()); else System.out.println("chat session not found.");
        } else if (tagname.equals("mute")) {
            ChatSession cs = a.csm.getSession(this.roomid);
            if (cs != null) {
                this.a.vsm.mute(e.getText());
            }
        } else if (tagname.equals("demute")) {
            ChatSession cs = a.csm.getSession(this.roomid);
            if (cs != null) {
                this.a.vsm.demute(e.getText());
            }
        }
    }

    void pushFolderContent() {
        this.send("<ret><filelist>" + a.dblayer.getFolderContent(this.name) + "</filelist></ret>");
    }
}
