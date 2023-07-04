package net.sf.xdc.processing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.ResourceBundle;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import javax.xml.transform.TransformerException;
import net.sf.xdc.util.Logging;
import net.sf.xdc.util.XmlException;
import net.sf.xdc.util.XmlUtils;
import net.sf.xdc.util.XslUtils;
import net.sf.xdc.util.IOUtils;
import net.sf.xdc.util.XPathUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

/**
 * This class is responsible for processing all source files by applying the
 * appropriate stylesheets to the sources specified during the invocation of
 * the XDC tool.
 *
 * @author Jens Vo�
 * @since 0.5
 * @version 0.5
 */
public class XdcProcessor {

    private static final Logger LOG = Logging.getLogger();

    private static final String XSL_PKG = "net/sf/xdc/xsl";

    private static final String RESOURCE_PKG = "net/sf/xdc/resources";

    private CommandLine line;

    private XdcSourceCollector sourceCollector;

    private File outputDir;

    private Properties baseProperties;

    private Map tables;

    private List patterns;

    private SourceProcessor sourceProcessor;

    private Charset encoding;

    private Charset docencoding;

    private Locale locale = Locale.US;

    /**
   * This public constructor uses the command line to collect all necessary
   * sources for processing.
   *
   * @param line The command line which specifies which sources are to be
   *         processed and details on how they should be processed
   */
    public XdcProcessor(CommandLine line) {
        this.line = line;
        sourceCollector = new XdcSourceCollector(line);
        if (line.hasOption("d")) {
            outputDir = new File(line.getOptionValue("d"));
        } else {
            outputDir = sourceCollector.getLeadingSourcePath();
        }
        if (!IOUtils.makeEmpty(outputDir)) {
            LOG.warn("Output directory " + outputDir.getPath() + " could not be emptied.");
        }
        baseProperties = XdcOptions.getXdcOptions().getOptionProperties(line);
        baseProperties.setProperty("framesetsize", String.valueOf(sourceCollector.getFramesetSize()));
        tables = new HashMap(3);
        tables.put("config", baseProperties);
        if (line.hasOption("linksource")) {
            sourceProcessor = new SourceProcessor(outputDir);
        }
        if (line.hasOption("encoding")) {
            String value = line.getOptionValue("encoding");
            try {
                encoding = Charset.forName(value);
                LOG.info("Using '" + value + "' for source file encoding.");
            } catch (UnsupportedCharsetException e) {
                LOG.warn("Source file encoding '" + value + "' not recognized - using default.");
            }
        } else {
            LOG.info("No source file encoding specified - using default.");
        }
        if (line.hasOption("docencoding")) {
            String value = line.getOptionValue("docencoding");
            try {
                docencoding = Charset.forName(value);
                LOG.info("Using '" + value + "' for target file encoding.");
            } catch (UnsupportedCharsetException e) {
                LOG.warn("Target file encoding '" + value + "' not recognized - using default.");
            }
        } else {
            if (encoding == null) {
                LOG.info("No target file encoding specified - using default.");
            } else {
                docencoding = encoding;
                LOG.info("No target file encoding specified - using source file encoding (" + docencoding.name() + ").");
            }
        }
        if (line.hasOption("locale")) {
            StringTokenizer tok = new StringTokenizer(line.getOptionValue("locale"), "_", false);
            switch(tok.countTokens()) {
                case 1:
                    locale = new Locale(tok.nextToken());
                    break;
                case 2:
                    locale = new Locale(tok.nextToken(), tok.nextToken());
                    break;
                case 3:
                    locale = new Locale(tok.nextToken(), tok.nextToken(), tok.nextToken());
                    break;
            }
        }
        LOG.info("Using locale " + locale.toString());
        Locale.setDefault(locale);
        ResourceBundle bundle = ResourceBundle.getBundle("net.sf.xdc.resources.xdc", locale);
        Properties dictionary = new Properties();
        for (Enumeration keys = bundle.getKeys(); keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            dictionary.setProperty(key, bundle.getString(key));
        }
        tables.put("dictionary", dictionary);
        patterns = new Vector();
    }

    /**
   * This method generates all files for the XDC documentation pages.
   */
    public void process() {
        processStylesheetFile();
        processHelpFile();
        preprocessBasicContent();
        processBasicContent();
        processSupportFiles();
    }

    private void preprocessBasicContent() {
        XdcSource[] sources = sourceCollector.getXdcSources();
        Set sourceFiles = new HashSet();
        for (int i = 0; i < sources.length; i++) {
            XdcSource source = sources[i];
            try {
                Document doc = XslUtils.transform(source.getFile().getAbsolutePath(), encoding, XSL_PKG + "/patterns.xsl", tables);
                NodeIterator iter = XPathUtils.selectNodes(doc, "//pattern");
                Node patternNode;
                while ((patternNode = iter.nextNode()) != null) {
                    String file = XPathUtils.selectNode(patternNode, "file/text()").getNodeValue();
                    String xpath = XmlUtils.getTextValue(XPathUtils.selectNode(patternNode, "xpath"));
                    XPathPattern pattern = new XPathPattern(file, xpath);
                    String sourceFile;
                    if (file == null || file.length() == 0) {
                        sourceFile = source.getSourceFileName();
                    } else if (file.indexOf('/') < 0) {
                        sourceFile = source.getPackageName() + '/' + file;
                    } else {
                        sourceFile = file;
                    }
                    pattern.setTargetFile(sourceFile);
                    sourceFiles.add(sourceFile);
                    patterns.add(pattern);
                    source.addPattern(pattern);
                }
            } catch (XmlException e) {
                LOG.error(e.getMessage(), e);
            } catch (TransformerException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        for (Iterator iter = sourceFiles.iterator(); iter.hasNext(); ) {
            String sourceFile = (String) iter.next();
            for (Iterator pIter = patterns.iterator(); pIter.hasNext(); ) {
                XPathPattern pattern = (XPathPattern) pIter.next();
                if (pattern.getTargetFile().equals(sourceFile)) {
                    XdcSource source = sourceCollector.getXdcSource(sourceFile);
                    if (source == null) {
                        continue;
                    }
                    FileInputStream in = null;
                    try {
                        in = new FileInputStream(source.getFile());
                        Document doc = XmlUtils.parse(in);
                        Node node = XPathUtils.selectNode(doc, pattern.getPattern());
                        if (node != null) {
                            String link = XmlUtils.getLink((Element) node);
                            pattern.setValue(link);
                        }
                    } catch (FileNotFoundException e) {
                        LOG.error(e.getMessage(), e);
                    } catch (XmlException e) {
                        LOG.error(e.getMessage(), e);
                    } catch (TransformerException e) {
                        LOG.error(e.getMessage(), e);
                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    private void processBasicContent() {
        String[] packageNames = sourceCollector.getPackageNames();
        for (int i = 0; i < packageNames.length; i++) {
            XdcSource[] sources = sourceCollector.getXdcSources(packageNames[i]);
            File dir = new File(outputDir, packageNames[i]);
            dir.mkdirs();
            Set pkgDirs = new HashSet();
            for (int j = 0; j < sources.length; j++) {
                XdcSource source = sources[j];
                Properties patterns = source.getPatterns();
                if (patterns != null) {
                    tables.put("patterns", patterns);
                }
                pkgDirs.add(source.getFile().getParentFile());
                DialectHandler dialectHandler = source.getDialectHandler();
                Writer out = null;
                try {
                    String sourceFilePath = source.getFile().getAbsolutePath();
                    source.setProcessingProperties(baseProperties, j > 0 ? sources[j - 1].getFileName() : null, j < sources.length - 1 ? sources[j + 1].getFileName() : null);
                    String rootComment = XslUtils.transformToString(sourceFilePath, XSL_PKG + "/source-header.xsl", tables);
                    source.setRootComment(rootComment);
                    Document htmlDoc = XslUtils.transform(sourceFilePath, encoding, dialectHandler.getXslResourcePath(), tables);
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Processing source file " + sourceFilePath);
                    }
                    out = IOUtils.getWriter(new File(dir, source.getFile().getName() + ".html"), docencoding);
                    XmlUtils.printHtml(out, htmlDoc);
                    if (sourceProcessor != null) {
                        sourceProcessor.processSource(source, encoding, docencoding);
                    }
                    XdcSource.clearProcessingProperties(baseProperties);
                } catch (XmlException e) {
                    LOG.error(e.getMessage(), e);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
            }
            for (Iterator iter = pkgDirs.iterator(); iter.hasNext(); ) {
                File docFilesDir = new File((File) iter.next(), "xdc-doc-files");
                if (docFilesDir.exists() && docFilesDir.isDirectory()) {
                    File targetDir = new File(dir, "xdc-doc-files");
                    targetDir.mkdirs();
                    try {
                        IOUtils.copyTree(docFilesDir, targetDir);
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void processSupportFiles() {
        processAllclassesFrame();
        processFrameset();
        if (sourceCollector.getFramesetSize() == 3) {
            processOverviewFiles();
        }
        String[] packageNames = this.sourceCollector.getPackageNames();
        for (int i = 0; i < packageNames.length; i++) {
            processPackageFiles(packageNames, i);
        }
    }

    private void processStylesheetFile() {
        InputStream in = null;
        OutputStream out = null;
        try {
            String filename;
            if (line.hasOption("stylesheetfile")) {
                filename = line.getOptionValue("stylesheetfile");
                in = new FileInputStream(filename);
                filename = filename.replace('\\', '/');
                filename = filename.substring(filename.lastIndexOf('/') + 1);
            } else {
                ClassLoader cl = this.getClass().getClassLoader();
                filename = "stylesheet.css";
                in = cl.getResourceAsStream(RESOURCE_PKG + "/stylesheet.css");
            }
            baseProperties.setProperty("stylesheetfilename", filename);
            File outFile = new File(outputDir, filename);
            if (LOG.isInfoEnabled()) {
                LOG.info("Processing generated file " + outFile.getAbsolutePath());
            }
            out = new FileOutputStream(outFile);
            IOUtils.copy(in, out);
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    private void processHelpFile() {
        InputStream in = null;
        if (line.hasOption("helpfile")) {
            OutputStream out = null;
            try {
                String filename = line.getOptionValue("helpfile");
                in = new FileInputStream(filename);
                filename = filename.replace('\\', '/');
                filename = filename.substring(filename.lastIndexOf('/') + 1);
                File outFile = new File(outputDir, filename);
                if (LOG.isInfoEnabled()) {
                    LOG.info("Processing generated file " + outFile.getAbsolutePath());
                }
                out = new FileOutputStream(outFile);
                baseProperties.setProperty("helpfile", filename);
                IOUtils.copy(in, out);
            } catch (FileNotFoundException e) {
                LOG.error(e.getMessage(), e);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
            return;
        }
        Properties props = new Properties(baseProperties);
        ClassLoader cl = this.getClass().getClassLoader();
        Document doc = null;
        try {
            in = cl.getResourceAsStream(RESOURCE_PKG + "/help-doc.xml");
            doc = XmlUtils.parse(in);
        } catch (XmlException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        transformResource(doc, "help-doc.xsl", props, "help-doc.html");
        baseProperties.setProperty("helpfile", "help-doc.html");
    }

    private void processAllclassesFrame() {
        Properties props = new Properties(baseProperties);
        try {
            XdcSource[] sources = this.sourceCollector.getXdcSources();
            Document xml = XmlUtils.createDocument();
            Element sourcesNode = xml.createElement("sources");
            xml.appendChild(sourcesNode);
            for (int i = 0; i < sources.length; i++) {
                Element sourceNode = xml.createElement("source");
                sourceNode.setAttribute("name", sources[i].getFile().getName());
                sourceNode.setAttribute("package", sources[i].getPackageName());
                sourcesNode.appendChild(sourceNode);
            }
            transformResource(xml, "allclasses-frame.xsl", props, "allclasses-noframe.html");
            props.setProperty("targetFrame", "classFrame");
            transformResource(xml, "allclasses-frame.xsl", props, "allclasses-frame.html");
        } catch (XmlException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void processFrameset() {
        Properties props = new Properties(baseProperties);
        String indexFileName = "index" + sourceCollector.getFramesetSize() + ".xsl";
        if (sourceCollector.getFramesetSize() == 2) {
            props.setProperty("package", sourceCollector.getPackageNames()[0]);
        }
        transformResource(indexFileName, props, "index.html");
    }

    private void processOverviewFiles() {
        Properties props = new Properties(baseProperties);
        props.setProperty("displayLevel", "overview");
        props.setProperty("summaryText", sourceCollector.getSummaryText());
        String[] packageNames = sourceCollector.getPackageNames();
        try {
            Document xml = XmlUtils.createDocument();
            Element packagesNode = xml.createElement("packages");
            xml.appendChild(packagesNode);
            for (int i = 0; i < packageNames.length; i++) {
                XdcPackage xdcPackage = sourceCollector.getXdcPackage(packageNames[i]);
                Element packageNode = xml.createElement("package");
                packageNode.setAttribute("name", packageNames[i]);
                packagesNode.appendChild(packageNode);
                packageNode.appendChild(xml.createTextNode(xdcPackage.getSummaryText()));
            }
            transformResource(xml, "overview-frame.xsl", props, "overview-frame.html");
            transformResource(xml, "overview-summary.xsl", props, "overview-summary.html");
        } catch (XmlException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void processPackageFiles(String[] packageNames, int i) {
        XdcPackage xdcPackage = sourceCollector.getXdcPackage(packageNames[i]);
        Properties props = new Properties(baseProperties);
        props.setProperty("package", packageNames[i]);
        props.setProperty("displayLevel", "package");
        props.setProperty("summaryText", xdcPackage.getSummaryText());
        if (i == 0) {
            props.remove("prevFileName");
        } else {
            props.setProperty("prevFileName", packageNames[i - 1] + "/package-summary.html");
        }
        if (i == packageNames.length - 1) {
            props.remove("nextFileName");
        } else {
            props.setProperty("nextFileName", packageNames[i + 1] + "/package-summary.html");
        }
        XdcSource[] sources = xdcPackage.getXdcSources();
        try {
            Document xml = XmlUtils.createDocument();
            Element sourcesNode = xml.createElement("sources");
            xml.appendChild(sourcesNode);
            for (int j = 0; j < sources.length; j++) {
                Element sourceNode = xml.createElement("source");
                sourceNode.setAttribute("name", sources[j].getFile().getName());
                sourcesNode.appendChild(sourceNode);
                sourceNode.appendChild(xml.createTextNode(sources[j].getRootComment()));
            }
            transformResource(xml, "package-frame.xsl", props, packageNames[i] + "/package-frame.html");
            transformResource(xml, "package-summary.xsl", props, packageNames[i] + "/package-summary.html");
        } catch (XmlException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void transformResource(String resourceFileName, Properties props, String outFileName) {
        try {
            transformResource(XmlUtils.createDocument(), resourceFileName, props, outFileName);
        } catch (XmlException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void transformResource(Document xml, String resourceFileName, Properties props, String outFileName) {
        props.setProperty("outFileName", outFileName);
        Writer out = null;
        Properties oldConfig = (Properties) tables.put("config", props);
        try {
            Document html = XslUtils.transform(xml, XSL_PKG + '/' + resourceFileName, tables);
            File outFile = new File(outputDir, outFileName);
            outFile.getParentFile().mkdirs();
            out = IOUtils.getWriter(outFile, docencoding);
            if (LOG.isInfoEnabled()) {
                LOG.info("Processing generated file " + outFile.getAbsolutePath());
            }
            XmlUtils.printHtml(out, html);
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } catch (XmlException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            if (oldConfig != null) {
                tables.put("config", oldConfig);
            } else {
                tables.remove("config");
            }
        }
    }
}
