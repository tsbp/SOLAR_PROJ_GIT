package com.voodoo.sunpos;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    double DEG_TO_RAD  = 0.01745329;
    double PI =  3.141592654;
    double TWOPI = 6.28318531;

    TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = (TextView)findViewById(R.id.tvResult);
        Button btnCalc = (Button) findViewById(R.id.btnCalculate);

        btnCalc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getAngles();
                tvResult.setText(print);
            }
        });
    }

    String print;
    void getAngles() {
        int hour, minute = 0, second = 0, month = 6, day = 21, year, zone = 5;
        double Lon = -75 * DEG_TO_RAD, Lat = 40 * DEG_TO_RAD;
        double T, JD_frac, L0, M, e, C, L_true, f, R, GrHrAngle, Obl, RA, Decl, HrAngle, elev, azimuth;
        long JD_whole, JDx;

        print = "";

        print += ("Longitude and latitude ");
        print += (Lon / DEG_TO_RAD + "  \r\n");
        print += (Lat / DEG_TO_RAD + "\r\n");
        print += ("year,month,day,local hour,minute,second,elevation,azimuth");

        year = 2015;
        // Changes may be required in for… loop to get complete
        // daylight coverage in time zones farther west.
//        for (hour=10; hour<=24; hour++) {
//            JD_whole=JulianDate(year,month,day);
//            JD_frac=(hour+minute/60.+second/3600.)/24.-.5;
//            T=JD_whole-2451545; T=(T+JD_frac)/36525.;
//            L0=DEG_TO_RAD*fmod(280.46645+36000.76983*T,360);
//            M=DEG_TO_RAD*fmod(357.5291+35999.0503*T,360);
//            e=0.016708617-0.000042037*T;
//            C=DEG_TO_RAD*((1.9146-0.004847*T)*sin(M)+(0.019993-0.000101*T)*sin(2*M)+0.00029*sin(3*M));
//            f=M+C;
//            5
//            Obl=DEG_TO_RAD*(23+26/60.+21.448/3600.-46.815/3600*T);
//            JDx=JD_whole-2451545;
//            GrHrAngle=280.46061837+(360*JDx)%360+.98564736629*JDx+360.98564736629*JD_frac;
//            GrHrAngle=fmod(GrHrAngle,360.);
//            L_true=fmod(C+L0,TWOPI);
//            R=1.000001018*(1-e*e)/(1+e*cos(f));
//            RA=atan2(sin(L_true)*cos(Obl),cos(L_true));
//            Decl=asin(sin(Obl)*sin(L_true));
//            HrAngle=DEG_TO_RAD*GrHrAngle+Lon-RA;
//            elev=asin(sin(Lat)*sin(Decl)+cos(Lat)*(cos(Decl)*cos(HrAngle)));
//            // Azimuth measured eastward from north.
//            azimuth=PI+atan2(sin(HrAngle),cos(HrAngle)*sin(Lat)-tan(Decl)*cos(Lat));
//            Serial.print(year); Serial.print(","); Serial.print(month);
//            Serial.print(","); Serial.print(day); Serial.print(", ");
//            Serial.print(hour-zone); Serial.print(",");
//            Serial.print(minute); Serial.print(","); Serial.print(second);
//            // (Optional) display results of intermediate calculations.
//            //Serial.print(","); Serial.print(JD_whole);
//            //Serial.print(","); Serial.print(JD_frac,7);
//            //Serial.print(","); Serial.print(T,7);
//            //Serial.print(","); Serial.print(L0,7);
//            //Serial.print(","); Serial.print(M,7);
//            //Serial.print(","); Serial.print(e,7);
//            //Serial.print(","); Serial.print(C,7);
//            //Serial.print(","); Serial.print(L_true,7);
//            //Serial.print(","); Serial.print(f,7);
//            //Serial.print(","); Serial.print(R,7);
//            //Serial.print(","); Serial.print(GrHrAngle,7);
//            //Serial.print(","); Serial.print(Obl,7);
//            //Serial.print(","); Serial.print(RA,7);
//            //Serial.print(","); Serial.print(Decl,7);
//            //Serial.print(","); Serial.print(HrAngle,7);
//            Serial.print(","); Serial.print(elev/DEG_TO_RAD,3);
//            Serial.print(","); Serial.print(azimuth/DEG_TO_RAD,3); Serial.println();
//    }
    }

    long JulianDate(int year, int month, int day) {
        long JD_whole;
        int A, B;
        if (month <= 2) {
            year--;
            month += 12;
        }
        A = year / 100;
        B = 2 - A + A / 4;
        JD_whole = (long) (365.25 * (year + 4716)) + (int) (30.6001 * (month + 1)) + day + B - 1524;
        return JD_whole;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
