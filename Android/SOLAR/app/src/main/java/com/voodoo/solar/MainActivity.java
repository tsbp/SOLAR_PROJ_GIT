package com.voodoo.solar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
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


    public final static String PARAM_PITCH = "pitch";
    public final static String PARAM_ROLL  = "roll";
    public final static String PARAM_HEAD  = "head";
    public final static String PARAM_LIGTH = "ligth";
    public final static String PARAM_TERM  = "term";

    public static double  pPitch, pRoll, pHead;

    public static UDPProcessor udpProcessor ;
    InetAddress deviceIP = null, broadcastIP;

    TextView tvDate, tvSunPos;
    EditText etLong, etLatit;
//    com.voodoo.solar.imgPosition imgSun;


    Button btnAnim;
    private Timer mTimer;
    private animTimerTask mMyTimerTask;

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

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); // unfocus Edited text

        lvClients = (ListView)findViewById(R.id.lvClients);

        udpProcessor = new UDPProcessor(7171);
        udpProcessor.setOnReceiveListener(this);
        udpProcessor.start();

        byte [] bcIP = getBroadcastIP4AsBytes();
        try {
            broadcastIP = InetAddress.getByAddress(bcIP);
        }
        catch (UnknownHostException e){}


        tvSunPos = (TextView)findViewById(R.id.tvSunPos);

        tvDate   = (TextView)findViewById(R.id.tvDate);
        etLong =  (EditText) findViewById(R.id.etLong);
        etLatit =  (EditText) findViewById(R.id.etLatit);
//        imgSun = (com.voodoo.solar.imgPosition) findViewById(R.id.imgPos);

        sunPos.Lon = Math.toRadians(Double.parseDouble(etLong.getText().toString()));
        sunPos.Lat = Math.toRadians(Double.parseDouble(etLatit.getText().toString()));

        Button btnCalc = (Button) findViewById(R.id.btnCalculate);
        btnCalc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                showSunInfo();

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
                        if(clientsIp != null)
                        {
                            for (int i = 0; i < clientsIp.length; i++) notAsweredCntr[i]++;
                            for (int i = 0; i < clientsIp.length; i++)
                                if (notAsweredCntr[i] > 5) deleteListRow(i);
                            byte time[] = getCurrentTime();
                            UDPCommands.sendCmd(UDPCommands.CMD_SYNC, time, broadcastIP);
                        }
//
//                        if(deviceIP == null)
//                            UDPCommands.sendCmd(UDPCommands.CMD_STATE, time, broadcastIP);
//                        else
//                            try
//                            {
//                                byte [] addr = broadcastIP.getAddress();
//
//                                if(currentClient < clientsIp.length) currentClient++;
//                                if(currentClient >= clientsIp.length)
//                                {
//                                    addr[3] = (byte)255;
//                                    currentClient = -1;
//                                }
//                                else
//                                {
//                                    addr[3] = (byte) clientsIp[currentClient];
//                                    notAsweredCntr[currentClient]++;
//                                }
//
//                                UDPCommands.sendCmd(UDPCommands.CMD_STATE, time, InetAddress.getByAddress(addr));
//
//                                // check for not answered
//
//                                for(int i = 0; i < clientsIp.length; i++)
//                                    if(notAsweredCntr[i] > 10) deleteListRow(i);
//
//                            }
//                            catch (UnknownHostException e){}


                    }
                });
            }
        }, 0, 1000);

        //==========================================================================================
        btnAnim = (Button) findViewById(R.id.btnAnimate);
        btnAnim.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mTimer == null)
                {
                    btnAnim.setText("Working...");
                    sunPos.Lon = Math.toRadians(Double.parseDouble(etLong.getText().toString()));
                    sunPos.Lat = Math.toRadians(Double.parseDouble(etLatit.getText().toString()));
                    mTimer = new Timer();
                    mMyTimerTask = new animTimerTask();
                    mTimer.schedule(mMyTimerTask, 1000, 1000);
                }
                else
                {
                    stopCntr = 5;
//                    mTimer.cancel();
//                    mTimer = null;
                    btnAnim.setText("Waiting...");
                }
            }
        });
    }
    //==============================================================================================
    byte [] getCurrentTime()
    {
        byte tmp [] = new byte[6];
        Calendar currentTime    = Calendar.getInstance();

        tmp[0] = (byte)(currentTime.get(Calendar.YEAR) - 2000);
        tmp[1] = (byte)(1 + currentTime.get(Calendar.MONTH));
        tmp[2] = (byte)(currentTime.get(Calendar.DAY_OF_MONTH));

        tmp[3] = (byte)(currentTime.get(Calendar.HOUR_OF_DAY));
        tmp[4] = (byte)(currentTime.get(Calendar.MINUTE));
        tmp[5] = (byte)(currentTime.get(Calendar.SECOND));
        return tmp;
    }
    //==============================================================================================
    int stopCntr = 0;
    //int hCntr;
    //==============================================================================================
    class animTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if(stopCntr == 0)
                    {
                        byte tmp[] = getCurrentTime();
                        tvDate.setText((tmp[0]+2000) + "." + tmp[1] + "." + tmp[2] + " * " + tmp[3] + ":" + tmp[4] + ":" + tmp[5]);

                        sunPos.Calculate(tmp[0] + 2000, tmp[1], tmp[2], tmp[3] - sunPos.Zone, tmp[4], tmp[5]);

                        tvSunPos.setText("Azimuth: " + String.format("%.3f", Math.toDegrees(sunPos.azimuth)) + ", Elevation: " + String.format("%.3f", Math.toDegrees(sunPos.elev)));

//                        imgPosition.azimuth = sunPos.azimuth;
//                        if(sunPos.elev < 0) imgPosition.elevation = sunPos.elev;
//                        else                imgPosition.elevation = 0;
//                        imgSun.invalidate();

                        byte data[] = new byte[4];
                        data[0] = (byte) ((int) (Math.toDegrees(sunPos.elev) * 100));
                        data[1] = (byte) (((int) (Math.toDegrees(sunPos.elev) * 100) >> 8));
                        data[2] = (byte) ((int) (Math.toDegrees(sunPos.azimuth) * 100));
                        data[3] = (byte) (((int) (Math.toDegrees(sunPos.azimuth) * 100) >> 8));

                        try {
                            UDPCommands.sendCmd(UDPCommands.CMD_SET_POSITION, data, InetAddress.getByAddress(getBroadcastIP4AsBytes()));
                        } catch (UnknownHostException e) {
                        }
                    }
                    else
                    {
                        stopCntr--;
                        if(stopCntr <= 0 )
                        {
                            mTimer.cancel();
                            mTimer = null;
                            btnAnim.setText("Start");
                        }
                        else
                        {
                            try {
                                UDPCommands.sendCmd(UDPCommands.CMD_GOHOME, null, InetAddress.getByAddress(getBroadcastIP4AsBytes()));
                            } catch (UnknownHostException e) {
                            }
                        }

                    }
                }
            });
        }
    }
    //==============================================================================================
    int selectedClient = 0;
    //==============================================================================================
    void sendIntent()
    {
        Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
        intent.putExtra(PARAM_PITCH, clientData[selectedClient][0]);
        intent.putExtra(PARAM_ROLL,  clientData[selectedClient][1]);
        intent.putExtra(PARAM_HEAD,  clientData[selectedClient][2]);
        intent.putExtra(PARAM_LIGTH, clientData[selectedClient][3]);
        intent.putExtra(PARAM_TERM,  clientData[selectedClient][4]);
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
                InetAddress iIP = null;
                ip[3] = clientsIp[position];
                try
                {
                   iIP = InetAddress.getByAddress(ip);
                }
                catch (UnknownHostException e){}
                clientActivityCreated = 1;
                selectedClient = position;

                Intent intent;
                if(clientData[selectedClient][0] == "M")
                {
                    ClientConfigMeteo.ip = iIP;
                    intent = new Intent(MainActivity.this, ClientConfigMeteo.class);
                }
                else
                {
                    ClientConfig.ip = iIP;
                    intent = new Intent(MainActivity.this, ClientConfig.class);
                }
                startActivity(intent);
            }
        });
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

            if(in[0] == UDPCommands.ID_SLAVE || in[0] == UDPCommands.ID_METEO)
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
                                if(in[0] == UDPCommands.ID_SLAVE) {
                                    clientData[i][0] = String.format("%.1f", Math.toDegrees((double) ax / 10000));
                                    clientData[i][1] = String.format("%.1f", Math.toDegrees((double) ay / 10000));
                                    clientData[i][3] = "" + light;
                                    clientData[i][2] = String.format("%.1f", tt);
                                    clientData[i][4] = terms;
                                }
                                else
                                {
                                    clientData[i][0] = "M";
                                    clientData[i][1] = "E";
                                    clientData[i][2] = "T";
                                    clientData[i][3] = "E";
                                    clientData[i][4] = "O";
                                    ClientConfigMeteo.data = new byte[14];
                                    for(int a = 0; a < 14; a++) ClientConfigMeteo.data[a] = in[a + 3];
//                                    clientData[i][0] = "MST";
//                                    clientData[i][1] = String.format("%.1f",  Math.toDegrees((double) ay / 1000));
//                                    clientData[i][2] = String.format("%.1f",  Math.toDegrees((double) az / 1000));
//                                    clientData[i][4] = "---";
                                }

                                lvBuid();
                                notAsweredCntr[i] = 0;

                                if(clientActivityCreated == 1) sendIntent();


                                if(i == selectedClient)
                                {
                                    OpenGLRenderer.pitch = (float)ax/10000;
                                    OpenGLRenderer.roll  = (float)ay/10000;
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
    void showSunInfo() {
        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.sun_info, (ViewGroup) findViewById(R.id.info));

        popDialog.setIcon(R.drawable.compass_small);
        popDialog.setTitle("Angles");
        popDialog.setView(Viewlayout);

        sunPos.Lon = Math.toRadians(Double.parseDouble(etLong.getText().toString()));
        sunPos.Lat = Math.toRadians(Double.parseDouble(etLatit.getText().toString()));

        final TextView tvResult = (TextView)Viewlayout.findViewById(R.id.tvResult);
        byte tmp [] = getCurrentTime();
        tvResult.setText("For " + (tmp[0] + 2000) + "." + tmp[1] + "." + tmp[2] + "\r\n"
                + sunPos.getAngles());



        popDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        popDialog.create();
        popDialog.show();
    }

    //==============================================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        udpProcessor.stop();
    }

}

