package ch.ivyteam.maven;

import java.io.File;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Node;

/**
 * Updates the version of the included features in a category.xml file
 */
class CategoryXmlFileUpdater extends AbstractProjectAwareXmlFileUpdater {

  CategoryXmlFileUpdater(File projectDirectory, String newVersion, Log log,
          List<String> externalBuiltArtifacts) {
    super(projectDirectory, new UpdateRun("category.xml", newVersion, log, externalBuiltArtifacts));
  }

  @Override
  protected boolean updateContent() throws Exception {
    boolean changed = false;
    changed = updateFeaturesVersion(changed);
    changed = updateBundlesVersion(changed);
    return changed;
  }

  private boolean updateFeaturesVersion(boolean changed) throws XPathExpressionException {
    String xPath = "/site/feature";
    List<Node> featureNodes = findNodes(xPath);
    for (Node featureNode : featureNodes) {
      Node versionNode = getVersionAttributeNode(featureNode);
      if (versionNeedsUpdate(featureNode, versionNode, featureVersion)) {
        String previousVersion = versionNode.getTextContent();
        replaceAttributeText(featureNode, versionNode, featureVersion);
        Node urlNode = getUrlAttributeNode(featureNode);
        String urlText = urlNode.getTextContent();
        urlText = StringUtils.replace(urlText, "_" + previousVersion, "_" + featureVersion);
        replaceAttributeText(featureNode, urlNode, urlText);
        update.log.info("Replace version " + previousVersion + " with version " + featureVersion
                + " in feature node " + getAttributeText(featureNode, "id") + " of category file "
                + xmlFile.getAbsolutePath());
        changed = true;
      }
    }
    return changed;
  }

  private boolean updateBundlesVersion(boolean changed) throws XPathExpressionException {
    String xPath = "/site/bundle";
    List<Node> bundleNodes = findNodes(xPath);
    for (Node bundleNode : bundleNodes) {
      Node versionNode = getVersionAttributeNode(bundleNode);
      if (versionNeedsUpdate(bundleNode, versionNode, featureVersion)) {
        replaceAttributeText(bundleNode, versionNode, featureVersion);
        update.log.info("Replace version " + versionNode.getTextContent() + " with version " + featureVersion
                + " in bundle node " + getAttributeText(bundleNode, "id") + " of category file "
                + xmlFile.getAbsolutePath());
        changed = true;
      }
    }
    return changed;
  }

  private Node getUrlAttributeNode(Node node) {
    return node.getAttributes().getNamedItem("url");
  }
}
