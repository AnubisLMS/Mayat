package jifx.connection.connector.iso;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.log4j.Logger;
import jifx.commons.messages.IMessage;
import jifx.message.iso8583.ISOPackager;

/**
 * This class writes data and sends an ISOMessage to the connected client. 
 *
 */
public class ConnectorWriter extends Thread {

    static Logger logger = Logger.getLogger(ConnectorISO.class);

    private OutputStream out;

    private boolean activate;

    private IMessage buffer;

    private ConnectorISO connector;

    public ConnectorWriter(OutputStream outputStream, ConnectorISO connector) {
        out = outputStream;
        activate = true;
        buffer = null;
        this.connector = connector;
    }

    /**
	 * Method which generates an ISOMessage and writes it to the output stream in order to
	 * communicate it to the client. 
	 */
    public void run() {
        try {
            while (activate) {
                synchronized (this) {
                    if (buffer != null) {
                        String msg = (String) new ISOPackager().pack(buffer);
                        out.write(msg.getBytes());
                        buffer = null;
                    }
                }
            }
        } catch (IOException e) {
            logger.error(connector.getChannelName() + "| Problema de escritura, " + e.getMessage() + "|");
        }
    }

    public boolean isActivate() {
        return activate;
    }

    public void setActivate(boolean activate) {
        this.activate = activate;
    }

    /**
	 * Method which is invoked from the TryConnectThread in order to send a message (ISOMessage)
	 * to a specific client.  
	 */
    public void sendMessage(IMessage message) {
        synchronized (this) {
            buffer = message;
        }
    }
}
