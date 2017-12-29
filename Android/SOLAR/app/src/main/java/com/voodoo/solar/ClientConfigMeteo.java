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
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.net.InetAddress;

import static com.voodoo.solar.MainActivity.BROADCAST_ACTION;

public class ClientConfigMeteo extends Activity {

    TextView tvIp, tvWind, tvTime, tvAzim, tvElev, tvWindS, tvlight;

    public final static String BC_CFG_DATA = "broadcast CFG data";

    public static InetAddress ip;
    BroadcastReceiver br, br_cfg;

    public static byte data[], cfgData[];
    double azimuth, elevation;


    com.voodoo.solar.imgPosition imgSun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meteo);

        tvWind = (TextView) findViewById(R.id.tvWind);
        tvTime = (TextView) findViewById(R.id.tvMeteoTime);
        tvAzim = (TextView) findViewById(R.id.tvMeteoAzimuth);
        tvElev = (TextView) findViewById(R.id.tvMeteoElevation);
        tvWindS = (TextView) findViewById(R.id.tvMeteoWind);
        tvlight = (TextView) findViewById(R.id.tvMeteoLight);

        tvIp = (TextView) findViewById(R.id.tvIPMeteo);
        tvIp.setText("" + ip.getHostAddress());

        imgSun = (com.voodoo.solar.imgPosition) findViewById(R.id.imgPos);

        //================================================
        br = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                String input = intent.getStringExtra(MainActivity.PARAM_LIGTH);
                tvWind.setText(input);

                tvTime.setText((data[0] & 0xff) + "." + (data[1] & 0xff) + "." + (data[2] & 0xff) + ", " +
                               (data[3] & 0xff) + ":" + (data[4] & 0xff) + ":" + (data[5] & 0xff));
                azimuth   = 0.01 * (double)((data[6] & 0xff) | ((data[7] << 8)));
                elevation = 0.01 * (double)((data[8] & 0xff) | ((data[9] << 8)));
                tvAzim.setText(String.format("%.1f", azimuth));
                tvElev.setText(String.format("%.1f", elevation));
                tvWindS.setText("" + ((data[10] & 0xff) | ((data[11] << 8))));
                tvlight.setText("" + ((data[12] & 0xff) | ((data[13] << 8))));

                imgPosition.azimuth = azimuth;//sunPos.azimuth;
//                if(sunPos.elev < 0)
                    imgPosition.elevation = elevation;
                //else                imgPosition.elevation = 0;
                imgSun.invalidate();
            }
        };
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(br, intFilt);

        //================================================
        br_cfg = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                dialog_set();
            }
        };

        IntentFilter intFilt2 = new IntentFilter(BC_CFG_DATA);
        registerReceiver(br_cfg, intFilt2);

        Button btnSet = (Button) findViewById(R.id.btnMeteoSett);
        //================================================
        btnSet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                byte [] c = new byte[1];
                c[0] = UDPCommands.GET;
                UDPCommands.sendCmd(UDPCommands.CMD_CFG, c, ip);
            }
        });

        Button wifi = (Button) findViewById(R.id.btnCfgMeteo);
        //================================================
        wifi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog_wifi();
            }
        });

        Button bSync = (Button) findViewById(R.id.btnMeteoSync);
        //================================================
        bSync.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                byte time[] = MainActivity.getCurrentTime();
                UDPCommands.sendCmd(UDPCommands.CMD_SYNC, time, ip);
            }
        });
    }

    //==============================================================================================
    void dialog_set() {
        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.dialog_meteo, (ViewGroup) findViewById(R.id.meteo));

        popDialog.setIcon(R.drawable.wifi_small);
        popDialog.setTitle("Установки");
        popDialog.setView(Viewlayout);

        final EditText lat = (EditText) Viewlayout.findViewById(R.id.tvMeteoLatit);
        lat.clearFocus();
        final EditText lon = (EditText) Viewlayout.findViewById(R.id.tvMeteoLongit);
        lon.clearFocus();
        final EditText zone = (EditText) Viewlayout.findViewById(R.id.tvMeteoZone);
        zone.clearFocus();
        final EditText wind = (EditText) Viewlayout.findViewById(R.id.tvMeteoWindCFG);
        wind.clearFocus();
        final EditText light = (EditText) Viewlayout.findViewById(R.id.tvMeteoLightCFG);
        light.clearFocus();


        double l = 0.01 * ((cfgData[0] & 0xff) | (cfgData[1] << 8));
        lat.setText  (String.format("%.2f", l).replace(',', '.'));
        l = 0.01 * ((cfgData[2] & 0xff) | (cfgData[3] << 8));
        lon.setText  (String.format("%.2f", l).replace(',', '.'));

        zone.setText ("" + ((cfgData[4] & 0xff) | ((cfgData[5] << 8))));
        wind.setText ("" + ((cfgData[6] & 0xff) | ((cfgData[7] << 8))));
        light.setText("" + ((cfgData[8] & 0xff) | ((cfgData[9] << 8))));

        popDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        byte[] tmp = new byte[11];
                        tmp[0] = UDPCommands.SET;
                        tmp[1] = (byte) (getInt(lat.getText().toString()) & 0xff);
                        tmp[2] = (byte) ((getInt(lat.getText().toString()) >> 8) & 0xff);
                        tmp[3] = (byte) (getInt(lon.getText().toString()) & 0xff);
                        tmp[4] = (byte) ((getInt(lon.getText().toString()) >> 8) & 0xff);
                        tmp[5] = (byte) (Integer.parseInt(zone.getText().toString()) & 0xff);
                        tmp[6] = (byte) ((Integer.parseInt(zone.getText().toString()) >> 8) & 0xff);
                        tmp[7] = (byte) (Integer.parseInt(wind.getText().toString()) & 0xff);
                        tmp[8] = (byte) ((Integer.parseInt(wind.getText().toString()) >> 8) & 0xff);
                        tmp[9] = (byte) (Integer.parseInt(light.getText().toString()) & 0xff);
                        tmp[10] =(byte) ((Integer.parseInt(light.getText().toString()) >> 8) & 0xff);
                        UDPCommands.sendCmd(UDPCommands.CMD_CFG, tmp, ip);
                        dialog.dismiss();
                    }
                });
        popDialog.create();
        popDialog.show();
    }
    //==============================================================================================
    int  getInt(String str)
    {
        return (int)( Double.parseDouble(str) * 100);
    }
    //==============================================================================================
    private String[] wifiMode= {"NULL_MODE","STATION_MODE","SOFTAP_MODE","STATIONAP_MODE"};
    private String[] wifiSecurityMode = {"AUTH_OPEN","AUTH_WEP","AUTH_WPA_PSK","AUTH_WPA2_PSK","AUTH_WPA_WPA2_PSK","AUTH_MAX"};
    byte wMode, wSecur;
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
