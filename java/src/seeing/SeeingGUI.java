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

public class SeeingGUI implements Runnable
{
    Detector detector;
    Mover mover;

    public JImage originalImage = new JImage();
    public JImage processedImage = new JImage();
    public ParameterGUI pg = new ParameterGUI();

    public SeeingGUI(ImageSource is) {
        pg.addIntSlider("xs", "X Start", 0, 1296, 0);
        pg.addIntSlider("xe", "X End", 0, 1296, 1296);
        pg.addIntSlider("ys", "Y Start", 0, 964, 0);
        pg.addIntSlider("ye", "Y End", 0, 964, 964);
        pg.addDoubleSlider("canny_min", "Canny Min Threshold", 0.0, 500.0, Constants.CANNY_MIN_THRESH);
        pg.addDoubleSlider("canny_max", "Canny Max Threshold", 0.0, 500.0, Constants.CANNY_MAX_THRESH);
        pg.addIntSlider("blur_size", "Blur Kernel Size", 1, 100, Constants.CANNY_BLUR_SIZE);
        pg.addIntSlider("dilate_size", "Dilate Kernel Size", 1, 25, Constants.DILATE_SIZE);
        pg.addIntSlider("erode_size", "Erode Kernel Size", 1, 25, Constants.ERODE_SIZE);
        pg.addButtons("rotate", "Rotate", "capture", "Capture", "reset", "Reset");
        detector = new Detector(pg, is, originalImage, processedImage);
        originalImage.setFit(true);
        processedImage.setFit(true);
        mover = new Mover();
    }

    public void run() {
        while(true) {
            detector.updateImage();
        }
    }

    public void rotate() {
        mover.goToNext();
        mover.updatePosition();
    }

    public Plane getCurrentPlane() {
        Plane plane = new Plane(mover.getNormal(), mover.getTangent(), mover.getBitangent());
        plane.emboss(detector.current);
        return plane;
    }

}
