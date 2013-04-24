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
import blocking.*;
import seeing.*;

public class Main
{

    SeeingGUI seeingGUI;
    ModelingGUI modelingGUI;
    BlockingGUI blockingGUI;
    BuildingGUI buildingGUI;

    public Main(String url) throws IOException {

        JFrame jf = new JFrame();
        jf.setLayout(null);
        ParameterGUIListener pgl = new ParameterGUIListener();

        seeingGUI = new SeeingGUI(ImageSource.make(url));
        new Thread(seeingGUI).start();
        jf.add(seeingGUI.originalImage);
        seeingGUI.originalImage.setBounds(0,0,500,500);
        jf.add(seeingGUI.processedImage);
        seeingGUI.processedImage.setBounds(500,0,500,500);
        jf.add(seeingGUI.pg);
        seeingGUI.pg.setBounds(0,500,1000,250);
        seeingGUI.pg.addListener(pgl);

        modelingGUI = new ModelingGUI();
        new Thread(modelingGUI).start();
        jf.add(modelingGUI.vc);
        modelingGUI.vc.setBounds(1000,0,500,500);
        jf.add(modelingGUI.pg);
        modelingGUI.pg.setBounds(1000,500,1000,175);

        blockingGUI = new BlockingGUI(modelingGUI);
        new Thread(blockingGUI).start();
        jf.add(blockingGUI.vc);
        blockingGUI.vc.setBounds(1500,0,500,500);
        jf.add(blockingGUI.jl);
        blockingGUI.jl.setBounds(1000,675,1000,75);

        buildingGUI = new BuildingGUI();
        new Thread(buildingGUI).start();
        jf.add(buildingGUI.vc);
        buildingGUI.vc.setBounds(2000,0,500,500);
        jf.add(buildingGUI.pg);
        buildingGUI.pg.setBounds(2000,500,500,250);
        buildingGUI.pg.addListener(pgl);

        jf.setSize(2500, 750);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setVisible(true);
    }

    class ParameterGUIListener implements ParameterListener
    {
        public void parameterChanged(ParameterGUI pg, String name)
        {
            if (name == "reset") {
                modelingGUI.resetPlanes();
            } else if (name == "rotate") {
                seeingGUI.rotate();
            } else if (name == "capture") {
                modelingGUI.addPlane(seeingGUI.getCurrentPlane());
            } else if (name == "start") {
                buildingGUI.start(blockingGUI.getCurrentPlan());
            } else if (name == "stop") {
                buildingGUI.stop();
            }
        }
    }

    public static void main(String args[]) throws IOException
    {
        System.out.printf("Cameras found:\n");
        for (String u : ImageSource.getCameraURLs()) {
            System.out.printf("  %s\n", u);
        }
        new Main(ImageSource.getCameraURLs().get(1));
    }

}
