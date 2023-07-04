package rabbit.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import rabbit.filter.HTMLFilter;
import rabbit.filter.HTMLFilterFactory;
import rabbit.html.HTMLBlock;
import rabbit.html.HTMLParseException;
import rabbit.html.HTMLParser;
import rabbit.http.HTTPHeader;
import rabbit.io.HTTPInputStream;
import rabbit.io.MultiOutputStream;
import rabbit.proxy.Connection;
import rabbit.util.Logger;
import rabbit.util.SProperties;

/** This class is used to filter html pages.
 */
public class FilterHandler extends GZIPHandler {

    private SProperties config = new SProperties();

    private List<HTMLFilterFactory> filterclasses = new ArrayList<HTMLFilterFactory>();

    public FilterHandler() {
    }

    /** Create a new FilterHandler for the given request.
     * @param con the Connection handling the request.
     * @param request the actual request made.
     * @param response the actual response.
     * @param contentstream the stream to read data from.
     * @param clientstream the stream to write data to.
     * @param maycache May we cache this request? 
     * @param mayfilter May we filter this request?
     * @param size the size of the data beeing handled.
     */
    public FilterHandler(Connection con, HTTPHeader request, HTTPHeader response, HTTPInputStream contentstream, MultiOutputStream clientstream, boolean maycache, boolean mayfilter, long size) {
        super(con, request, response, contentstream, clientstream, maycache, mayfilter, size);
        if (this.mayfilter) {
            response.removeHeader("Content-Length");
            if (!con.getChunking()) con.setKeepalive(false);
        }
    }

    @Override
    public Handler getNewInstance(Connection connection, HTTPHeader header, HTTPHeader webheader, HTTPInputStream contentStream, MultiOutputStream out, boolean maycache, boolean mayfilter, long size) {
        FilterHandler fh = new FilterHandler(connection, header, webheader, contentStream, out, maycache, mayfilter, size);
        fh.config = config;
        fh.filterclasses = filterclasses;
        return fh;
    }

    /** Send the actual data (read data, filter data, send data).
     * @throws IOException if reading or writing of the data fails.
     */
    public void send() throws IOException {
        if (!mayfilter) {
            super.send();
            return;
        }
        byte[] v = new byte[2048];
        int read;
        List<HTMLFilter> filters = initFilters();
        HTMLParser parser = new HTMLParser();
        HTMLBlock block = null;
        long total = 0;
        int start = 0;
        int len = v.length;
        while ((size < 0 || total < size) && (read = contentstream.read(v, start, len)) > 0) {
            total += read;
            read += start;
            parser.setText(v, read);
            try {
                block = parser.parse();
                int fsize = filters.size();
                for (int i = 0; i < fsize; i++) {
                    HTMLFilter hf = filters.get(i);
                    hf.filterHTML(block);
                }
                block.send(clientstream);
            } catch (HTMLParseException e) {
                logError(Logger.INFO, "Bad HTML: " + e.toString());
                clientstream.write(v, 0, read);
            }
            if (block != null && block.restSize() > 0) {
                start = block.restSize();
                if (start == v.length) v = new byte[v.length + 2048];
                len = v.length - start;
                block.insertRest(v);
            } else {
                start = 0;
                len = v.length;
            }
            if (size > -1) {
                long l = size - total;
                if (l < len) len = (int) l;
            }
        }
        if (block != null) {
            block.sendRest(clientstream);
        }
        if (size > 0 && total != size) setPartialContent(total, size);
    }

    /** Setup this class.
     * @param prop the properties of this class.
     */
    @Override
    public void setup(Logger logger, SProperties prop) {
        config = prop;
        String fs = config.getProperty("filters", "");
        StringTokenizer st = new StringTokenizer(fs, ",");
        filterclasses = new ArrayList<HTMLFilterFactory>();
        String classname = "unknown";
        while (st.hasMoreTokens()) {
            try {
                classname = st.nextToken();
                Class<? extends HTMLFilterFactory> cls = Class.forName(classname).asSubclass(HTMLFilterFactory.class);
                filterclasses.add(cls.newInstance());
            } catch (ClassNotFoundException e) {
                logError(Logger.WARN, "Could not find filter: '" + classname + "'");
            } catch (InstantiationException e) {
                logError(Logger.WARN, "Could not instanciate class: '" + classname + "' " + e);
            } catch (IllegalAccessException e) {
                logError(Logger.WARN, "Could not get constructor for: '" + classname + "' " + e);
            }
        }
    }

    /** Initialize the filter we are using.
     * @return a List of HTMLFilters.
     */
    protected List<HTMLFilter> initFilters() {
        int fsize = filterclasses.size();
        List<HTMLFilter> fl = new ArrayList<HTMLFilter>(fsize);
        Class<HTMLFilter> cls = null;
        for (int i = 0; i < fsize; i++) {
            HTMLFilterFactory hff = filterclasses.get(i);
            fl.add(hff.newFilter(con, request, response));
        }
        return fl;
    }
}
