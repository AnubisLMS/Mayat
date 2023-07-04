package org.myrpg.atlas;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Main class of the MyRPG Atlas server.
 */
public class Atlas extends HttpServlet {

    /**
	 *
	 *
	 */
    public void init(ServletConfig config) {
    }

    /**
     * Process the HTTP GET requests.
     *
     * @req@
     * @res@
     *
     * @throws	IOException
     * @throws	ServletException
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String url;
        ByteArrayOutputStream baos;
        PrintWriter out;
        url = request.getParameter("access");
        ;
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("text/html");
        baos = new ByteArrayOutputStream(8192);
        out = new PrintWriter(baos, true);
        if ("download".equals(url)) generateDownloadPage(request, out); else generateEntrance(request, out);
        out.close();
        response.setContentLength(baos.size());
        baos.writeTo(response.getOutputStream());
    }

    /**
     * Generates the home-page of the atlas web application.
     *
     * @request@
     * @param   output  The output stream.
     *
     * @throws  IOException
     * @throws  ServletException
     */
    private void generateEntrance(HttpServletRequest request, PrintWriter out) throws IOException, ServletException {
        out.println("<html>");
        out.println("<head>");
        out.print("<title>");
        out.print("MyRPG - Atlas server");
        out.println("</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Atlas servlet: OK</h1><br>");
        out.println("<br>request.getPathInfo() = " + request.getPathInfo());
        out.println("<br>request.getPathTranslated() = " + request.getPathTranslated());
        out.println("<br>request.getQueryString() = " + request.getQueryString());
        out.println("<br>request.getRequestURI() = " + request.getRequestURI());
        out.println("<br>request.getServletPath() = " + request.getServletPath());
        out.println("</body>");
        out.print("</html>");
    }

    /**
     * Generates the home-page of the atlas web application.
     *
     * @request@
     * @param   output  The output stream.
     *
     * @throws  IOException
     * @throws  ServletException
     */
    private void generateDownloadPage(HttpServletRequest request, PrintWriter out) throws IOException, ServletException {
        String userId = request.getParameter("userid");
        out.println("<html>");
        out.println("<head>");
        out.print("<title>");
        out.print("MyRPG - Atlas server - Download Page");
        out.println("</title>");
        out.println("</head>");
        out.println("<body onload='directDownloadNow();'>");
        out.println("<table border='0' cellspacing='0' cellpadding='2' width='100%'>");
        out.println("<tr>");
        out.println("<td bgcolor='#525D76'>");
        out.println("<font color='#ffffff' face='arial,helvetica,sanserif'>");
        out.println("<strong>Registration successfull</strong>");
        out.println("</font>");
        out.println("</td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td>");
        out.println("<blockquote>");
        out.println("<p>");
        out.println("<h4>The download is going to start, please wait for a window to appear...</h4><br>");
        out.println("If download fails to begin automatically, click <a href='/myrpg-atlas/download_hicare/hicare.jar'><b>here</b></a> to download HiCare.");
        out.println("</p>");
        out.println("</blockquote>");
        out.println("</p>");
        out.println("</td>");
        out.println("</tr>");
        out.println("</table>");
        out.println("</body>");
        out.println("</html>");
        out.println("<script language='Javascript'>");
        out.println("<!--");
        out.println("function directDownloadNow()");
        out.println("{");
        out.println("window.location = \"/myrpg-atlas/download_hicare/hicare.jar\";");
        out.println("}");
        out.println("-->");
        out.print("</script>");
    }

    /**
	 * Sends the contents of the specified file to the output stream
	 *
	 * @param	filename	The file to send.
	 * @param	out	The output stream to write the file.
	 *
	 * @throws	FileNotFoundException	If the file does not exist.
	 * @throws	IOException	If an I/O error occurs.
	 */
    public static void sendFile(String filename, OutputStream out) throws FileNotFoundException, IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filename);
            byte[] buf = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buf)) != -1) out.write(buf, 0, bytesRead);
        } finally {
            if (fis != null) fis.close();
        }
    }
}
