import java.io.*;

import lcm.lcm.*;
import april.dynamixel.*;
import april.jserial.*;
import april.util.*;
import lcmtypes.*;


public class ArmPlatformDriver implements LCMSubscriber
{
    AbstractServo servos[] = new AbstractServo[2];
    LCM lcm = LCM.getSingleton();

    ExpiringMessageCache<dynamixel_command_list_t> cmdCache;

    public ArmPlatformDriver(AbstractBus bus, String chanCommand, String chanStatus)
    {
        // self-test
        for (int id = 0; id < 2; id++) {
            servos[id] = bus.getServo(id);
            if (servos[id] == null)  {
                System.out.printf("Could not communicate with servo %d\n", id);
                System.exit(-1);
            }
            System.out.printf("Servo %d : %s present!\n", id, servos[id].getClass().getSimpleName());
        }

        cmdCache = new ExpiringMessageCache<dynamixel_command_list_t>(0.25);

        lcm.subscribe(chanCommand, this);

        System.out.printf("Subscribed to channel: %s\n", chanCommand);
        new StatusThread(chanStatus).start();
    }

    public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins)
    {
        try {
            messageReceivedEx(lcm, channel, ins);
        } catch (IOException ex) {
            System.out.println("ex: "+ex);
        }
    }

    public void messageReceivedEx(LCM lcm, String channel, LCMDataInputStream ins) throws IOException
    {
        if (channel.equals(Constants.ARM_PLATFORM_COMMAND)) {
            dynamixel_command_list_t cmdlist = new dynamixel_command_list_t(ins);
            if (cmdlist.len != 2)
                System.out.println("WRN: Invalid command length received");
            else {
                synchronized(this) {
                    cmdCache.put(cmdlist, TimeUtil.utime());
                    this.notify();
                }
            }
        }
    }

    public void run()
    {
        dynamixel_command_list_t cmdlist;
        dynamixel_command_list_t lastCmdList = new dynamixel_command_list_t();
        lastCmdList.len = 2;
        lastCmdList.commands = new dynamixel_command_t[2];
        for (int id = 0; id < 2; id++)
            lastCmdList.commands[id] = null;

        while (true) {
            synchronized(this) {
                cmdlist = cmdCache.get();
                while (cmdlist == null) {
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                    }
                    cmdlist = cmdCache.get();
                }
            }
            for (int id = 0; id < cmdlist.len; id++) {
                dynamixel_command_t cmd = cmdlist.commands[id];
                dynamixel_command_t lastCmd = lastCmdList.commands[id];

                boolean update = (lastCmd == null || (cmd.utime - lastCmd.utime) > 1000000 ||
                                  lastCmd.position_radians != cmd.position_radians ||
                                  lastCmd.speed != cmd.speed ||
                                  lastCmd.max_torque != cmd.max_torque);

                if (update) {
                    servos[id].setGoal(cmd.position_radians,
                                       Math.max(0,        Math.min(1,       cmd.speed)),
                                       Math.max(0,        Math.min(1,       cmd.max_torque)));
                    lastCmdList.commands[id] = cmd;
                }
            }
        }
    }

    class StatusThread extends Thread
    {
        String channel;

        StatusThread(String channel)
        {
            this.channel = channel;
        }

        public void run()
        {
            dynamixel_status_list_t dslist = new dynamixel_status_list_t();
            dslist.len = 2;
            dslist.statuses = new dynamixel_status_t[dslist.len];
            long utime = TimeUtil.utime();
            while (true) {
                for (int id = 0; id < dslist.len; id++) {
                    dynamixel_status_t ds = new dynamixel_status_t();

                    AbstractServo.Status s = servos[id].getStatus();

                    ds.utime = TimeUtil.utime();
                    ds.error_flags = s.errorFlags;
                    ds.position_radians = s.positionRadians;
                    ds.speed = s.speed;
                    ds.load = s.load;
                    ds.voltage = s.voltage;
                    ds.temperature = s.temperature;

                    dslist.statuses[id] = ds;
                }

                lcm.publish(channel, dslist);
                double HZ = 15;
                int maxDelay = (int) (1000 / HZ);
                long now = TimeUtil.utime();
                int delay = Math.min((int)((now - utime) / 1000.0), maxDelay);
                utime = now;
                TimeUtil.sleep(maxDelay - delay);
            }
        }
    }

    public static void main(String args[]) throws IOException
    {
        GetOpt gopt = new GetOpt();
        gopt.addString('d', "device", "/dev/ttyUSB0", "USBDynamixel device path, or 'sim'");
        gopt.addString('s', "chanStatus", Constants.ARM_PLATFORM_LISTENER, "LCM channel for dynamixel_status_list_t");
        gopt.addString('c', "chanCommand", Constants.ARM_PLATFORM_COMMAND, "LCM channel for dynamixel_command_list_t");
        gopt.addBoolean('h', "help", false, "Show this help");

        if (!gopt.parse(args) || gopt.getBoolean("help")) {
            gopt.doHelp();
            return;
        }
        AbstractBus bus;
        String device = gopt.getString("device");

        if (device.equals("sim")) {

            SimBus sbus = new SimBus(10);
            sbus.addMX28(0);
            sbus.addMX28(1);
            bus = sbus;
        } else {
            JSerial js = new JSerial(device, 1000000);
            js.setCTSRTS(true);

            bus = new SerialBus(js);
        }
        new ArmPlatformDriver(bus, gopt.getString("chanCommand"), gopt.getString("chanStatus")).run();
    }
}
