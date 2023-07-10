package net.sf.beenuts.net;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Base class for network connections. Ctor: Starts a new thread to establish the connection.
 * This thread starts a Reader and a Writer Thread if connection could be established. 
 * The inter-thread communication is completely handled by this base class. Every Connection instance registers
 * itself to the ConnectionController a singleton responsible to look every n milliseconds if new packets
 * arrived or if something should be send.
 * You can either extend the behavior of an connection by extending it and override the onHandle method 
 * (protocol specific things like response to a ping with a pong) and it is also permit to register ConnectionHandlers
 * which act as listeners.
 * @see ConnectionHandler
 * @see ConnectionController
 * @author Tim Janus
 */
public class Connection implements Runnable {

    /** An object based connection (sends/receives objects extending Serializable) **/
    public static final int OBJECT_BASED_CONNECTION = 1;

    /** An string base connection (sends/receives strings, a '\n' indicates the end of packet. **/
    public static final int LINE_BASED_CONNECTION = 2;

    /** boolean indicating if the connection should continue running or not **/
    protected boolean outerRun = true;

    private Socket mSocket;

    private int mType;

    private Reader reader;

    private Writer writer;

    /** This thread is used for connecting... after connecting it will start reader and writer thread and then terminate. **/
    private Thread connThread;

    /** Thread used for reading from the socket **/
    private Thread readerThread;

    /** Thread used for writing to the socket **/
    private Thread writerThread;

    /**
	 *  string containing last occured error message. Its used in multiple threads. So dont set it directly.
	 * 	@see setLastError
	 */
    private String lastError = "";

    /** ip used by the connection **/
    private String ip;

    /** port used by the connection **/
    private short port;

    /** list of handlers registers to this connection **/
    private List<ConnectionHandler> handlers = new LinkedList<ConnectionHandler>();

    /** queue used for informaing about received objects/strings (inter-thread communciation) **/
    private Queue<Object> receivedQueue = new LinkedList<Object>();

    /** queue used for sending operations (inter-thread communciation) **/
    private Queue<Object> sendQueue = new LinkedList<Object>();

    /**
	 * Implementation of a Reader for a connection. It can either read objects or strings depending
	 * on the type of the connection. An instance of this class runs in its own thread.
	 * @author Tim Janus
	 */
    private class Reader implements Runnable {

        /** Stream used for reading (Socket.getInputStream()). **/
        private InputStream mStream;

        public Reader(InputStream stream) {
            mStream = stream;
        }

        @Override
        public void run() {
            if (mType == OBJECT_BASED_CONNECTION) {
                runObjectReader();
            } else if (mType == LINE_BASED_CONNECTION) {
                runLineReader();
            }
        }

        /**
		 * Helper method: runs a line reader and pushes the received strings on the receive queue.
		 */
        private void runLineReader() {
            BufferedReader in;
            String line;
            try {
                in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                while (outerRun) {
                    line = in.readLine();
                    synchronized (receivedQueue) {
                        receivedQueue.add(line.trim());
                    }
                }
            } catch (SocketException e) {
                outerRun = false;
            } catch (IOException e) {
                setLastError(e.getMessage());
                outerRun = false;
                e.printStackTrace();
            }
        }

        /**
		 * Helper method: runs an object reader and pushs the received objects onto the received queue.
		 */
        private void runObjectReader() {
            try {
                ObjectInputStream ois = new ObjectInputStream(mStream);
                while (outerRun) {
                    Object reval = ois.readObject();
                    synchronized (receivedQueue) {
                        receivedQueue.add(reval);
                    }
                }
                ois.close();
            } catch (EOFException e) {
                outerRun = false;
            } catch (SocketException e) {
                outerRun = false;
            } catch (IOException e) {
                setLastError(e.getMessage());
                outerRun = false;
            } catch (ClassNotFoundException e) {
                setLastError(e.getMessage());
            }
        }
    }

    /**
	 * Implementation of a Writer for a connection. It can either write objects or string to the sockets output
	 * stream depending on the selected Connection type.
	 * @author Tim Janus
	 */
    private class Writer implements Runnable {

        private OutputStream mStream;

        public Writer(OutputStream stream) {
            mStream = stream;
        }

        @Override
        public void run() {
            if (mType == LINE_BASED_CONNECTION) {
                PrintWriter out;
                out = new PrintWriter(mStream);
                while (outerRun) {
                    synchronized (sendQueue) {
                        while (sendQueue.size() > 0) {
                            try {
                                String toSend = sendQueue.poll() + "\n";
                                out.write(toSend);
                                out.flush();
                            } catch (ClassCastException ex) {
                                setLastError("Cant send not string class in LINE_BASE_CONNECTION Mode: " + ex.getMessage());
                            }
                        }
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else if (mType == OBJECT_BASED_CONNECTION) {
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(mStream);
                    while (outerRun) {
                        synchronized (sendQueue) {
                            while (sendQueue.size() > 0) {
                                try {
                                    Serializable obj = (Serializable) sendQueue.poll();
                                    oos.writeObject(obj);
                                } catch (ClassCastException ex) {
                                    setLastError("Cant send object not dereived from Serializable: " + ex.getMessage());
                                }
                            }
                            oos.flush();
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    oos.close();
                } catch (IOException e) {
                    setLastError(e.getMessage());
                    outerRun = false;
                }
            }
        }
    }

    /**
	 * Ctor: Creates a new connection.
	 * @param sock	reference to the socket which should be used for the connection.
	 * @param type	type of the connection either object based or string based
	 * @see Connection.OBJECT_BASED_CONNECTION
	 * @see Connection.STRING_BASED_CONNECTION
	 */
    public Connection(Socket sock, int type) {
        mSocket = sock;
        this.ip = sock.getInetAddress().getHostAddress();
        init(type);
    }

    /**
	 * Ctor: Creates a new Connection
	 * @param ip	the endpoints ip
	 * @param port	the endpoints port
	 * @param type	type of the connection either object based or string based
	 * @see Connection.OBJECT_BASED_CONNECTION
	 * @see Connection.STRING_BASED_CONNECTION
	 */
    public Connection(String ip, short port, int type) {
        this.ip = ip;
        this.port = port;
        init(type);
    }

    /**
	 * Adds a hander to the list of handlers.
	 * @param ch
	 * @see ConnectionHandler
	 */
    public void addHandler(ConnectionHandler ch) {
        handlers.add(ch);
    }

    /**
	 * Removes the given handler from the list of handlers.
	 * @param ch
	 * @return	true if remove was successful otherwise false.
	 */
    public boolean removeHandler(ConnectionHandler ch) {
        return handlers.remove(ch);
    }

    public void removeAllHandlers() {
        handlers.clear();
    }

    @Override
    public String toString() {
        return ip + ":" + String.valueOf(port);
    }

    private void init(int type) {
        if (type != LINE_BASED_CONNECTION && type != OBJECT_BASED_CONNECTION) throw new IllegalArgumentException("type must be LINE_ or OBJECT_ BASE_CONNECTION");
        mType = type;
        ConnectionController.getInstance().registerConnection(this);
        connThread = new Thread(this);
        connThread.start();
    }

    /**
	 * This method is normally called by ConnectionHandler.unregisterConnection
	 * Use ConnectionController.unregisterConnection to stop a connection (shutdown).
	 */
    protected void internalStop() {
        outerRun = false;
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (mSocket == null) {
            try {
                if (mSocket == null) {
                    mSocket = new Socket(ip, port);
                }
            } catch (ConnectException ce) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Throwable t) {
                t.printStackTrace();
                synchronized (lastError) {
                    setLastError(t.getMessage());
                }
                return;
            }
        }
        try {
            reader = new Reader(mSocket.getInputStream());
            readerThread = new Thread(reader);
            readerThread.start();
            writer = new Writer(mSocket.getOutputStream());
            writerThread = new Thread(writer);
            writerThread.start();
        } catch (IOException e1) {
            setLastError(e1.getMessage());
            e1.printStackTrace();
        }
    }

    void update() {
        synchronized (receivedQueue) {
            while (receivedQueue.size() > 0) onHandle(receivedQueue.poll());
        }
        synchronized (lastError) {
            if (lastError != "") onError(lastError);
            lastError = "";
        }
        if (!outerRun) {
            ConnectionController.getInstance().unregisterConnection(this);
            onClose();
        }
    }

    /**
	 * Sets the string with the last occured error. This method synchronised on the lastError
	 * attribute so dont change lastError directly. Use this method instead.
	 * @param error	The new value of the error message.
	 */
    protected void setLastError(String error) {
        synchronized (lastError) {
            lastError = error != null ? error : "";
        }
    }

    /**
	 * Helper method: Sending a string with a "\n" at the end.
	 * This method helps by the inter-thread communication.
	 * @param s	String with the data to send.
	 */
    public void sendLine(String s) {
        synchronized (sendQueue) {
            sendQueue.add(s);
        }
    }

    /**
	 * Helper method: Sends a object over the network by using the
	 * java object streams.
	 * This method helps by the inter-thread communication.
	 * @param object	Reference to the object which should be send.
	 */
    public void sendObject(Serializable object) {
        synchronized (sendQueue) {
            sendQueue.add(object);
        }
    }

    /**
	 * Is called when an error occurs. Informs the registers listeners. Sub classes can
	 * add their own error handling here, but they must call super otherwise
	 * the handlers wont get informed.
	 * @param error	String with the error message.
	 */
    protected void onError(String error) {
        for (ConnectionHandler h : handlers) h.errorOccurred(this, error);
    }

    /**
	 * Is called when the connection receives something to handle. Informs the registers listeners. 
	 * Sub classes can add their own handling code here, but they must also call super otherwise
	 * the handlers wont get informed.
	 * @param error	String with the error message.
	 */
    protected void onHandle(Object obj) {
        for (ConnectionHandler h : handlers) h.dataReceived(this, obj);
    }

    /**
	 * Is called when the connection was closed. Informs the registers listeners. Sub classes can
	 * implement their own behavior when the connection got closed here but the must call super otherwise
	 * the handlers wont get informed.
	 */
    protected void onClose() {
        for (ConnectionHandler h : handlers) h.connectionClosed(this);
    }
}
