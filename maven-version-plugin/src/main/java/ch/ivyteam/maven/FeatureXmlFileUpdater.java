package ch.ivyteam.maven;

import java.io.File;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Node;

/**
 * Updates the version of the feature and the included features and plugins
 */
class FeatureXmlFileUpdater extends AbstractProjectAwareXmlFileUpdater {

  FeatureXmlFileUpdater(File projectDirectory, String newVersion, Log log,
          List<String> externalBuiltArtifacts) {
    super(projectDirectory, new UpdateRun("feature.xml", newVersion, log, externalBuiltArtifacts));
  }

  @Override
  protected boolean updateContent() throws Exception {
    boolean changed = false;
    changed = updateVersion(changed);
    changed = updateIncludesVersion(changed);
    changed = updatePluginsVersion(changed);
    return changed;
  }

  private boolean updatePluginsVersion(boolean changed) throws XPathExpressionException {
    String xPath = "/feature/plugin";
    List<Node> pluginNodes = findNodes(xPath);
    for (Node pluginNode : pluginNodes) {
      Node versionNode = getVersionAttributeNode(pluginNode);
      if (versionNeedsUpdate(pluginNode, versionNode, featureVersion)) {
        replaceAttributeText(pluginNode, versionNode, featureVersion);
        update.log.info("Replace version " + versionNode.getTextContent() + " with version " + featureVersion
                + " in plugin node " + getAttributeText(pluginNode, "id") + " of feature file "
                + xmlFile.getAbsolutePath());
        changed = true;
      }
    }
    return changed;
  }

  private boolean updateIncludesVersion(boolean changed) throws XPathExpressionException {
    String xPath = "/feature/includes";
    List<Node> includesNodes = findNodes(xPath);
    for (Node includesNode : includesNodes) {
      Node versionNode = getVersionAttributeNode(includesNode);
      if (versionNeedsUpdate(includesNode, versionNode, featureVersion)) {
        replaceAttributeText(includesNode, versionNode, featureVersion);
        update.log.info("Replace version " + versionNode.getTextContent() + " with version " + featureVersion
                + " in includes node " + getAttributeText(includesNode, "id") + " of file feature "
                + xmlFile.getAbsolutePath());
        changed = true;
      }
    }
    return changed;
  }

  private boolean updateVersion(boolean changed) throws Exception {
    String xPath = "/feature";
    Node featureNode = findNode(xPath);
    Node versionNode = getVersionAttributeNode(featureNode);
    if (versionNeedsUpdate(versionNode, featureVersion)) {
      replaceAttributeText(featureNode, versionNode, featureVersion);
      update.log.info("Replace feature version " + versionNode.getTextContent() + " with version "
              + featureVersion + " in feature file " + xmlFile.getAbsolutePath());
      return true;
    }
    return changed;
  }
}
