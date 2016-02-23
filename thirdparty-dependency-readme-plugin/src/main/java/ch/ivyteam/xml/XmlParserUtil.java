package ch.ivyteam.xml;


import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Some static methods for parsing.
 * Parsers use IvyEntity resolver.
 */
public class XmlParserUtil
{
  /** A factory for validating DOM parsers. */
  private static final DocumentBuilderFactory  VALIDATING_DOC_BUILDER_FACTORY;

  /** A factory for non-validating DOM parsers. */
  private static final DocumentBuilderFactory NONVALIDATING_DOC_BUILDER_FACTORY;
  
  static
  {
    VALIDATING_DOC_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    VALIDATING_DOC_BUILDER_FACTORY.setValidating(true);
    NONVALIDATING_DOC_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    NONVALIDATING_DOC_BUILDER_FACTORY.setValidating(false);
  }
  
  /**
   * Parses a XML document.
   * @param file
   * @param validate
   * @return The parsed document.
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  public static Document parseXmlDocument(File file, boolean validate) 
    throws ParserConfigurationException, SAXException, IOException
  {
    return getDomParser(validate).parse(file);
  }

  /**
   * Returns a new DOM parser.
   * @param validate
   * @return A DOM parser.
   * @throws ParserConfigurationException
   */
  public static DocumentBuilder getDomParser(boolean validate) 
      throws ParserConfigurationException
  {
    DocumentBuilderFactory  docBuilderFactory = 
      validate ? VALIDATING_DOC_BUILDER_FACTORY : NONVALIDATING_DOC_BUILDER_FACTORY;
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    docBuilder.setEntityResolver(new IvyEntities());
    return docBuilder;
  }
}