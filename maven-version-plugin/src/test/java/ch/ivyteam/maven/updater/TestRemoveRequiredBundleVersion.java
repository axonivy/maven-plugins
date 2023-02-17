package ch.ivyteam.maven.updater;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import ch.ivyteam.maven.BundleManifestFileUpdater;

class TestRemoveRequiredBundleVersion {

  @Test
  void canHandleRequiredBundleRanges() throws IOException {
    var srcPluginDir = Files.createTempDirectory("SourcePlugin").toFile();
    var manifest = new File(srcPluginDir, BundleManifestFileUpdater.MANIFEST_MF);
    createFile(manifest, streamOf("MANIFEST.MF_source"));
    new BundleManifestFileUpdater(srcPluginDir, "6.1.0.XXX", new SystemStreamLog(), List.of(), false).update();
    var updatedManifest = Files.readString(manifest.toPath());
    var expectedManifest = IOUtils.toString(streamOf("MANIFEST.MF_expected"), StandardCharsets.UTF_8);
    assertThat(updatedManifest).isEqualTo(expectedManifest);
  }

  @Test
  void canRemoveBundleRanges() throws IOException {
    var srcPluginDir = Files.createTempDirectory("SourcePlugin").toFile();
    var manifest = new File(srcPluginDir, BundleManifestFileUpdater.MANIFEST_MF);
    createFile(manifest, streamOf("MANIFEST.MF_source"));
    new BundleManifestFileUpdater(srcPluginDir, "6.1.0.XXX", new SystemStreamLog(), List.of(), true).update();
    var updatedManifest = Files.readString(manifest.toPath());
    var expectedManifest = IOUtils.toString(streamOf("MANIFEST.MF_expectedNoRanges"), StandardCharsets.UTF_8);
    assertThat(updatedManifest).isEqualToIgnoringWhitespace(expectedManifest);
  }

  private static void createFile(File file, InputStream content) throws IOException {
    file.getParentFile().mkdirs();
    file.createNewFile();
    IOUtils.copy(content, new FileOutputStream(file));
  }

  private static InputStream streamOf(String resouce) {
    return TestRemoveRequiredBundleVersion.class.getResourceAsStream(resouce);
  }
}
