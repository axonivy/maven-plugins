package ch.ivyteam.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TestBundleManifestFileUpdater {

  @Test
  void splitIntoBundles() {
    var bundleText = "org.eclipse.ui,or\ng.eclipse.ui.views.properties.tabbed;bundle-version=\"3.5.300\",ch.ivyt\neam.ivy.guiComponents;bundle-version=\"[6.0.0,6.1.0)\"";
    assertThat(BundleManifestFileUpdater.splitIntoBundles(bundleText))
      .containsExactly(
              "org.eclipse.ui",
              "or\ng.eclipse.ui.views.properties.tabbed;bundle-version=\"3.5.300\"",
              "ch.ivyt\neam.ivy.guiComponents;bundle-version=\"[6.0.0,6.1.0)\"");
  }
}
