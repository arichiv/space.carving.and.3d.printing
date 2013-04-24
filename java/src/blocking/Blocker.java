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

public class Blocker {

    ModelingGUI modelingGUI;
    volatile int[][][] plan;

    public int count = 0;
    
    public Blocker(ModelingGUI mg){
        modelingGUI = mg;
    }
    
    public VisChain getModel(){

        Space.Checker spaceChecker = modelingGUI.space.getChecker();
        int[][][] _plan = new int[10][10][10];

        VisChain block_model = new VisChain();
        VzBox block_normal = new VzBox(1, 1, 1, new VzMesh.Style(Color.green)); 
        VzBox block_filler = new VzBox(1, 1, 1, new VzMesh.Style(Color.red)); 

        count = 0;
        for(int z = 9; z >= 0; z--){
            for(int y = 0; y < 10; y++){
                for(int x = 0; x < 10; x++){
                    if(spaceChecker.pointIsInside(x - 4.5, y - 4.5, z - 4.5)) {
                        _plan[z][y][x] = 1;
                        block_model.add(new VisChain(LinAlg.translate(x - 4.5, y - 4.5, z + .5), block_normal)); 
                        count++;
                    } else if (z < 9 && _plan[z+1][y][x] > 0) {
                        _plan[z][y][x] = 2;
                        block_model.add(new VisChain(LinAlg.translate(x - 4.5, y - 4.5, z + .5), block_filler)); 
                        count++;
                    }
                }
            }
        }
        plan = _plan;

        block_model.add(new VisChain(LinAlg.translate(0, 0, 2), new VzBox(4, 4, 4, new VzMesh.Style(ColorUtil.setAlpha(Color.red, 56)))));
        return block_model;
    }

    public ArrayList<double[]> getPlan(){
        ArrayList<double[]> pointPlan = new ArrayList<double[]>();
        for(int z = 0; z < 4; z++){
            for(int y = 3; y < 7; y++){
                for(int x = 3; x < 7; x++){
                    if(plan[z][y][x] > 0) {
                        pointPlan.add(new double[]{(x - 4.5)*1.2, (y - 4.5)*1.2, z, 1});
                    }
                }
            }
        }
        return pointPlan;
    }

 }
 
 

