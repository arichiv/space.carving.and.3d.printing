import java.awt.*;
import java.io.*;
import java.lang.Math.*;
import java.util.Random;
import java.util.*;

import javax.swing.*;

import lcm.lcm.*;
import april.jmat.*;
import april.jmat.geom.*;
import java.awt.event.*;
import april.util.*;
import april.vis.*;
import lcmtypes.*;

public class Planner implements LCMSubscriber
{
    volatile dynamixel_status_list_t arm_status;
    ArrayList<double[]> collisionLocations = new ArrayList<double[]>();
    Random ran = new Random();

    public Planner()
    {
        LCM.getSingleton().subscribe(Constants.ARM_BUILD_LISTENER,this);
    }

    //*********************Major Planning/Algorithms*******************************
    public void grabNextBlock() throws Exception
    {
        plan(Constants.PRE_BLOCK_ONE_POSE);
        sleep();
        publishLCM(Constants.PRE_GRAB_ONE_POSE, .2);
        sleep();
        grab();
        publishLCM(Constants.POST_BLOCK_ONE_POSE, .2);
        //double[] goal = new double[]{-.1,.17,.27,1};
        //plan(goal);
    }/*grabNextBlock*/

    public boolean selfCollision(double[] angles)
    {
        //Collision Rules Set #1
        if((angles[0] < -(Math.PI)) || (angles[0] > (Math.PI))) {
            return true;
        }
        if ((angles[1] < -2.39) || (angles[1] > 2.15)) {
            return true;
        }        
        if ((angles[2] < -2.17) || (angles[2] > 2.22)) {
            return true;
        }        
        if ((angles[3] < -2.19) || (angles[3] > 2.22)) {
            return true;
        }        
        if ((angles[4] < -2.5) || (angles[4] > 2.5)) {
            return true;
        }
        if ((angles[5] < 0) || (angles[5] > 2)) {
            return true;
        }
            
        double[][] joints = getJointXYZLocs(angles); //first dimension is joint #, second dim is x,y,z,1    
                    
        //Collision Rules Set #2        
        if (joints[2][2] <= 0.005) {
            return true;            
        }    
        if (joints[4][2] <= 0.005) {
            return true;
        }
        if(joints[3][2] <= .06 && joints[4][2] <= .04){
            return true;
        }    

        //Collision Rules Set #3
        for (int i=0; i < Constants.BASELOCS.length; i++) {
            if (LinAlg.distance(joints[3],Constants.BASELOCS[i]) <= 0.04) {
                return true;
            }
            if (LinAlg.distance(joints[4],Constants.BASELOCS[i]) <= 0.04) {
                return true;
            }
        }

        //Collision Rules Set #4, collision with actual ground
        if(joints[4][2] >= Constants.HEIGHT_OF_BOARD) {
           // System.out.println("Collision With board");
            return true;
        }

        //Collision Rules Set #5
        for(int i =0; i < 6; i++) {
            if(Double.isNaN(angles[i])) {
                return true;
            }
        }
        return false;
    }/*selfCollision*/

    public double[] randomConfiguration()
    {
        double[] r_config = new double[]{0.0,0.0,0.0,0.0,0.0,0.0};
        double[] old_config = getServoAngles();
        r_config[5] = old_config[5];
        boolean ok;
        double[][] joints;
        do {        
            //Generate a random angle from [-PI,PI) for base servo.
            //r_config[0] = (Math.random() * Math.PI * 2) - Math.PI;
            r_config[0] = ((ran.nextGaussian()/3+0.5) * Math.PI * 2) - Math.PI;
            if (r_config[0] >= Math.PI) r_config[0] = Math.PI-0.1;
            if (r_config[0] < -Math.PI) r_config[0] = -Math.PI;
            //Generate a random angle from [-2.39,2.15) for second servo. 
            //r_config[1] = (Math.random() * 4.54) - 2.39;
            r_config[1] = ((ran.nextGaussian()/3+0.5) * 2.39); //- 2.39;
            if (r_config[1] >= 2.15) r_config[1] = 2.05;
            if (r_config[1] < -2.39) r_config[1] = -2.39;
            //Generate a random angle from [-2.17,2.22) for third servo.
            //r_config[2] = (Math.random() * 4.39) - 2.17;
            r_config[2] = ((ran.nextGaussian()/3+0.5) * 4.39) - 2.17;
            if (r_config[2] >= 2.22) r_config[2] = 2.12;
            if (r_config[2] < -2.17) r_config[2] = -2.17;
            //Generate a random angle from [-2.19, 2.22) for fourth servo.
            //r_config[3] = (Math.random() * 4.41) - 2.19;
            r_config[3] = ((ran.nextGaussian()/3+0.5) * 4.41 * 2) - 2.19;
            if (r_config[3] >= 2.22) r_config[3] = 2.12;
            if (r_config[3] < -2.19) r_config[3] = -2.19;
        } while(selfCollision(r_config));    //Checks for self and ground collisions at final position.
        return r_config;
    }/*randomConfiguration*/

    public boolean objectCollisionCheck(double[] angles)
    {
        double[][] collisionPoints = getArmXYZLocs(angles);
        for(int i = 0; i < collisionPoints.length; ++i){
            for (double[] collision : collisionLocations) {
                if(dist3D(collisionPoints[i], collision) < Constants.BLOCK_RADIUS) {
                    return true;
                }
            }
            for(int j = 0; j < Constants.STRUCTURE_COLLISIONS.length; ++j) {
                if(dist3D(collisionPoints[i], Constants.STRUCTURE_COLLISIONS[j]) < Constants.STRUCTURE_COLLISION_RADIUS) {
                    return true;
                }
            }
            for(int j = 0; j < Constants.LEG_COLLISIONS.length; ++j) {
                if(dist3D(collisionPoints[i], Constants.LEG_COLLISIONS[j]) < Constants.LEG_COLLISIONS_RADIUS) {
                    return true;
                }
            }
        }
        return false;
    }/*objectCollisionCheck*/

    public boolean interpolate(double[] a, double[] a1, boolean updateA)
    {
        boolean[] incr = new boolean[4];
        boolean collision = false;
        double[] a0 = new double[a.length];
        for(int i = 0; i < 4; ++i) {
            a0[i] = a[i];
            if(a0[i]-a1[i] < 0)
                incr[i] = true;
            else
                incr[i] = false;
        }
        int counter = 0;
        while(counter <= Constants.STEP_SIZE &&
                !collision &&  (
                a0[0] != a1[0] ||
                a0[1] != a1[1] ||
                a0[2] != a1[2] ||
                a0[3] != a1[3] )) {
            if(updateA) {
                counter++;
            }
            for(int i = 0; i < 4; ++i) {
                if(incr[i] && a0[i] >= a1[i]) {
                    a0[i] = a1[i];            
                }
                else if(incr[i]) {
                    a0[i] += Constants.STEP_WIDTH;
                }
                else if(a0[i] <= a1[i]) {
                    a0[i] = a1[i];
                }
                else {
                    a0[i] -= Constants.STEP_WIDTH;
                }
            }
            collision = objectCollisionCheck(a0) || selfCollision(a0);
        }
        if(!collision && updateA) {
            a1 = a0;
        }
        //System.out.println("Counter: "+counter);
        return collision;
    }/*interpolate*/

    public double[] convertToAngles(double[] realLocs) throws Exception
    {
        double[] thetaLocs = new double[]{0,0,0,0,0,0};
        double r,z_joint2,dist,tempTheta,tempHeight,tempWidth;

        //Solve for the elbow down case
        thetaLocs[0] = Math.atan2(realLocs[1],realLocs[0]);
        r = pythag(realLocs[0],realLocs[1]);
        z_joint2 = realLocs[2]-Constants.Z_JOINTS[3]-Constants.Z_JOINTS[4];
        dist =  pythag((z_joint2-Constants.Z_JOINTS[0]),r);
        thetaLocs[2] = -(Math.PI-lawOfCos(Constants.Z_JOINTS[1],Constants.Z_JOINTS[2],dist));
        tempHeight = z_joint2-Constants.Z_JOINTS[0];
        if(tempHeight >= 0) {
            tempTheta = Math.acos(tempHeight/dist) + lawOfCos(Constants.Z_JOINTS[1], dist, Constants.Z_JOINTS[2]);
        }
        else {
            tempTheta = Math.PI/2 + Math.acos(r/dist) + lawOfCos(Constants.Z_JOINTS[1], dist, Constants.Z_JOINTS[2]);
        }
        thetaLocs[1] = tempTheta;
        double[][] tempxyz = getJointXYZLocs(thetaLocs);
        tempHeight = z_joint2 - tempxyz[1][2];
        tempTheta = -Math.acos(tempHeight/Constants.Z_JOINTS[2]);
        thetaLocs[3] = tempTheta;
        thetaLocs[4] = 0;
        thetaLocs[5] = 1.7;

        //Solve for the elbow up case if need be
        if(objectCollisionCheck(thetaLocs) || selfCollision(thetaLocs)) {
            thetaLocs[2] = -thetaLocs[2];
            if(tempHeight >= 0) {
                tempTheta = Math.PI/2 - (Math.asin(tempHeight/dist) + lawOfCos(Constants.Z_JOINTS[1], dist, Constants.Z_JOINTS[2]));
            }
            else {
                tempTheta = Math.PI/2 + Math.acos(r/dist) - lawOfCos(Constants.Z_JOINTS[1], dist, Constants.Z_JOINTS[2]);
            }
            thetaLocs[1] = tempTheta;
            tempxyz = getJointXYZLocs(thetaLocs);
            tempHeight = z_joint2 - tempxyz[1][2];
            tempTheta = -(Math.PI - Math.acos(tempHeight/Constants.Z_JOINTS[2]));  
        }
       if(objectCollisionCheck(thetaLocs) || selfCollision(thetaLocs)) {
            System.out.println("Failed to find a valid configuration");
            throw(new Exception());
        }
        return thetaLocs;
    }

    public void plan(double[] goal) throws Exception
    {
        ArrayList<double[]> planAngles = new ArrayList<double[]>();
        double[] goalTheta;
        if(goal.length != 6) {        //If function is called with coordinate.    
            goalTheta = convertToAngles(goal);
        }
        else {                        //If function is called with angles.
            goalTheta = goal;
        }            
        RRT myRRT = new RRT(getServoAngles());
        COORD temp;
        boolean done = false;
        while(!done) {
            if(myRRT.data.size() > Constants.MAX_RRT_NODES)
                myRRT = new RRT(getServoAngles()); 
            do {
                temp = myRRT.findClosestNode(randomConfiguration());
            }while(interpolate(myRRT.data.get(temp.parent).angles,temp.angles,true));
            myRRT.addNode(temp);
            temp = myRRT.findClosestNode(goalTheta);
            if(parameterDistance(temp.angles,myRRT.data.get(temp.parent).angles) < Constants.MIN_DIST && !interpolate(myRRT.data.get(temp.parent).angles,temp.angles,false)) {
                System.out.println("Going to goal");
                while(temp.parent != -1) {
                    planAngles.add(temp.angles);
                    temp = myRRT.data.get(temp.parent);
                }
                for(int i = planAngles.size()-1; i >= 0; --i) {
                    publishLCM(planAngles.get(i),.1);
                    System.out.println("Publishing message");
                }
                done = true;
            }
        }
    }/*plan*/

    //*************************Utility Functions*************************************
    public void home()
    {
        System.out.printf("home\n");
        publishLCM(Constants.HOME_POSE,Constants.HOME_SPEED);
    }/*home*/ 

    public void sleep()
    {
        try {Thread.sleep(500);} catch(InterruptedException e) {}        
    }/*sleep*/ 

    public void addCollision(double[] col) {
        collisionLocations.add(col);
    }/*addCollision*/ 

    public void grab()
    {
        double[] angles = getServoAngles();
        angles[5] = Constants.GRAB_ANGLE_CLOSED;
        publishLCM(angles,.4);
        try {
                Thread.sleep(500);
            }
        catch(InterruptedException e) {}
    }/*grab*/

    public void release()
    {
        double[] angles = getServoAngles();
        angles[5] = Constants.GRAB_ANGLE_RELEASE;
        publishLCM(angles,.2);
        try {
                Thread.sleep(500);
            }
        catch(InterruptedException e) {}
    }/*release*/

    synchronized double[] getServoAngles()
    {
        while(arm_status == null);
        double[] s_angles = new double[6];
        for(int i = 0; i < 6; i++) {
            s_angles[i] = arm_status.statuses[i].position_radians;
        }
        return s_angles;
    }/*getServoAngles*/

    public double[][] getJointXYZLocs(double[] angles) //returns the 4 joint locations+pos of end effector center
    {
        double[] loc = {0,0,0,1};                //location of base, homogeneous coords
        double[] transBy = {0,0,0};             //update this for each trans
        double[][] transMat = new double[4][4];    //use this to store prev trans/rotations
        
        double[][] joints = new double[5][4];    //stores outputted x,y,z, plus 1 in 4th dimension
        
        transMat = LinAlg.rotateZ(angles[0]);
        transBy[2] = Constants.Z_JOINTS[0];                        //z translate by L1
        transMat = LinAlg.matrixAB(transMat,LinAlg.translate(transBy));
        joints[0] = LinAlg.matrixAB(transMat,loc);
        
        for(int i = 1; i < 5; ++i) {
            if(i != 4) {        
                transMat = LinAlg.matrixAB(transMat,LinAlg.rotateY(angles[i]));
            }
            transBy[2] = Constants.Z_JOINTS[i];
            transMat = LinAlg.matrixAB(transMat,LinAlg.translate(transBy));
            joints[i] = LinAlg.matrixAB(transMat,loc);        
        }                
        return joints;
    }/*getJointXYZLocs*/

    public double[][] getArmXYZLocs(double[] angles)
    {
        double[] loc = {0,0,0,1};                //location of base, homogeneous coords
        double[] transBy = {0,0,0};             //update this for each trans
        double[][] transMat = new double[4][4];    //use this to store prev trans/rotations
        
        double[][] armPoints = new double[12][4]; //stores outputted x,y,z plust 1 in 4th dimension.
        double temp;
                
        transMat = LinAlg.rotateZ(angles[0]);
        transBy[2] = Constants.Z_JOINTS[0];                        //z translate by L1
        transMat = LinAlg.matrixAB(transMat,LinAlg.translate(transBy));
        
        for(int i = 1; i < 5; ++i) {
            if(i != 4) {
                transMat = LinAlg.matrixAB(transMat,LinAlg.rotateY(angles[i]));
            }
            temp = Constants.Z_JOINTS[i]/Constants.ARM_SEGMENTATION;
            for(int j = 0; j < Constants.ARM_SEGMENTATION; ++j) {
                transBy[2] = temp;
                transMat = LinAlg.matrixAB(transMat,LinAlg.translate(transBy));
                armPoints[(int)((i-1)*Constants.ARM_SEGMENTATION+j)] = LinAlg.matrixAB(transMat,loc);
            }        
        }
        return armPoints;        
    }/*getArmXYZLocs*/

    //*************************Print Functions***************************************
    public void printJointXYZLocs()
    {
        double[][] joints = getJointXYZLocs(getServoAngles());
        for(int i = 0; i < joints.length; i++) {
            System.out.println("Joint["+i+"]\nx: "+joints[i][0]+"\ny: "+joints[i][1]+"\nz: "+joints[i][2]);
        }
    }/*printJointXYZLocs*/

    synchronized public void printServoAngles()
    {
        while(arm_status == null);
        for (int i=0;i<6;i++)
            System.out.printf("b%d:%f\n",i,arm_status.statuses[i].position_radians);
    }/*printServoAngles*/

    //**************************Basic Laws/calculations******************************
    //Calculate angle opposite of the thrid side.
    public double lawOfCos(double a, double b, double c)
    {
        double arg = (a*a + b*b - c*c)/(2*a*b);
        return Math.acos(arg);
    }/*lawOfCos*/

    public double dist2d(double x1, double x2, double y1, double y2)
    {
        return Math.sqrt(Math.pow(x2-x1,2)+Math.pow(y2-y1,2));
    }/*dist2d*/

    public double pythag(double pos1, double pos2)
    {
        return Math.sqrt(    Math.pow(pos1, 2) + 
                            Math.pow(pos2, 2) );
    }/*pythag*/

    public double dist3D(double[] pos1, double[] pos2)
    {
        return Math.sqrt(    Math.pow(pos1[0]-pos2[0], 2) + 
                            Math.pow(pos1[1]-pos2[1], 2) + 
                            Math.pow(pos1[2]-pos2[2], 2));
    }/*dist3d*/

    public double configDistance(double[] angles0, double[] angles1)
    {
        return Math.sqrt(     Math.pow((angles0[0]-angles1[0]),2) +
                            Math.pow((angles0[1]-angles1[1]),2) +
                            Math.pow((angles0[2]-angles1[2]),2) +
                            Math.pow((angles0[3]-angles1[3]),2) );
    }/*configDistance*/

    public double parameterDistance(double[] angles0, double[] angles1)
    {
        double[][] xyz0 = getJointXYZLocs(angles0);
        double[][] xyz1 = getJointXYZLocs(angles1);
        return dist3D(xyz0[4],xyz1[4]);    
    }/*parameterDistance*/

    //************************LCM stuff********************************************
    public void publishLCM(double[] angles, double speed)
    {
        long now = TimeUtil.utime();
        dynamixel_command_list_t cmdlist = new dynamixel_command_list_t();
        cmdlist.len = 6;
        cmdlist.commands = new dynamixel_command_t[cmdlist.len];

        for (int i = 0; i < 6; i++) {
            dynamixel_command_t cmd = new dynamixel_command_t();
            cmd.position_radians = MathUtil.mod2pi(angles[i]);
            cmd.utime = now;
            cmd.speed = speed;
            cmd.max_torque = Constants.MAX_TORQUE;
            cmdlist.commands[i] = cmd;
        }
        LCM lcm = LCM.getSingleton();
        lcm.publish(Constants.ARM_BUILD_COMMAND, cmdlist);
        while(!reachedDestination(angles)){
            //System.out.println("trapped in here"); 
        }
    } /*publishLCM*/

    public boolean reachedDestination(double[] angles)
    {
        double[] currentAngles = getServoAngles();
        double val;

        //check to see if all servo's are within an allowable error of requested range.
        for(int i = 0; i < 5; i++) {
            double compare = angles[i];
            if(compare > Math.PI)
                compare = -(2*Math.PI)+compare;
            else if(compare < -Math.PI)
                compare = (2*Math.PI)+compare;    
            val = (currentAngles[i] - compare)%(2*Math.PI-0.05);  
            
            //System.out.println(val +" "+ i+" "+compare+" "+angles[i]);  
            if((val > Constants.SERVO_ERROR_RANGE) || (val < -Constants.SERVO_ERROR_RANGE)) {
                return false;
            }
        }    
        return true;
    }/*reachedDestination*/

    @Override
    public void messageReceived(LCM lcm, String channel, LCMDataInputStream dins)
    {
        try {
            arm_status = new dynamixel_status_list_t(dins);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
    }/*messageReceived*/

    public class RRT 
    {
        ArrayList<COORD> data;        //Store the tree.

        public double configDistance(double[] angles0, double[] angles1)
        {
            return Math.sqrt(     Math.pow((angles0[0]-angles1[0]),2) +
                                Math.pow((angles0[1]-angles1[1]),2) +
                                Math.pow((angles0[2]-angles1[2]),2) +
                                Math.pow((angles0[3]-angles1[3]),2) );
        }/*configDistance*/
            
        public RRT(double[] angles)
        {
            data = new ArrayList<COORD>();
            COORD c = new COORD(angles,-1);
            data.add(c);
        }
        public COORD findClosestNode(double[] angles)
        {
            double minDist = 0;
            double comp;
            int parent = 0;
            for(int i = 0; i < data.size(); ++i) {
                comp = configDistance(data.get(i).angles, angles);
                if(comp < minDist || minDist == 0) {
                    minDist = comp;
                    parent = i;             
                }
            }
            COORD c = new COORD(angles,parent);
            return c;    
        }
        public void addNode(COORD c)
        {
            data.add(c);
        }
    }

    public class COORD
    {
        int parent;
        double[] angles;
        public COORD(double[] a, int p)
        {
            angles = a;
            parent = p;
        }
    }
}/*public class Planner*/
