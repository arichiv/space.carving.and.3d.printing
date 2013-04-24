import java.io.*;
import java.util.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;

import april.jcam.*;
import april.util.*;
import april.jmat.*;
import april.jmat.geom.*;

public class Plane
{

    private double[] normal, tangent, bitangent;
    private Pixel[][] pixels;

    /* Assume plane rooted at origin */
    public Plane(double[] norm, double[] tan, double[] bitan) {
        normal = LinAlg.normalize(norm);
        tangent = LinAlg.normalize(tan);
        bitangent = LinAlg.normalize(bitan);
        pixels = new Pixel[20][20];
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                pixels[x][y] = new Pixel(x/2.0 - 4.75, y/2.0 - 4.75);
            }
        }
    }

    public void emboss(Image img) {
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                pixels[x][y].setInside(img);
            }
        }
    }

    public boolean pointIsInside(double x, double y, double z) {
        double[] pointInSpace = new double[]{x, y, z};
        double[] projectedPointInSpace = LinAlg.subtract(pointInSpace, LinAlg.scale(normal, LinAlg.dotProduct(pointInSpace, normal)));
        double xPlane = LinAlg.dotProduct(projectedPointInSpace, tangent);
        double yPlane = LinAlg.dotProduct(projectedPointInSpace, bitangent);
        int xIdx = (int)Math.round((xPlane + 4.75) * 2);
        int yIdx = (int)Math.round((yPlane + 4.75) * 2);
        if (xIdx < 0 || xIdx > 19 || yIdx < 0 || yIdx > 19) {
            return false;
        }
        return pixels[xIdx][yIdx].getInside();
    }

    private class Pixel
    {

        private double x, y;
        private boolean inside;

        public Pixel(double _x, double _y) {
            x = _x;
            y = _y;
        }

        public void setInside(Image img) {
            inside = img.pointIsInside(x, y);
        }

        public boolean getInside() {
            return inside;
        }
        
    }
    
}
