package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Node;

abstract class AbstractProjectAwareXmlFileUpdater extends AbstractXmlFileUpdater
{
  protected File projectDirectory;
  protected String featureVersion;
  protected String requiredVersion;
  protected Log log;
  private String xmlFileName;

  AbstractProjectAwareXmlFileUpdater(File projectDirectory, String xmlFileName, String newVersion, Log log)
  {
    super(new File(projectDirectory, xmlFileName));
    this.projectDirectory = projectDirectory;
    this.xmlFileName = xmlFileName;
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

  void update() throws IOException, Exception
  {
    if (xmlFile.exists())
    {
      readXml();
      boolean changed = updateContent();
      if (changed)
      {
        saveXml();
      }
      else
      {
        log.info("File "+xmlFile.getAbsolutePath()+" is up to date. Nothing to do.");
      }
    }
    else
    {
      log.info("No "+xmlFileName+" found in project "+projectDirectory+". Nothing to do");
    }
  }

  /**
   * @return true if any changes have been made on the xmlContent
   * @throws Exception
   */
  protected abstract boolean updateContent() throws Exception;
  
  protected boolean versionNeedsUpdate(Node parentNode, Node versionNode, String version)
  {
    String id = getAttributeText(parentNode, "id");
    return IvyArtifactDetector.isLocallyBuildIvyArtifact(id, version) &&
           versionNeedsUpdate(versionNode, version);
  }
  
  protected String getAttributeText(Node elementNode, String attribute)
  {
    return elementNode.getAttributes().getNamedItem(attribute).getTextContent();
  }

  protected Node getVersionAttributeNode(Node node)
  {
    return node.getAttributes().getNamedItem("version");
  }
}
