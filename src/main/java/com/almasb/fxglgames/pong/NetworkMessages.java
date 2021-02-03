package com.almasb.fxglgames.pong;

/**
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public class NetworkMessages {

    public static final String HIT_WALL_LEFT = "HIT_WALL_LEFT";
    public static final String HIT_WALL_RIGHT = "HIT_WALL_RIGHT";
    public static final String HIT_WALL_UP = "HIT_WALL_UP";
    public static final String HIT_WALL_DOWN = "HIT_WALL_DOWN";


    public static final String BALL_HIT_BAT1 = "BALL_HIT_BAT1";
    public static final String BALL_HIT_BAT2 = "BALL_HIT_BAT2";

    /**Extended network messages which enable the server to
     * inform a client when they have fired a ball and when
     * they have chosen to quit the session.
     *
     * @author
     * E.R.Walker (E.walker5@uni.brighton.ac.uk)
     */
    public static final String BAT1_FIRED_BALL = "BAT1_FIRED_BALL";
    public static final String BAT2_FIRED_BALL = "BAT2_FIRED_BALL";

    public static final String PLAYER1_CONNECT = "PLAYER1_CONNECT";
    public static final String PLAYER2_CONNECT = "PLAYER2_CONNECT";

    public static final String PLAYER1_QUIT  = "PLAYER1_QUIT";
    public static final String PLAYER2_QUIT  = "PLAYER2_QUIT";
}
