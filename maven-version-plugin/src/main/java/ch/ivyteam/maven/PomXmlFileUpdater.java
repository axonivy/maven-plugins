package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;

import javax.xml.xpath.XPathExpressionException;

import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Updates the version in maven pom files
 */
class PomXmlFileUpdater extends AbstractXmlFileUpdater {

  private final UpdateRun update;

  public PomXmlFileUpdater(File xmlFile, String version, Log log, List<String> externalBuiltArtifacts) {
    super(xmlFile);
    update = new UpdateRun(xmlFile.getName(), version, log, externalBuiltArtifacts);
  }

  void update() throws SAXException, IOException {
    update(this::updateAll);
  }

  private boolean updateAll() {
    try {
      boolean changed = false;
      changed = updateVersion(changed);
      changed = updateParentVersion(changed);
      changed = updateDependenciesVersion(changed);
      changed = updateDependenciesVersionInTychoSurefirePlugin(changed);
      changed = updateIvyMajorVersionProperty(changed);
      changed = updateIvyMinorVersionProperty(changed);
      changed = updateIvyServiceVersionProperty(changed);
      return changed;
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  void updateParentVersoin() throws SAXException, IOException {
    update(this::updateOnlyParentVersoin);
  }

  private boolean updateOnlyParentVersoin() {
    try {
      return updateParentVersion(false);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void update(BooleanSupplier supplier) throws SAXException, IOException {
    if (!xmlFile.exists()) {
      update.log.debug("No pom.xml file found in project " + xmlFile.getAbsolutePath() + ". Nothing to do");
      return;
    }
    readXml();
    boolean changed = supplier.getAsBoolean();
    if (changed) {
      saveXml();
    } else {
      update.log.info("Pom file " + xmlFile.getAbsolutePath() + " is up to date. Nothing to do.");
    }
  }

  private boolean updateVersion(boolean changed) throws XPathExpressionException {
    return updateVersion(changed, "/project/version");
  }

  private boolean updateParentVersion(boolean changed) throws XPathExpressionException {
    return updateVersion(changed, "/project/parent/version");
  }

  private boolean updateDependenciesVersion(boolean changed) throws XPathExpressionException {
    for (Node dependency : findNodes("/project/dependencies/dependency")) {
      if (dependencyVersionNeedsUpdate(dependency)) {
        updateDependencyVersion(dependency);
        changed = true;
      }
    }
    return changed;
  }

  private boolean updateDependenciesVersionInTychoSurefirePlugin(boolean changed)
          throws XPathExpressionException {
    for (Node plugin : findNodes("/project/build/plugins/plugin")) {
      if (isTychoSurefirePlugin(plugin)) {
        for (Node dependency : getDependencyNodes(plugin)) {
          if (pluginDependencyVersionNeedsUpdate(dependency)) {
            updatePluginDependencyVersion(dependency);
            changed = true;
          }
        }
      }
    }
    return changed;
  }

  private boolean updateIvyMajorVersionProperty(boolean changed) throws XPathExpressionException {
    return updateVersion(changed, "/project/properties/ivy-major-version",
            Integer.toString(update.getArtifactVersion().getMajorVersion()));
  }

  private boolean updateIvyMinorVersionProperty(boolean changed) throws XPathExpressionException {
    return updateVersion(changed, "/project/properties/ivy-minor-version",
            Integer.toString(update.getArtifactVersion().getMinorVersion()));
  }

  private boolean updateIvyServiceVersionProperty(boolean changed) throws XPathExpressionException {
    return updateVersion(changed, "/project/properties/ivy-service-version",
            Integer.toString(update.getArtifactVersion().getIncrementalVersion()));
  }

  private boolean pluginDependencyVersionNeedsUpdate(Node dependency) {
    return isIvyArtifact(dependency) && versionNodeNeedsUpdate(dependency);
  }

  private boolean dependencyVersionNeedsUpdate(Node dependency) {
    return isXpertIvyGroupAndIvyArtifact(dependency) && versionNodeNeedsUpdate(dependency);
  }

  private boolean isXpertIvyGroupAndIvyArtifact(Node dependency) {
    return getChildNodeText(dependency, "groupId").equals("Xpert.ivy") && isIvyArtifact(dependency);
  }

  private boolean isIvyArtifact(Node dependency) {
    String artifactId = getChildNodeText(dependency, "artifactId");
    return update.isLocalBuiltArtifact(artifactId);
  }

  private boolean versionNodeNeedsUpdate(Node dependency) {
    Node versionNode = getChildNode(dependency, "version");
    return versionNeedsUpdate(versionNode, update.newVersion);
  }

  private List<Node> getDependencyNodes(Node plugin) {
    Node configurationNode = getChildNode(plugin, "configuration");
    Node dependenciesNode = getChildNode(configurationNode, "dependencies");
    return getChildNodes(dependenciesNode, "dependency");
  }

  private boolean isTychoSurefirePlugin(Node plugin) {
    return "org.eclipse.tycho".equals(getChildNodeText(plugin, "groupId")) &&
            "tycho-surefire-plugin".equals(getChildNodeText(plugin, "artifactId"));
  }

  private void updateDependencyVersion(Node dependency) {
    Node versionNode = getChildNode(dependency, "version");
    replaceElementText(versionNode, update.newVersion);
    update.log.info(
            "Replace version " + versionNode.getTextContent() + " with version " + update.newVersion + " "
                    + "in dependency node of artifact " + getChildNodeText(dependency, "artifactId")
                    + " of pom file " + xmlFile.getAbsolutePath());
  }

  private void updatePluginDependencyVersion(Node dependency) {
    Node versionNode = getChildNode(dependency, "version");
    replaceElementText(versionNode, update.versionNoMavenQualifier());
    update.log.info("Replace version " + versionNode.getTextContent() + " with version "
            + update.versionNoMavenQualifier() + " "
            + "in tycho-surefire-plugin dependency node of artifact "
            + getChildNodeText(dependency, "artifactId") + " of pom file " + xmlFile.getAbsolutePath());
  }

  private boolean updateVersion(boolean changed, String xPathStr)
          throws XPathExpressionException {
    return updateVersion(changed, xPathStr, update.newVersion);
  }

  private boolean updateVersion(boolean changed, String xPathStr, String newVersion)
          throws XPathExpressionException {
    Node versionNode = findNode(xPathStr);
    if (versionNeedsUpdate(versionNode, newVersion)) {
      replaceElementText(versionNode, newVersion);
      update.log.info("Replace version " + versionNode.getTextContent() + " with version " + newVersion
              + " in node " + xPathStr + " of pom file " + xmlFile.getAbsolutePath());
      changed = true;
    }
    return changed;
  }
}
