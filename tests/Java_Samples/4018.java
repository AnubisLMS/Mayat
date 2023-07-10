package com.googlecode.layout4j.parser;

import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.tidy.Tidy;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class HtmlToXmlConverter {

    private static Logger log = LoggerFactory.getLogger(HtmlToXmlConverter.class);

    private static final String XML_FURNITURE_START = "<?xml version='1.0' encoding='UTF-8'?>" + "<!DOCTYPE html>" + "<root xmlns:cms='http://code.google.com/p/layout4j/'>";

    private static final String XML_FURNITURE_END = "</root>";

    private static final String XSLT_FILENAME = "htmlToXml.xsl";

    public static String toXml(String original, boolean addXmlFurniture) throws LayoutException {
        String input = original;
        Reader reader = new StringReader(input);
        return toXml(reader, addXmlFurniture);
    }

    public static String toXml(Reader sourceReader, boolean addXmlFurniture) throws LayoutException {
        Writer tidySourceWriter = new StringWriter();
        HtmlToXmlConverter.tidy(sourceReader, tidySourceWriter);
        if (log.isDebugEnabled()) {
            log.debug("Tidied input: " + tidySourceWriter.toString());
        }
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = tFactory.newTransformer(new StreamSource(HtmlToXmlConverter.class.getResourceAsStream(XSLT_FILENAME)));
        } catch (TransformerConfigurationException e) {
            throw new LayoutException(e);
        }
        StreamSource tidySource = new StreamSource(initReader(tidySourceWriter, true));
        StringWriter resultWriter = new StringWriter();
        try {
            transformer.transform(tidySource, new StreamResult(resultWriter));
        } catch (TransformerException e) {
            throw new LayoutException(e);
        }
        return resultWriter.toString();
    }

    private static Reader initReader(Writer writer, boolean addXmlFurniture) {
        if (addXmlFurniture) {
            return new StringReader(XML_FURNITURE_START + writer.toString() + XML_FURNITURE_END);
        } else {
            return new StringReader(writer.toString());
        }
    }

    private static void tidy(Reader reader, Writer writer) {
        Tidy tidy = new Tidy();
        tidy.setXHTML(true);
        tidy.setPrintBodyOnly(true);
        tidy.setForceOutput(false);
        tidy.parse(reader, writer);
    }
}
