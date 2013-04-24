public class Constants {

    /*Arm Lengths*/
    static final double   M_PER_IN = .0254;
    static final double[] X_ARM = {.05,.045,.045,.045,.045,.050,.01,.01};
    static final double[] Y_ARM = {.04,.03,.035,.035,.035,.055,.01,.01};
    static final double[] Z_ARM = {.076,.054,.113,.110,.073,.043,.061,.078};
    static final double[] Z_JOINTS = {.117,.1025,.10,.098,.10};
    static final double ARM_SEGMENTATION = 3;
    //static final double[] COLLISION_Z_POINTS = {.0342,.033,.0326,.0266};
    static final double HEIGHT_OF_BOARD = .40;
    static final double BLOCK_RADIUS = .01796;
    static final double GRAB_ANGLE_CLOSED = 1.75;
    static final double GRAB_ANGLE_OPEN = 1.073787;
    static final double GRAB_ANGLE_RELEASE = 1.559;
    static final double[][] BASELOCS = {{0,0,0.07,1},{0,0,0.12,1}};
    static final double STRUCTURE_COLLISION_RADIUS = .06;
    static final double[][] STRUCTURE_COLLISIONS = {{-.364,0,.035,1},{.390,0,.035,1}};
    static final double LEG_COLLISIONS_RADIUS = .1;
    static final double[][] LEG_COLLISIONS = {{.30,.28,.1,1},{.30,.28,.2,1},{-.29,-.29,.1,1},{-.29,-.29,.2,1},{-.28,.28,.1,1},{-.28,.28,.2,1},{-.28,.28,.3,1}};
    static final double[] HOME_POSE = {0, 1.990826, -1.153068, -0.894882, 0, Constants.GRAB_ANGLE_OPEN};
    
    static final double[] PRE_BLOCK_ONE_POSE = {-3.061806, 1.249732, 1.988024, -1.677152, 0, Constants.GRAB_ANGLE_OPEN};
    
    static final double[] PRE_GRAB_ONE_POSE = {-3.061806, 1.249732, 1.504606, -0.973300, -0.030680, Constants.GRAB_ANGLE_OPEN};
    
    static final double[] POST_BLOCK_ONE_POSE = {-3.061806, 1.249732, 1.988024, -1.677152, 0, Constants.GRAB_ANGLE_CLOSED};
    
    static final double[] PRE_BLOCK_TWO_POSE = {-0.131187, 1.524382, 1.133121, -1.262978, 0.485761, Constants.GRAB_ANGLE_OPEN};
    static final double[] PRE_GRAB_TWO_POSE = {-0.126584, 1.596497, 0.562341, -0.705631, 0.485761, Constants.GRAB_ANGLE_OPEN};

    static final double MIN_DIST = .20;
    static final double STEP_WIDTH = .001;
    static final int STEP_SIZE = 50;
    static final int MAX_RRT_NODES = 4096;

    /*LCM Channels*/
    static final String ARM_BUILD_LISTENER    = "ARM_BUILD_LISTENER";
    static final String ARM_BUILD_COMMAND     = "ARM_BUILD_COMMAND";
    static final String ARM_PLATFORM_LISTENER = "ARM_PLATFORM_LISTENER";
    static final String ARM_PLATFORM_COMMAND  = "ARM_PLATFORM_COMMAND";

    /*Servo Constants*/
    static final double SERVO_ERROR_RANGE = 0.1;
    static final double MAX_TORQUE = 1.0;
    static final double HOME_SPEED = 0.2;

    /* Vision Constants */
    static final double SCALE_FOCUS_X = 10;
    static final double SCALE_FOCUS_Y = 10;
    static final int    IMAGE_MAT_THRESH = 10;
    static final double CANNY_MIN_THRESH = 20;
    static final double CANNY_MAX_THRESH = 60;
    static final int    CANNY_BLUR_SIZE = 4;
    static final int    DILATE_SIZE = 19;
    static final int    ERODE_SIZE = 1;

    /*DEPRECIATED*/
    static final double SMOOTHING_SIGMA_SEG     = 0.040;
    static final double SMOOTHING_SIGMA_SAMP    = 0.000;
    static final double MIN_MAGNITUDE           = 0.002;
    static final double MAX_EDGE_COST_RAD       = 0.682;
    static final double MAGNITUDE_THRESH        = 1200.0;
    static final double THETA_THRESH            = 51.7;
    static final int    ERR_RECOVERY_BITS       = 1;
    static final int    WEIGHT_SCALE            = 100;
    static final boolean SEG_DECIMATE           = true;

}
