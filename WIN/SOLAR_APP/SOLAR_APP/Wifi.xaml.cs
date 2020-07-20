/*
 * Created by SharpDevelop.
 * User: Voodoo
 * Date: 07/20/2020
 * Time: 09:30
 * 
 * To change this template use Tools | Options | Coding | Edit Standard Headers.
 */
using System;
using System.Collections.Generic;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Net;
using System.Net.Sockets;

namespace SOLAR_APP
{
	/// <summary>
	/// Interaction logic for Wifi.xaml
	/// </summary>
	public partial class Wifi : Window
	{
		//==========================================================================================
		Socket sock;
		IPEndPoint endPoint;
		
		//==========================================================================================
		public Wifi(IPAddress ip)
		{
			InitializeComponent();
			sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
			                         ProtocolType.Udp);		
			endPoint = new IPEndPoint(ip, 7171);
			winWifi.Title = ip.ToString();
		}
		
		//==========================================================================================
		void btnApplyClick(object sender, RoutedEventArgs e)
		{
			
			//String wCfgStr = (char) wMode + "" + (char) wSecur + ssid.getText().toString() + "$" + ssidPass.getText().toString() + "#" + otaIp.getText().toString();
			
			string wSettings = "" 
				+ (char) cbWifiMode.SelectedIndex
				+ (char) cbWifiSecurity.SelectedIndex
				+ tbWifiSSID.Text  + "$"
				+ tbWifiSSIDPas.Text  + "#"
				+ tbWifiOTAIP.Text;
			
			byte[] data = Encoding.UTF8.GetBytes(wSettings);
			byte[] buf = new byte[data.Length + 5];
			buf[0] = (byte) Window1.ID_MASTER;;
			buf[1] = Window1.CMD_WIFI;
			buf[2] = (byte) buf.Length;
			buf[buf.Length - 2] = (byte) 0xcc;
			buf[buf.Length - 1] = (byte) 0xcc;
			
			Array.Copy(data, 0, buf, 3, data.Length);
			
			sock.SendTo(buf , endPoint);
			winWifi.Close();
		}
	}
}