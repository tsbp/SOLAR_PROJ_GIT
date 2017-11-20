package com.voodoo.solar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.voodoo.solar.UDPProcessor.OnReceiveListener;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.voodoo.solar.IPHelper.getBroadcastIP4AsBytes;
import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;


public class MainActivity extends Activity implements OnReceiveListener  {

    public static UDPProcessor udpProcessor ;
    InetAddress deviceIP = null, broadcastIP;

    TextView tCompass, tAccel, tLight, tAngleV, tAngleH, tNorth, tAzimuth, tAngle;
    SeekBar sbCompass, sbAccel;

    public static double  pPitch, pRoll, pHead;

//    static byte CMD_SET_AZIMUTH = (byte)0x10;
//    static byte CMD_SET_ANGLE   = (byte)0x11;

    final byte CMD_ANGLE   = (byte)0x10;
    final byte CMD_AZIMUTH	= (byte)0x11;
    final byte CMD_LEFT	= (byte)0x20;
    final byte CMD_RIGHT	= (byte)0x21;
    final byte CMD_UP	    = (byte)0x22;
    final byte CMD_DOWN	= (byte)0x23;
    final byte CMD_STATE	= (byte)0xA0;
    final byte CMD_CFG		= (byte)0xC0;

    final byte ID_MASTER	= (byte)0x7e;
    final byte ID_SLAVE     = (byte)0x3c;


    Button btnIn1, btnIn2, btnIn3, btnIn4;

    ListView lvClients;
    int clientsIp[];
    String clientData[][];
    String [] lvStrings = {"100", "101", "102", "105", "255"};
    int [][] vals = {{1,2,3,4,5}, {1,2,3,4,5}, {1,2,3,4,5}, {10,20,30,40,50}, {0,9,8,7,6}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GLSurfaceView view = (GLSurfaceView) findViewById(R.id.w3D);
        view.setRenderer(new OpenGLRenderer());

        tCompass = (TextView) findViewById(R.id.tvCompass);
        tAccel   = (TextView) findViewById(R.id.tvAccel);
        tLight   = (TextView) findViewById(R.id.tvLight);
        tAngleV  = (TextView) findViewById(R.id.tvAngleV);
        tAngleH  = (TextView) findViewById(R.id.tvAngleH);
        tNorth   = (TextView) findViewById(R.id.tvNorth);

        tAzimuth   = (TextView) findViewById(R.id.tvAzimuth);
        tAngle   = (TextView) findViewById(R.id.tvAngle);

        sbCompass = (SeekBar) findViewById(R.id.sbCompass);
        sbAccel   = (SeekBar) findViewById(R.id.sbAccel);

        lvClients = (ListView)findViewById(R.id.lvClients);
        //lvBuid();

        btnIn1 = (Button)findViewById(R.id.btnIn1);
        btnIn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //lvBuid();

            }
        });
        btnIn2 = (Button)findViewById(R.id.btnIn2);
        btnIn3 = (Button)findViewById(R.id.btnIn3);
        btnIn4 = (Button)findViewById(R.id.btnIn4);
        btnIn4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), sunPos.class);
                startActivity(intent);

            }
        });

        udpProcessor = new UDPProcessor(7171);
        udpProcessor.setOnReceiveListener(this);
        udpProcessor.start();

        byte [] bcIP = getBroadcastIP4AsBytes();
        try {
            broadcastIP = InetAddress.getByAddress(bcIP);
        }
        catch (UnknownHostException e){}

        sendCmd((byte) 0, broadcastIP);
        //================================================
        Button btnUp = (Button) findViewById(R.id.btnUp);
        btnUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(deviceIP != null)
                    sendCmd(CMD_UP, deviceIP);
                else
                    sendCmd(CMD_UP, broadcastIP);
            }
        });
        //================================================
        Button btnDown = (Button) findViewById(R.id.btndown);
        btnDown.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(deviceIP != null)
                    sendCmd(CMD_DOWN, deviceIP);
                else
                    sendCmd(CMD_DOWN, broadcastIP);
            }
        });
        //================================================
        Button btnRight = (Button) findViewById(R.id.btnRight);
        btnRight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(deviceIP != null)
                    sendCmd(CMD_RIGHT, deviceIP);
                else
                    sendCmd(CMD_RIGHT, broadcastIP);
            }
        });
        //================================================
        Button btnLeft = (Button) findViewById(R.id.btnLeft);
        btnLeft.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(deviceIP != null)
                    sendCmd(CMD_LEFT, deviceIP);
                else
                    sendCmd(CMD_LEFT, broadcastIP);
            }
        });
        //==========================================================================================
        sbCompass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
               tAzimuth.setText("Azimyth: " + progress*3.6);
                double tmp = Math.toRadians(progress * 3.6);
                //if(tmp > Math.PI) tmp -= Math.PI * 2;
                azimuth =  (short)(tmp * 10000);

                cmdBuffer[2] = (byte) ((azimuth) & (byte)0xff);
                cmdBuffer[3] = (byte) ((azimuth >> 8) & (byte)0xff);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(deviceIP != null)
                    sendCmd(CMD_AZIMUTH, deviceIP);
                else
                    sendCmd(CMD_AZIMUTH, broadcastIP);
            }
        });
        //==========================================================================================
        sbAccel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tAngle.setText("Angle: " + progress * 0.9);
                angle =  (short)(Math.toRadians(progress * 0.9) * 10000);

                cmdBuffer[2] = (byte) ((angle) & (byte)0xff);
                cmdBuffer[3] = (byte) ((angle >> 8) & (byte)0xff);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(deviceIP != null)
                    sendCmd(CMD_ANGLE, deviceIP);
                else
                    sendCmd(CMD_ANGLE, broadcastIP);
            }
        });
        //==========================================================================================
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(deviceIP != null)
                            sendCmd(CMD_STATE, deviceIP);
                        else
                            sendCmd(CMD_STATE, broadcastIP);
                    }
                });
            }
        }, 0, 100);
    }
    //==============================================================================================
    private final String ATTRIBUTE_IP = "attr_ip";
    private final String ATTRIBUTE_V1 = "attr_v1";
    private final String ATTRIBUTE_V2 = "attr_v2";
    private final String ATTRIBUTE_V3 = "attr_v3";
    private final String ATTRIBUTE_V4 = "attr_v4";
    private final String ATTRIBUTE_V5 = "attr_v5";
    //==============================================================================================
    void lvBuid()
    {
        ArrayList<Map<String, Object>> data = new ArrayList<>(
                clientsIp.length);
        Map<String, Object> m;
        for (int i = 0; i < clientsIp.length; i++) {

            m = new HashMap<>();
            m.put(ATTRIBUTE_IP, clientsIp[i]);
            m.put(ATTRIBUTE_V1, clientData[i][0]);
            m.put(ATTRIBUTE_V2, clientData[i][1]);
            m.put(ATTRIBUTE_V3, clientData[i][2]);
            m.put(ATTRIBUTE_V4, clientData[i][3]);

            data.add(m);

        }
        String[] from = {ATTRIBUTE_IP, ATTRIBUTE_V1, ATTRIBUTE_V2, ATTRIBUTE_V3, ATTRIBUTE_V4,};
        int[] to = {R.id.i1, R.id.i2, R.id.i3, R.id.i4, R.id.i5};
        SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.client_item, from, to);
        //lvClients = (ListView) findViewById(R.id.lvClients);
        lvClients.setAdapter(sAdapter);
    }
    //==============================================================================================
    short azimuth;
    short angle;
    byte angIncrement  = 10;

    byte[] cmdBuffer = new byte[6];
    //==============================================================================================
    void sendCmd(byte aCmd, InetAddress aIP)
    {
//        cmdBuffer[0] = (byte) 0xc0;
//        cmdBuffer[1] =        aCmd;
//
//        cmdBuffer[4] = (byte) (0xcc);
//        cmdBuffer[5] = (byte) (0xcc);
        int dataLng = 0;
        byte buf[] = null;
        switch(aCmd)
        {
            case CMD_ANGLE:
                dataLng = 2;
                buf = new byte[5 + dataLng];
                buf[3] = (byte) ((angle) & (byte)0xff);
                buf[4] = (byte) ((angle >> 8) & (byte)0xff);
                break;

            case CMD_AZIMUTH:
                dataLng = 2;
                buf = new byte[5 + dataLng];
                buf[3] = (byte) ((azimuth) & (byte)0xff);
                buf[4] = (byte) ((azimuth >> 8) & (byte)0xff);
                break;

            case CMD_LEFT:
            case CMD_RIGHT:
            case CMD_UP:
            case CMD_DOWN:
                dataLng = 2;
                buf = new byte[5 + dataLng];
                buf[3] = (byte) ((angIncrement) & (byte)0xff);
                buf[4] = (byte) ((angIncrement >> 8) & (byte)0xff);
                break;

            case CMD_STATE:
                dataLng = 0;
                buf = new byte[5 + dataLng];
                break;

            case CMD_CFG:
                break;
        }

        if(aIP != null && buf != null)
        {
            buf[0] = (byte) ID_MASTER;
            buf[1] = aCmd;
            buf[2] = (byte) dataLng;

            // add crc16
            buf[dataLng + 3] = (byte) 0xcc;
            buf[dataLng + 4] = (byte) 0xcc;

            udpSend(buf, aIP);
        }
    }
    //==============================================================================================
    void udpSend(byte[] aByte, InetAddress ip)
    {
        DataFrame df = new DataFrame(aByte);
        udpProcessor.send(ip,df);
    }
    //==============================================================================================
    int cntr = 0;
    double RollAng;
    double PitchAng;
    //==============================================================================================
    short cx, cy, cz, ax, ay, az;
    //==============================================================================================
    public void onFrameReceived(InetAddress ip, IDataFrame frame)
    {
        byte[] in = frame.getFrameData();

        if(deviceIP == null) deviceIP = ip;
        try {

            if(in[0] == ID_SLAVE)
            {
                //==========================================
                if(clientsIp == null)
                {
                    clientsIp = new int[1];
                    clientsIp[0] = ip.getAddress()[3];
                    clientData = new String[1][4];
                    clientData[0][0] = "";
                }
                else
                { //add client

                }
                //==========================================
                switch(in[1])
                {
                    case CMD_ANGLE:
                        break;
                    case CMD_AZIMUTH:
                        break;
                    case CMD_LEFT:
                        break;
                    case CMD_RIGHT:
                        break;
                    case CMD_UP:
                        break;
                    case CMD_DOWN:
                        break;
                    case CMD_STATE:
                        if((in[11] & (short)1) == 1)   btnIn1.setBackgroundColor(Color.RED);
                        else                           btnIn1.setBackgroundColor(Color.GREEN);
                        if((in[11] & (short)2) == 2)   btnIn2.setBackgroundColor(Color.RED);
                        else                           btnIn2.setBackgroundColor(Color.GREEN);
                        if((in[11] & (short)4) == 4)   btnIn3.setBackgroundColor(Color.RED);
                        else                           btnIn3.setBackgroundColor(Color.GREEN);
                        if((in[11] & (short)8) == 8)   btnIn4.setBackgroundColor(Color.RED);
                        else                           btnIn4.setBackgroundColor(Color.GREEN);

                        int light = (((in[10] << 8) & 0xff00) | (int) (in[9] & 0xff));
                        tLight.setText("" + light);

                        ax = (short) (((in[4] << 8) & 0xff00) | (int) (in[3] & 0xff));
                        ay = (short) (((in[6] << 8) & 0xff00) | (int) (in[5] & 0xff));
                        az = (short) (((in[8] << 8) & 0xff00) | (int) (in[7] & 0xff));

                        tAccel.setText(ax + "\t\t\t" + ay + "\t\t\t" + az + "\t\t\t ");

                        RollAng =       (double)ax/10000;
                        PitchAng      = (double)ay/10000;
                        double north  = (double)az/10000;

                        tAngleV.setText(String.format("%.1f", Math.toDegrees(RollAng)));
                        tAngleH.setText(String.format("%.1f", Math.toDegrees(PitchAng)));

                        double tt = Math.toDegrees(north);
                        if(tt < 0) tt += 360;
                        tNorth.setText(String.format("%.1f", tt));

                        // find ip
                        for(int i = 0; i < clientsIp.length; i++)
                        {
                            if(ip.getAddress()[3] == clientsIp[i])
                            {
                               clientData[i][0] = String.format("%.1f", Math.toDegrees(RollAng));
                               clientData[i][1] = String.format("%.1f", Math.toDegrees(PitchAng));
                               clientData[i][2] = String.format("%.1f", tt);
                               lvBuid();
                            }
                        }

                        break;

                    case CMD_CFG:
                        break;
                }
            }
        }
        catch (Exception e)
        {

        }
        cntr++;
    }
    //==============================================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        udpProcessor.stop();
    }

}

class OpenGLRenderer implements GLSurfaceView.Renderer {

    private Cube mCube = new Cube();
    private float mCubeRotation;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);

        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_NICEST);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        gl.glTranslatef(0.0f, 0.0f, -10.0f);
        //gl.glRotatef(mCubeRotation, 1.0f, 1.0f, 1.0f);

        gl.glRotatef(mCubeRotation, 0.0f, 0.0f, 1.0f);
        gl.glRotatef((float)Math.toDegrees(MainActivity.pRoll), 1.0f, 0.0f, 0.0f);
        gl.glRotatef((float)Math.toDegrees(MainActivity.pHead), 0.0f, 1.0f, 0.0f);

        mCube.draw(gl);

        gl.glLoadIdentity();

        mCubeRotation = (-1) * (float) Math.toDegrees(MainActivity.pPitch);
        //mCubeRotation -= 0.15f;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 20.0f, (float)width / (float)height, 0.1f, 100.0f);
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
}

class Cube {

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mColorBuffer;
    private ByteBuffer  mIndexBuffer;

    private float vertices[] = {
            -1.0f, -0.2f, -1.0f,
            1.0f, -0.2f, -1.0f,
            1.0f,  0.2f, -1.0f,
            -1.0f, 0.2f, -1.0f,
            -1.0f, -0.2f,  1.0f,
            1.0f, -0.2f,  1.0f,
            1.0f,  0.2f,  1.0f,
            -1.0f,  0.2f,  1.0f
    };
    private float colors[] = {
            0.0f,  1.0f,  0.0f,  1.0f,
            0.0f,  1.0f,  0.0f,  1.0f,
            1.0f,  0.5f,  0.0f,  1.0f,
            1.0f,  0.5f,  0.0f,  1.0f,
            1.0f,  0.0f,  0.0f,  1.0f,
            1.0f,  0.0f,  0.0f,  1.0f,
            0.0f,  0.0f,  1.0f,  1.0f,
            1.0f,  0.0f,  1.0f,  1.0f
    };

    private byte indices[] = {
            0, 4, 5, 0, 5, 1,
            1, 5, 6, 1, 6, 2,
            2, 6, 7, 2, 7, 3,
            3, 7, 4, 3, 4, 0,
            4, 7, 6, 4, 6, 5,
            3, 0, 1, 3, 1, 2
    };

    public Cube() {
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mVertexBuffer = byteBuf.asFloatBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        byteBuf = ByteBuffer.allocateDirect(colors.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mColorBuffer = byteBuf.asFloatBuffer();
        mColorBuffer.put(colors);
        mColorBuffer.position(0);

        mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
        mIndexBuffer.put(indices);
        mIndexBuffer.position(0);
    }

    public void draw(GL10 gl) {
        gl.glFrontFace(GL10.GL_CW);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE,
                mIndexBuffer);

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
    }
}
