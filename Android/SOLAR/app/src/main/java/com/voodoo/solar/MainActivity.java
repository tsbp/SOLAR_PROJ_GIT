package com.voodoo.solar;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.voodoo.solar.UDPProcessor.OnReceiveListener;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


import static com.voodoo.solar.IPHelper.getBroadcastIP4AsBytes;

public class MainActivity extends Activity implements OnReceiveListener  {


    public final static String PARAM_PITCH = "pitch";
    public final static String PARAM_ROLL  = "roll";
    public final static String PARAM_HEAD  = "head";
    public final static String PARAM_LIGTH = "ligth";
    public final static String PARAM_TERM  = "term";
    public final static String PARAM_STT  = "dStt";



    public static UDPProcessor udpProcessor ;
    InetAddress deviceIP = null, broadcastIP;


    EditText etLong, etLatit;

    TextView tvRegion, tvPackets;

    ListView lvClients, lvForecast;


    LinkedList<Byte> clientsIp;
    String clientData[][];
    int notAsweredCntr[] = new int[100];

    public static int clientActivityCreated = 0;

    public final static String BROADCAST_ACTION = "broadcast Cient data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); // unfocus Edited text

        lvClients = (ListView)findViewById(R.id.lvClients);
        lvForecast = (ListView) findViewById(R.id.lvForecast);

        udpProcessor = new UDPProcessor(7171);
        udpProcessor.setOnReceiveListener(this);
        udpProcessor.start();

        byte [] bcIP = getBroadcastIP4AsBytes();
        try {
            broadcastIP = InetAddress.getByAddress(bcIP);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        tvRegion   = (TextView)findViewById(R.id.tvRegion);
        tvPackets   = (TextView)findViewById(R.id.tvPackets);

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

        Button btnFind = (Button) findViewById(R.id.btnFind);
        btnFind.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                UDPCommands.sendCmd(UDPCommands.CMD_STATE, null, broadcastIP);

            }
        });

        //==========================================================================================
        Timer timeoutTimer = new Timer();
        timeoutTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(clientsIp != null)
                        {
                            //for (int i = 0; i < clientsIp.length; i++) notAsweredCntr[i]++;
                            for (int i = 0; i < clientsIp.size(); i++) notAsweredCntr[i]++;
                            //for (int i = 0; i < clientsIp.length; i++)
                            for (int i = 0; i < clientsIp.size(); i++)
                                if (notAsweredCntr[i] > 5) deleteListRow(i);
                        }

                    }
                });
            }
        }, 0, 1000);


    }
    //==============================================================================================
    public static byte [] getCurrentTime()
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
        intent.putExtra(PARAM_STT,   clientData[selectedClient][5]);
        sendBroadcast(intent);
    }
    //==============================================================================================
    void deleteListRow(int aRow)
    {
        {
            clientsIp.remove(aRow);
            if(clientsIp.size() == 0) lvBuid();
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
                clientsIp.size());
        Map<String, Object> m;
        for (int i = 0; i < clientsIp.size(); i++) {

            m = new HashMap<>();
            m.put(ATTRIBUTE_IP, clientsIp.get(i)/*[i]*/ & 0xff);
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
                assert ip != null;
                ip[3] = clientsIp.get(position)/*[position]*/;
                try
                {
                   iIP = InetAddress.getByAddress(ip);
                }
                catch (UnknownHostException e){
                    e.printStackTrace();
                }
                clientActivityCreated = 1;
                selectedClient = position;

                Intent intent;
                if(clientData[selectedClient][0].equals("M"))
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
    /***********************************************************************************************
     *
     */
    private final String [] FORECAST_ITEMS = {"Wind", "Temp", "Humid"};
    private void lvForecastBuild(byte[] receiveBytes) {

        double [] fcData = new double[3];

        fcData[0] = (double) ((receiveBytes[18] & 0xff) | (receiveBytes[19] & 0xff ) << 8) / 100;
        fcData[1] = (double) ((receiveBytes[20] & 0xff) | (receiveBytes[21] & 0xff ) << 8) / 100;
        fcData[2] = (double) ((receiveBytes[22] & 0xff) | (receiveBytes[23] & 0xff ) << 8) / 100;

        int strEnd;
        for( strEnd = 0; strEnd < 20; strEnd++) {if(receiveBytes[24 + strEnd] == 0) break;}
        tvRegion.setText(new String(Arrays.copyOfRange(receiveBytes, 24, 24 + strEnd)));


        //http://api.openweathermap.org/data/2.5/weather?lat=48.5&lon=32.22&units=metric&appid=7412b643dfdbf390970f2f65a1bad7ce

        ArrayList<Map<String, Object>> data = new ArrayList<>(3);
        Map<String, Object> m;
        for (int i = 0; i < 3; i++) {
            m = new HashMap<>();
            m.put(ATTRIBUTE_IP, FORECAST_ITEMS[i]);
            m.put(ATTRIBUTE_V1, fcData[i]);
            data.add(m);

        }
        String[] from = {ATTRIBUTE_IP, ATTRIBUTE_V1};
        int[] to = {R.id.i1, R.id.i2};
        SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.forecast_item, from, to);
        lvForecast.setAdapter(sAdapter);

        // compass
        double azimuth   = 0.01 * (double)((receiveBytes[9] & 0xff) | ((receiveBytes[10] << 8)));
        double elevation = 0.01 * (double)((receiveBytes[11] & 0xff) | ((receiveBytes[12] << 8)));
        imgPosition.azimuth = azimuth;
        imgPosition.elevation = elevation;
        findViewById(R.id.imgPos).invalidate();
    }
    //==============================================================================================
   /* void udpSend(byte[] aByte, InetAddress ip)
    {
        DataFrame df = new DataFrame(aByte);
        udpProcessor.send(ip,df);
    }*/
    //==============================================================================================
    int cntr = 0;
    //==============================================================================================
    short /*cx, cy, cz,*/ ax, ay, az, packCntr = 0;
    //==============================================================================================
    public void onFrameReceived(InetAddress ip, IDataFrame frame)
    {
        byte[] in = frame.getFrameData();
        packCntr++;
        tvPackets.setText("Packs {" + packCntr +"}");

        if(deviceIP == null) deviceIP = ip;
        try {

            if(in[0] == UDPCommands.ID_SLAVE || in[0] == UDPCommands.ID_METEO)
            {

                //==========================================
                if(clientsIp == null)
                {
                    clientsIp = new LinkedList<>();
                    clientsIp.add(ip.getAddress()[3]);
                    clientData = new String[1][6];
                    clientData[0][0] = "";
                }
                else
                { //add client
                    boolean clientFound = false;
                    for(byte curIp : clientsIp){
                        if(ip.getAddress()[3] == curIp) clientFound = true;
                    }
                    if(!clientFound)
                    {
                        clientsIp.add(ip.getAddress()[3]);
                        clientData = new String[clientData.length + 1][6];
                    }
                }
                Intent intent;
                //==========================================
                switch(in[1])
                {
                    case UDPCommands.CMD_CALIB:
                        intent = new Intent(ClientConfig.CALIB_DATA);
                        switch(in[3])
                        {
                            case UDPCommands.GET_COMPASS_RAW:
                            {
                                intent.putExtra("x_raw", "" + (short) (((in[5] << 8) & 0xff00) | in[4] & 0xff));
                                intent.putExtra("y_raw", "" + (short) (((in[7] << 8) & 0xff00) | in[6] & 0xff));
                                intent.putExtra("z_raw", "" + (short) (((in[9] << 8) & 0xff00) | in[8] & 0xff));
                                sendBroadcast(intent);
                            }break;

                            case UDPCommands.GET_ACCEL_RAW:
                            {
                                intent.putExtra("acc_raw", "" + (short) (((in[5] << 8) & 0xff00) | in[4] & 0xff));
                                sendBroadcast(intent);
                            }break;
                        }
                        break;
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
                        int stt = (in[12] & 0xff);

                        ax = (short) (((in[4] << 8) & 0xff00) | in[3] & 0xff);
                        ay = (short) (((in[6] << 8) & 0xff00) | in[5] & 0xff);
                        az = (short) (((in[8] << 8) & 0xff00) | in[7] & 0xff);


                        double tt = Math.toDegrees((double)az/10000);
                        if(tt < 0) tt += 360;
                        // find ip
                        for(int i = 0; i < clientsIp.size(); i++)
                        {
                             if(ip.getAddress()[3] == clientsIp.get(i)/*clientsIp[i]*/)
                            {
                                if(in[0] == UDPCommands.ID_SLAVE) {
                                    clientData[i][0] = String.format(Locale.ENGLISH, "%.1f", Math.toDegrees((double) ax / 10000));
                                    clientData[i][1] = String.format(Locale.ENGLISH, "%.1f", Math.toDegrees((double) ay / 10000));
                                    clientData[i][3] = "" + light;
                                    clientData[i][2] = String.format(Locale.ENGLISH, "%.1f", tt);
                                    clientData[i][4] = terms;
                                    clientData[i][5] = "" + stt;
                                }
                                else
                                {
                                    clientData[i][0] = "M";
                                    clientData[i][1] = "E";
                                    clientData[i][2] = "T";
                                    clientData[i][3] = String.format(Locale.ENGLISH, "%.1f", 0.01 * (double)((in[9]  & 0xff) | ((in[10] << 8))));
                                    clientData[i][4] = String.format(Locale.ENGLISH,"%.1f", 0.01 * (double)((in[11] & 0xff) | ((in[12] << 8))));

                                    if(in.length > 19) {
                                        lvForecastBuild(in);
                                    }
                                    ClientConfigMeteo.data = Arrays.copyOfRange(in, 3, 3 + 15);
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
                        ClientConfigMeteo.cfgData = Arrays.copyOfRange(in, 3, 3 + 18)/*new byte[18]*/;

                        intent = new Intent(ClientConfigMeteo.BC_CFG_DATA);
                        sendBroadcast(intent);
                        break;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
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
        String str = "For " + (tmp[0] + 2000) + "." + tmp[1] + "." + tmp[2] + "\r\n"
                + sunPos.getAngles();
        tvResult.setText(str);



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

