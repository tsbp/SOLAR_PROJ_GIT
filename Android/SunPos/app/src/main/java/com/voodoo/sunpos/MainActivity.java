package com.voodoo.sunpos;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.lang.Math;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends ActionBarActivity {

    double DEG_TO_RAD  = 0.01745329;
    double PI =  3.141592654;
    double TWOPI = 6.28318531;

    int Hour, Minute = 0, Second = 0, Month = 11, Day = 15, Year, Zone = +2;
    double Lon = -75 * DEG_TO_RAD, Lat = 40 * DEG_TO_RAD;

    TextView tvResult, tvDate;
    EditText etLong, etLatit;
    com.voodoo.sunpos.imagePosition imgPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = (TextView)findViewById(R.id.tvResult);
        tvDate   = (TextView)findViewById(R.id.tvDate);

        etLong =  (EditText) findViewById(R.id.etLong);
        etLatit =  (EditText) findViewById(R.id.etLatit);

        imgPos  = (com.voodoo.sunpos.imagePosition)findViewById(R.id.imgPos);

        Button btnCalc = (Button) findViewById(R.id.btnCalculate);
        btnCalc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Lon = DEG_TO_RAD * Double.parseDouble(etLong.getText().toString());
                Lat = DEG_TO_RAD * Double.parseDouble(etLatit.getText().toString());

                DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                String date = df.format(Calendar.getInstance().getTime());
                tvDate.setText(date);

                getAngles();
                tvResult.setText(print);
            }
        });
    }
    //==============================================================================================
    void Calculate(int aYear, int aMonth, int aDay, int aHour, int aMinute, int aSecond)
    {
        double T, JD_frac, L0, M, e, C, L_true, f, R, GrHrAngle, Obl, RA, Decl, HrAngle;
        long JD_whole, JDx;

        JD_whole = JulianDate(aYear, aMonth, aDay);
        JD_frac = (aHour + aMinute/60. + aSecond/3600.)/24. - 0.5;
        T  = JD_whole - 2451545; T = (T + JD_frac)/36525.;
        L0 = DEG_TO_RAD * Math.IEEEremainder(280.46645 + 36000.76983 * T, 360);
        M  = DEG_TO_RAD * Math.IEEEremainder(357.5291  + 35999.0503  * T, 360);
        e  = 0.016708617 - 0.000042037 * T;
        C  = DEG_TO_RAD * ((1.9146-0.004847*T)*Math.sin(M) + (0.019993-0.000101*T)*Math.sin(2*M) + 0.00029*Math.sin(3*M));
        f  = M + C;
        Obl = DEG_TO_RAD * (23 + 26/60. + 21.448/3600. - 46.815/3600*T);
        JDx = JD_whole - 2451545;
        GrHrAngle = 280.46061837 + (360*JDx)%360 + .98564736629*JDx + 360.98564736629*JD_frac;
        GrHrAngle = Math.IEEEremainder(GrHrAngle,360.);
        L_true    = Math.IEEEremainder(C+L0,TWOPI);
        R    = 1.000001018*(1-e*e)/(1+e*Math.cos(f));
        RA   = Math.atan2(Math.sin(L_true)*Math.cos(Obl),Math.cos(L_true));
        Decl = Math.asin(Math.sin(Obl)*Math.sin(L_true));
        HrAngle = DEG_TO_RAD*GrHrAngle+Lon-RA;
        elev = Math.asin(Math.sin(Lat)*Math.sin(Decl) + Math.cos(Lat)*(Math.cos(Decl)*Math.cos(HrAngle)));

        // Azimuth measured eastward from north.
        azimuth = PI + Math.atan2(Math.sin(HrAngle),Math.cos(HrAngle)*Math.sin(Lat) - Math.tan(Decl)*Math.cos(Lat));
    }
    //==============================================================================================
    String print;
    double elev, azimuth;
    //==============================================================================================
    void getAngles()
    {
        print = "";
        print += ("Longitude and latitude: ");
        print += (String.format("%.2f", Lon / DEG_TO_RAD) + ", ");
        print += (String.format("%.2f", Lat / DEG_TO_RAD) + "\r\n");
        //print += ("year,month,day,local hour,minute,second,elevation,azimuth");
       // print+=("\r\n");

        Year = 2017;
        // Changes may be required in for… loop to get complete
        // daylight coverage in time zones farther west.
        for (Hour=4; Hour<=22; Hour++)
        {
            Calculate(Year, Month, Day, Hour, Minute, Second);

//            print+= year + "." + month + "." + day;  print+=(", ");
            print+= "| " + sstr(Hour + Zone) + ":" + sstr(Minute) + ":" + sstr(Second);

            print+=(",\te: "); print+= String.format("%.2f", elev    / DEG_TO_RAD);
            print+=(",\ta: "); print+= String.format("%.2f", azimuth / DEG_TO_RAD);
            print+=("\r\n");
        }
    }
    //==============================================================================================
    String sstr(int a)
    {
        if(a < 10) return "0" + a;
        return "" + a;
    }
    //==============================================================================================
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
    //==============================================================================================
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
