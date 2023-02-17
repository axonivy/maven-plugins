package ch.ivyteam.maven;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Node;

abstract class AbstractProjectAwareXmlFileUpdater extends AbstractXmlFileUpdater {

  protected File projectDirectory;
  protected final String featureVersion;
  protected final UpdateRun update;

  AbstractProjectAwareXmlFileUpdater(File projectDirectory, UpdateRun update) {
    super(new File(projectDirectory, update.xmlFileName));
    this.update = update;
    this.projectDirectory = projectDirectory;
    this.featureVersion = update.versionEclipseQualified();
  }

  void update() throws IOException, Exception {
    if (xmlFile.exists()) {
      readXml();
      boolean changed = updateContent();
      if (changed) {
        saveXml();
      } else {
        update.log.info("File " + xmlFile.getAbsolutePath() + " is up to date. Nothing to do.");
      }
    } else {
      update.log.debug(
              "No " + update.xmlFileName + " found in project " + projectDirectory + ". Nothing to do");
    }
  }

  /**
   * @return true if any changes have been made on the xmlContent
   * @throws Exception
   */
  protected abstract boolean updateContent() throws Exception;

  protected boolean versionNeedsUpdate(Node parentNode, Node versionNode, String version) {
    String id = getAttributeText(parentNode, "id");
    return update.isLocalBuiltArtifact(id) &&
            versionNeedsUpdate(versionNode, version);
  }

  protected String getAttributeText(Node elementNode, String attribute) {
    return elementNode.getAttributes().getNamedItem(attribute).getTextContent();
  }

  protected Node getVersionAttributeNode(Node node) {
    return node.getAttributes().getNamedItem("version");
  }
}
