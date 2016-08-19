package ch.ivyteam.ivy.maven;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

public class DpiConverter {
	private final static double INCH_2_CM = 2.54;
	private final static String formatName = "png";

	public static void setDpiOfFile(File image, int dpi) throws IOException {
		final BufferedImage gridImage = ImageIO.read(image);
		image.delete();
		
		MetaHolder holder = getMetaDataHolder();
		setDPIInternal(holder.meta, dpi);
		save(image, gridImage, holder);
	}


	private static void save(File output, final BufferedImage gridImage, MetaHolder holder) throws IOException {
		final ImageOutputStream stream = ImageIO.createImageOutputStream(output);
		try {
			holder.writer.setOutput(stream);
			holder.writer.write(holder.meta, new IIOImage(gridImage, null, holder.meta),
					holder.writer.getDefaultWriteParam());
		} finally {
			stream.close();
		}
	}

	private static MetaHolder getMetaDataHolder() {
		for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext();) {
			ImageWriter writer = iw.next();
			ImageWriteParam writeParam = writer.getDefaultWriteParam();
			ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier
					.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
			IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
			if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
				continue;
			}
			return new MetaHolder(metadata, writer);
		}
		return null;
	}

	private static class MetaHolder {
		public final IIOMetadata meta;
		public final ImageWriter writer;

		public MetaHolder(IIOMetadata meta, ImageWriter writer) {
			this.meta = meta;
			this.writer = writer;
		}
	}

	private static void setDPIInternal(IIOMetadata metadata, int dpi) throws IIOInvalidTreeException {

		double dotsPerMilli = 1.0 * dpi / 10 / INCH_2_CM;

		IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
		horiz.setAttribute("value", Double.toString(dotsPerMilli));

		IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
		vert.setAttribute("value", Double.toString(dotsPerMilli));

		IIOMetadataNode dim = new IIOMetadataNode("Dimension");
		dim.appendChild(horiz);
		dim.appendChild(vert);

		IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
		root.appendChild(dim);

		metadata.mergeTree("javax_imageio_1.0", root);
	}

}