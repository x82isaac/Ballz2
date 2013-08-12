package com.thedevel.ballz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.*;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class BallzActivity extends Activity
{

    /**
     * Eventually we will require some kind of highscore tracking. Likely to be done using Swarm.
     * Maybe a home brew system: http://wiki.unity3d.com/index.php?title=Server_Side_Highscores
     */

    //this main game view, where all the rendering happens.
    private BallzSurfaceView    gameSurface = null;

    //this is an object to be drawn on the gameSurface, which is in a different thread (yikes!)
    public volatile C_Label     lblTitleBar = new C_Label();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window w = getWindow();

        w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //setContentView(R.layout.main);
        BallzSurfaceView gameSurface = new BallzSurfaceView(this);

        lblTitleBar.Txt = String.format("%s", "Ballz");
        lblTitleBar.Y = 20;
        lblTitleBar.X = 10;
        lblTitleBar.PaintStyle.setColor(Color.BLACK);

        setContentView((View) gameSurface);
    }

    @Override
    public void onStart()
    {
        super.onStart();

    }

    @Override
    public void onRestart()
    {
        super.onRestart();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if(gameSurface != null)
        {
            gameSurface.setPaused(false);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(gameSurface != null)
        {
            gameSurface.setPaused(true);
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy()
    {
        if( gameSurface != null )
        {
            gameSurface.APP_STATE = BallzSurfaceView.APP_STATE_EXIT;
        }

        super.onDestroy();
    }

    public static int getRandomInt(int min, int max)
    {
        Random randomizer = new Random();
        return randomizer.nextInt((max - min) + 1) + min;
    }
}


// label class
class C_Label
{
    public int      X = 0;
    public int      Y = 0;
    public String   Txt = "";
    public Paint    PaintStyle = new Paint();
    public Bitmap   BitmapImage;

    //construct me
    public C_Label()
    {
        PaintStyle.setColor(Color.RED);
        PaintStyle.setAntiAlias(true);
        PaintStyle.setTextSize(25);
        PaintStyle.setTextAlign(Paint.Align.LEFT);
    }

    public void Render(Canvas c)
    {
        if((Txt != "") && (c != null))
        {
            c.drawText(this.Txt, this.X, this.Y, this.PaintStyle);
        }
    }
}


// ball class
class C_Ball
{

    public int      Damage;
    public float    X;
    public float    Y;

    public float    Diameter;

    public float    SpeedX;
    public float    SpeedY;

    public float    Gravity; /** 0.150000 - 0.800000 */

    public float    BounceDecay;

    private float   fStart; //start position
    private float   fFloor; //where is the floor located?

    private Paint   PaintStyle = new Paint();

    public C_Ball()
    {
        RandomizeBall();
    }

    public void RandomizeBall()
    {
        float fGravity;
        float fDamage;

        fGravity = (float)(BallzActivity.getRandomInt(100000, 300000) / 1.0e6);  // 0.10 - 0.40

        fDamage = (float)((BallzActivity.getRandomInt(1, 3) * 10));

        if( BallzActivity.getRandomInt(1, 25) == 13 )
        {
            /** 1:25 balls are huge (randomly) */
            fDamage = (float)40;
        }

        X = 0;
        Y = (float)(BallzActivity.getRandomInt(5, 65));

        fStart = Y;
        fFloor = BallzSurfaceView.SCREEN_H;

        Gravity = fGravity;

        SpeedX = (float)(BallzActivity.getRandomInt(2, 4));
        SpeedY = 0;

        BounceDecay = (float)(0.94);

        Diameter = fDamage;
        Damage = (int)(fDamage);

        PaintStyle.setColor(Color.BLACK);
        PaintStyle.setStyle(Paint.Style.STROKE);
        PaintStyle.setStrokeWidth(2);
        PaintStyle.setAntiAlias(true);
    }

    public void Update()
    {
        if ((Y + Diameter) > fFloor)
        {
            Y = (fFloor - Diameter);
            SpeedY = -(SpeedY * BounceDecay);
        }

        SpeedY = SpeedY + Gravity;

        Y = Y + SpeedY;
        X = X + SpeedX;
    }

    public void Render(Canvas c)
    {
        if (c != null)
        {
            c.drawCircle(this.X, this.Y, this.Diameter, this.PaintStyle);
        }
    }
}


class C_Actor
{

    public float                X;
    public float                Y;
    public float                H; //height
    public float                W; //width

    public Paint                PaintStyle = new Paint();

    public static final int     MOVE_NONE       = 0;
    public static final int     MOVE_LEFT       = 1;
    public static final int     MOVE_RIGHT      = 2;

    public int                  MOVE_DIR        = MOVE_NONE;

    public C_Actor()
    {
        PaintStyle.setColor(Color.MAGENTA);
        PaintStyle.setStyle(Paint.Style.STROKE);
        PaintStyle.setStrokeWidth(3);
        PaintStyle.setAntiAlias(true);
    }

    public void Render(Canvas c)
    {
        if (c != null)
        {
            if(MOVE_DIR == MOVE_LEFT)
            {
                this.PaintStyle.setColor(Color.GREEN);
            } else if(MOVE_DIR == MOVE_RIGHT) {
                this.PaintStyle.setColor(Color.MAGENTA);
            } else {
                this.PaintStyle.setColor(Color.BLACK);
            }

            c.drawCircle(this.X, this.Y, 25, this.PaintStyle);
        }
    }
}


class BallzRenderer implements Runnable
{
    public BallzSurfaceView gameSurface = null;

    public BallzRenderer(BallzSurfaceView b)
    {
        gameSurface = b;
    }

    @Override
    public void run()
    {
        //To change body of implemented methods use File | Settings | File Templates.

        while ( gameSurface.APP_STATE != BallzSurfaceView.APP_STATE_EXIT )
        {
            try
            {
                gameSurface.RenderLoop();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


class BallzLogic implements Runnable
{

    //public boolean isRunning = true;
    public BallzSurfaceView gameSurface = null;

    private final int       MAX_TICKS = 60;
    private final int       SKIP_INTERVAL = 1000 / MAX_TICKS;

    private long            nextTick = 0;
    private long            sleepTime = 0;

    private long            totalLoops = 0;

    //if we average the sleep timer it might provide a smoother experience.
    private long            sleepTimeAvg = 0;

    public BallzLogic(BallzSurfaceView b)
    {
        gameSurface = b;
    }

    public void run()
    {
        if(gameSurface == null)
        {
            //exit if we have no game surface
            return;
        }

        nextTick = System.currentTimeMillis();
        sleepTime = nextTick;
        sleepTimeAvg = sleepTime;

        while ( gameSurface.APP_STATE != BallzSurfaceView.APP_STATE_EXIT )
        {

            //Improved frame rate delay, thanks to Joshua J. Kincaid M.Sc., Oregon State University

            totalLoops += 1;
            nextTick += SKIP_INTERVAL;
            sleepTime = (nextTick - System.currentTimeMillis());
            sleepTimeAvg = ((((totalLoops - 1) * sleepTimeAvg) + sleepTime) / totalLoops);

            if( sleepTimeAvg > 0 )
            {
                synchronized (Thread.currentThread())
                {
                    try
                    {
                        Thread.currentThread().wait( sleepTimeAvg );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {

            }

            switch (gameSurface.APP_STATE)
            {
                case BallzSurfaceView.APP_STATE_PLAYING:
                {
                    gameSurface.Logic();
                    break;
                }
                case BallzSurfaceView.APP_STATE_EXIT:
                {
                    return;
                }
                case BallzSurfaceView.APP_STATE_PAUSED:
                {
                    //pass
                }
            }
        }
    }
}


class BallzSurfaceView extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener
{

    // states of the game
    public static final int     APP_STATE_PAUSED    = 0;
    public static final int     APP_STATE_PLAYING   = 1;
    public static final int     APP_STATE_DEAD      = 2;
    public static final int     APP_STATE_EXIT      = 4;

    // drop a new ball in play for each APP_DROP_ON_SCORE
    public volatile int         APP_DROP_ON_SCORE   = 10;

    // our current score
    public volatile int         APP_SCORE           = 0;

    // what game state are we presently in
    public volatile int         APP_STATE           = APP_STATE_PLAYING;

    private SurfaceHolder       sv_Holder;
    private Canvas              sv_Canvas;
    private boolean             sv_bHaveSurface;
    private DisplayMetrics      sv_Screen;

    private volatile C_Label    lblClock = new C_Label();
    private volatile C_Label    lblBallz = new C_Label();

    private C_Actor             Andrizzt = new C_Actor();

    private ArrayList<C_Ball>   BallArray = new ArrayList<C_Ball>();
    private Timer               BallDropTimer = new Timer();

    public BallzActivity        ballzActivity = null;

    public BallzLogic           ballzLogic = new BallzLogic(this);
    public BallzRenderer        ballzRenderer = new BallzRenderer(this);

    public Thread               ballzRendererThread = new Thread(ballzRenderer);
    public Thread               ballzLogicThread = new Thread(ballzLogic);

    //from the Android API reference
    private SensorManager       mSensorManager;
    private Sensor              mSensor;

    public static int           SCREEN_W = 0;
    public static int           SCREEN_H = 0;

    private long                currentFrameTime = System.currentTimeMillis();
    private long                currentFrameCount = 0;
    private C_Label             lblSensor = new C_Label();

    public BallzSurfaceView(Context context) {
        super(context);

        //this is the activity that created this SurfaceView
        ballzActivity = (BallzActivity)context;

        sv_Holder = this.getHolder();
        sv_Holder.addCallback(this);

        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensor, 100000);  //microsecond delay

        //10000 microsecond in millisecond

        sv_Screen = new DisplayMetrics();

        ballzActivity.getWindowManager().getDefaultDisplay().getMetrics(sv_Screen);

        SCREEN_W = sv_Screen.widthPixels;  //x axis
        SCREEN_H = sv_Screen.heightPixels; //y axis

        BallDropTimer.schedule(new C_DropBall(), BallzActivity.getRandomInt(1000, 2000));
        BallDropTimer.schedule(new C_DropBall(), BallzActivity.getRandomInt(1000, 2000));
        BallDropTimer.schedule(new C_DropBall(), BallzActivity.getRandomInt(1000, 2000));
        BallDropTimer.schedule(new C_DropBall(), BallzActivity.getRandomInt(1000, 2000));
        BallDropTimer.schedule(new C_DropBall(), BallzActivity.getRandomInt(1000, 2000));
        BallDropTimer.schedule(new C_DropBall(), BallzActivity.getRandomInt(1000, 2000));

        lblClock.Y = 40;
        lblClock.X = 10;

        lblSensor.Y = 60;
        lblSensor.X = 10;

        lblBallz.Y = 80;
        lblBallz.X = 10;

        Andrizzt.H = 20;
        Andrizzt.W = 20;
        Andrizzt.X = (BallzSurfaceView.SCREEN_W / 2);
        Andrizzt.Y = (BallzSurfaceView.SCREEN_H - Andrizzt.H);


        ballzLogicThread.start();
        ballzRendererThread.start();
    }

    public void Logic()
    {
        if(APP_STATE == APP_STATE_PAUSED)
        {
            return;
        }

        lblBallz.Txt = String.format("%s : %s", "Ballz", BallArray.size());

        for (int x = 0; (x < BallArray.size()); x++)
        {
            //a ball leaves the sceen, make a new one (twice).
            try
            {
                if (BallArray.get(x).X > SCREEN_W)
                {
                    BallArray.remove(x);
                    BallDropTimer.schedule(new C_DropBall(), BallzActivity.getRandomInt(1000, 2000));
                    continue;
                }

                BallArray.get(x).Update();
            } catch (NullPointerException e) {
                //wtf?
                System.out.print(e.getStackTrace());
            }

        }
    }

    public void RenderLoop()
    {
        if(APP_STATE == APP_STATE_PAUSED)
        {
            return;
        }

        if(!sv_bHaveSurface)
        {
            return;
        }

        sv_Canvas = sv_Holder.lockCanvas();

        if(sv_Canvas != null)
        {
            currentFrameCount += 1;

            if((System.currentTimeMillis() - currentFrameTime) >= 1000)
            {
                lblClock.Txt = String.format("FPS : %s", currentFrameCount);
                lblClock.Render(sv_Canvas);

                currentFrameTime = System.currentTimeMillis();
                currentFrameCount = 0;
            }

            // lock the canvas for drawing
            sv_Canvas.drawColor(Color.WHITE);

            ballzActivity.lblTitleBar.Render(sv_Canvas);

            for (int x = 0; (x < BallArray.size()); x++)
            {
                BallArray.get(x).Render(sv_Canvas);
            }

            lblBallz.Render(sv_Canvas);
            lblClock.Render(sv_Canvas);
            lblSensor.Render(sv_Canvas);

            Andrizzt.Render(sv_Canvas);

            sv_Holder.unlockCanvasAndPost(sv_Canvas);
        }
    }

    private class C_DropBall extends TimerTask
    {
        @Override
        public void run() {
            BallArray.add(new C_Ball());
        }
    }

    public void setPaused(boolean b)
    {
        if(b == true)
        {
            APP_STATE = APP_STATE_PAUSED;
            synchronized (ballzLogicThread)
            {
                try
                {
                    ballzLogicThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            APP_STATE = APP_STATE_PLAYING;
            synchronized (ballzLogicThread)
            {
                ballzLogicThread.notifyAll();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //pass
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        sv_bHaveSurface = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        sv_bHaveSurface = false;

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {

        /**
         * Tilted right are positive values, Left are negative.
         * If we drop the floating point and use just the integer value
         * the incremental changes are absolute values between 1-6
         * Anything beyond 6 is extremely tilted.
         */
        int x = Math.round(sensorEvent.values[1]);

        if(x < 0)
        {
            Andrizzt.MOVE_DIR = C_Actor.MOVE_LEFT;
        } else if(x > 0) {
            Andrizzt.MOVE_DIR = C_Actor.MOVE_RIGHT;
        } else {
            Andrizzt.MOVE_DIR = C_Actor.MOVE_NONE;
        }

        lblSensor.Txt = String.format("%s : %s", "Tilt", x);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {
        //pass
    }
}