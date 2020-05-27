package com.voodoo.solar;

import java.net.InetAddress;

/**
 * Created by Voodoo on 23.11.2017.
 */

public class UDPCommands
{
    public final static byte GET   = (byte)0x0;
    public final static byte SET   = (byte)0x1;

    public final static byte GET_COMPASS_RAW   = (byte)0x0;
    public final static byte SET_COMPASS_CALIBS   = (byte)0x1;
    public final static byte GET_ACCEL_RAW   = (byte)0x2;
    public final static byte SET_ACCEL_CALIBS   = (byte)0x3;

//    #define GET_COMPASS_RAW		(0)
//    #define SET_COMPASS_CALIBS	(1)
//    #define GET_ACCEL_RAW		(2)
//    #define SET_ACCEL_CALIBS	(3)


    public final static byte CMD_ANGLE   = (byte)0x10;
    public final static byte CMD_AZIMUTH	= (byte)0x11;
    public final static byte CMD_SET_POSITION	= (byte)0x12;

    public final static byte CMD_LEFT	= (byte)0x20;
    public final static byte CMD_RIGHT	= (byte)0x21;
    public final static byte CMD_UP	    = (byte)0x22;
    public final static byte CMD_DOWN	= (byte)0x23;
    public final static byte CMD_GOHOME	= (byte)0x24;
    public final static byte CMD_MANUAL_MOVE	=	(byte)0x25;

    public final static byte CMD_CALIB	= (byte)0x30;

    public final static byte CMD_STATE	= (byte)0xA0;
    public final static byte CMD_MODE	= (byte)0xA1;

    public final static byte CMD_CFG		= (byte)0xC0;
    public final static byte CMD_WIFI		= (byte)0xC1;
    public final static byte CMD_FWUPDATE	= (byte)0xC2;

    public final static byte CMD_SYNC		= (byte)0xE0;
    public final static byte CMD_SERVICE	= (byte)0xE1;

    public final static byte ID_MASTER	= (byte)0x7e;
    public final static byte ID_SLAVE     = (byte)0x3c;
    public final static byte ID_METEO     = (byte)0x3D;

    public final static byte NOP     = (byte)0xff;
    //==============================================================================================
    //public  static void sendCmd(byte aCmd, short angle, short azimuth, byte angIncrement, InetAddress aIP)
    public  static void sendCmd(byte aCmd, byte [] aData, InetAddress aIP)
    {
        int dataLng;// = aData.length;
        if (aData != null) dataLng = aData.length;
        else dataLng = 0;
        byte buf[] = new byte[5 + dataLng];//null;
        switch(aCmd)
        {
            case CMD_SERVICE:
            case CMD_GOHOME:
            case CMD_CALIB:
            case CMD_ANGLE:
            case CMD_AZIMUTH:
            case CMD_LEFT:
            case CMD_RIGHT:
            case CMD_UP:
            case CMD_DOWN:
            case CMD_SET_POSITION:
            case CMD_SYNC:
            case CMD_CFG:
            case CMD_WIFI:
            case CMD_MODE:
            case CMD_MANUAL_MOVE:
                //dataLng = aData.length;
//                buf = new byte[5 + aData.length];
                for(int i = 0; i < dataLng; i++) buf[i+3] = aData[i];
                break;
        }

        if(aIP != null && buf != null)
        {
            buf[0] = ID_MASTER;
            buf[1] = aCmd;
            buf[2] = (byte) dataLng;

            // add crc16
            buf[dataLng + 3] = (byte) 0xcc;
            buf[dataLng + 4] = (byte) 0xcc;

            udpSend(buf, aIP);
        }
    }
    //==============================================================================================
    public static void udpSend(byte[] aByte, InetAddress ip)
    {
        DataFrame df = new DataFrame(aByte);
        MainActivity.udpProcessor.send(ip,df);
    }
}
