import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.lang.Math;

import april.jcam.*;
import april.util.*;
import april.jmat.*;
import april.vis.*;

public class Space
{

    Voxel[][][] voxels;
    double[] rotation, translation, scaling;

    public Space() {
        rotation = new double[]{0, 0, 0};
        translation = new double[]{0, 0, 0};
        scaling = new double[]{1, 1, 1};
        voxels = new Voxel[20][20][20];
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                for (int z = 0; z < 20; z++) {
                    voxels[x][y][z] = new Voxel(x/2.0 - 4.75, y/2.0 - 4.75, z/2.0 - 4.75);
                }
            }
        }
    }

    public void carve(Plane p) {
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                for (int z = 0; z < 20; z++) {
                    voxels[x][y][z].setInside(p);
                }
            }
        }
    }

    public void rotate(double x, double y, double z) {
        rotation = new double[]{x, y, z};
    }

    public void translate(double x, double y, double z) {
        translation = new double[]{x, y, z};
    }

    public void scale(double x, double y, double z) {
        scaling = new double[]{x, y, z};
    }

    public VisChain getModel() {
        VisChain chain = new VisChain();
        chain.add(
            LinAlg.translate(translation),
            LinAlg.rotateX(rotation[0]),
            LinAlg.rotateY(rotation[1]),
            LinAlg.rotateZ(rotation[2]),
            LinAlg.scale(scaling[0], scaling[1], scaling[2])
        );
        VzBox box = new VzBox(.5, .5, .5, new VzMesh.Style(Color.blue));
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                for (int z = 0; z < 20; z++) {
                    if (voxels[x][y][z].getInside()) {
                        chain.add(voxels[x][y][z].getModel(box));
                    }
                }
            }
        }
        return chain;
    }

    public Checker getChecker() {
        return new Checker();
    }

    class Checker
    {

        private ArrayList<double[]> locations;

        public Checker() {
            locations = new ArrayList<double[]>();
            for (int x = 0; x < 20; x++) {
                for (int y = 0; y < 20; y++) {
                    for (int z = 0; z < 20; z++) {
                        if (voxels[x][y][z].getInside()) {
                            locations.add(voxels[x][y][z].getWorldLocation());
                        }
                    }
                }
            }
        }

        public boolean pointIsInside(double _x, double _y, double _z) {
            double[] ploc = new double[]{_x, _y, _z};
            for (double[] vloc : locations) {
                if (LinAlg.distance(ploc, vloc) < .5) {
                    return true;
                }
            }
            return false;
        }
        
    }

    private class Voxel
    {

        private double x, y, z;
        private double[][] locMat;
        private boolean inside;

        public Voxel(double _x, double _y, double _z) {
            x = _x;
            y = _y;
            z = _z;
            locMat = new double[][]{new double[]{x}, new double[]{y}, new double[]{z}, new double[]{1}};
            inside = true;
        }

        public void setInside(Plane p) {
            inside = inside && p.pointIsInside(x, y, z);
        }

        public boolean getInside() {
            return inside;
        }

        public double[] getWorldLocation() {
            double[][] mat = LinAlg.multiplyMany(
                LinAlg.translate(translation),
                LinAlg.rotateX(rotation[0]),
                LinAlg.rotateY(rotation[1]),
                LinAlg.rotateZ(rotation[2]),
                LinAlg.scale(scaling[0], scaling[1], scaling[2]),
                locMat
            );
            return new double[]{mat[0][0], mat[1][0], mat[2][0]};
        }

        public VisChain getModel(VzBox box) {
            return new VisChain(
                LinAlg.translate(x, y, z),
                box
            );
        }
        
    }

}
