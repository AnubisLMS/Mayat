package org.granite.gravity.tomcat;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.CometEvent;
import org.granite.gravity.AsyncHttpContext;
import org.granite.gravity.Gravity;
import org.granite.gravity.GravityManager;
import org.granite.logging.Logger;
import flex.messaging.messages.Message;

/**
 * @author Franck WOLFF
 */
public class GravityTomcatServlet extends AbstractCometProcessor {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(GravityTomcatServlet.class);

    @Override
    public CometIO createCometIO() {
        return new ByteArrayCometIO();
    }

    @Override
    public boolean handleRequest(CometEvent event, InputStream content) throws IOException, ServletException {
        Gravity gravity = GravityManager.getGravity(getServletContext());
        HttpServletRequest request = event.getHttpServletRequest();
        HttpServletResponse response = event.getHttpServletResponse();
        try {
            initializeRequest(gravity, request, response);
            Message[] amf3Requests = deserialize(gravity, request, content);
            log.debug(">> [AMF3 REQUESTS] %s", (Object) amf3Requests);
            Message[] amf3Responses = null;
            boolean accessed = false;
            for (int i = 0; i < amf3Requests.length; i++) {
                Message amf3Request = amf3Requests[i];
                Message amf3Response = gravity.handleMessage(amf3Request);
                String channelId = (String) amf3Request.getClientId();
                if (!accessed) accessed = gravity.access(channelId);
                if (amf3Response == null) {
                    if (amf3Requests.length > 1) throw new IllegalArgumentException("Only one connect request is allowed on tunnel.");
                    TomcatChannel channel = (TomcatChannel) gravity.getChannel(channelId);
                    if (channel == null) throw new NullPointerException("No channel on tunnel connect");
                    if (channel.runReceived(new AsyncHttpContext(request, response, amf3Request))) return true;
                    setConnectMessage(request, amf3Request);
                    channel.setCometEvent(event);
                    return false;
                }
                if (amf3Responses == null) amf3Responses = new Message[amf3Requests.length];
                amf3Responses[i] = amf3Response;
            }
            log.debug("<< [AMF3 RESPONSES] %s", (Object) amf3Responses);
            serialize(gravity, response, amf3Responses);
        } catch (IOException e) {
            log.error(e, "Gravity message error");
            throw e;
        } catch (Exception e) {
            log.error(e, "Gravity message error");
            throw new ServletException(e);
        } finally {
            try {
                if (content != null) content.close();
            } finally {
                cleanupRequest(request);
            }
        }
        return true;
    }

    @Override
    public boolean handleEnd(CometEvent event) throws IOException, ServletException {
        return true;
    }

    @Override
    public boolean handleError(CometEvent event) throws IOException {
        if (EventUtil.isErrorButNotTimeout(event)) log.warn("Got an error event: %s", EventUtil.toString(event));
        try {
            HttpServletRequest request = event.getHttpServletRequest();
            Message connect = getConnectMessage(request);
            if (connect != null) {
                Gravity gravity = GravityManager.getGravity(getServletContext());
                String channelId = (String) connect.getClientId();
                TomcatChannel channel = (TomcatChannel) gravity.getChannel(channelId);
                if (channel != null) channel.setCometEvent(null);
            }
        } catch (Exception e) {
            log.error(e, "Error while processing event: %s", EventUtil.toString(event));
        }
        return true;
    }
}
