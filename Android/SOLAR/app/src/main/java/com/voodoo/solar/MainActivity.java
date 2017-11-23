package com.voodoo.solar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
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

public class MainActivity extends Activity implements OnReceiveListener  {



    public static double  pPitch, pRoll, pHead;

    public static UDPProcessor udpProcessor ;
    InetAddress deviceIP = null, broadcastIP;




    ListView lvClients;
    byte clientsIp[];
    String clientData[][];
    int currentClient = 0;
    int notAsweredCntr[] = new int[100];

    public static int clientActivityCreated = 0;

    public final static String BROADCAST_ACTION = "broadcast Cient data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        lvClients = (ListView)findViewById(R.id.lvClients);

        udpProcessor = new UDPProcessor(7171);
        udpProcessor.setOnReceiveListener(this);
        udpProcessor.start();

        byte [] bcIP = getBroadcastIP4AsBytes();
        try {
            broadcastIP = InetAddress.getByAddress(bcIP);
        }
        catch (UnknownHostException e){}

        //sendCmd((byte) 0, broadcastIP);


        //================================================
        Button bSunCalc = (Button) findViewById(R.id.bSet);
        bSunCalc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), sunPos.class);
                startActivity(intent);
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

                        if(deviceIP == null)
                            UDPCommands.sendCmd(UDPCommands.CMD_STATE, (short)0, (short)0, (byte)0, broadcastIP);
                        else
                            try
                            {
                                byte [] addr = broadcastIP.getAddress();

                                if(currentClient < clientsIp.length) currentClient++;
                                if(currentClient >= clientsIp.length)
                                {
                                    addr[3] = (byte)255;
                                    currentClient = -1;
                                }
                                else
                                {
                                    addr[3] = (byte) clientsIp[currentClient];
                                    notAsweredCntr[currentClient]++;
                                }

                                UDPCommands.sendCmd(UDPCommands.CMD_STATE, (short)0, (short)0, (byte)0, InetAddress.getByAddress(addr));

                                // check for not answered

                                for(int i = 0; i < clientsIp.length; i++)
                                    if(notAsweredCntr[i] > 10) deleteListRow(i);

                            }
                            catch (UnknownHostException e){}


                    }
                });
            }
        }, 0, 200);
    }
    //==============================================================================================
    int selectedClient = 0;
    //==============================================================================================
    void sendIntent()
    {
        Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
        intent.putExtra(ClientConfig.PARAM_PITCH, clientData[selectedClient][0]);
        intent.putExtra(ClientConfig.PARAM_ROLL,  clientData[selectedClient][1]);
        intent.putExtra(ClientConfig.PARAM_HEAD,  clientData[selectedClient][2]);
        intent.putExtra(ClientConfig.PARAM_LIGTH, clientData[selectedClient][3]);
        intent.putExtra(ClientConfig.PARAM_TERM,  clientData[selectedClient][4]);
        sendBroadcast(intent);
    }
    //==============================================================================================
    void deleteListRow(int aRow)
    {
        {
            for (int i = aRow; i < clientsIp.length - 1; i++)
                clientsIp[i] = clientsIp[i + 1];

            byte tmp[] = new byte[clientsIp.length - 1];
            for (int i = 0; i < tmp.length; i++)
                tmp[i] = clientsIp[i];
            clientsIp = new byte[clientsIp.length - 1];
            clientsIp = tmp;

            clientData = new String[clientData.length - 1][4];
            if(clientsIp.length == 0) lvBuid();
        }
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
            m.put(ATTRIBUTE_IP, clientsIp[i] & 0xff);
            m.put(ATTRIBUTE_V1, clientData[i][0]);
            m.put(ATTRIBUTE_V2, clientData[i][1]);
            m.put(ATTRIBUTE_V3, clientData[i][2]);
            m.put(ATTRIBUTE_V4, clientData[i][3]);
            m.put(ATTRIBUTE_V5, clientData[i][4]);
            data.add(m);

        }
        String[] from = {ATTRIBUTE_IP, ATTRIBUTE_V1, ATTRIBUTE_V2, ATTRIBUTE_V3, ATTRIBUTE_V4, ATTRIBUTE_V5};
        int[] to = {R.id.i1, R.id.i2, R.id.i3, R.id.i4, R.id.i5, R.id.i6};
        SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.client_item, from, to);
        //lvClients = (ListView) findViewById(R.id.lvClients);
        lvClients.setAdapter(sAdapter);
        //==========================================================
        lvClients.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id)
            {
                byte [] ip = getBroadcastIP4AsBytes();
                ip[3] = clientsIp[position];
                try
                {
                    ClientConfig.ip = InetAddress.getByAddress(ip);
                }
                catch (UnknownHostException e){}
                clientActivityCreated = 1;
                selectedClient = position;
                Intent intent = new Intent(MainActivity.this, ClientConfig.class);
                startActivity(intent);
            }
        });
    }
    //==============================================================================================


//    byte[] cmdBuffer = new byte[6];
//    //==============================================================================================
//    public  void sendCmd(byte aCmd, InetAddress aIP)
//    {
////        cmdBuffer[0] = (byte) 0xc0;
////        cmdBuffer[1] =        aCmd;
////
////        cmdBuffer[4] = (byte) (0xcc);
////        cmdBuffer[5] = (byte) (0xcc);
//        int dataLng = 0;
//        byte buf[] = null;
//        switch(aCmd)
//        {
//            case CMD_ANGLE:
//                dataLng = 2;
//                buf = new byte[5 + dataLng];
//                buf[3] = (byte) ((angle) & (byte)0xff);
//                buf[4] = (byte) ((angle >> 8) & (byte)0xff);
//                break;
//
//            case CMD_AZIMUTH:
//                dataLng = 2;
//                buf = new byte[5 + dataLng];
//                buf[3] = (byte) ((azimuth) & (byte)0xff);
//                buf[4] = (byte) ((azimuth >> 8) & (byte)0xff);
//                break;
//
//            case CMD_LEFT:
//            case CMD_RIGHT:
//            case CMD_UP:
//            case CMD_DOWN:
//                dataLng = 2;
//                buf = new byte[5 + dataLng];
//                buf[3] = (byte) ((angIncrement) & (byte)0xff);
//                buf[4] = (byte) ((angIncrement >> 8) & (byte)0xff);
//                break;
//
//            case CMD_STATE:
//                dataLng = 0;
//                buf = new byte[5 + dataLng];
//                break;
//
//            case CMD_CFG:
//                break;
//        }
//
//        if(aIP != null && buf != null)
//        {
//            buf[0] = ID_MASTER;
//            buf[1] = aCmd;
//            buf[2] = (byte) dataLng;
//
//            // add crc16
//            buf[dataLng + 3] = (byte) 0xcc;
//            buf[dataLng + 4] = (byte) 0xcc;
//
//            udpSend(buf, aIP);
//        }
//    }
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

            if(in[0] == UDPCommands.ID_SLAVE)
            {
                //==========================================
                if(clientsIp == null)
                {
                    clientsIp = new byte[1];
                    clientsIp[0] = (byte)((int)ip.getAddress()[3]);
                    clientData = new String[1][5];
                    clientData[0][0] = "";
                }
                else
                { //add client
                    int found = 0;
                    for(int i = 0; i < clientsIp.length; i++)
                        if(ip.getAddress()[3] == (byte)clientsIp[i]) found = 1;
                    if(found == 0)
                    {
                        byte tmp [];
                        tmp = clientsIp;
                        clientsIp = new byte[tmp.length +1];
                        for(int i = 0; i < tmp.length; i++)
                            clientsIp[i] = tmp[i];
                        clientsIp[clientsIp.length - 1] = ip.getAddress()[3];

                        //String tmpd[][] = clientData;
                        clientData = new String[clientData.length + 1][5];

//                        for(int i = 0; i < clientsIp.length; i++)
//                            for(int j = 0; j < 4; j++)
//                                clientData[i][j] = tmpd[i][j];
                    }
                }
                //==========================================
                switch(in[1])
                {
                    case UDPCommands.CMD_ANGLE:
                        break;
                    case UDPCommands.CMD_AZIMUTH:
                        break;
                    case UDPCommands.CMD_LEFT:
                        break;
                    case UDPCommands.CMD_RIGHT:
                        break;
                    case UDPCommands.CMD_UP:
                        break;
                    case UDPCommands.CMD_DOWN:
                        break;
                    case UDPCommands.CMD_STATE:

                        String terms = "";
                        if((in[11] & (short)1) == 1)   terms += "1"; //btnIn1.setBackgroundColor(Color.RED);
                        else                           terms += "0"; //btnIn1.setBackgroundColor(Color.GREEN);
                        if((in[11] & (short)2) == 2)   terms += "1"; //btnIn2.setBackgroundColor(Color.RED);
                        else                           terms += "0"; //btnIn2.setBackgroundColor(Color.GREEN);
                        if((in[11] & (short)4) == 4)   terms += "1"; //btnIn3.setBackgroundColor(Color.RED);
                        else                           terms += "0"; //btnIn3.setBackgroundColor(Color.GREEN);
                        if((in[11] & (short)8) == 8)   terms += "1"; //btnIn4.setBackgroundColor(Color.RED);
                        else                           terms += "0"; //btnIn4.setBackgroundColor(Color.GREEN);

                        int light = (((in[10] << 8) & 0xff00) | in[9] & 0xff);

                        ax = (short) (((in[4] << 8) & 0xff00) | in[3] & 0xff);
                        ay = (short) (((in[6] << 8) & 0xff00) | in[5] & 0xff);
                        az = (short) (((in[8] << 8) & 0xff00) | in[7] & 0xff);


                        double tt = Math.toDegrees((double)az/10000);
                        if(tt < 0) tt += 360;
                        // find ip
                        for(int i = 0; i < clientsIp.length; i++)
                        {
                            if(ip.getAddress()[3] == clientsIp[i])
                            {
                                clientData[i][0] = String.format("%.1f", Math.toDegrees((double)ax/10000));
                                clientData[i][1] = String.format("%.1f", Math.toDegrees((double)ay/10000));
                                clientData[i][2] = String.format("%.1f", tt);
                                clientData[i][3] = "" + light;
                                clientData[i][4] = terms;
                                lvBuid();
                                notAsweredCntr[i] = 0;

                                if(clientActivityCreated == 1) sendIntent();


                                if(i == 0)
                                {
                                    pPitch = (double)ax/10000;
                                    pRoll  = (double)ay/10000;
                                }
                            }
                        }

                        break;

                    case UDPCommands.CMD_CFG:
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

//class OpenGLRenderer implements GLSurfaceView.Renderer {
//
//    private Cube mCube = new Cube();
//    private float mCubeRotation;
//
//    @Override
//    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);
//
//        gl.glClearDepthf(1.0f);
//        gl.glEnable(GL10.GL_DEPTH_TEST);
//        gl.glDepthFunc(GL10.GL_LEQUAL);
//
//        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
//                GL10.GL_NICEST);
//
//    }
//
//    @Override
//    public void onDrawFrame(GL10 gl) {
//        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
//        gl.glLoadIdentity();
//
//        gl.glTranslatef(0.0f, 0.0f, -10.0f);
//        //gl.glRotatef(mCubeRotation, 1.0f, 1.0f, 1.0f);
//
//        gl.glRotatef(mCubeRotation, 0.0f, 0.0f, 1.0f);
//        gl.glRotatef((float)Math.toDegrees(MainActivity.pRoll), 1.0f, 0.0f, 0.0f);
//        gl.glRotatef((float)Math.toDegrees(MainActivity.pHead), 0.0f, 1.0f, 0.0f);
//
//        mCube.draw(gl);
//
//        gl.glLoadIdentity();
//
//        mCubeRotation = (-1) * (float) Math.toDegrees(MainActivity.pPitch);
//        //mCubeRotation -= 0.15f;
//    }
//
//    @Override
//    public void onSurfaceChanged(GL10 gl, int width, int height) {
//        gl.glViewport(0, 0, width, height);
//        gl.glMatrixMode(GL10.GL_PROJECTION);
//        gl.glLoadIdentity();
//        GLU.gluPerspective(gl, 20.0f, (float)width / (float)height, 0.1f, 100.0f);
//        gl.glViewport(0, 0, width, height);
//
//        gl.glMatrixMode(GL10.GL_MODELVIEW);
//        gl.glLoadIdentity();
//    }
//}
//
//class Cube {
//
//    private FloatBuffer mVertexBuffer;
//    private FloatBuffer mColorBuffer;
//    private ByteBuffer  mIndexBuffer;
//
//    private float vertices[] = {
//            -1.0f, -0.2f, -1.0f,
//            1.0f, -0.2f, -1.0f,
//            1.0f,  0.2f, -1.0f,
//            -1.0f, 0.2f, -1.0f,
//            -1.0f, -0.2f,  1.0f,
//            1.0f, -0.2f,  1.0f,
//            1.0f,  0.2f,  1.0f,
//            -1.0f,  0.2f,  1.0f
//    };
//    private float colors[] = {
//            0.0f,  1.0f,  0.0f,  1.0f,
//            0.0f,  1.0f,  0.0f,  1.0f,
//            1.0f,  0.5f,  0.0f,  1.0f,
//            1.0f,  0.5f,  0.0f,  1.0f,
//            1.0f,  0.0f,  0.0f,  1.0f,
//            1.0f,  0.0f,  0.0f,  1.0f,
//            0.0f,  0.0f,  1.0f,  1.0f,
//            1.0f,  0.0f,  1.0f,  1.0f
//    };
//
//    private byte indices[] = {
//            0, 4, 5, 0, 5, 1,
//            1, 5, 6, 1, 6, 2,
//            2, 6, 7, 2, 7, 3,
//            3, 7, 4, 3, 4, 0,
//            4, 7, 6, 4, 6, 5,
//            3, 0, 1, 3, 1, 2
//    };
//
//    public Cube() {
//        ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
//        byteBuf.order(ByteOrder.nativeOrder());
//        mVertexBuffer = byteBuf.asFloatBuffer();
//        mVertexBuffer.put(vertices);
//        mVertexBuffer.position(0);
//
//        byteBuf = ByteBuffer.allocateDirect(colors.length * 4);
//        byteBuf.order(ByteOrder.nativeOrder());
//        mColorBuffer = byteBuf.asFloatBuffer();
//        mColorBuffer.put(colors);
//        mColorBuffer.position(0);
//
//        mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
//        mIndexBuffer.put(indices);
//        mIndexBuffer.position(0);
//    }
//
//    public void draw(GL10 gl) {
//        gl.glFrontFace(GL10.GL_CW);
//
//        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
//        gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer);
//
//        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
//        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
//
//        gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE,
//                mIndexBuffer);
//
//        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
//        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
//    }
//}
