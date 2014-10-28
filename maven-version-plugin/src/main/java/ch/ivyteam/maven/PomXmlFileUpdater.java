package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** 
 * Updates the version in maven pom files
 */
class PomXmlFileUpdater extends AbstractXmlFileUpdater
{
  private String version;
  private Log log;
  private String bundleVersion;

  public PomXmlFileUpdater(File xmlFile, String version, Log log)
  {
    super(xmlFile);
    this.version = version;
    if (version.indexOf('-') >= 0)
    {
      bundleVersion = StringUtils.substringBefore(version, "-");
    }
    else
    {
      bundleVersion = version;
    }

    this.log = log;
  }

  void update() throws SAXException, IOException, XPathExpressionException
  {
    boolean changed = false;
    readXml();

    changed = updateVersion(changed);
    changed = updateParentVersion(changed);
    changed = updateDependenciesVersion(changed);
    changed = updateDependenciesVersionInTychoSurefirePlugin(changed);
    changed = updateIvyVersionProperty(changed);

    if (changed)
    {
      saveXml();
    }
    else
    {
      log.info("Pom file "+xmlFile.getAbsolutePath()+" is up to date. Nothing to do.");
    }
  }

  private boolean updateVersion(boolean changed) throws XPathExpressionException
  {
    return updateVersion(changed, "/project/version");
  }
  
  private boolean updateParentVersion(boolean changed) throws XPathExpressionException
  {
    return updateVersion(changed, "/project/parent/version");
  }

  private boolean updateDependenciesVersion(boolean changed) throws XPathExpressionException
  {
    for (Node dependency : findNodes("/project/dependencies/dependency"))
    {
      if (dependencyVersionNeedsUpdate(dependency))
      {
        updateDependencyVersion(dependency);
        changed = true;
      }
    }
    return changed;
  }

  private boolean updateDependenciesVersionInTychoSurefirePlugin(boolean changed) throws XPathExpressionException
  {
    for (Node plugin : findNodes("/project/build/plugins/plugin"))
    {
      if (isTychoSurefirePlugin(plugin))
      {
        for (Node dependency : getDependencyNodes(plugin))
        {
          if (pluginDependencyVersionNeedsUpdate(dependency))
          {
            updatePluginDependencyVersion(dependency);
            changed = true;
          }
        }
      }
    }
    return changed;
  }
  
  private boolean updateIvyVersionProperty(boolean changed) throws XPathExpressionException
  {
    return updateVersion(changed, "/project/properties/ivy-version", bundleVersion);
  }
  
  private boolean pluginDependencyVersionNeedsUpdate(Node dependency)
  {
    return isIvyArtifact(dependency) && versionNodeNeedsUpdate(dependency);
  }

  private boolean dependencyVersionNeedsUpdate(Node dependency)
  {
    return isXpertIvyGroupAndIvyArtifact(dependency)&&versionNodeNeedsUpdate(dependency);
  }

  private boolean isXpertIvyGroupAndIvyArtifact(Node dependency)
  {
    return getChildNodeText(dependency, "groupId").equals("Xpert.ivy") && isIvyArtifact(dependency);
  }

  private boolean isIvyArtifact(Node dependency)
  {
    String artifactId = getChildNodeText(dependency, "artifactId");
    return IvyArtifactDetector.isLocallyBuildIvyArtifact(artifactId, bundleVersion);
  }

  private boolean versionNodeNeedsUpdate(Node dependency)
  {
    Node versionNode = getChildNode(dependency, "version");
    return versionNeedsUpdate(versionNode, version);
  }

  private List<Node> getDependencyNodes(Node plugin)
  {
    Node configurationNode = getChildNode(plugin, "configuration");
    Node dependenciesNode = getChildNode(configurationNode, "dependencies");
    return getChildNodes(dependenciesNode, "dependency");
  }

  private boolean isTychoSurefirePlugin(Node plugin)
  {
    return getChildNodeText(plugin, "groupId").equals("org.eclipse.tycho") && 
           getChildNodeText(plugin, "artifactId").equals("tycho-surefire-plugin");
  }

  private void updateDependencyVersion(Node dependency)
  {
    Node versionNode = getChildNode(dependency, "version");
    replaceElementText(versionNode, version);
    log.info("Replace version "+versionNode.getTextContent()+" with version "+version+" in dependency node of artifact "+getChildNodeText(dependency, "artifactId")+" of pom file "+xmlFile.getAbsolutePath());
  }
  
  private void updatePluginDependencyVersion(Node dependency)
  {
    Node versionNode = getChildNode(dependency, "version");
    replaceElementText(versionNode, bundleVersion);
    log.info("Replace version "+versionNode.getTextContent()+" with version "+bundleVersion+" in tycho-surefire-plugin dependency node of artifact "+getChildNodeText(dependency, "artifactId")+" of pom file "+xmlFile.getAbsolutePath());
  }


  private boolean updateVersion(boolean changed, String xPathStr)
          throws XPathExpressionException
  {
    return updateVersion(changed, xPathStr, version);
  }

  private boolean updateVersion(boolean changed, String xPathStr, String newVersion)
          throws XPathExpressionException
  {
    Node versionNode = findNode(xPathStr);
    if (versionNeedsUpdate(versionNode, newVersion))
    {
      replaceElementText(versionNode, newVersion);
      log.info("Replace version "+versionNode.getTextContent()+" with version "+newVersion+" in node "+xPathStr+" of pom file "+xmlFile.getAbsolutePath());
      changed = true;
    }
    return changed;
  }

  
}
