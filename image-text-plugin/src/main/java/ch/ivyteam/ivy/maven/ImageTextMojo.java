package ch.ivyteam.ivy.maven;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "write-on-image", requiresProject = false, threadSafe = true)
public class ImageTextMojo extends AbstractMojo {

  @Parameter(required = true, property = "sourceImage")
  File sourceImage;
  @Parameter(required = true, property = "targetImage")
  File targetImage;
  /** the text that will be written on the image */
  @Parameter(defaultValue = "undefined text", property = "text")
  String text;
  @Parameter(defaultValue = "arial", property = "font")
  String font;
  @Parameter(defaultValue = "12", property = "fontSize")
  Integer fontSize;
  /** the RGB font color, defaults to WHITE */
  @Parameter(defaultValue = "255,255,255")
  String fontColor;
  /** either plain, italic or bold, defaults to plain */
  @Parameter(defaultValue = "plain", property = "fontStyle")
  String fontStyle;
  /** A font file in TTF format that will be used. If this parameter is set, */
  @Parameter(property = "fontFile")
  File fontFile;
  /** relative to left corner */
  @Parameter(defaultValue = "20", property = "x")
  Integer x;
  /** relative to upper corner */
  @Parameter(defaultValue = "20", property = "y")
  Integer y;
  /**
   * how the text should be aligned relative to the x. Possible values are
   * "left", "center", "right"
   */
  @Parameter(defaultValue = "left", property = "align")
  String align;
  @Parameter(defaultValue = "true", property = "antialising")
  Boolean antialising;
  /**
   * controls whether the positioning of individual character glyphs takes into
   * account the sub-pixel accuracy of the scaled character advances of the font
   * or whether such advance vectors are rounded to an integer number of whole
   * device pixels. This hint only recommends how much accuracy should be used
   * to position the glyphs and does not specify or recommend whether or not the
   * actual rasterization or pixel bounds of the glyph should be modified to
   * match.
   * @See {@link RenderingHints#KEY_FRACTIONALMETRICS}
   */
  @Parameter(defaultValue = "false", property = "fractionalMetrics")
  Boolean fractionalMetrics;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var image = readSourceImage();
    var modified = modifyImage(image);
    writeModifiedImage(modified);
  }

  private BufferedImage readSourceImage() throws MojoExecutionException {
    try {
      getLog().debug("Loading image " + sourceImage.getAbsolutePath());
      var image = ImageIO.read(sourceImage);
      if (image == null) {
        throw new MojoExecutionException("The image '" + sourceImage.getAbsolutePath() + "' could not be read.");
      }
      return image;
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to read source image '" + sourceImage.getAbsolutePath() + "'", ex);
    }
  }

  private BufferedImage modifyImage(BufferedImage template) throws MojoExecutionException {
    var editImage = new BufferedImage(template.getWidth(), template.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
    var graphic = editImage.createGraphics();
    graphic.drawImage(template, 0, 0, null);
    graphic.setRenderingHint(RenderingHints.KEY_ANTIALIASING, getAntialisingHint());
    graphic.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, getFractionalMetricsHint());
    graphic.setPaint(getColor());
    graphic.setFont(getFont());

    var txt = renderText();
    int textLength = graphic.getFontMetrics().stringWidth(txt);
    x += alignX(textLength);
    getLog().info("writing '" + text + "' on image '" + targetImage.getName() + "'");
    graphic.drawString(txt, x, y);
    graphic.dispose();
    return editImage;
  }

  private String renderText() {
    if (text == null) {
      return "";
    }
    var currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    return text.replace("#currentYear", currentYear);
  }

  private Object getAntialisingHint() {
    if (antialising) {
      return RenderingHints.VALUE_ANTIALIAS_ON;
    }
    return RenderingHints.VALUE_ANTIALIAS_OFF;
  }

  private Object getFractionalMetricsHint() {
    if (fractionalMetrics) {
      return RenderingHints.VALUE_FRACTIONALMETRICS_ON;
    }
    return RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT;
  }

  private Font getFont() throws MojoExecutionException {
    if (fontFile != null) {
      return createFontFromFile();
    } else {
      var systemFont = Arrays
              .stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
              .filter(awtFont -> awtFont.getFontName().equals(font))
              .findAny();
      if (!systemFont.isPresent()) {
        return createNonSystemFont();
      }
      return systemFont.get().deriveFont(getFontStyle(), fontSize.intValue());
    }
  }

  private Font createFontFromFile() throws MojoExecutionException {
    try {
      getLog().info("Loading " + fontFile);
      return Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(getFontStyle(), fontSize.intValue());
    } catch (Exception ex) {
      throw new MojoExecutionException("Failed to read TTF font from file '" + fontFile + "'", ex);
    }
  }

  private Font createNonSystemFont() {
    getLog().warn("Font '" + font + "' seems not to be installed on this system. Run with -X to see available fonts.");
    if (getLog().isDebugEnabled()) {
      var fontNames = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
              .map(awtFont -> awtFont.getFontName())
              .collect(Collectors.joining(","));
      getLog().debug("Available fonts are: " + fontNames);
    }
    return new Font(font, getFontStyle(), fontSize.intValue());
  }

  private Color getColor() {
    var rgbTokens = fontColor.split(",");
    if (rgbTokens == null || rgbTokens.length != 3) {
      getLog().warn("Font color '" + fontColor + "' does not contain RGB values. Using default font (white).");
      return Color.WHITE;
    }
    try {
      int red = Integer.parseInt(rgbTokens[0]);
      int green = Integer.parseInt(rgbTokens[1]);
      int blue = Integer.parseInt(rgbTokens[2]);
      return new Color(red, green, blue);
    } catch (IllegalArgumentException ex) {
      getLog().warn("Failed to parse font color '" + fontColor + "'. Using default font (white).", ex);
      return Color.WHITE;
    }
  }

  private int getFontStyle() {
    return switch (Style.valueOf(fontStyle.toUpperCase())) {
      case ITALIC -> Font.ITALIC;
      case BOLD -> Font.BOLD;
      case PLAIN -> Font.PLAIN;
    };
  }

  private void writeModifiedImage(BufferedImage modified) throws MojoExecutionException {
    try {
      getLog().debug("Writing modified image '" + targetImage.getAbsolutePath() + "'");
      targetImage.getParentFile().mkdirs();
      ImageIO.write(modified, "bmp", targetImage);
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to write modified image '" + targetImage + "'", ex);
    }
  }

  private int alignX(int textLength) {
    return switch (Align.valueOf(align.toUpperCase())) {
      case CENTER -> -textLength / 2;
      case RIGHT -> -textLength;
      default -> 0;
    };
  }

  enum Align {
    LEFT, CENTER, RIGHT;
  }

  enum Style {
    PLAIN, BOLD, ITALIC;
  }
}
