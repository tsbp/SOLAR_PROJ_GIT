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
import android.widget.ImageView;
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

public class MainActivity extends Activity implements OnReceiveListener {


    public final static String PARAM_PITCH = "pitch";
    public final static String PARAM_ROLL = "roll";
    public final static String PARAM_HEAD = "head";
    public final static String PARAM_LIGTH = "ligth";
    public final static String PARAM_TERM = "term";
    public final static String PARAM_STT = "dStt";


    public static UDPProcessor udpProcessor;
    InetAddress deviceIP = null, broadcastIP;


    EditText etLong, etLatit;

    TextView tvRegion, tvPackets;

    ListView lvClients, lvForecast;

    com.voodoo.solar.Compass compass;


    LinkedList<Client> clients = new LinkedList<>();

    public static int clientActivityCreated = 0;

    public final static String BROADCAST_ACTION = "broadcast Cient data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); // unfocus Edited text

        lvClients = (ListView) findViewById(R.id.lvClients);
        lvForecast = (ListView) findViewById(R.id.lvForecast);

        udpProcessor = new UDPProcessor(7171);
        udpProcessor.setOnReceiveListener(this);
        udpProcessor.start();

        byte[] bcIP = getBroadcastIP4AsBytes();
        try {
            broadcastIP = InetAddress.getByAddress(bcIP);
        } catch (Exception e) {
            e.printStackTrace();
        }

        tvRegion = (TextView) findViewById(R.id.tvRegion);
        tvPackets = (TextView) findViewById(R.id.tvPackets);

        etLong = (EditText) findViewById(R.id.etLong);
        etLatit = (EditText) findViewById(R.id.etLatit);
        //com.voodoo.solar.imgPosition imgSun = (com.voodoo.solar.imgPosition) findViewById(R.id.imgPos);

        compass = (com.voodoo.solar.Compass) findViewById(R.id.compassImg);

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
                        if (clients.size() >= 0) {
                            for (int i = 0; i < clients.size(); i++) {
                                Client tmp = clients.get(i);
                                tmp.notAnswerDownCounter--;
                                if (tmp.notAnswerDownCounter <= 0) {
                                    deleteListRow(i);
                                }
                            }
                        }
                    }
                });
            }
        }, 0, 1000);


    }

    //==============================================================================================
    public static byte[] getCurrentTime() {
        byte tmp[] = new byte[6];
        Calendar currentTime = Calendar.getInstance();

        tmp[0] = (byte) (currentTime.get(Calendar.YEAR) - 2000);
        tmp[1] = (byte) (1 + currentTime.get(Calendar.MONTH));
        tmp[2] = (byte) (currentTime.get(Calendar.DAY_OF_MONTH));

        tmp[3] = (byte) (currentTime.get(Calendar.HOUR_OF_DAY));
        tmp[4] = (byte) (currentTime.get(Calendar.MINUTE));
        tmp[5] = (byte) (currentTime.get(Calendar.SECOND));
        return tmp;
    }

    //==============================================================================================
    int selectedClient = 0;

    //==============================================================================================
    void sendIntent() {
        Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
        intent.putExtra(PARAM_PITCH, clients.get(selectedClient).data[0]);
        intent.putExtra(PARAM_ROLL, clients.get(selectedClient).data[1]);
        intent.putExtra(PARAM_HEAD, clients.get(selectedClient).data[2]);
        intent.putExtra(PARAM_LIGTH, clients.get(selectedClient).data[3]);
        intent.putExtra(PARAM_TERM, clients.get(selectedClient).data[4]);
        intent.putExtra(PARAM_STT, clients.get(selectedClient).data[5]);
        sendBroadcast(intent);
    }

    //==============================================================================================
    void deleteListRow(int aRow) {
        {
            /*clientsIp.*/clients.remove(aRow);
            if (/*clientsIp.*/clients.size() == 0) lvBuid();
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
    void lvBuid() {
        ArrayList<Map<String, Object>> data = new ArrayList<>(clients.size());
        Map<String, Object> m;

        for (Client c : clients) {
            m = new HashMap<>();
            m.put(ATTRIBUTE_IP, c.ip & 0xff);
            m.put(ATTRIBUTE_V1, c.data[0]);
            m.put(ATTRIBUTE_V2, c.data[1]);
            m.put(ATTRIBUTE_V3, c.data[2]);
            m.put(ATTRIBUTE_V4, c.data[3]);
            m.put(ATTRIBUTE_V5, c.data[4]);
            data.add(m);

        }

        String[] from = {ATTRIBUTE_IP, ATTRIBUTE_V1, ATTRIBUTE_V2, ATTRIBUTE_V3, ATTRIBUTE_V4, ATTRIBUTE_V5};
        int[] to = {R.id.i1, R.id.i2, R.id.i3, R.id.i4, R.id.i5, R.id.i6};
        SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.client_item, from, to);
        lvClients.setAdapter(sAdapter);
        //==========================================================
        lvClients.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                byte[] ip = getBroadcastIP4AsBytes();
                InetAddress iIP = null;
                assert ip != null;
                ip[3] = clients.get(position).ip;
                try {
                    iIP = InetAddress.getByAddress(ip);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                clientActivityCreated = 1;
                selectedClient = position;

                Intent intent;
                if (clients.get(selectedClient).data[0].equals("M"))
                {
                    ClientConfigMeteo.ip = iIP;
                    intent = new Intent(MainActivity.this, ClientConfigMeteo.class);
                } else {
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
    private final String[] FORECAST_ITEMS = {"Wind", "Temp", "Humid"};

    private void lvForecastBuild(byte[] receiveBytes) {

        //http://api.openweathermap.org/data/2.5/weather?lat=32.23&lon=48.5&units=metric&appid=7412b643dfdbf390970f2f65a1bad7ce
        String[] fcData = new String[3];

        fcData[1] = (double) ((receiveBytes[18] & 0xff) | (receiveBytes[19] & 0xff) << 8) / 100 + " m/s";
        fcData[0] = (double) ((receiveBytes[20] & 0xff) | (receiveBytes[21] & 0xff) << 8) / 100 + " \u2103";
        fcData[2] = (double) ((receiveBytes[22] & 0xff) | (receiveBytes[23] & 0xff) << 8) / 100 + "%";

        int strEnd;
        for (strEnd = 0; strEnd < 20; strEnd++) {
            if (receiveBytes[24 + strEnd] == 0) break;
        }
        tvRegion.setText(new String(Arrays.copyOfRange(receiveBytes, 24, 24 + strEnd)));


        int[] pictos = {0, R.drawable.wind, R.drawable.humid};
        byte[] tmp = {receiveBytes[44], receiveBytes[45], receiveBytes[46]};
        String imgPicto = new String(tmp);
        pictos[0] = getImage(imgPicto);

        ArrayList<Map<String, Object>> data = new ArrayList<>(3);
        Map<String, Object> m;
        for (int i = 0; i < 3; i++) {
            m = new HashMap<>();
            m.put(ATTRIBUTE_IP, pictos[i]);
            m.put(ATTRIBUTE_V1, fcData[i]);
            data.add(m);
        }
        String[] from = {ATTRIBUTE_IP, ATTRIBUTE_V1};
        int[] to = {R.id.picto, R.id.i1};
        SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.forecast_item, from, to);
        lvForecast.setAdapter(sAdapter);
        redrawSunPosition(receiveBytes);
    }

    com.voodoo.solar.imgPosition iw;
    //**********************************************************************************************
    void redrawSunPosition(byte[] receiveBytes) {
        double azimuth = 0.01 * (double) ((receiveBytes[9] & 0xff) | ((receiveBytes[10] << 8)));
        double elevation = 0.01 * (double) ((receiveBytes[11] & 0xff) | ((receiveBytes[12] << 8)));
        Compass.azimuth = azimuth;
        Compass.elevation = elevation;
        compass = (com.voodoo.solar.Compass) findViewById(R.id.compassImg);
        compass.invalidate();
    }

    /***********************************************************************************************
     *
     * @param imgPicto
     * @return
     */
    private int getImage(String imgPicto) {
        switch (imgPicto) {
            case "01d":
                return R.drawable.w01d;
            case "01n":
                return R.drawable.w01n;
            case "02n":
                return R.drawable.w02d;
            case "02d":
                return R.drawable.w02n;
            case "03n":
                return R.drawable.w03n;
            case "03d":
                return R.drawable.w03d;
            case "04n":
                return R.drawable.w04n;
            case "04d":
                return R.drawable.w04d;
            case "09n":
                return R.drawable.w09n;
            case "09d":
                return R.drawable.w09d;
            case "10n":
                return R.drawable.w10n;
            case "10d":
                return R.drawable.w10d;
            case "11n":
                return R.drawable.w11n;
            case "11d":
                return R.drawable.w11d;
            case "13n":
                return R.drawable.w13n;
            case "13d":
                return R.drawable.w13d;
            case "50n":
                return R.drawable.w50n;
            case "50d":
                return R.drawable.w50d;
        }
        return R.drawable.w01d;
    }

    //==============================================================================================
    int cntr = 0;
    //==============================================================================================
    short ax, ay, az, packCntr = 0;

    //==============================================================================================
    public void onFrameReceived(InetAddress ip, IDataFrame frame) {
        byte[] in = frame.getFrameData();
        packCntr++;
        tvPackets.setText("Packs {" + packCntr + "}");

        if (deviceIP == null) deviceIP = ip;
        try {

            if (in[0] == UDPCommands.ID_SLAVE || in[0] == UDPCommands.ID_METEO) {

                //==========================================
                if (clients.size() <= 0) {
                    clients.add(new Client(ip.getAddress()[3], new String[6]));
                } else { //add client

                    boolean clientFound = false;
                    for (Client cl : clients) {
                        if (ip.getAddress()[3] == cl.ip) {
                            clientFound = true;
                            break;
                        }
                    }
                    if(!clientFound) {
                        clients.add(new Client(ip.getAddress()[3], new String[6]));
                    }
                }
                Intent intent;
                //==========================================
                switch (in[1]) {
                    case UDPCommands.CMD_CALIB:
                        intent = new Intent(ClientConfig.CALIB_DATA);
                        switch (in[3]) {
                            case UDPCommands.GET_COMPASS_RAW: {
                                intent.putExtra("x_raw", "" + (short) (((in[5] << 8) & 0xff00) | in[4] & 0xff));
                                intent.putExtra("y_raw", "" + (short) (((in[7] << 8) & 0xff00) | in[6] & 0xff));
                                intent.putExtra("z_raw", "" + (short) (((in[9] << 8) & 0xff00) | in[8] & 0xff));
                                sendBroadcast(intent);
                            }
                            break;

                            case UDPCommands.GET_ACCEL_RAW: {
                                intent.putExtra("acc_raw", "" + (short) (((in[5] << 8) & 0xff00) | in[4] & 0xff));
                                sendBroadcast(intent);
                            }
                            break;
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
                        if ((in[11] & (short) 1) == 1)
                            terms += "1"; //btnIn1.setBackgroundColor(Color.RED);
                        else terms += "0"; //btnIn1.setBackgroundColor(Color.GREEN);
                        if ((in[11] & (short) 2) == 2)
                            terms += "1"; //btnIn2.setBackgroundColor(Color.RED);
                        else terms += "0"; //btnIn2.setBackgroundColor(Color.GREEN);
                        if ((in[11] & (short) 4) == 4)
                            terms += "1"; //btnIn3.setBackgroundColor(Color.RED);
                        else terms += "0"; //btnIn3.setBackgroundColor(Color.GREEN);
                        if ((in[11] & (short) 8) == 8)
                            terms += "1"; //btnIn4.setBackgroundColor(Color.RED);
                        else terms += "0"; //btnIn4.setBackgroundColor(Color.GREEN);

                        int light = (((in[10] << 8) & 0xff00) | in[9] & 0xff);
                        int stt = (in[12] & 0xff);

                        ax = (short) (((in[4] << 8) & 0xff00) | in[3] & 0xff);
                        ay = (short) (((in[6] << 8) & 0xff00) | in[5] & 0xff);
                        az = (short) (((in[8] << 8) & 0xff00) | in[7] & 0xff);


                        double tt = Math.toDegrees((double) az / 10000);
                        if (tt < 0) tt += 360;
                        // find ip
                        for (int i = 0; i < clients.size(); i++) {
                            Client currentClient = clients.get(i);
                            if (ip.getAddress()[3] == clients.get(i).ip/*clientsIp[i]*/) {
                                if (in[0] == UDPCommands.ID_SLAVE) {
                                    currentClient.data[0] = String.format(Locale.ENGLISH, "%.1f", Math.toDegrees((double) ax / 10000));
                                    currentClient.data[1] = String.format(Locale.ENGLISH, "%.1f", Math.toDegrees((double) ay / 10000));
                                    currentClient.data[2] = String.format(Locale.ENGLISH, "%.1f", tt);
                                    currentClient.data[3] = "" + light;
                                    currentClient.data[4] = terms;
                                    currentClient.data[5] = "" + stt;
                                } else {
                                    currentClient.data[0] = "M";
                                    currentClient.data[1] = "E";
                                    currentClient.data[2] = "T";
                                    currentClient.data[3] = String.format(Locale.ENGLISH, "%.1f", 0.01 * (double) ((in[9] & 0xff) | ((in[10] << 8))));
                                    currentClient.data[4] = String.format(Locale.ENGLISH, "%.1f", 0.01 * (double) ((in[11] & 0xff) | ((in[12] << 8))));

                                    if (in.length > 19) {
                                        lvForecastBuild(in);
                                    }
                                    ClientConfigMeteo.data = Arrays.copyOfRange(in, 3, 3 + 15);
                                    //redrawSunPosition(in);
                                }

                                lvBuid();
                                currentClient.notAnswerDownCounter = 5;

                                if (clientActivityCreated == 1) sendIntent();

                                if (i == selectedClient) {
                                    OpenGLRenderer.pitch = (float) ax / 10000;
                                    OpenGLRenderer.roll = (float) ay / 10000;
                                }
                            }
                        }
                        break;

                    case UDPCommands.CMD_CFG:
                        ClientConfigMeteo.cfgData = Arrays.copyOfRange(in, 3, 3 + 18);
                        intent = new Intent(ClientConfigMeteo.BC_CFG_DATA);
                        sendBroadcast(intent);
                        break;
                }
            }

            // zatychka ot neprihodyaschih paketov
            UDPCommands.sendCmd(UDPCommands.NOP, null, broadcastIP);
        } catch (Exception e) {
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

        final TextView tvResult = (TextView) Viewlayout.findViewById(R.id.tvResult);
        byte tmp[] = getCurrentTime();
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

class Client {
    public int notAnswerDownCounter = 5;
    public byte ip;
    public String data[];

    Client(byte ip, String[] data) {
        this.ip = ip;
        this.data = data;
    }

}

