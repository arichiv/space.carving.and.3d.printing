import java.io.*;
import java.util.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;

import april.jcam.*;
import april.util.*;
import april.jmat.*;
import april.jmat.geom.*;

public class Image
{
    BufferedImage img;

    public Image()
    {
        img = null;
    }

    public Image(BufferedImage _img)
    {
        img = _img;
    }

    // Test if x,y is inside the object within the image
    public boolean pointIsInside(double x, double y) {

        // Focus (inches to pixels)
        double x_pixels = x * img.getWidth()/Constants.SCALE_FOCUS_X;
        double y_pixels = y * img.getHeight()/Constants.SCALE_FOCUS_Y;

        /* polar coordinates */
        double r = Math.sqrt(Math.pow(x_pixels, 2) + Math.pow(y_pixels, 2));
        double t = Math.atan2(y_pixels, x_pixels);

        // run function in reverse
        double r_prime = r;

        // translate back into new x/y and remove centering
        double x_prime = r_prime*Math.cos(t);
        double y_prime = r_prime*Math.sin(t);

        /* re-center */
        double x_origin = x_prime + img.getWidth()/2.0;
        double y_origin = img.getHeight()/2.0 - y_prime;

        // test all points near the pixel
        int count = 0;
        for (double dx = -img.getWidth()/(Constants.SCALE_FOCUS_X*4); dx < img.getWidth()/(Constants.SCALE_FOCUS_X*4); dx++) {
            for (double dy = -img.getHeight()/(Constants.SCALE_FOCUS_Y*4); dy < img.getHeight()/(Constants.SCALE_FOCUS_Y*4); dy++) {
                int new_x = (int)(x_origin+dx);
                int new_y = (int)(y_origin+dy);
                if (new_x >= 0 && new_x < img.getWidth() && new_y >= 0 && new_y < img.getHeight()) {
                    int data = img.getRGB(new_x, new_y);
                    count += (((data>>8)&0xff) > 0) ? 1 : 0;
                }
            }
        }
        
        if(count > Constants.IMAGE_MAT_THRESH) {
            return true;
        } else {
            return false;
        }
    }

    static class MockDoughnut extends Image
    {

        public boolean pointIsInside(double x, double y) {
            double distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
            return distance > 2 && distance <= 5;
        }

    }

}
