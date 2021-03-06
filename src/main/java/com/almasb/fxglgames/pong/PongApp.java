/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.almasb.fxglgames.pong;

import com.almasb.fxgl.animation.Interpolators;
import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.core.math.FXGLMath;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.net.*;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.ui.UI;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.almasb.fxglgames.pong.NetworkMessages.*;

/**
 * A simple clone of Pong.
 * Sounds from https://freesound.org/people/NoiseCollector/sounds/4391/ under CC BY 3.0.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class PongApp extends GameApplication implements MessageHandler<String> {

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Tank Battle");
        settings.setVersion("1.0");
        settings.setFontUI("UniversCondensed.ttf");
        settings.setApplicationMode(ApplicationMode.DEBUG);
    }

    private Entity player1;
    private Entity player2;
    private BatComponent player1Bat;
    private BatComponent player2Bat;
    private int playerConnectionFlag = 0;
    private int enemyConnectionFlag = 0;
    private int playerInputFlag = 0;
    private int enemyInputFlag = 0;
    private int playerBallFlag = 0;
    private int enemyBallFlag = 0;

    private Server<String> server;

    @Override
    protected void initInput() {
        /**Player controls including LR movement and firing.
         *
         * @author
         * E.R.Walker (E.walker5@uni.brighton.ac.uk)
         */
        getInput().addAction(new UserAction("Left"){
            @Override
            protected void onAction(){
                if(playerConnectionFlag == 1 && playerInputFlag == 1){
                    player1Bat.left();
                }

                if (enemyConnectionFlag == 1 && enemyInputFlag == 1){
                    player2Bat.left();
                }
            }

            @Override
            protected void onActionEnd(){
                if(playerConnectionFlag == 1 && playerInputFlag == 0){
                    System.out.println("Player 1 told to stop moving left.");
                    player1Bat.stop();
                }

                if (enemyConnectionFlag == 1  && enemyInputFlag == 0){
                    System.out.println("Player 2 told to stop moving left.");
                    player2Bat.stop();
                }
            }
        }, KeyCode.A);

        getInput().addAction(new UserAction("Right"){
            @Override
            protected void onAction(){
                if(playerConnectionFlag == 1 && playerInputFlag == 1){
                    player1Bat.right();
                }

                if (enemyConnectionFlag == 1  && enemyInputFlag == 1){
                    player2Bat.right();
                }
            }

            @Override
            protected void onActionEnd(){
                if(playerConnectionFlag == 1 && playerInputFlag == 0){
                    System.out.println("Player 1 told to stop moving right.");
                    player1Bat.stop();
                }

                if (enemyConnectionFlag == 1  && enemyInputFlag == 0){
                    System.out.println("Player 2 told to stop moving right.");
                    player2Bat.stop();
                }
            }
        }, KeyCode.D);

        getInput().addAction(new UserAction("Fire") {
            @Override
            protected void onActionBegin(){
                if(playerConnectionFlag == 1 && playerInputFlag == 1){
                    player1Bat.fire();
                    server.broadcast(BAT1_FIRED_BALL);
                }

                if (enemyConnectionFlag == 1 && enemyInputFlag == 1){
                    player2Bat.fire();
                    server.broadcast(BAT2_FIRED_BALL);
                }
            }
        }, KeyCode.W);
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("player1score", 0);
        vars.put("player2score", 0);
    }

    @Override
    protected void initGame() {
        // creates a writer and reader for the server to use when communicating with clients.
        Writers.INSTANCE.addTCPWriter(String.class, outputStream -> new MessageWriterS(outputStream));
        Readers.INSTANCE.addTCPReader(String.class, in -> new MessageReaderS(in));

        // initializes the server.
        server = getNetService().newTCPServer(55555, new ServerConfig<>(String.class));

        // Detects when a client connects to the server.
        server.setOnConnected(connection -> {
            connection.addMessageHandlerFX(this);
            server.broadcast("Number of players connected is now: " + server.getConnections().size());

            if(server.getConnections().size() == 1){
                playerConnectionFlag = 1;
                connection.send(PLAYER1_CONNECT);
            } else if (server.getConnections().size() == 2){
                enemyConnectionFlag = 1;
                connection.send(PLAYER2_CONNECT);
            }
        });

        getGameWorld().addEntityFactory(new PongFactory());
        getGameScene().setBackgroundColor(Color.rgb(100, 100, 100));

        initScreenBounds();
        initGameObjects();

        var t = new Thread(server.startTask()::run);
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 0);

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.WALL) {
            protected void onHitBoxTrigger(Entity a, Entity b, HitBox boxA, HitBox boxB) {
                if (boxB.getName().equals("BOT")) {
                    server.broadcast(HIT_WALL_DOWN);
                } else if (boxB.getName().equals("TOP")) {
                    server.broadcast(HIT_WALL_UP);
                } else if (boxB.getName().equals("LEFT")) {
                    server.broadcast(HIT_WALL_LEFT);
                } else if (boxB.getName().equals("RIGHT")) {
                    server.broadcast(HIT_WALL_RIGHT);
                }
            }
        });

        CollisionHandler ballBatHandler = new CollisionHandler(EntityType.BALL, EntityType.PLAYER_BAT) {
            /**Handles ball on bat (bullet on tank) collisions.
             * When a ball hits the opposing player a point is
             * rewarded to the player and the ball is removed
             * from the game world.
             *
             * @author
             * E.R.Walker (E.walker5@uni.brighton.ac.uk)
             */
            @Override
            protected void onCollisionBegin(Entity ball, Entity bat) {
                if( bat == player1  &&
                    ball == player2Bat.ball){
                    ball.removeFromWorld();
                    getGameScene().getViewport().shakeTranslational(5);
                    inc("player2score", +1);
                    player2Bat.reload();
                } else if(  bat == player2  &&
                            ball == player1Bat.ball) {
                    ball.removeFromWorld();
                    getGameScene().getViewport().shakeTranslational(5);
                    inc("player1score", +1);
                    player1Bat.reload();
                }
                playHitAnimation(bat);
                server.broadcast(bat == player1 ? BALL_HIT_BAT1 : BALL_HIT_BAT2);
            }
        };

        getPhysicsWorld().addCollisionHandler(ballBatHandler);
        getPhysicsWorld().addCollisionHandler(ballBatHandler.copyFor(EntityType.BALL, EntityType.ENEMY_BAT));
    }

    @Override
    protected void initUI() {
        MainUIController controller = new MainUIController();
        UI ui = getAssetLoader().loadUI("main.fxml", controller);

        controller.getLabelScorePlayer().textProperty().bind(getip("player1score").asString());
        controller.getLabelScoreEnemy().textProperty().bind(getip("player2score").asString());

        getGameScene().addUI(ui);
    }

    //Sends server data to the client(s) via a message.
    @Override
    protected void onUpdate(double tpf) {
        System.out.println("Server connections: " + server.getConnections().size());

        if (!server.getConnections().isEmpty()) {

            var message = "GAME_DATA,"
                    + player1.getX()
                    + ","
                    + player2.getX()
                    + ","
                    + getip("player1score").get()
                    + ","
                    + getip("player2score").get()
                    + ","
                    + playerBallFlag
                    + ","
                    + enemyBallFlag;

            /**Concatenates the message for the balls only
             * once they've been spawned (not null). This
             * avoids a NullPointerException being thrown
             * by the JRE during client runtime.
             *
             * @author
             * E.R.Walker (E.walker5@uni.brighton.ac.uk)
             */
            if(player1Bat.ball != null){
                playerBallFlag = 1;
                message += ","
                        + player1Bat.ball.getX()
                        + ","
                        + player1Bat.ball.getY();
            } else {
                playerBallFlag = 0;
            }

            if(player2Bat.ball != null){
                enemyBallFlag = 1;
                message += ","
                        + player2Bat.ball.getX()
                        + ","
                        + player2Bat.ball.getY();
            } else {
                enemyBallFlag = 0;
            }
            server.broadcast(message);
        }
    }

    private void initScreenBounds() {
        Entity walls = entityBuilder()
                .type(EntityType.WALL)
                .collidable()
                .buildScreenBounds(150);

        getGameWorld().addEntity(walls);
    }

    private void initGameObjects() {
        player1 = spawn("bat", new SpawnData(getAppWidth() / 2, getAppHeight() - 120).put("isPlayer", true));
        player2 = spawn("bat", new SpawnData(getAppWidth() / 2, 30).put("isPlayer", false));

        player1Bat = player1.getComponent(BatComponent.class);
        player2Bat = player2.getComponent(BatComponent.class);
        /**Initializes the firing offsets and firing
         * velocities of the player bats.
         *
         * @author
         * E.R.Walker (E.walker5@uni.brighton.ac.uk)
         */
        player1Bat.initFiringOffsetX((int)player1.getBoundingBoxComponent().getWidth()/2 - 7);
        player2Bat.initFiringOffsetX((int)player2.getBoundingBoxComponent().getWidth()/2 - 7);
        player1Bat.initFiringOffsetY(-(int)player1.getBoundingBoxComponent().getHeight() + 80);
        player2Bat.initFiringOffsetY((int)player2.getBoundingBoxComponent().getHeight() + 10);
        player1Bat.initFiringVelocityY(-1000);
        player2Bat.initFiringVelocityY(1000);
        player1Bat.reload();
        player2Bat.reload();

        //alternative to rotating the bounding box 180 degrees
        player1.getBoundingBoxComponent().transform.setScaleX(-1);
        player1.getBoundingBoxComponent().transform.setScaleY(-1);
    }

    private void playHitAnimation(Entity bat) {
        animationBuilder()
                .autoReverse(true)
                .duration(Duration.seconds(0.5))
                .interpolator(Interpolators.BOUNCE.EASE_OUT())
                .rotate(bat)
                .from(FXGLMath.random(-25, 25))
                .to(0)
                .buildAndPlay();
    }

    //Processes input from the client(s) and maps it to input here on the server.
    @Override
    public void onReceive(Connection<String> connection, String message) {
        var tokens = message.split(",");
        Arrays.stream(tokens).skip(1).forEach(key -> {
            //Detects input from a client based on their connection number.

            if (key.endsWith("_PLAYERDOWN")) {
                playerInputFlag = 1;
                getInput().mockKeyPress(KeyCode.valueOf(key.substring(0, 1)));
            } else if (key.endsWith("_PLAYERUP")) {
                playerInputFlag = 0;
                getInput().mockKeyRelease(KeyCode.valueOf(key.substring(0, 1)));
            } else if(key.endsWith("PLAYERQUIT")){
                connection.send(PLAYER1_QUIT);
                connection.terminate();
                System.out.println("Player 1 Quit.");
            }

            if(key.endsWith("_ENEMYDOWN")){
                enemyInputFlag = 1;
                getInput().mockKeyPress(KeyCode.valueOf(key.substring(0, 1)));
            } else if(key.endsWith("_ENEMYUP")){
                enemyInputFlag = 0;
                getInput().mockKeyRelease(KeyCode.valueOf(key.substring(0, 1)));
            } else if(key.endsWith("ENEMYQUIT")){
                connection.send(PLAYER2_QUIT);
                connection.terminate();
                System.out.println("Player 2 Quit.");
            }
        });
    }

    static class MessageWriterS implements TCPMessageWriter<String> {

        private OutputStream os;
        private PrintWriter out;

        MessageWriterS(OutputStream os) {
            this.os = os;
            out = new PrintWriter(os, true);
        }

        @Override
        public void write(String s) throws Exception {
            out.print(s.toCharArray());
            out.flush();
        }
    }

    static class MessageReaderS implements TCPMessageReader<String> {

        private BlockingQueue<String> messages = new ArrayBlockingQueue<>(50);

        private InputStreamReader in;

        MessageReaderS(InputStream is) {
            in =  new InputStreamReader(is);

            var t = new Thread(() -> {
                try {

                    char[] buf = new char[36];

                    int len;

                    while ((len = in.read(buf)) > 0) {
                        var message = new String(Arrays.copyOf(buf, len));

                        System.out.println("Recv message: " + message);

                        messages.put(message);
                    }

                } catch (Exception e) {
                    System.out.println("Warning: Connection could not be read.");
                    // Once length of the buffer is 0, this implies a connection has been terminated.
                    // The last message they would have send would have been a quit message to trigger the disconnect.
                    messages.add("QUIT");
                }
            });

            t.setDaemon(true);
            t.start();
        }

        @Override
        public String read() throws Exception {
            // Assigns the message at the end of the queue to a String variable and compares it to the QUIT command
            String message = messages.take();
            if(message.equals("QUIT")){
                System.out.println("Handling EOFException.");
                throw new EOFException();
            }
            return message;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
