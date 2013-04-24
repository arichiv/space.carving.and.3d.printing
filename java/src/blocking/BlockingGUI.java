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

public class BlockingGUI implements Runnable
{
    VisWorld vw = new VisWorld();
    VisLayer vl  = new VisLayer(vw);
    ModelingGUI modelingGUI;

    volatile public Blocker blocker;
    public VisCanvas vc = new VisCanvas(vl);
    public JLabel jl = new JLabel("");

    public BlockingGUI(ModelingGUI mg) {
        jl.setFont(new Font("Serif", Font.PLAIN, 20));
        modelingGUI = mg;
        blocker = new Blocker(mg);
        vl.cameraManager.uiLookAt(
            new double[] {10, 10, 10 },
            new double[] { 0,  0, 0 },
            new double[] { -1,  -1, 1 }, 
            true);
    }

    public void run()
    {
        while(true) {
            VisWorld.Buffer vb = vw.getBuffer("blocking");
            vb.addBack(blocker.getModel());
            vb.swap();
            jl.setText("" + blocker.count);
        }
    }

    public ArrayList<double[]> getCurrentPlan() {
        return blocker.getPlan();
    }

}
