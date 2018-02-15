package com.voodoo.solar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.voodoo.solar.MainActivity.BROADCAST_ACTION;

public class ClientConfig extends Activity {

    TextView tvIp, tvPitch, tvRoll, tvHead, tvLigth, tvTerm, tAzimuth, tAngle;
    SeekBar sbCompass, sbAccel;

    public final static String CALIB_DATA = "Calib data";

    public static InetAddress ip;
    BroadcastReceiver br;

    short azimuth = 0;
    short angle = 0;
    byte angIncrement  = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_config);

        GLSurfaceView view = (GLSurfaceView) findViewById(R.id.w3D);
        view.setRenderer(new OpenGLRenderer());

        tAzimuth = (TextView)findViewById(R.id.tvAzimuth);
        tAngle   = (TextView)findViewById(R.id.tvAngle);

        tvPitch = (TextView) findViewById(R.id.tvPitch);
        tvRoll  = (TextView) findViewById(R.id.tvRoll);
        tvHead  = (TextView) findViewById(R.id.tvHead);
        tvLigth = (TextView) findViewById(R.id.tvLigth);
        tvTerm  = (TextView) findViewById(R.id.tvTerm);

        tvIp = (TextView) findViewById(R.id.tvIP);
        tvIp.setText("" + ip.getHostAddress());

        sbCompass = (SeekBar) findViewById(R.id.sbCompass);
        sbAccel = (SeekBar) findViewById(R.id.sbAccel);

        //================================================
        br = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                String input = intent.getStringExtra(MainActivity.PARAM_PITCH);
                tvPitch.setText(input);
                input = intent.getStringExtra(MainActivity.PARAM_ROLL);
                tvRoll.setText(input);
                input = intent.getStringExtra(MainActivity.PARAM_HEAD);
                tvHead.setText(input);
                input = intent.getStringExtra(MainActivity.PARAM_LIGTH);
                tvLigth.setText(input);
                input = intent.getStringExtra(MainActivity.PARAM_TERM);
                tvTerm.setText(input);
            }
        };
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(br, intFilt);

        //================================================
        Button btnUp = (Button) findViewById(R.id.btnUp);
        btnUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                UDPCommands.sendCmd(UDPCommands.CMD_UP,  null, ip);
            }
        });
        //================================================
        Button btnDown = (Button) findViewById(R.id.btndown);
        btnDown.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                UDPCommands.sendCmd(UDPCommands.CMD_DOWN,  null, ip);
            }
        });
        //================================================
        Button btnRight = (Button) findViewById(R.id.btnRight);
        btnRight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UDPCommands.sendCmd(UDPCommands.CMD_RIGHT,  null, ip);
            }
        });
        //================================================
        Button btnLeft = (Button) findViewById(R.id.btnLeft);
        btnLeft.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UDPCommands.sendCmd(UDPCommands.CMD_LEFT,  null, ip);
            }
        });

        Button wifi = (Button) findViewById(R.id.btnCfg);
        //================================================
        wifi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog_wifi();
            }
        });

        Button calib = (Button) findViewById(R.id.btnCmpCal);
        //================================================
        calib.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                for(int i = 1; i< 4; i++)
                    for(int j = 1; j < 4; j++)
                        vals[i][j] = "0";
                dialog_calibrate_compass();
            }
        });

        Button calib_acc = (Button) findViewById(R.id.btnAccCal);
        //================================================
        calib_acc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                dialog_calibrate_accel();
            }
        });

        Button bSendPos = (Button) findViewById(R.id.btnSendPos);
        //================================================
        bSendPos.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                byte [] tmp = new byte[4];
                tmp[0] = (byte) (angle & 0xff);
                tmp[1] = (byte)((angle >> 8) & 0xff);
                tmp[2] = (byte) (azimuth & 0xff);
                tmp[3] = (byte)((azimuth >> 8) & 0xff);
                UDPCommands.sendCmd(UDPCommands.CMD_SET_POSITION, tmp, ip);
            }
        });

        //==========================================================================================
        sbCompass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
               tAzimuth.setText("Azimyth: " + progress*3.6);
                azimuth = (short)(progress * 3.6 * 100);

//                azimuth =  (short)(tmp * 10000);
//
//                cmdBuffer[2] = (byte) ((azimuth) & (byte)0xff);
//                cmdBuffer[3] = (byte) ((azimuth >> 8) & (byte)0xff);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                if(deviceIP != null)
//                    sendCmd(CMD_AZIMUTH, deviceIP);
//                else
//                    sendCmd(CMD_AZIMUTH, broadcastIP);
            }
        });
        //==========================================================================================
        sbAccel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tAngle.setText("Angle: " + progress * 0.9);
                angle =  (short)(progress * 0.9 * 100);
//
//                cmdBuffer[2] = (byte) ((angle) & (byte)0xff);
//                cmdBuffer[3] = (byte) ((angle >> 8) & (byte)0xff);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                if(deviceIP != null)
//                    sendCmd(CMD_ANGLE, deviceIP);
//                else
//                    sendCmd(CMD_ANGLE, broadcastIP);
            }
        });


    }
    //==============================================================================================
    private final String ATTRIBUTE_T = "attr_t";
    private final String ATTRIBUTE_X = "attr_x";
    private final String ATTRIBUTE_Y = "attr_y";
    private final String ATTRIBUTE_Z = "attr_z";
    String[][] vals = {
            {" ",   "X", "Y", "Z"},
            {"V",   "1", "2", "3"},
            {"MAX", "4", "5", "6"},
            {"MIN", "4", "5", "6"}};
    //==============================================================================================
    void dialog_calibrate_compass() {
        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.dialo_calibrate, (ViewGroup) findViewById(R.id.lvCalib));

        popDialog.setIcon(R.drawable.wifi_small);
        popDialog.setTitle("Калибровка");
        popDialog.setView(Viewlayout);

        final ListView lvData = (ListView)Viewlayout.findViewById(R.id.lvCalib);

        //================================================
        final BroadcastReceiver brd = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {

                vals[1][1] = intent.getStringExtra("x_raw");
                vals[1][2] = intent.getStringExtra("y_raw");
                vals[1][3] = intent.getStringExtra("z_raw");

                for(int i = 1; i < 4; i++)
                {
                    if(Integer.parseInt(vals[1][i]) >= Integer.parseInt(vals[2][i])) vals[2][i] = vals[1][i];
                    if(Integer.parseInt(vals[1][i]) <  Integer.parseInt(vals[3][i])) vals[3][i] = vals[1][i];
                }

                ArrayList<Map<String, Object>> data = new ArrayList<>(4);
                Map<String, Object> m;
                for (int i = 0; i < 4; i++) {

                    m = new HashMap<>();
                    m.put(ATTRIBUTE_T, vals[i][0]);
                    m.put(ATTRIBUTE_X, vals[i][1]);
                    m.put(ATTRIBUTE_Y, vals[i][2]);
                    m.put(ATTRIBUTE_Z, vals[i][3]);
                    data.add(m);
                }
                String[] from = {ATTRIBUTE_T, ATTRIBUTE_X, ATTRIBUTE_Y, ATTRIBUTE_Z};
                int[] to = {R.id.i1, R.id.i2, R.id.i3, R.id.i4};
                SimpleAdapter sAdapter = new SimpleAdapter(context, data, R.layout.calb_item, from, to);
                lvData.setAdapter(sAdapter);

            }
        };
        IntentFilter intFilt = new IntentFilter(CALIB_DATA);
        registerReceiver(brd, intFilt);


        //================================================
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        byte [] b = {UDPCommands.GET_COMPASS_RAW};
                        UDPCommands.sendCmd(UDPCommands.CMD_CALIB, b, ip);
                    }
                });
            }
        }, 0, 300);
        //================================================
        popDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                unregisterReceiver(brd);
                timer.cancel();
                dialog.dismiss();
            }
        });
        popDialog.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        byte [] b = new byte[13];
                        b[0] = UDPCommands.SET_COMPASS_CALIBS;
                        int cnt = 1;
                        for(int i = 0; i < 2; i++)
                            for(int j = 0; j < 3; j++)
                            {
                                short a = (short)Integer.parseInt(vals[i + 2][j + 1]);
                                b[cnt] = (byte)(a & 0xff); cnt++;
                                b[cnt] = (byte)((a >> 8) & 0xff); cnt++;
                            }


                        UDPCommands.sendCmd(UDPCommands.CMD_CALIB, b, ip);
                        unregisterReceiver(brd);
                        timer.cancel();
                        dialog.dismiss();
                    }
                });
        popDialog.create();
        popDialog.show();
    }
    //==============================================================================================
    void dialog_calibrate_accel() {
        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.dialo_calibrate_acc, (ViewGroup) findViewById(R.id.dial_calib_acc));

        popDialog.setIcon(R.drawable.wifi_small);
        popDialog.setTitle("Калибровка");
        popDialog.setView(Viewlayout);

        final TextView tvAcc = (TextView)Viewlayout.findViewById(R.id.tvAccRaw);
        tvAcc.setText("45,0");

        final TextView t0  = (TextView)Viewlayout.findViewById(R.id.tv0deg);
        final TextView t90 = (TextView)Viewlayout.findViewById(R.id.tv90deg);

        final Button b0  = (Button)Viewlayout.findViewById(R.id.btn0deg);
        b0.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                t0.setText(tvAcc.getText().toString());
            }
        });

        final Button b90 = (Button)Viewlayout.findViewById(R.id.btn90deg);
        b90.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                t90.setText(tvAcc.getText().toString());
            }
        });

        //================================================
        final BroadcastReceiver brd = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {

                vals[1][1] = intent.getStringExtra("acc_raw");

                int ang =  Integer.parseInt(intent.getStringExtra("acc_raw"));

                tvAcc.setText("" + String.format("%.3f", Math.toDegrees((double)ang/10000)));


            }
        };
        IntentFilter intFilt = new IntentFilter(CALIB_DATA);
        registerReceiver(brd, intFilt);


        //================================================
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        byte [] b = {UDPCommands.GET_ACCEL_RAW};
                        UDPCommands.sendCmd(UDPCommands.CMD_CALIB, b, ip);
                    }
                });
            }
        }, 0, 300);
        //================================================
        popDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                unregisterReceiver(brd);
                timer.cancel();
                dialog.dismiss();
            }
        });
        popDialog.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        byte [] b = new byte[5];
                        b[0] = UDPCommands.SET_ACCEL_CALIBS;

//                        double a0  = Math.toRadians(Double.parseDouble(t0.getText().toString().replace(',','.')));
//                        double a90 = Math.toRadians(Double.parseDouble(t90.getText().toString().replace(',','.')));

                        double a0  = Math.toRadians(45.0);
                        double a90 = Math.toRadians(30.5);

                        b[1] = (byte)((int)(a0 * 10000) & 0xff);
                        b[2] = (byte)(((int)(a0 * 10000) >> 8) & 0xff);
                        b[3] = (byte)((int)(a90 * 10000) & 0xff);
                        b[4] = (byte)(((int)(a90 * 10000) >> 8) & 0xff);


                        UDPCommands.sendCmd(UDPCommands.CMD_CALIB, b, ip);
                        unregisterReceiver(brd);
                        timer.cancel();
                        dialog.dismiss();
                    }
                });
        popDialog.create();
        popDialog.show();
    }
    //==============================================================================================
    private String[] wifiMode= {"NULL_MODE","STATION_MODE","SOFTAP_MODE","STATIONAP_MODE"};
    private String[] wifiSecurityMode = {"AUTH_OPEN","AUTH_WEP","AUTH_WPA_PSK","AUTH_WPA2_PSK","AUTH_WPA_WPA2_PSK","AUTH_MAX"};
    byte wMode, wSecur;
//    String wCfgStr;
    //==============================================================================================
    void dialog_wifi() {
        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.dialog_wifi, (ViewGroup) findViewById(R.id.wifi));

        popDialog.setIcon(R.drawable.wifi_small);
        popDialog.setTitle("Установки wifi");
        popDialog.setView(Viewlayout);

        final EditText ssid = (EditText) Viewlayout.findViewById(R.id.etSSID);
        ssid.clearFocus();
        final EditText ssidPass = (EditText) Viewlayout.findViewById(R.id.etSSIDPASS);
        ssidPass.clearFocus();

        ArrayAdapter<String> adapterW = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wifiMode);
        ArrayAdapter<String> adapterS = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wifiSecurityMode);

        Spinner spinnerW = (Spinner) Viewlayout.findViewById(R.id.spinwWifiMode);
        Spinner spinnerS = (Spinner) Viewlayout.findViewById(R.id.spinSecur);

        spinnerW.setAdapter(adapterW);
        spinnerS.setAdapter(adapterS);

        byte[]cfg = loadConfig();
        if(cfg.length > 0)
        {

            spinnerS.setSelection((int)cfg[1]);
            spinnerW.setSelection((int)cfg[0]);
            String str = new String(cfg);
            ssid.setText(str.substring(2,str.indexOf('$')));
            ssidPass.setText(str.substring(str.indexOf('$') + 1, str.length()));
        }

        spinnerS.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                wSecur = (byte) position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        spinnerW.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                wMode = (byte) position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        popDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String wCfgStr = (char)wMode + "" + (char)wSecur + ssid.getText().toString() + "$" + ssidPass.getText().toString();
                        saveConfig(wCfgStr);
//                        sendCmdmd(CMD_SET_WIFI, deviceIP);
//                        UDPCommands.wifiSettings = wCfgStr;
                        UDPCommands.sendCmd(UDPCommands.CMD_WIFI, wCfgStr.getBytes(), ip);
                        dialog.dismiss();
                    }
                });
        popDialog.create();
        popDialog.show();
    }
    //==============================================================================================
    public static String configReference = "com.voodoo.solar";
    //==============================================================================================
    void saveConfig(String aStr) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(configReference, aStr);

        editor.apply();
    }
    //==============================================================================================
    byte[] loadConfig() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String conf = sharedPreferences.getString(configReference, "") ;
        return conf.getBytes();
    }
    //==============================================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity.clientActivityCreated = 0;
    }
}
