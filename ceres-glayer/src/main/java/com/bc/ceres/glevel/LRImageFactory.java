package com.bc.ceres.glevel;

import java.awt.image.RenderedImage;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public interface LRImageFactory {
    RenderedImage createLRImage(int level);
}
