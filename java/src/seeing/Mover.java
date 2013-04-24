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

public class Mover
{

    volatile double currentAngle = 0;
    double angleDelta = Math.PI/4;

    public void updatePosition()
    {
        dynamixel_command_list_t cmds = new dynamixel_command_list_t();
        cmds.len = 2;
        cmds.commands = new dynamixel_command_t[cmds.len];
        for (int i = 0; i < 2; i++) {
            dynamixel_command_t cmd = new dynamixel_command_t();
            cmd.position_radians = (i == 0) ? currentAngle : 0;
            cmd.utime = TimeUtil.utime();
            cmd.speed = Constants.HOME_SPEED;
            cmd.max_torque = Constants.MAX_TORQUE;
            cmds.commands[i] = cmd;
        }
        LCM lcm = LCM.getSingleton();
        lcm.publish(Constants.ARM_PLATFORM_COMMAND, cmds);
    }

    public void goToNext() {
        currentAngle = MathUtil.mod2pi(currentAngle + angleDelta);
    }

    public double[] getNormal() {
        return new double[]{Math.cos(currentAngle), Math.sin(currentAngle), 0};
    }

    public double[] getTangent() {
        return new double[]{Math.cos(currentAngle + angleDelta), Math.sin(currentAngle + angleDelta), 0};
    }

    public double[] getBitangent() {
        return new double[]{0, 0, 1};
    }
}
