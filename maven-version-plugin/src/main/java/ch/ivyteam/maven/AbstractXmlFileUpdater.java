package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AbstractXmlFileUpdater {

  protected File xmlFile;
  private Document doc;
  private String xmlContent;

  public AbstractXmlFileUpdater(File xmlFile) {
    this.xmlFile = xmlFile;
  }

  protected Node findNode(String xPathStr) throws XPathExpressionException {
    XPath xpath = XPathFactory.newInstance().newXPath();
    return ((NodeList) xpath.compile(xPathStr).evaluate(doc, XPathConstants.NODESET)).item(0);
  }

  protected List<Node> findNodes(String xPathStr) throws XPathExpressionException {
    XPath xpath = XPathFactory.newInstance().newXPath();
    ArrayList<Node> nodes = new ArrayList<>();
    NodeList nodelist = (NodeList) xpath.compile(xPathStr).evaluate(doc, XPathConstants.NODESET);
    for (int pos = 0; pos < nodelist.getLength(); pos++) {
      nodes.add(nodelist.item(pos));
    }
    return nodes;
  }

  protected List<Node> getChildNodes(Node parentNode, String nodeName) {
    if (parentNode == null) {
      return Collections.emptyList();
    }
    NodeList children = parentNode.getChildNodes();
    List<Node> childNodes = new ArrayList<>();
    for (int pos = 0; pos < children.getLength(); pos++) {
      Node child = children.item(pos);
      if (child.getNodeName().equals(nodeName)) {
        childNodes.add(child);
      }
    }
    return childNodes;
  }

  protected Node getChildNode(Node parentNode, String nodeName) {
    NodeList children = parentNode.getChildNodes();
    for (int pos = 0; pos < children.getLength(); pos++) {
      Node child = children.item(pos);
      if (child.getNodeName().equals(nodeName)) {
        return child;
      }
    }
    return null;
  }

  protected String getChildNodeText(Node parentNode, String nodeName) {
    Node child = getChildNode(parentNode, nodeName);
    if (child != null) {
      return child.getTextContent();
    }
    return null;
  }

  protected void readXml() throws SAXException, IOException {
    doc = LineNumberXmlReader.readXml(xmlFile);
    xmlContent = Files.readString(xmlFile.toPath());
  }

  protected void saveXml() throws IOException {
    Files.writeString(xmlFile.toPath(), xmlContent);
  }

  protected boolean versionNeedsUpdate(Node node, String referenceVersion) {
    return node != null && !node.getTextContent().trim().equals(referenceVersion.trim());
  }

  protected void replaceElementText(Node elementNode, String newNodeText) {
    StringBuilder builder = new StringBuilder(xmlContent.length() + 100);
    builder.append(getXmlContentBeforeElement(elementNode));
    String oldContent = getXmlContentStartingWithElement(elementNode);
    String newContent = oldContent.replaceFirst(Pattern.quote(elementNode.getTextContent()), newNodeText);
    builder.append(newContent);
    xmlContent = builder.toString();
  }

  protected void replaceAttributeText(Node elementNode, Node attributeNode, String newAttributeText) {
    StringBuilder builder = new StringBuilder(xmlContent.length() + 100);
    builder.append(getXmlContentBeforeElement(elementNode));
    String oldContent = getXmlContentStartingWithElement(elementNode);
    String newContent = oldContent.replaceFirst(getAttributePattern(attributeNode),
            "$1" + newAttributeText + "$2");
    builder.append(newContent);
    xmlContent = builder.toString();
  }

  private String getAttributePattern(Node attributeNode) {
    return "(" + Pattern.quote(attributeNode.getNodeName()) + "\\s*=\\s*\")"
            + Pattern.quote(attributeNode.getTextContent()) + "(\")";
  }

  private String getXmlContentBeforeElement(Node elementNode) {
    return xmlContent.substring(0, getElementStartPosition(elementNode));
  }

  private String getXmlContentStartingWithElement(Node elementNode) {
    return xmlContent.substring(getElementStartPosition(elementNode), xmlContent.length());
  }

  private int getElementStartPosition(Node elementNode) {
    int lineNumber = LineNumberXmlReader.getLinePosition(elementNode);
    int pos = 0;
    for (int currentLine = 0; currentLine < lineNumber; currentLine++) {
      pos = xmlContent.indexOf('\n', pos) + 1;
    }
    String contentAfterElement = xmlContent.substring(0, pos);
    return contentAfterElement.lastIndexOf("<" + elementNode.getNodeName());
  }
}
