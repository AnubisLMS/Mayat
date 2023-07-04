package vavi.net.im.protocol.oscar;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.logging.Logger;
import vavi.net.im.protocol.Connection;
import vavi.net.im.protocol.oscar.command.Command;
import vavi.net.im.protocol.oscar.util.ByteUtils;

/**
 * A thread to send all outgoing packets to AIM.
 *
 * @author Raghu
 */
public class WriterThread extends Thread {

    /** */
    private Logger log = Logger.getLogger(WriterThread.class.getName());

    /**
     * The connection used for writing.
     */
    private Connection conn;

    /**
     * The buffer from which to read.
     */
    private List<Command> buffer;

    /**
     * Flag to stop this thread.
     */
    private boolean quit;

    /**
     * Construct a new writer thread which reads from a specified buffer and
     * writes to a specified connection.
     *
     * @param conn the connection for writing.
     * @param buffer the buffer from which to get the packets.
     */
    public WriterThread(Connection conn, List<Command> buffer) {
        this.conn = conn;
        this.buffer = buffer;
        this.quit = false;
        setName("vavi.net.im.protocol.oscar.WriterThread");
        setDaemon(true);
    }

    /**
     * Change the connection object and start using a new one. This
     * will close the old connection if it is not already closed.
     *
     * @param conn the new connection object.
     */
    public void changeConnection(Connection conn) {
        synchronized (this.conn) {
            try {
                this.conn.close();
            } catch (IOException e) {
            }
            this.conn = conn;
        }
    }

    /**
     * The thread starts here.
     */
    public void run() {
        while (!quit) {
            Command cmd = null;
            synchronized (buffer) {
                while (buffer.isEmpty()) {
                    try {
                        buffer.wait();
                    } catch (InterruptedException e) {
                        log.severe("writerThread interrupted!");
                        break;
                    }
                }
                if (buffer.isEmpty()) {
                    break;
                }
                cmd = buffer.remove(0);
            }
            try {
                if (cmd != null) {
                    byte[] cmdBytes = cmd.getBytes();
                    StringWriter sw = new StringWriter();
                    ByteUtils.dump(cmdBytes, sw);
                    log.severe(">>> SENDING: \n" + sw.toString());
                    conn.write(cmdBytes);
                    conn.flush();
                }
            } catch (IOException e) {
                return;
            }
        }
    }

    /**
     * Stop this thread safely.
     */
    public void stopWriting() {
        quit = true;
        this.interrupt();
    }
}
