package com.voodoo.solar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class ClientConfig extends Activity {

    TextView tCompass, tAccel, tLight, tAngleV, tAngleH, tNorth, tAzimuth, tAngle;
    SeekBar sbCompass, sbAccel;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_config);

        tAzimuth   = (TextView) findViewById(R.id.tvAzimuth);
        tAngle   = (TextView) findViewById(R.id.tvAngle);

        sbCompass = (SeekBar) findViewById(R.id.sbCompass);
        sbAccel   = (SeekBar) findViewById(R.id.sbAccel);

        Button btnIn4 = (Button)findViewById(R.id.btnIn4);
        btnIn4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), sunPos.class);
                startActivity(intent);

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
}
