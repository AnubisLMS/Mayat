package freelands;

import freelands.actor.Player;
import freelands.protocol.ChatChannel;
import freelands.protocol.TextColor;
import freelands.protocol.message.toclient.RawMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

public class NonBlockingServer {

    public static Selector sel = null;

    public static ServerSocketChannel server = null;

    public static SocketChannel socket = null;

    private static final HashMap<Short, NetworkClient> CLIENTS = new HashMap<Short, NetworkClient>(Main.preferences.CLIENTS_MAX);

    private static final List<NetworkClient> inprogressclients = new ArrayList<NetworkClient>();

    private static final HashMap<String, Short> NAMETOID = new HashMap<String, Short>();

    public static void initialize() throws IOException, UnknownHostException {
        Main.preferences.LOGGER.info("Initializing freelands...");
        sel = SelectorProvider.provider().openSelector();
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        InetSocketAddress isa = new InetSocketAddress(Main.preferences.IP, Main.preferences.PORT);
        server.socket().bind(isa);
        Main.preferences.LOGGER.log(Level.INFO, "OK {0}", Main.preferences.IP.getHostAddress() + ":" + Main.preferences.PORT);
    }

    private static void acceptClient(SelectionKey key) throws IOException, InterruptedException {
        ServerSocketChannel nextReady = (ServerSocketChannel) key.channel();
        SocketChannel channel = nextReady.accept();
        channel.configureBlocking(false);
        SelectionKey readKey = channel.register(sel, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        NetworkClient nc = new NetworkClient(Main.preferences.NUMCLIENT++);
        nc.hisSocket = channel;
        CLIENTS.put(nc.id, nc);
        inprogressclients.add(nc);
        readKey.attach(nc);
        Main.preferences.LOGGER.log(Level.FINE, "Client has connected to position {0}", nc.id);
    }

    private static void readMessageFromNetwork(SelectionKey key) {
        NetworkClient distant = (NetworkClient) key.attachment();
        ByteBuffer byteBuffer = ByteBuffer.allocate(500);
        if (distant.msgbuffer != null) {
            byteBuffer.put(distant.msgbuffer);
            distant.msgbuffer = null;
        }
        if (distant.isToBeKilled) {
            return;
        }
        int readsize;
        try {
            readsize = distant.getChannel().read(byteBuffer);
            if (readsize == -1) {
                throw new java.io.IOException();
            }
        } catch (IOException e) {
            Main.preferences.LOGGER.fine("In NonBlockingServer::ReadMessageFromNetwork -> killing a client!");
            distant.isToBeKilled = true;
            try {
                distant.hisSocket.close();
            } catch (IOException e2) {
                Main.preferences.LOGGER.fine("In NonBlockingServer::ReadMessageFromNetWork ->" + "can't close socket!");
            }
            return;
        }
        processBuffer(byteBuffer, distant, readsize);
    }

    private static void processBuffer(ByteBuffer byteBuffer, NetworkClient distant, int readsize) {
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.rewind();
        short s = byteBuffer.getShort(1);
        if (readsize > s + 2) {
            byteBuffer.position(s + 2);
            byte other[] = new byte[readsize - s - 2];
            byteBuffer.get(other);
            distant.msgbuffer = other;
        }
        byteBuffer.rewind();
        if (!InputProcessor.processMessageFromClient(distant, byteBuffer)) {
            distant.addPacketTosend(RawMessage.rawTextMessage(ChatChannel.LOCAL, TextColor.c_red1, "Network error, disconnect player"));
            distant.isToBeKilled = true;
        }
    }

    private static void readBufferedMessage(NetworkClient nc) {
        if (!nc.isToBeKilled && nc.msgbuffer != null) {
            processBuffer(ByteBuffer.wrap(nc.msgbuffer), nc, nc.msgbuffer.length);
            nc.msgbuffer = null;
        }
    }

    public static void startServer() throws IOException, InterruptedException {
        initialize();
        SelectionKey acceptKey = server.register(sel, SelectionKey.OP_ACCEPT);
        while (true) {
            try {
                if (acceptKey.selector().selectNow() > 0) {
                    Iterator<SelectionKey> it = sel.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        if (key.isAcceptable()) {
                            acceptClient(key);
                        } else if (key.isReadable()) {
                            readMessageFromNetwork(key);
                        }
                    }
                }
                for (NetworkClient nc : CLIENTS.values()) {
                    readBufferedMessage(nc);
                }
                checkClients();
                GameTime.checkForNewMinute();
                for (NetworkClient nc : CLIENTS.values()) {
                    nc.sendCurrentsMessages();
                }
            } catch (IOException e) {
                Main.preferences.LOGGER.severe("error in Main server loop");
            }
        }
    }

    static void playerIsInThread(NetworkClient from) {
        inprogressclients.remove(from);
    }

    private static void checkClients() {
        List<NetworkClient> toberemove = new ArrayList<NetworkClient>();
        for (NetworkClient p : CLIENTS.values()) {
            if (p.isToBeKilled) {
                toberemove.add(p);
            }
        }
        for (NetworkClient nc : toberemove) {
            Main.preferences.mapManager.remove(nc.player);
            removePlayer(nc.id);
        }
    }

    public static void removePlayer(short id) {
        NetworkClient nc = CLIENTS.remove(id);
        if (nc.player == null) {
            inprogressclients.remove(nc);
        } else {
            FreelandsThread.requestRemovePlayer(nc.id);
            NAMETOID.remove(nc.player.getContent().name.toLowerCase());
            CharDatabase.saveChar(nc.player.getContent(), nc.player.getInventory());
        }
    }

    public static boolean playerExist(short id) {
        return CLIENTS.containsKey(id);
    }

    public static boolean playerAlreadyIn(String name) {
        Object[] os = CLIENTS.values().toArray();
        for (Object o : os) {
            if (((NetworkClient) o).player != null && ((NetworkClient) o).player.getContent().name.equals(name)) {
                return ((NetworkClient) o).isLoggedIn;
            }
        }
        return false;
    }

    public static void sendMessageToAll(ByteBuffer msg) {
        for (NetworkClient nc : CLIENTS.values()) {
            nc.addPacketTosend(msg);
        }
    }

    public static boolean sendMessage(short id, ByteBuffer msg) {
        NetworkClient nc = CLIENTS.get(id);
        if (nc != null) {
            nc.addPacketTosend(msg);
            return true;
        } else {
            return false;
        }
    }

    public static Player nameToPlayer(String name) {
        Short id = NAMETOID.get(name.toLowerCase());
        if (id == null) {
            return null;
        }
        NetworkClient nc = CLIENTS.get(id);
        if (nc == null) {
            return null;
        }
        return nc.player;
    }
}
