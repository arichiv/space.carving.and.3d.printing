import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

import april.jcam.*;
import april.util.*;
import april.jmat.*;

import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.*;

public class Detector
{
    static{ System.loadLibrary("opencv_java245"); }

    ImageSource is;
    ImageSourceFormat fmt;
    JImage originalImage;
    JImage processedImage;
    ParameterGUI pg;
    public volatile Image current;

    public Detector(ParameterGUI _pg, ImageSource _is, JImage original, JImage processed)
    {
        pg = _pg;
        is = _is;
        is.start();
        fmt = is.getCurrentFormat();
        originalImage = original;
        processedImage = processed;
        updateImage();
    }

    public void updateImage()
    {
        byte buf[] = is.getFrame().data;
        
        // Debayer raw camera image
        Mat bw = new Mat(fmt.height, fmt.width, CvType.CV_8UC1);
        Mat pic = new Mat(fmt.height, fmt.width, CvType.CV_32SC1);
        bw.put(0, 0, buf);
        Imgproc.cvtColor(bw, pic, Imgproc.COLOR_BayerGR2RGB);
        
        // Blur + Canny on image
        Mat canny = new Mat();
        Imgproc.blur(pic, pic, new Size(pg.gi("blur_size"), pg.gi("blur_size")));
        Imgproc.Canny(pic, canny, pg.gd("canny_min"), pg.gd("canny_max"), 3, true);
        
        // Dilate + erode the edges
        Mat dil_kernel = new Mat(pg.gi("dilate_size"), pg.gi("dilate_size"), CvType.CV_8UC1);
        Mat ero_kernel = new Mat(pg.gi("erode_size"), pg.gi("erode_size"), CvType.CV_8UC1);
        Mat dilate = new Mat();
        dil_kernel.setTo(new Scalar(255));
        ero_kernel.setTo(new Scalar(255));
        Imgproc.dilate(canny, dilate, dil_kernel, new org.opencv.core.Point(-1,-1), 1);
        Imgproc.erode(dilate, dilate, ero_kernel, new org.opencv.core.Point(-1,-1), 1);
        
        // Find contours on the detected edges
        Vector<MatOfPoint> contours = new Vector<MatOfPoint>();
        Mat hierarchy = new Mat();
        Mat con_mat = dilate.clone();
        Imgproc.findContours(con_mat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        
        // Draw the contours
        Mat drawing = Mat.zeros(con_mat.size(), CvType.CV_8UC1);
        Imgproc.drawContours(drawing, contours, -1, new Scalar(255), -1);

        // Convert to a BufferedImage and display
        originalImage.setImage(convert(ImageConvert.convertToImage(fmt.format, fmt.width, fmt.height, buf)));
        drawing.convertTo(drawing, CvType.CV_32SC1, 255.0);
        int[] pic_data = new int[(int)drawing.total()];
        drawing.get(0, 0, pic_data);
        BufferedImage image_processed = new BufferedImage(fmt.width, fmt.height, BufferedImage.TYPE_INT_RGB);
        image_processed.setRGB(0, 0, fmt.width, fmt.height, pic_data, 0, fmt.width);
        processedImage.setImage(convert(image_processed));
        current = new Image(convert(image_processed));
    }

    BufferedImage convert(BufferedImage src) {
        if (pg.gi("xs") > pg.gi("xe") || pg.gi("ys") > pg.gi("ye")) {
            return src;
        }
        return src.getSubimage(pg.gi("xs"), pg.gi("ys"), pg.gi("xe") - pg.gi("xs"), pg.gi("ye") - pg.gi("ys"));
    }
}
