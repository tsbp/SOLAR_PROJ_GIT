package com.voodoo.solar;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.InetAddress;

import static com.voodoo.solar.MainActivity.BROADCAST_ACTION;

public class ClientConfig extends Activity {

    TextView tvIp, tvPitch, tvRoll, tvHead, tvLigth, tvTerm, tAzimuth, tAngle;
    SeekBar sbCompass, sbAccel;

    public final static String PARAM_PITCH = "pitch";
    public final static String PARAM_ROLL  = "roll";
    public final static String PARAM_HEAD  = "head";
    public final static String PARAM_LIGTH = "ligth";
    public final static String PARAM_TERM  = "term";


    public static InetAddress ip;
    BroadcastReceiver br;

    short azimuth;
    short angle;
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
                String input = intent.getStringExtra(PARAM_PITCH);
                tvPitch.setText(input);
                input = intent.getStringExtra(PARAM_ROLL);
                tvRoll.setText(input);
                input = intent.getStringExtra(PARAM_HEAD);
                tvHead.setText(input);
                input = intent.getStringExtra(PARAM_LIGTH);
                tvLigth.setText(input);
                input = intent.getStringExtra(PARAM_TERM);
                tvTerm.setText(input);
            }
        };
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(br, intFilt);

        //================================================
        Button btnUp = (Button) findViewById(R.id.btnUp);
        btnUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                UDPCommands.sendCmd(UDPCommands.CMD_UP,  (short)0, (short)0, (byte)0, ip);
            }
        });
        //================================================
        Button btnDown = (Button) findViewById(R.id.btndown);
        btnDown.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                UDPCommands.sendCmd(UDPCommands.CMD_DOWN,  (short)0, (short)0, (byte)0, ip);
            }
        });
        //================================================
        Button btnRight = (Button) findViewById(R.id.btnRight);
        btnRight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UDPCommands.sendCmd(UDPCommands.CMD_RIGHT,  (short)0, (short)0, (byte)0, ip);
            }
        });
        //================================================
        Button btnLeft = (Button) findViewById(R.id.btnLeft);
        btnLeft.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UDPCommands.sendCmd(UDPCommands.CMD_LEFT,  (short)0, (short)0, (byte)0, ip);
            }
        });

        //==========================================================================================
        sbCompass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
               tAzimuth.setText("Azimyth: " + progress*3.6);
                double tmp = Math.toRadians(progress * 3.6);

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
//                angle =  (short)(Math.toRadians(progress * 0.9) * 10000);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity.clientActivityCreated = 0;
    }
}
