package ch.ivyteam.maven;

import java.util.Arrays;
import java.util.List;

/**
 * Detects ivy artifacts
 * @author rwei
 * @since 23.10.2013
 */
public class IvyArtifactDetector
{
  private static final List<String> TRUNK_EXTERNAL_BUILD_ARTIFACTS = Arrays.asList("ch.ivyteam.ulc.base", "ch.ivyteam.ulc.extension", "ch.ivyteam.ivy.designer.cm.ui", "ch.ivyteam.vn.feature");
  private static final List<String> BRANCH_5_1_EXTERNAL_BUILD_ARTIFACTS = Arrays.asList("ch.ivyteam.ulc.base", "ch.ivyteam.ulc.extension", "ch.ivyteam.ivy.designer.cm.ui", "ch.ivyteam.vn.feature");
  private static final List<String> BRANCH_5_0_EXTERNAL_BUILD_ARTIFACTS = Arrays.asList("ch.ivyteam.ulc.base", "ch.ivyteam.ulc.extension", "ch.ivyteam.ulc.feature");

  public static boolean isLocallyBuildIvyArtifact(String artifactId, String version)
  {
    return artifactId.startsWith("ch.ivyteam.") && isNotBuildExternal(artifactId, version);
  }

  private static boolean isNotBuildExternal(String artifactId, String version)
  {
    if (version.startsWith("5.0"))
    {
      return !BRANCH_5_0_EXTERNAL_BUILD_ARTIFACTS.contains(artifactId);
    }
    else if (version.startsWith("5.1"))
    {
      return !BRANCH_5_1_EXTERNAL_BUILD_ARTIFACTS.contains(artifactId);
    }
    else
    {
      return !TRUNK_EXTERNAL_BUILD_ARTIFACTS.contains(artifactId);
    }
  }

}
