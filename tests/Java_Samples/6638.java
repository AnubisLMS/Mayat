package org.granite.messaging.webapp;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.granite.util.JDOMUtil;
import org.granite.util.StreamGobbler;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

public class MXMLCompilerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(MXMLCompilerServlet.class);

    private ServletConfig config = null;

    private File compilerHome = null;

    private Map<String, String> compilerOptions = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        this.config = config;
        String compilerHomePath = config.getInitParameter("compilerHome");
        if (compilerHomePath == null) {
            log.error("Init parameter 'compilerHome' is missing (web.xml)");
            throw new ServletException("Init parameter 'compilerHome' is missing (web.xml)");
        }
        compilerHome = new File(compilerHomePath);
        if (!compilerHome.exists() || !compilerHome.isDirectory()) {
            log.error("Invalid 'compilerHome' parameter in (web.xml): " + compilerHome);
            throw new ServletException("Invalid 'compilerHome' parameter in (web.xml): " + compilerHome);
        }
        compilerOptions = new HashMap<String, String>();
        String compilerOptionsFile = config.getServletContext().getRealPath("/" + config.getInitParameter("compilerOptions"));
        if (compilerOptionsFile == null) log.warn("Init parameter 'compilerClassName' is missing (web.xml). Using default."); else {
            Properties props = new Properties();
            InputStream is = null;
            try {
                is = new FileInputStream(compilerOptionsFile);
                props.load(is);
                for (Map.Entry<Object, Object> entry : props.entrySet()) compilerOptions.put((String) entry.getKey(), (String) entry.getValue());
            } catch (Exception e) {
                throw new ServletException("Could not load compiler options from file: " + compilerOptionsFile, e);
            } finally {
                if (is != null) try {
                    is.close();
                } catch (Exception e) {
                    log.warn("Could not close input stream: " + compilerOptionsFile, e);
                }
            }
            String[] ignoredOptions = new String[] { "-file-specs", "-o", "-output", "-context-root", "-compiler.context-root" };
            for (String ignoredOption : ignoredOptions) {
                if (compilerOptions.containsKey(ignoredOption)) {
                    log.warn("Ignoring " + ignoredOption + " option in: " + compilerOptionsFile);
                    compilerOptions.remove(ignoredOption);
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        File mxml = new File(config.getServletContext().getRealPath(request.getRequestURI().substring(request.getContextPath().length())));
        String mxmlPath = mxml.getCanonicalPath();
        if (!mxml.exists() || !mxml.isFile()) throw new ServletException("Invalid MXML file: " + mxmlPath);
        File swf = new File(mxmlPath.substring(0, (mxmlPath.length() - 4)) + "swf");
        boolean compile = (!swf.exists() || swf.lastModified() < mxml.lastModified());
        if (!compile) {
            for (File dependency : findDependencies(config.getServletContext().getRealPath("/"), mxml)) {
                if (!mxml.equals(dependency) && swf.lastModified() < dependency.lastModified()) {
                    compile = true;
                    break;
                }
            }
        }
        if (compile) {
            String FS = System.getProperty("file.separator");
            List<String> command = new ArrayList<String>();
            command.add("java");
            command.add("-Dapplication.home=" + compilerHome.getAbsolutePath());
            command.add("-Xmx384M");
            command.add("-jar");
            command.add(compilerHome.getAbsolutePath() + FS + "lib" + FS + "mxmlc.jar");
            for (Map.Entry<String, String> entry : compilerOptions.entrySet()) {
                command.add(entry.getKey());
                command.add(entry.getValue());
            }
            command.add("-file-specs");
            command.add(mxmlPath);
            command.add("-context-root");
            command.add(request.getContextPath());
            command.add("-output");
            command.add(swf.getCanonicalPath());
            log.info("Executing: " + command);
            int status = 0;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(new File(config.getServletContext().getRealPath("/")));
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                StreamGobbler gobbler = new StreamGobbler(new BufferedInputStream(process.getInputStream()), baos);
                gobbler.start();
                status = process.waitFor();
            } catch (Exception e) {
                log.error("Could not call flex compiler: " + command, e);
                throw new ServletException("Could not call flex compiler: " + command, e);
            }
            log.info(baos.toString());
            if (status != 0) throw new ServletException("Error while compiling " + mxmlPath + ": " + baos.toString());
            swf = new File(swf.getCanonicalPath());
        }
        response.setContentType("application/x-shockwave-flash");
        response.setContentLength((int) swf.length());
        response.setBufferSize((int) swf.length());
        response.setDateHeader("Expires", 0);
        OutputStream os = null;
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(swf));
            os = response.getOutputStream();
            for (int b = is.read(); b != -1; b = is.read()) os.write(b);
        } finally {
            if (is != null) try {
                is.close();
            } finally {
                if (os != null) os.close();
            }
        }
    }

    private Set<File> findDependencies(String contextPath, File compileUnit) {
        Set<File> dependencies = new HashSet<File>();
        try {
            findDependencies(contextPath, dependencies, compileUnit);
        } catch (Exception e) {
            log.warn("Could not find dependencies for: " + compileUnit, e);
        }
        return dependencies;
    }

    private void findDependencies(String contextPath, Set<File> dependencies, File compileUnit) throws IOException, JDOMException {
        dependencies.add(compileUnit);
        String path = compileUnit.getCanonicalPath();
        if (path.endsWith(".mxml")) {
            Document doc = JDOMUtil.readDocument(path);
            for (Iterator<Element> descendants = JDOMUtil.getDescendantElements(doc.getRootElement()); descendants.hasNext(); ) {
                Element descendant = descendants.next();
                Namespace ns = descendant.getNamespace();
                if ("http://www.adobe.com/2006/mxml".equals(ns.getURI()) && "Script".equals(descendant.getName())) {
                } else if (!ns.getURI().startsWith("http://") && ns.getURI().endsWith("*")) {
                    String dPath = ns.getURI().substring(0, ns.getURI().length() - 1).replace('.', '/') + descendant.getName();
                    File dFile = new File(contextPath + dPath + ".mxml");
                    if (!dFile.exists() || !dFile.isFile()) dFile = new File(contextPath + dPath + ".as");
                    if (dFile.exists() && dFile.isFile() && !dependencies.contains(dFile)) findDependencies(contextPath, dependencies, dFile);
                }
            }
        } else if (path.endsWith(".as")) {
        }
    }

    @Override
    public void destroy() {
        config = null;
        compilerHome = null;
        compilerOptions = null;
    }

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        MXMLCompilerServlet compiler = new MXMLCompilerServlet();
        Set<File> dependencies = compiler.findDependencies("/workspace312/granite/test.war/", new File("C:\\workspace312\\granite\\test.war\\Products.mxml"));
        log.info(dependencies);
    }
}
