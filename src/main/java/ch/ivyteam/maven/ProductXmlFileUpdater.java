package ch.ivyteam.maven;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

class ProductXmlFileUpdater extends AbstractXmlFileUpdater
{
  private File projectDirectory;
  private String productVersion;
  private Log log;

  ProductXmlFileUpdater(File projectDirectory, String newVersion, Log log)
  {
    super(getProductFile(projectDirectory));
    this.projectDirectory = projectDirectory;
    if (newVersion.indexOf('-') >= 0)
    {
      productVersion = StringUtils.substringBefore(newVersion, "-");
    }
    else
    {
      productVersion = newVersion;
    }
    this.log = log;
  }

  private static File getProductFile(File projectDirectory)
  {
    File[] productFiles = projectDirectory.listFiles((FileFilter)new SuffixFileFilter(".product"));
    if (productFiles == null || productFiles.length == 0)
    {
      return new File ("NoProductFileExists.product");
    }
    return productFiles[0];
  }

  public void update() throws SAXException, IOException, XPathExpressionException
  {
    boolean changed = false;
    if (xmlFile.exists())
    {
      readXml();
      changed = updateVersion(changed);
      if (changed)
      {
        saveXml();
      }
      else
      {
        log.info("Product file "+xmlFile.getAbsolutePath()+" is up to date. Nothing to do.");
      }
    }
    else
    {
      log.info("No *.product file found in project "+projectDirectory+". Nothing to do");
    }
    
  }

  private boolean updateVersion(boolean changed) throws XPathExpressionException
  {
    String xPath = "/product";
    Node productNode = findNode(xPath);
    Node versionNode = getVersionAttributeNode(productNode);
    if (versionNeedsUpdate(versionNode, productVersion))
    {
      replaceAttributeText(productNode, versionNode, productVersion);
      log.info("Replace product version "+versionNode.getTextContent()+" with version "+productVersion+" in product file "+xmlFile.getAbsolutePath());
      return true;      
    }
    return changed;
  }
  
  private Node getVersionAttributeNode(Node node)
  {
    return node.getAttributes().getNamedItem("version");
  }

}
