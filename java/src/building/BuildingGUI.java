import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.lang.Math;
import java.util.concurrent.*;

import april.jcam.*;
import april.util.*;
import april.jmat.*;
import april.vis.*;

import modeling.*;

public class BuildingGUI implements Runnable
{
    VisWorld vw = new VisWorld();
    VisLayer vl  = new VisLayer(vw);
    LinkedBlockingQueue<double[]> blocks = new LinkedBlockingQueue<double[]>();
    volatile VisChain visBlocks = new VisChain();

    public VisCanvas vc = new VisCanvas(vl);
    public ParameterGUI pg = new ParameterGUI();
    Planner planner = new Planner();

    public BuildingGUI() {
        pg.addButtons("start", "Build", "stop", "Cancel");
        vl.cameraManager.uiLookAt(
            new double[] {10, 10, 10 },
            new double[] { 0,  0, 0 },
            new double[] { -1,  -1, 1 }, 
            true);
    }

    public void start(ArrayList<double[]> _blocks) {
        visBlocks = new VisChain();
        for (double[] block : _blocks) {
            blocks.add(block);
        }
    }

    public void stop() {
        blocks.clear();
    }

    public void run() {
        ColorUtil.seededColor(498);
        planner.home();
        while (true) {

            // grab block
            try {
                planner.grabNextBlock();
            } catch (Exception e) {
                continue;
            }

            while (true) {
                try {
                    // get the block, add to vis
                    double[] block = null;
                    try { block = blocks.take(); } catch (Exception e) {}
                    visBlocks.add(new VisChain(LinAlg.translate(block[0] - 4.5, block[1] - 4.5, block[2] + .5), new VzBox(1, 1, 1, new VzMesh.Style(ColorUtil.randomColor()))));
                    VisWorld.Buffer vb = vw.getBuffer("blocking");
                    vb.addBack(visBlocks);
                    vb.swap();
                    
                    // get block for placement, decide where to place block, place it, and add it to collision list
                    block[0] = block[0]*Constants.M_PER_IN + 0.0;
                    block[1] = block[1]*Constants.M_PER_IN + 0.15;
                    block[2] = Constants.HEIGHT_OF_BOARD - block[2]*Constants.M_PER_IN;
                    System.out.println("" + block[0] + "," + block[1] + "," + block[2]);
                    planner.plan(block);
                    planner.release();
                    block[2] += Constants.M_PER_IN;
                    planner.addCollision(block);
                    break;
                } catch (Exception e) {
                    continue;
                }
            }
        }
    }

}
