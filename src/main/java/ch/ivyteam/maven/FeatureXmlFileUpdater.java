package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Node;

/**
 * Updates the version of the feature and the included features and plugins 
 */
class FeatureXmlFileUpdater extends AbstractXmlFileUpdater
{
  private File projectDirectory;
  private String featureVersion;
  private String requiredVersion;
  private Log log;

  FeatureXmlFileUpdater(File projectDirectory, String newVersion, Log log)
  {
    super(new File(projectDirectory, "feature.xml"));
    this.projectDirectory = projectDirectory;
    if (newVersion.indexOf('-') >= 0)
    {
      featureVersion = StringUtils.substringBefore(newVersion, "-");
    }
    else
    {
      featureVersion = newVersion;
    }
    requiredVersion = featureVersion;
    featureVersion += ".qualifier";    
    this.log = log;
  }

  void update() throws IOException, ParserConfigurationException, Exception
  {
    boolean changed = false;
    if (xmlFile.exists())
    {
      readXml();
      changed = updateVersion(changed);
      changed = updateIncludesVersion(changed);
      changed = updatePluginsVersion(changed);
      if (changed)
      {
        saveXml();
      }
      else
      {
        log.info("Feature file "+xmlFile.getAbsolutePath()+" is up to date. Nothing to do.");
      }
    }
    else
    {
      log.info("No feature.xml found in project "+projectDirectory+". Nothing to do");
    }
  }

  private boolean updatePluginsVersion(boolean changed) throws XPathExpressionException
  {
    String xPath = "/feature/plugin";
    List<Node> pluginNodes = findNodes(xPath);
    for (Node pluginNode : pluginNodes)
    {
      Node versionNode = getVersionAttributeNode(pluginNode);
      if (versionNeedsUpdate(pluginNode, versionNode, requiredVersion))
      {
        replaceAttributeText(pluginNode, versionNode, requiredVersion);
        log.info("Replace version "+versionNode.getTextContent()+" with version "+requiredVersion+" in plugin node "+getAttributeText(pluginNode, "id")+" of feature file "+xmlFile.getAbsolutePath());
        changed = true;       
      }
    }
    return changed;
  }

  private boolean updateIncludesVersion(boolean changed) throws XPathExpressionException
  {
    String xPath = "/feature/includes";
    List<Node> includesNodes = findNodes(xPath);
    for (Node includesNode : includesNodes)
    {
      Node versionNode = getVersionAttributeNode(includesNode);
      if (versionNeedsUpdate(includesNode, versionNode, requiredVersion))
      {
        replaceAttributeText(includesNode, versionNode, requiredVersion);
        log.info("Replace version "+versionNode.getTextContent()+" with version "+requiredVersion+" in includes node "+getAttributeText(includesNode, "id")+" of file feature "+xmlFile.getAbsolutePath());
        changed = true;       
      }
    }
    return changed;
  }

  private boolean updateVersion(boolean changed) throws Exception
  {
    String xPath = "/feature";
    Node featureNode = findNode(xPath);
    Node versionNode = getVersionAttributeNode(featureNode);
    if (versionNeedsUpdate(versionNode, featureVersion))
    {
      replaceAttributeText(featureNode, versionNode, featureVersion);
      log.info("Replace feature version "+versionNode.getTextContent()+" with version "+featureVersion+" in feature file "+xmlFile.getAbsolutePath());
      return true;      
    }
    return changed;
  }

  private boolean versionNeedsUpdate(Node parentNode, Node versionNode, String version)
  {
    String id = getAttributeText(parentNode, "id");
    return id.startsWith("ch.ivyteam.") && 
           !id.equals("ch.ivyteam.ulc.feature") &&
           !id.equals("ch.ivyteam.vn.feature") &&
           versionNeedsUpdate(versionNode, version);
  }
  
  private String getAttributeText(Node elementNode, String attribute)
  {
    return elementNode.getAttributes().getNamedItem(attribute).getTextContent();
  }

  private Node getVersionAttributeNode(Node node)
  {
    return node.getAttributes().getNamedItem("version");
  }
}
