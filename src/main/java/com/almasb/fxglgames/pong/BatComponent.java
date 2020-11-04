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

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;

import static com.almasb.fxgl.dsl.FXGL.spawn;

/**
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class BatComponent extends Component {

    private static final double BAT_SPEED = 420;

    protected PhysicsComponent physics;
    protected Entity ball;
    protected int firingOffsetY;
    protected double firingVelocityY;

    /**
     * Player bat.
     *
     * @author
     * E.R.Walker (E.walker5@uni.brighton.ac.uk)
     */
    public void left(){
        if(entity.getX() >= BAT_SPEED / 60)
            physics.setVelocityX(-BAT_SPEED);
        else
            stop();
    }

    public void right(){
        if(entity.getRightX() <= FXGL.getAppWidth() - (BAT_SPEED / 60))
            physics.setVelocityX(BAT_SPEED);
        else
            stop();
    }

    public void stop() {
        physics.setLinearVelocity(0, 0);
    }

    public void reload(){
        ball = null;
    }

    public void fire(){
        if(ball == null){
            ball = spawn("ball",
                    this.physics.getEntity().getX(),
                    (this.physics.getEntity().getY() + firingOffsetY));
            ball.getComponent(BallComponent.class).initVelocity(firingVelocityY);
        }
    }

    public void initFiringOffsetY(int offsetY){
        firingOffsetY = offsetY;
    }

    public void initFiringVelocityY(double velocityY){
        firingVelocityY = velocityY;
    }
}
