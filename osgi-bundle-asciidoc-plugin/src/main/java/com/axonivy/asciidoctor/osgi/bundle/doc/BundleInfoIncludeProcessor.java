package com.axonivy.asciidoctor.osgi.bundle.doc;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.asciidoctor.ast.DocumentRuby;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

public class BundleInfoIncludeProcessor extends IncludeProcessor
{
  public BundleInfoIncludeProcessor(Map<String, Object> config)
  {
    super(config);
  }

  @Override
  public void process(DocumentRuby document, PreprocessorReader reader, String target,
          Map<String, Object> attributes)
  {
    StringBuilder builder = new StringBuilder();
    File docFile = new File((String)document.getAttr("docfile"));
    BundleInfo bundle = BundleInfo.read(docFile.getParentFile().getParentFile());
    appendTitle(builder, bundle);
    appendDescription(builder, bundle);
    appendRequiredBundles(builder, bundle);
    appendExportedPackages(builder, docFile, bundle);   
    reader.push_include(builder.toString(), target, target, 1, attributes);
  }

  private void appendTitle(StringBuilder builder, BundleInfo bundle)
  {
    builder.append("= ");
    builder.append(bundle.getManifest().getName());
    builder.append(" (");
    builder.append(bundle.getManifest().getSymbolicName());
    builder.append(")\n");
    builder.append("\n");
  }

  private void appendRequiredBundles(StringBuilder builder, BundleInfo bundle)
  {
    builder.append("== Required Bundles\n");
    builder.append(".Required Bundles\n");
    builder.append("|===\n");
    builder.append("|Symbolic Name |Description\n");
    builder.append("\n");
    for (String requiredBundleName : bundle.getManifest().getRequireBundles())
    {
      builder.append("|");
      BundleInfo requiredBundle = BundleInfo.read(new File(bundle.getProjectDirectory().getParentFile(), requiredBundleName));
      if (requiredBundle != null)
      {
        String techdocRelativePath = requiredBundle.getTechnicalDocumentationWorkspaceRelativPath();
        if (techdocRelativePath != null)
        {
          builder.append("link:../../");
          builder.append(techdocRelativePath);
          builder.append("[");
        }
        builder.append(requiredBundleName);
        if (techdocRelativePath != null)
        {
          builder.append("]");
        }
        builder.append("\n");
        builder.append("|");
        builder.append(requiredBundle.getPom().getDescription());
        builder.append("\n");
      }
      else
      {
        builder.append(requiredBundleName);
        builder.append("\n");
        builder.append("|");
        builder.append("\n");
      }
    }
    builder.append("|===\n");
    builder.append("\n");
    builder.append("\n");
  }

  private void appendExportedPackages(StringBuilder builder, File docFile, BundleInfo bundle)
  {
    builder.append("== Exported Packages\n");
    builder.append(".Exported Packages\n");
    builder.append("|===\n");
    builder.append("|Package Name |Description\n");
    builder.append("\n");
    for (String exportedPackage : bundle.getManifest().getPublicExportedPackages())
    {
      builder.append("|");
      builder.append("link:../../../api/index.html?");
      builder.append(exportedPackage.replace(".", "/"));
      builder.append("/package-summary.html");
      builder.append("[");
      builder.append(exportedPackage);
      builder.append("]");
      builder.append("\n");
      builder.append("|");
      builder.append(getPackageDescription(docFile, exportedPackage));
      builder.append("\n");
    }
    builder.append("|===\n");

    builder.append("\n\n");
  }

  private void appendDescription(StringBuilder builder, BundleInfo bundle)
  {
    builder.append("== Description\n");
    builder.append(bundle.getPom().getDescription());
    builder.append("\n");
    builder.append("\n");
  }

  private String getPackageDescription(File docFile, String exportedPackage)
  {
    try
    {
      exportedPackage = exportedPackage.replace(".", "/");
      File packageSummaryFile = new File(docFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(), "generated-docs/api/"+exportedPackage+"/package-summary.html");
      if (!packageSummaryFile.exists())
      {
        return "";
      }
      String packageSummary = FileUtils.readFileToString(packageSummaryFile, "UTF-8");
      packageSummary = StringUtils.substringAfter(packageSummary, "<div class=\"docSummary\">\r\n<div class=\"block\">");
      packageSummary = StringUtils.substringBefore(packageSummary, "</div>");
      packageSummary = packageSummary.replaceAll("\\<[^>]*>","");
      return packageSummary.trim();
    }
    catch (IOException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public boolean handles(String target)
  {
    return target.toUpperCase().endsWith("BUNDLE_INFO");
  }
}
