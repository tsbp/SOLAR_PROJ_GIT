/*
 * Created by SharpDevelop.
 * User: Voodoo
 * Date: 03/26/2018
 * Time: 15:13
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
using System.Windows.Media.Imaging;
using System.Threading;
using System.Windows.Threading;
using System.Net;
using System.Net.Sockets;
using System.ComponentModel;

namespace SOLAR_APP
{
	/// <summary>
	/// Interaction logic for SlaveCfg.xaml
	/// </summary>
	public partial class SlaveCfg : Window
	{
		
		DispatcherTimer dispatcherTimer;
		
		
		//======================================================================
		public class slaveMode: INotifyPropertyChanged
        {
			private string _sMode;
			public string sMode
			{ 
				get
				{ return _sMode;}
				set
				{
					_sMode = value;
					if (PropertyChanged != null)
						PropertyChanged(this, new PropertyChangedEventArgs("sMode"));
				}
			}
			
			public event PropertyChangedEventHandler PropertyChanged;
        }
		public slaveMode slMode {get; private set;}
		//===========================================================================
		public SlaveCfg()
		{
			InitializeComponent();
			this.DataContext = this;						
			
			dispatcherTimer = new DispatcherTimer();
			dispatcherTimer.Tick += new EventHandler(dispatcherTimer_Tick);
			dispatcherTimer.Interval = TimeSpan.FromMilliseconds(100);//new TimeSpan(0, 0, 1);
			dispatcherTimer.Start();
			
			slMode = new slaveMode();
			//slMode.sMode = "/SOLAR_APP;component/Images/light.png";

		}
		
		//======================================================================
		private void dispatcherTimer_Tick(object sender, EventArgs e)
		{
			bManual.Content = "" + Window1.slavestt;
			lPitch.Content  = Window1.items[Window1.currentSlave]._pitch;			
			lHead.Content   = Window1.items[Window1.currentSlave]._head;
			lLight.Content  = Window1.items[Window1.currentSlave]._light;
			

			if(((byte)(Window1.slavestt & (byte)0xff) & 0x02) != 0)
				slMode.sMode = "/SOLAR_APP;component/Images/control.ico";
			else
				slMode.sMode = "/SOLAR_APP;component/Images/auto.gif";
			
			
			
				
		}
		
		//======================================================================
		void bManual_Click(object sender, RoutedEventArgs e)
		{
			Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
			                         ProtocolType.Udp);		
			IPEndPoint endPoint = new IPEndPoint(Window1.sIp, 7171);			
			
			byte[] buf = new byte[6]; //{(byte) 0x7e, (byte) 0xc0, (byte) 0x01, (byte) 0x00, (byte) 0xcc, (byte) 0xcc};
			
			buf[0]  = (byte) 0x7e;
			buf[1]  = (byte) 0xA1;
			buf[2]  = (byte) 0x01;
//			buf[3]  = (byte) 0x01; //set
			
			buf[4] = (byte) 0xcc;
			buf[5] = (byte) 0xcc;
			
			
			if(((byte)(Window1.slavestt & (byte)0xff) & 0x02) != 0)
				buf[3] = 2;
			else
				buf[3] = 0;
			
			sock.SendTo(buf , endPoint);
		}
	}
}