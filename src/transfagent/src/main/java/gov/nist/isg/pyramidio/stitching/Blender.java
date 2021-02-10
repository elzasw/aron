/*
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of
 * their official duties. Pursuant to title 17 Section 105 of the United
 * States Code this software is not subject to copyright protection and is
 * in the public domain. This software is an experimental system. NIST assumes
 * no responsibility whatsoever for its use by other parties, and makes no
 * guarantees, expressed or implied, about its quality, reliability, or
 * any other characteristic. We would appreciate acknowledgement if the
 * software is used.
 */
package gov.nist.isg.pyramidio.stitching;

import java.awt.image.BufferedImage;

/**
 *
 * @author Antoine Vandecreme
 */
public interface Blender {
    
    /**
     * Blend the specified image positioning it at the (x,y) location
     * @param image the image to blend
     * @param x the x coordinate where to blend the image
     * @param y the y coordinate where to blend the image
     */
    void blend(BufferedImage image, int x, int y);
    
    BufferedImage getResult();
    
}