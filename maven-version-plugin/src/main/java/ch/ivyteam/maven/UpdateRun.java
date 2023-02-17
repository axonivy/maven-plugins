package ch.ivyteam.maven;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.logging.Log;

public class UpdateRun {

  public final String xmlFileName;
  public final String newVersion;
  public final Log log;
  private final List<String> externalBuiltArtifacts;

  public UpdateRun(String xmlFileName, String newVersion, Log log, List<String> externalBuiltArtifacts) {
    this.xmlFileName = xmlFileName;
    this.externalBuiltArtifacts = externalBuiltArtifacts;
    this.newVersion = newVersion;
    this.log = log;
  }

  public String versionNoMavenQualifier() {
    if (StringUtils.contains(newVersion, "-")) {
      return StringUtils.substringBefore(newVersion, "-");
    }
    return newVersion;
  }

  public ArtifactVersion getArtifactVersion() {
    return new DefaultArtifactVersion(newVersion);
  }

  public String versionEclipseQualified() {
    String version = versionNoMavenQualifier();
    boolean hasEclipseQualifier = StringUtils.countMatches(version, ".") == 3;
    if (hasEclipseQualifier) {
      return version;
    }
    return version + ".qualifier";
  }

  public boolean isLocalBuiltArtifact(String artifact) {
    return new IvyArtifactDetector(externalBuiltArtifacts).isLocallyBuildIvyArtifact(artifact);
  }
}
