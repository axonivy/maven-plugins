package com.axonivy.asciidoctor.osgi.bundle.doc;

import java.io.File;

public class BundleInfo
{
  private File projectDirectory;
  private MavenPom pom;
  private OsgiBundleManifest manifest;
  
  private BundleInfo(File projectDirectory, MavenPom pom, OsgiBundleManifest manifest)
  {
    this.projectDirectory = projectDirectory;
    this.pom = pom;
    this.manifest = manifest;
  }
  
  public static BundleInfo read(File bundleProjectDirectory)
  {
    MavenPom pom = MavenPom.read(bundleProjectDirectory);
    if (pom == null)
    {
      return null;
    }
    OsgiBundleManifest manifest = OsgiBundleManifest.read(bundleProjectDirectory);
    if (manifest == null)
    {
      return null;
    }
    return new BundleInfo(bundleProjectDirectory, pom, manifest);
  }
  
  public MavenPom getPom()
  {
    return pom;
  }
  
  public OsgiBundleManifest getManifest()
  {
    return manifest;
  }
  
  public File getProjectDirectory()
  {
    return projectDirectory;
  }
  
  public File getTechnicalDocumentation()
  {
    File bundleDir = getProjectDirectory();
    File asciidoc = new File(bundleDir, "techdoc/index.adoc");
    File techdoc = new File(bundleDir, "techdoc/index.html");
    if (asciidoc.exists() || techdoc.exists())
    {
      return techdoc;
    }

    techdoc = new File(bundleDir, "techdoc/default.html");
    if (techdoc.exists())
    {
      return techdoc;
    }
    return null;
  }
  
  public String getTechnicalDocumentationBundleRelativPath()
  {
    File techdoc = getTechnicalDocumentation();
    if (techdoc == null)
    {
      return null;
    }
    File bundleDir = getProjectDirectory();
    return relativePath(techdoc, bundleDir);
  }

  public String getTechnicalDocumentationWorkspaceRelativPath()
  {
    File techdoc = getTechnicalDocumentation();
    if (techdoc == null)
    {
      return null;
    }
    File bundleDir = getProjectDirectory().getParentFile();
    return relativePath(techdoc, bundleDir);
  }

  private String relativePath(File file, File baseDir)
  {
    return file.getAbsolutePath().substring(baseDir.getAbsolutePath().length()+1);
  }
}
