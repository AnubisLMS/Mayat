package rabbit.meta;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import rabbit.html.*;
import rabbit.http.*;
import rabbit.io.*;
import rabbit.proxy.*;
import rabbit.proxy.Proxy;

/** A class to send files from the htdocs directory.
 *  This makes RabbIT act as a very simple web server.
 */
public class FileSender implements MetaHandler {

    /** handler of a MetaPage, that is a special page the proxy supports (like the status page).
     * @param input the InputStream from the client.
     * @param output the OutputStream to the client.
     * @param header the http request header
     * @param htab the supplied argument to the page (CGI-parameters).
     * @param con the Connection that is serving the request.
     */
    public void handle(InputStream input, MultiOutputStream output, HTTPHeader header, Properties htab, Connection con) {
        String file = htab.getProperty("argstring");
        if (file == null) throw (new IllegalArgumentException("no file given."));
        if (file.indexOf("..") >= 0) throw (new IllegalArgumentException("Bad filename given"));
        String filename = "htdocs/" + file;
        if (filename.endsWith("/")) filename = filename + "index.html";
        filename = filename.replace('/', File.separatorChar);
        HTTPHeader rheader = con.getResponseHandler().getHeader();
        if (filename.endsWith("gif")) rheader.setHeader("Content-type", "image/gif"); else if (filename.endsWith("jpeg") || filename.endsWith("jpg")) rheader.setHeader("Content-type", "image/jpeg"); else if (filename.endsWith("txt")) rheader.setHeader("Content-type", "text/plain");
        File fle = new File(filename);
        long length = fle.length();
        rheader.setHeader("Content-Length", Long.toString(length));
        con.setContentLength(rheader.getHeader("Content-Length"));
        Date lm = new Date(fle.lastModified() - con.getProxy().getOffset());
        rheader.setHeader("Last-Modified", HTTPDateParser.getDateString(lm));
        FileInputStream fis;
        try {
            fis = new FileInputStream(filename);
        } catch (IOException e) {
            throw (new IllegalArgumentException("Could not open file: " + file + "."));
        }
        try {
            WritableByteChannel wc = output.getChannel();
            if (wc != null) {
                channelTransfer(rheader, fis.getChannel(), length, wc);
            } else {
                simpleTransfer(rheader, fis, output);
            }
        } catch (IOException e) {
            throw (new IllegalArgumentException("Could not send: " + file + "."));
        }
        try {
            if (fis != null) fis.close();
        } catch (IOException e) {
            throw (new IllegalArgumentException("Could not close: " + file + "."));
        }
    }

    /** Write the header and the file to the output. 
     */
    private void simpleTransfer(HTTPHeader rheader, FileInputStream fis, MultiOutputStream output) throws IOException {
        output.writeHTTPHeader(rheader);
        byte buffer[] = new byte[2048];
        int i;
        while ((i = fis.read(buffer)) >= 0) {
            output.write(buffer, 0, i);
        }
    }

    /** Write the header and the file to the output. 
     */
    private void channelTransfer(HTTPHeader rheader, FileChannel fis, long length, WritableByteChannel wc) throws IOException {
        byte[] buf = rheader.toString().getBytes();
        ByteBuffer bb = ByteBuffer.wrap(buf);
        wc.write(bb);
        fis.transferTo(0, length, wc);
    }
}
