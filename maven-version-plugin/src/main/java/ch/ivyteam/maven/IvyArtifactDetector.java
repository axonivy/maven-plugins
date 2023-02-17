package ch.ivyteam.maven;

import java.util.List;

/**
 * Detects ivy artifacts
 * @author rwei
 * @since 23.10.2013
 */
public class IvyArtifactDetector {

  private List<String> externallyBuiltArtifacts;

  public IvyArtifactDetector(List<String> externallyBuiltArtifacts) {
    this.externallyBuiltArtifacts = externallyBuiltArtifacts;
  }

  public boolean isLocallyBuildIvyArtifact(String artifactId) {
    return artifactId.startsWith("ch.ivyteam.") &&
            (!externallyBuiltArtifacts.contains(artifactId));
  }
}
