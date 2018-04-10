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
			
			sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
			                         ProtocolType.Udp);		
			endPoint = new IPEndPoint(Window1.sIp, 7171);
			
			buf[0]  = (byte) Window1.ID_MASTER;
			buf[2]  = (byte) 0x01;
			buf[4] = (byte) 0xcc;
			buf[5] = (byte) 0xcc;	
			
			slMode = new slaveMode();
			//slMode.sMode = "/SOLAR_APP;component/Images/light.png";
			winSlave.Title = "IP: " + Window1.sIp.ToString();
			
//			//========== get version =======================
//			buf[1]  = (byte) Window1.CMD_VERSION;	
//			sock.SendTo(buf , endPoint);

		}
		
		//======================================================================
		private void dispatcherTimer_Tick(object sender, EventArgs e)
		{
			lblVersion.Content = "ver: " + Window1.version;
			//bManual.Content = "" + Window1.slavestt;
			lPitch.Content  = Window1.items[Window1.currentSlave]._pitch;			
			lHead.Content   = Window1.items[Window1.currentSlave]._head;
			lLight.Content  = Window1.items[Window1.currentSlave]._light;
			

			if(((byte)(Window1.slavestt & (byte)0xff) & 0x02) != 0)
			{
				slMode.sMode = "/SOLAR_APP;component/Images/control.png";
				btnUp.IsEnabled = true;
				btnDwn.IsEnabled = true;
				btnLeft.IsEnabled = true;
				btnRight.IsEnabled = true;	
			}
			else
			{
				slMode.sMode = "/SOLAR_APP;component/Images/auto.png";
				btnUp.IsEnabled = false;
				btnDwn.IsEnabled = false;
				btnLeft.IsEnabled = false;
				btnRight.IsEnabled = false;				
			}
				
		}
		
		//======================================================================
		Socket sock;
		IPEndPoint endPoint;
		byte[] buf = new byte[6];
		//======================================================================
		void bManual_Click(object sender, RoutedEventArgs e)
		{		
			buf[1]  = (byte) Window1.CMD_MODE;	
			
			if(((byte)(Window1.slavestt & (byte)0xff) & 0x02) != 0)
				buf[3] = 2;
			else
				buf[3] = 0;
			
			sock.SendTo(buf , endPoint);			
		}
		
		//===========================================================================================
		public void OnSlaveCfgwClosing(object sender, CancelEventArgs e) 
		{
			dispatcherTimer.Stop();	
			sock.Close();
		}
		
		//===========================================================================================
		void bUPkeyDwn(object sender, MouseButtonEventArgs e)
		{			
			buf[1]  = (byte) Window1.CMD_MANUAL_MOVE;			
			buf[3]  = (byte) 0x80; //set
			
			sock.SendTo(buf , endPoint);			
		}
		
		//===========================================================================================
		void bUPkeyUp(object sender, MouseButtonEventArgs e)
		{
			
			buf[1]  = (byte) Window1.CMD_MANUAL_MOVE;			
			buf[3]  = (byte) 0x00; //set
			
			sock.SendTo(buf , endPoint);
		}	
		
		//===========================================================================================
		void bDWNkeyDwn(object sender, MouseButtonEventArgs e)
		{			
			buf[1]  = (byte) Window1.CMD_MANUAL_MOVE;			
			buf[3]  = (byte) 0x08; //set
			
			sock.SendTo(buf , endPoint);			
		}
		
		//===========================================================================================
		void bDWNkeyUp(object sender, MouseButtonEventArgs e)
		{
			
			buf[1]  = (byte) Window1.CMD_MANUAL_MOVE;			
			buf[3]  = (byte) 0x00; //set
			
			sock.SendTo(buf , endPoint);
		}	
		
		//===========================================================================================
		void bLFTkeyDwn(object sender, MouseButtonEventArgs e)
		{			
			buf[1]  = (byte) Window1.CMD_MANUAL_MOVE;			
			buf[3]  = (byte) 0x04; //set
			
			sock.SendTo(buf , endPoint);			
		}
		
		//===========================================================================================
		void bLFTkeyUp(object sender, MouseButtonEventArgs e)
		{
			
			buf[1]  = (byte) Window1.CMD_MANUAL_MOVE;			
			buf[3]  = (byte) 0x00; //set
			
			sock.SendTo(buf , endPoint);
		}	
		
		//===========================================================================================
		void bRIGkeyDwn(object sender, MouseButtonEventArgs e)
		{			
			buf[1]  = (byte) Window1.CMD_MANUAL_MOVE;			
			buf[3]  = (byte) 0x02; //set
			
			sock.SendTo(buf , endPoint);			
		}
		
		//===========================================================================================
		void bRIGkeyUp(object sender, MouseButtonEventArgs e)
		{
			
			buf[1]  = (byte) Window1.CMD_MANUAL_MOVE;			
			buf[3]  = (byte) 0x00; //set
			
			sock.SendTo(buf , endPoint);
		}	
	}		
}