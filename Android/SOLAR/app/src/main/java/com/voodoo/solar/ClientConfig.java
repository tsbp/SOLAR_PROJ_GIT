package com.voodoo.solar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.voodoo.solar.IPHelper.getBroadcastIP4AsBytes;
import static com.voodoo.solar.MainActivity.BROADCAST_ACTION_CLIENT;

public class ClientConfig extends Activity {

    TextView tvIp/*, tvStt, tvPitch, tvRoll, tvHead, tvLigth, tvTerm*/;

    ListView lvClientInfo;
    ImageView ivMotor, ivSensor;

    public final static String CALIB_DATA = "Calib data";

    public static InetAddress ip;
    BroadcastReceiver br;

    byte sysState;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_config);

        GLSurfaceView view = (GLSurfaceView) findViewById(R.id.w3D);
        view.setRenderer(new OpenGLRenderer());


//        tvPitch = (TextView) findViewById(R.id.tvPitch);
//        tvRoll = (TextView) findViewById(R.id.tvRoll);
//        tvHead = (TextView) findViewById(R.id.tvHead);
//        tvLigth = (TextView) findViewById(R.id.tvLigth);
//        tvTerm = (TextView) findViewById(R.id.tvTerm);
//        tvStt = (TextView) findViewById(R.id.tvFault);

        ivMotor = (ImageView) findViewById(R.id.ivMotorFault);
        ivSensor = (ImageView) findViewById(R.id.ivSensorFault);

        lvClientInfo = (ListView) findViewById(R.id.lvClientInfo);

        tvIp = (TextView) findViewById(R.id.tvIP);
        tvIp.setText("" + ip.getHostAddress());

        //================================================
        final Button btnManual = (Button) findViewById(R.id.btnManual);
        btnManual.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                byte[] data = new byte[1];
                if ((sysState & (byte) 0x02) == 0)
                    data[0] = 0x00;
                else
                    data[0] = 0x02;
                UDPCommands.sendCmd(UDPCommands.CMD_MODE, data, ip);
            }
        });
        //================================================
        br = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                try {
                    lvBuid(intent);
                    String input = intent.getStringExtra(MainActivity.PARAM_STT);
                    sysState = Byte.parseByte(input);
                    if ((sysState & (byte) 0x02) != 0)
                        btnManual.setBackgroundResource(R.drawable.control);
                    else
                        btnManual.setBackgroundResource(R.drawable.auto);

                    input = intent.getStringExtra(MainActivity.PARAM_TERM);
                    if(input.contains("Д")) {
                        ivSensor.setImageResource(R.drawable.sensor);
                    }
                    else {
                        ivSensor.setImageResource(R.drawable.ok);
                    }

                    String s = "M";
                    if(input.contains(s)) {
                        ivMotor.setImageResource(R.drawable.check);
                    }
                    else {
                        ivMotor.setImageResource(R.drawable.ok);
                    }

//                        state = "А:ДM";
//                    else if(((byte)(in[12] & 0xff) & (byte)0x20) == (byte)0x20)
//                        state = "А:М";
//                    else if(((byte)(in[12] & 0xff) & (byte)0x08) == (byte)0x08)
//                        state = "А:Д";
                }
                catch (Exception ignored){

                }
            }
        };
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION_CLIENT);
        registerReceiver(br, intFilt);

        //================================================
        Button btnUp = (Button) findViewById(R.id.btnUp);

        btnUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                buttoEventHandle(event, (byte) 0x80);
                return true;
            }
        });
        //================================================
        Button btnDown = (Button) findViewById(R.id.btnDown);
        btnDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                buttoEventHandle(event, (byte) 0x08);
                return true;
            }
        });
        //================================================
        Button btnRight = (Button) findViewById(R.id.btnRight);
        btnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                buttoEventHandle(event, (byte) 0x02);
                return true;
            }
        });
        //================================================
        Button btnLeft = (Button) findViewById(R.id.btnLeft);
        btnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                buttoEventHandle(event, (byte) 0x04);
                return true;
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

                for (int i = 1; i < 4; i++)
                    for (int j = 1; j < 4; j++)
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

        Button bUpdate = (Button) findViewById(R.id.btnUpdate);
        //================================================
        bUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                dialog_update();
            }
        });
    }

    //==============================================================================================
    private final String ATTRIBUTE_LOGO = "attr_logo";
    private final String ATTRIBUTE_VALUE = "attr_value";

    private String [] paramStrings = {
            MainActivity.PARAM_PITCH, MainActivity.PARAM_ROLL,
            MainActivity.PARAM_HEAD, MainActivity.PARAM_LIGTH/*,
            MainActivity.PARAM_TERM, MainActivity.PARAM_STT*/};
    private int [] logos = {
            R.drawable.angle, R.drawable.tilt, R.drawable.compass_small, R.drawable.light};
    //==============================================================================================
    void lvBuid(Intent intent) {

        ArrayList<Map<String, Object>> data = new ArrayList<>(paramStrings.length);
        Map<String, Object> m;

        for (int i = 0; i < paramStrings.length ; i++) {
            m = new HashMap<>();
            m.put(ATTRIBUTE_LOGO, logos[i]);
            m.put(ATTRIBUTE_VALUE, intent.getStringExtra(paramStrings[i]));
            data.add(m);
        }

        String[] from = {ATTRIBUTE_LOGO, ATTRIBUTE_VALUE};
        int[] to = {R.id.picto, R.id.value};
        SimpleAdapter sAdapter = new SimpleAdapter(this, data, R.layout.client_info_item2, from, to);
        lvClientInfo.setAdapter(sAdapter);
    }

    //==============================================================================================
    private String getFaults(String stringExtra) {
        return "Norma";
    }

    //==============================================================================================
    private void buttoEventHandle(MotionEvent event, byte direction) {
        byte[] tmp;
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                tmp = new byte[]{direction};
                break;

            case MotionEvent.ACTION_UP:
                tmp = new byte[]{0x00};
                break;

            default:
                return;
        }
        UDPCommands.sendCmd(UDPCommands.CMD_MANUAL_MOVE, tmp, ip);
    }

    //==============================================================================================
    void dialog_update() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update");
        builder.setMessage("Firmware update?");
        builder.setCancelable(true);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { // Кнопка ОК
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UDPCommands.sendCmd(UDPCommands.CMD_FWUPDATE, null, ip);
                dialog.dismiss(); // Отпускает диалоговое окно
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() { // Кнопка ОК
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Отпускает диалоговое окно
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //==============================================================================================
    private final String ATTRIBUTE_T = "attr_t";
    private final String ATTRIBUTE_X = "attr_x";
    private final String ATTRIBUTE_Y = "attr_y";
    private final String ATTRIBUTE_Z = "attr_z";
    String[][] vals = {
            {" ", "X", "Y", "Z"},
            {"V", "1", "2", "3"},
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

        final ListView lvData = (ListView) Viewlayout.findViewById(R.id.lvCalib);

        //================================================
        final BroadcastReceiver brd = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {

                vals[1][1] = intent.getStringExtra("x_raw");
                vals[1][2] = intent.getStringExtra("y_raw");
                vals[1][3] = intent.getStringExtra("z_raw");

                for (int i = 1; i < 4; i++) {
                    if (Integer.parseInt(vals[1][i]) >= Integer.parseInt(vals[2][i]))
                        vals[2][i] = vals[1][i];
                    if (Integer.parseInt(vals[1][i]) < Integer.parseInt(vals[3][i]))
                        vals[3][i] = vals[1][i];
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
                        byte[] b = {UDPCommands.GET_COMPASS_RAW};
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
                        byte[] b = new byte[13];
                        b[0] = UDPCommands.SET_COMPASS_CALIBS;
                        int cnt = 1;
                        for (int i = 0; i < 2; i++)
                            for (int j = 0; j < 3; j++) {
                                short a = (short) Integer.parseInt(vals[i + 2][j + 1]);
                                b[cnt] = (byte) (a & 0xff);
                                cnt++;
                                b[cnt] = (byte) ((a >> 8) & 0xff);
                                cnt++;
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

        final TextView tvAcc = (TextView) Viewlayout.findViewById(R.id.tvAccRaw);
        tvAcc.setText("45,0");

        final TextView t0 = (TextView) Viewlayout.findViewById(R.id.tv0deg);
        final TextView t90 = (TextView) Viewlayout.findViewById(R.id.tv90deg);

        final Button b0 = (Button) Viewlayout.findViewById(R.id.btn0deg);
        b0.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                t0.setText(tvAcc.getText().toString());
            }
        });

        final Button b90 = (Button) Viewlayout.findViewById(R.id.btn90deg);
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
                int ang = Integer.parseInt(intent.getStringExtra("acc_raw"));
                tvAcc.setText("" + String.format("%.3f", Math.toDegrees((double) ang / 10000)));
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
                        byte[] b = {UDPCommands.GET_ACCEL_RAW};
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
                        byte[] b = new byte[5];
                        b[0] = UDPCommands.SET_ACCEL_CALIBS;

                        double a0 = Math.toRadians(Double.parseDouble(t0.getText().toString().replace(',', '.')));
                        double a90 = Math.toRadians(Double.parseDouble(t90.getText().toString().replace(',', '.')));

                        b[1] = (byte) ((int) (a0 * 10000) & 0xff);
                        b[2] = (byte) (((int) (a0 * 10000) >> 8) & 0xff);
                        b[3] = (byte) ((int) (a90 * 10000) & 0xff);
                        b[4] = (byte) (((int) (a90 * 10000) >> 8) & 0xff);


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
    private String[] wifiMode = {"NULL_MODE", "STATION_MODE", "SOFTAP_MODE", "STATIONAP_MODE"};
    private String[] wifiSecurityMode = {"AUTH_OPEN", "AUTH_WEP", "AUTH_WPA_PSK", "AUTH_WPA2_PSK", "AUTH_WPA_WPA2_PSK", "AUTH_MAX"};
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
        final EditText otaIp = (EditText) Viewlayout.findViewById(R.id.etOTAIP);
        otaIp.clearFocus();


        ArrayAdapter<String> adapterW = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wifiMode);
        ArrayAdapter<String> adapterS = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wifiSecurityMode);

        Spinner spinnerW = (Spinner) Viewlayout.findViewById(R.id.spinwWifiMode);
        Spinner spinnerS = (Spinner) Viewlayout.findViewById(R.id.spinSecur);

        spinnerW.setAdapter(adapterW);
        spinnerS.setAdapter(adapterS);

        byte[] cfg = loadConfig();
        if (cfg.length > 0) {

            spinnerS.setSelection((int) cfg[1]);
            spinnerW.setSelection((int) cfg[0]);
            String str = new String(cfg);
            ssid.setText(str.substring(2, str.indexOf('$')));
            ssidPass.setText(str.substring(str.indexOf('$') + 1, str.indexOf('#')));
            otaIp.setText(str.substring(str.indexOf('#') + 1, str.length()));
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
                        String wCfgStr = (char) wMode + "" + (char) wSecur + ssid.getText().toString() + "$" + ssidPass.getText().toString() + "#" + otaIp.getText().toString();
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
        String conf = sharedPreferences.getString(configReference, "");
        return conf.getBytes();
    }

    //==============================================================================================
    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity.clientActivityCreated = "";
    }
}
