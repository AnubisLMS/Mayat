package com.plexobject.docusearch.http.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.SocketException;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.validator.GenericValidator;
import org.apache.log4j.Logger;
import com.plexobject.docusearch.Configuration;
import com.plexobject.docusearch.domain.Tuple;
import com.plexobject.docusearch.http.RestClient;
import com.plexobject.docusearch.http.RestException;
import com.plexobject.docusearch.metrics.Metric;
import com.plexobject.docusearch.metrics.Timer;

/**
 * This class provides APIs to call remote Web service with JSON payload
 * 
 * @author Shahzad Bhatti
 */
public class RestClientImpl implements RestClient {

    private static final Logger LOGGER = Logger.getLogger(RestClientImpl.class);

    private static final int CONNECTION_TIMEOUT_MILLIS = Configuration.getInstance().getInteger("rest.connection.timeout.millis", 10000);

    private HttpClient httpClient = new HttpClient();

    private final String url;

    public RestClientImpl(final String url) {
        this(url, null, null);
    }

    public RestClientImpl(final String url, final String username, final String password) {
        if (GenericValidator.isBlankOrNull(url)) {
            throw new IllegalArgumentException("url not specified");
        }
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        httpClient.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        httpClient.getParams().setSoTimeout(CONNECTION_TIMEOUT_MILLIS);
        httpClient.getParams().setParameter("http.socket.timeout", new Integer(CONNECTION_TIMEOUT_MILLIS));
        httpClient.getParams().setParameter("http.connection.timeout", new Integer(CONNECTION_TIMEOUT_MILLIS));
        if (!GenericValidator.isBlankOrNull(username) && !GenericValidator.isBlankOrNull(password)) {
            httpClient.getParams().setAuthenticationPreemptive(true);
            final Credentials creds = new UsernamePasswordCredentials(username, password);
            httpClient.getState().setCredentials(AuthScope.ANY, creds);
        }
    }

    public Tuple get(final String path) throws IOException {
        return execute(new GetMethod(url(path)));
    }

    public Tuple put(final String path, final String body) throws IOException {
        final PutMethod method = new PutMethod(url(path));
        if (body != null) {
            method.setRequestEntity(new StringRequestEntity(body, "application/json", "UTF-8"));
        }
        try {
            return execute(method);
        } finally {
            method.releaseConnection();
        }
    }

    public int delete(final String path) throws IOException {
        final DeleteMethod method = new DeleteMethod(url(path));
        try {
            return httpClient.executeMethod(method);
        } catch (SocketException e) {
            throw new IOException("Failed to connet to " + url(path), e);
        } finally {
            method.releaseConnection();
        }
    }

    private String url(final String path) {
        return String.format("%s/%s", url, path);
    }

    public Tuple post(final String path, final String body) throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Posting to " + path + ": " + body);
        }
        final PostMethod post = new PostMethod(url(path));
        post.setRequestEntity(new StringRequestEntity(body, "application/json", "UTF-8"));
        return execute(post);
    }

    private Tuple execute(final HttpMethodBase method) throws IOException {
        return execute(method, 0);
    }

    private Tuple execute(final HttpMethodBase method, int numTries) throws IOException {
        final Timer timer = Metric.newTimer("RestClientImpl.execute");
        try {
            final int sc = httpClient.executeMethod(method);
            if (sc < OK_MIN || sc > OK_MAX) {
                throw new RestException("Unexpected status code: " + sc + ": " + method.getStatusText() + " -- " + method, sc);
            }
            final InputStream in = method.getResponseBodyAsStream();
            try {
                final StringWriter writer = new StringWriter(2048);
                IOUtils.copy(in, writer, method.getResponseCharSet());
                return new Tuple(sc, writer.toString());
            } finally {
                in.close();
            }
        } catch (NullPointerException e) {
            if (numTries < 3) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.interrupted();
                }
                return execute(method, numTries + 1);
            }
            throw new IOException("Failed to connet to " + url + " [" + method + "]", e);
        } catch (SocketException e) {
            if (numTries < 3) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.interrupted();
                }
                return execute(method, numTries + 1);
            }
            throw new IOException("Failed to connet to " + url + " [" + method + "]", e);
        } catch (IOException e) {
            if (numTries < 3) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.interrupted();
                }
                return execute(method, numTries + 1);
            }
            throw e;
        } finally {
            method.releaseConnection();
            timer.stop();
        }
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof RestClientImpl)) {
            return false;
        }
        RestClientImpl rhs = (RestClientImpl) object;
        return new EqualsBuilder().append(this.url, rhs.url).isEquals();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(786529047, 1924536713).append(this.url).toHashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).append("url", this.url).toString();
    }
}
