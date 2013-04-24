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

import modeling.*;

public class ModelingGUI implements Runnable
{
    VisWorld vw = new VisWorld();
    VisLayer vl  = new VisLayer(vw);

    volatile public Space space = new Space();
    public VisCanvas vc = new VisCanvas(vl);
    public ParameterGUI pg = new ParameterGUI();

    public ModelingGUI() {
        pg.addDoubleSlider("rx", "Rotation X", -Math.PI, Math.PI, 0);
        pg.addDoubleSlider("ry", "Rotation Y", -Math.PI, Math.PI, 0);
        pg.addDoubleSlider("rz", "Rotation Z", -Math.PI, Math.PI, 0);
        pg.addDoubleSlider("sx", "Scaling X", 0, 2, 1);
        pg.addDoubleSlider("sy", "Scaling Y", 0, 2, 1);
        pg.addDoubleSlider("sz", "Scaling Z", 0, 2, 1);
        pg.addDoubleSlider("tx", "Translation X", -10, 10, 0);
        pg.addDoubleSlider("ty", "Translation Y", -10, 10, 0);
        pg.addDoubleSlider("tz", "Translation Z", -10, 10, 0);
        vl.cameraManager.uiLookAt(
            new double[] {10, 10, 10 },
            new double[] { 0,  0, 0 },
            new double[] { -1,  -1, 1 }, 
            true);
    }

    public void addPlane(Plane plane) {
        space.carve(plane);
    }

    public void resetPlanes() {
        space = new Space();
    }

    public void run()
    {
        while(true) {
            VisWorld.Buffer vb = vw.getBuffer("modeling");
            space.rotate(pg.gd("rx"), pg.gd("ry"), pg.gd("rz"));
            space.scale(pg.gd("sx"), pg.gd("sy"), pg.gd("sz"));
            space.translate(pg.gd("tx"), pg.gd("ty"), pg.gd("tz"));
            vb.addBack(space.getModel());
            vb.swap();
        }
    }

}
