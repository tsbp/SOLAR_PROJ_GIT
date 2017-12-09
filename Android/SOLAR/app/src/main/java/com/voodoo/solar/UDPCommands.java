package com.voodoo.solar;

import java.net.InetAddress;

/**
 * Created by Voodoo on 23.11.2017.
 */

public class UDPCommands
{
    public final static byte CMD_ANGLE   = (byte)0x10;
    public final static byte CMD_AZIMUTH	= (byte)0x11;
    public final static byte CMD_SET_POSITION	= (byte)0x12;

    public final static byte CMD_LEFT	= (byte)0x20;
    public final static byte CMD_RIGHT	= (byte)0x21;
    public final static byte CMD_UP	    = (byte)0x22;
    public final static byte CMD_DOWN	= (byte)0x23;
    public final static byte CMD_GOHOME	= (byte)0x24;

    public final static byte CMD_STATE	= (byte)0xA0;

    public final static byte CMD_CFG		= (byte)0xC0;
    public final static byte CMD_WIFI		= (byte)0xC1;

    public final static byte ID_MASTER	= (byte)0x7e;
    public final static byte ID_SLAVE     = (byte)0x3c;
    public final static byte ID_METEO     = (byte)0x3D;

//    //==============================================================================================
//    byte[] cmdBuffer = new byte[6];
//    public static String  wifiSettings = "";
    //==============================================================================================
    //public  static void sendCmd(byte aCmd, short angle, short azimuth, byte angIncrement, InetAddress aIP)
    public  static void sendCmd(byte aCmd, byte [] aData, InetAddress aIP)
    {
//        cmdBuffer[0] = (byte) 0xc0;
//        cmdBuffer[1] =        aCmd;
//
//        cmdBuffer[4] = (byte) (0xcc);
//        cmdBuffer[5] = (byte) (0xcc);
        int dataLng = 0;
        byte buf[] = null;
        switch(aCmd)
        {
            case CMD_GOHOME:
                dataLng = 0;
                buf = new byte[5 + dataLng];
                break;

            case CMD_SET_POSITION:
                dataLng = aData.length;
                buf = new byte[5 + aData.length];
                for(int i = 0; i < aData.length; i++) buf[i+3] = aData[i];
                break;

            case CMD_ANGLE:
                dataLng = 2;
                buf = new byte[5 + dataLng];
//                buf[3] = (byte) ((angle) & (byte)0xff);
//                buf[4] = (byte) ((angle >> 8) & (byte)0xff);
                break;

            case CMD_AZIMUTH:
                dataLng = 2;
                buf = new byte[5 + dataLng];
//                buf[3] = (byte) ((azimuth) & (byte)0xff);
//                buf[4] = (byte) ((azimuth >> 8) & (byte)0xff);
                break;

            case CMD_LEFT:
            case CMD_RIGHT:
            case CMD_UP:
            case CMD_DOWN:
                dataLng = 2;
                buf = new byte[5 + dataLng];
//                buf[3] = (byte) ((angIncrement) & (byte)0xff);
//                buf[4] = (byte) ((angIncrement >> 8) & (byte)0xff);
                break;

            case CMD_STATE:
                dataLng = 0;
                buf = new byte[5 + dataLng];
                break;

            case CMD_CFG:
                break;

            case CMD_WIFI:
                dataLng = aData.length;
                buf = new byte[5 + aData.length];
                for(int i = 0; i < aData.length; i++) buf[i+3] = aData[i];
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
