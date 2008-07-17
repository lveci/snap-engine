package com.bc.jai;

import com.bc.jai.tools.Utils;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

public class TransparencyTest {


    public static void main(String[] args) {
        Utils.configureJAI();

        int width = 3 * 1024;
        int height = 3 * 1024;
        int featureCount = 2500;

        ArrayList<Feature> features1 = new ArrayList<Feature>(featureCount);
        for (int i = 0; i < featureCount; i++) {
            double r = 10 + Math.random() * width / 40.0;
            Shape shape = new Ellipse2D.Double(Math.random() * width,
                                               Math.random() * height,
                                               r, r);
            Feature feature = new Feature(shape,
                                          new Color((float) Math.random(), (float) Math.random(), (float) Math.random()),
                                          new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
            features1.add(feature);
        }
        RenderedImage image1 = FeatureOpImage.create(width, height, features1.toArray(new Feature[0]));

        ArrayList<Feature> features2 = new ArrayList<Feature>(featureCount);
        for (int i = 0; i < featureCount; i++) {
            double r = 10 + Math.random() * width / 50.0;
            Shape shape = new Rectangle2D.Double(Math.random() * width,
                                                 Math.random() * height,
                                                 r, r);
            Feature feature = new Feature(shape,
                                          new Color((float) Math.random(), (float) Math.random(), (float) Math.random()),
                                          new Color((float) Math.random(), (float) Math.random(), (float) Math.random()));
            features2.add(feature);
        }
        RenderedImage image2 = FeatureOpImage.create(width, height, features2.toArray(new Feature[0]));

        Utils.displayImages("UhahaTest",
                            new RenderedImage[]{
                                    image1,
                                    image2
                            },
                            new AffineTransform[]{
                                    new AffineTransform(),
                                    AffineTransform.getTranslateInstance(16,16)
                            },
                            8, true);

    }

    private static class FeatureOpImage extends SourcelessOpImage {
        private Feature[] features;

        public static FeatureOpImage create(int width, int height, Feature[] features) {
            BufferedImage proto = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            ImageLayout imageLayout = new ImageLayout();
            imageLayout.setWidth(width);
            imageLayout.setHeight(height);
            imageLayout.setTileWidth(256);
            imageLayout.setTileHeight(256);
            imageLayout.setSampleModel(proto.getSampleModel().createCompatibleSampleModel(width, height));
            imageLayout.setColorModel(proto.getColorModel());
            return new FeatureOpImage(imageLayout, features);
        }

        private FeatureOpImage(ImageLayout imageLayout, Feature[] features) {
            super(imageLayout,
                  null,
                  imageLayout.getSampleModel(null),
                  0, 0,
                  imageLayout.getWidth(null),
                  imageLayout.getHeight(null));
            setTileCache(JAI.getDefaultInstance().getTileCache());
            this.features = features;
        }

        @Override
        protected void computeRect(PlanarImage[] sources, WritableRaster destTile, Rectangle destRect) {
            BufferedImage image = new BufferedImage(getColorModel(),
                                                    WritableRaster.createWritableRaster(
                                                            destTile.getSampleModel(),
                                                            destTile.getDataBuffer(), new Point(0, 0)), false, null);
            Graphics2D g = image.createGraphics();
            g.translate(-destRect.x, -destRect.y);
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            for (Feature feature : features) {
                g.setColor(feature.fillColor);
                g.fill(feature.shape);
                g.setColor(feature.lineColor);
                g.draw(feature.shape);
            }
            g.dispose();
        }

    }

    public static class Feature {
        Shape shape;
        Color lineColor;
        Color fillColor;

        private Feature(Shape shape, Color lineColor, Color fillColor) {
            this.shape = shape;
            this.lineColor = lineColor;
            this.fillColor = fillColor;
        }
    }
}