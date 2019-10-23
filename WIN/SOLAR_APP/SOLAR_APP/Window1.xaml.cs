/*
 * Created by SharpDevelop.
 * User: Voodoo
 * Date: 06.03.2018
 * Time: 10:19
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
using System.Threading;
using System.Windows.Threading;
using System.ComponentModel;
using System.Collections.ObjectModel;
using System.Windows.Media.Imaging;

using System.Windows.Shapes;


namespace SOLAR_APP
{
	/// <summary>
	/// Interaction logic for Window1.xaml
	/// </summary>
	public partial class Window1 : Window
	{
		public const byte  OK			=		(0xff);
		public const byte BAD			=		(0x00);

		public const byte ID_SLAVE		=	(byte)(0x3C);
		public const byte ID_METEO		=	(0x3D);
		public const byte ID_MASTER		=	(0x7E);

		public const byte CMD_ANGLE   	=	(0x10);
		public const byte CMD_AZIMUTH	=	(0x11);
		public const byte CMD_SET_POSITION=	(0x12);

		public const byte CMD_LEFT		=	(0x20);
		public const byte CMD_RIGHT		=	(0x21);
		public const byte CMD_UP	    =	(0x22);
		public const byte CMD_DOWN		=	(0x23);
		public const byte CMD_GOHOME	=	(0x24);
		public const byte CMD_MANUAL_MOVE	=	(0x25);

		public const byte CMD_STATE		=	(0xA0);
		public const byte CMD_MODE		=	(0xA1);
		public const byte CMD_VERSION	=	(0xA2);

		public const byte CMD_SYNC		=	(0xE0);
		public const byte CMD_SERVICE	=	(0xE1);

		public const byte CMD_CFG		=	(0xC0);
		public const byte CMD_WIFI		=	(0xC1);
		public const byte CMD_FWUPDATE	=	(0xC2);
		
		//======================================================================
		public const byte STOPPED   = (byte)0x0;
		public const byte TRACKING  = (byte)0x1;
		public const byte ALARM     = (byte)0x2;
		public const byte MANUAL_ALARM     = (byte)0x3;
		//byte meteoState = 0;
		//======================================================================
		
		public static string version = "";
		
		public class User: INotifyPropertyChanged
        {
			private String _mstate;
			public string mState
			{ 
				get
				{ return _mstate;}
				set
				{
					_mstate = value;
					if (PropertyChanged != null)
						PropertyChanged(this, new PropertyChangedEventArgs("mState"));
				}
			}
			
			private String _url;
			public string url
			{ 
				get
				{ return _url;}
				set
				{
					_url = value;
					if (PropertyChanged != null)
						PropertyChanged(this, new PropertyChangedEventArgs("url"));
				}
			}
                
               
			public event PropertyChangedEventHandler PropertyChanged;
        }
		
		//======================================================================
		public class Phone: INotifyPropertyChanged
		{
//			public string ImagePath { get; set; }
			private String _ImagePath;
			public string ImagePath
			{ 
				get
				{ return _ImagePath;}
				set
				{
					_ImagePath = value;
					if (PropertyChanged != null)
						PropertyChanged(this, new PropertyChangedEventArgs("ImagePath"));
				}
			}
			
			//public string Title { get; set; } // модель телефона	
			private String _Title;
			public string Title
			{ 
				get
				{ return _Title;}
				set
				{
					_Title = value;
					if (PropertyChanged != null)
						PropertyChanged(this, new PropertyChangedEventArgs("Title"));
				}
			}
			
            public event PropertyChangedEventHandler PropertyChanged;			
		}
		public ObservableCollection<Phone> Phones { get; set; }
		//======================================================================
		
		
		DispatcherTimer dispatcherTimer;
		public User us
		{get; private set;}
		
		bool first = true, show = false;
		
		public static ObservableCollection<SlaveState> items = new ObservableCollection<SlaveState>();
		
		public Window1()
		{
			InitializeComponent();
			this.DataContext = this;
			//AddHandler(FrameworkElement.MouseDownEvent, new MouseButtonEventHandler(meteoCfg), true);
			// Создаем поток для прослушивания
			Thread tRec = new Thread(new ThreadStart(Receiver));
			tRec.IsBackground = true;
                tRec.Start();
                
			dispatcherTimer = new DispatcherTimer();
			dispatcherTimer.Tick += new EventHandler(dispatcherTimer_Tick);
			dispatcherTimer.Interval = TimeSpan.FromMilliseconds(100);//new TimeSpan(0, 0, 1);
			dispatcherTimer.Start();
			
			us= new User();
			lvSlave.ItemsSource = items;
			
			us.url = "/SOLAR_APP;component/Images/dual_compass_rose.png";
			
			btnSync.IsChecked = true;
			
			Phones = new ObservableCollection<Phone>
			{
				new Phone {ImagePath="/Images/wind.png", Title="iPhone 6S"},
				new Phone {ImagePath="/Images/temper.png", Title="Lumia 950"},
				new Phone {ImagePath="/Images/humid.png", Title="Nexus 5X"},
				
			};
			phonesList.ItemsSource = Phones;
			
		}
		//======================================================================
		static string returnData, cfgIncome;
		struct itemInfo
		{
			public int ip;
			public string pitch;
			public string roll;
			public string head;
			public string light;
			public string terms;
			public string stt;
			public int timeOut;
		};
		
		struct masterInfo
		{
			public string date;
			public string time;
			public int azim;
			public int elev;
			public int wind;
			public int light;
			public int stt;
			
			public int fcWind;
			public int fcTemp;
			public int fcHumid;		
			public string fcIcon;
			public string fcRegion;
		};
		static masterInfo mInfo;
		
		static itemInfo []iInfo = new itemInfo[256];		
		int cX, cY;
		
		public static string lat = "48.5";
		public static string lon = "32.24";
		//======================================================================
		public static int currentSlave = 0, slavestt; 
		//======================================================================
		private void dispatcherTimer_Tick(object sender, EventArgs e)
		{
			
			if(appModeMaster) 	
			{				
				bMaster.Content = "ON";
				Socket sockA = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
			                         ProtocolType.Udp);		
				IPEndPoint endPointA = new IPEndPoint(IPAddress.Parse("192.168.43.255"), 7171);
				
				byte[] send_buffer = {ID_MASTER, CMD_STATE, 0, (byte) 0xcc, (byte) 0xcc};
				
				sockA.SendTo(send_buffer , endPointA);
				sockA.Close();
			}
			else				bMaster.Content = "OFF";
			
			
//			us.url = "https://maps.googleapis.com/maps/api/staticmap?center=" +
//				lat +
//				"," +
//				lon +
//				"&zoom=12&size=300x300&path=weight:3%7Ccolor:blue%7Cenc:{coaHnetiVjM??_SkM??~R" +
//				"&key=AIzaSyD_D1xWxD7orZOlcgizFhepXfGFacQMXck";
			
			if(returnData != null) 
			{
				lblDate.Content  = mInfo.date + ", " + mInfo.time;				 
				lblAzim.Content  = String.Format("{0}°", (double)mInfo.azim / 100);
				lblElev.Content  = String.Format("{0}°", (double)mInfo.elev / 100);
				lblWind.Content  = mInfo.wind;
				lblLight.Content = mInfo.light;
				
				Phones[0].Title  = String.Format("{0} m/s", (float)mInfo.fcWind  / 100);
				Phones[1].Title  = String.Format("{0} °C",   (float)mInfo.fcTemp  / 100);
				Phones[2].Title = String.Format("{0} %",   (float)mInfo.fcHumid / 100);
				
				string icon = "/SOLAR_APP;component/Images/OpenWeather/" + mInfo.fcIcon + "@2x.png";
				
				fcIcon.Source = new BitmapImage(new Uri(icon, UriKind.Relative)); 
				lblRegion.Content = mInfo.fcRegion;
				
				returnData = null;
				
				
				string str = mInfo.date + ", " + mInfo.time + '\n' +
					"Azimith: "  + mInfo.azim + '\n' +
					"Elevation: "  + mInfo.elev + '\n' +
					"Wind: "  + mInfo.wind+ '\n' +
					"Light: "  + mInfo.light + '\n'; 
				returnData = null;				
				us.mState = str;
				
				//============== buttons ======================================
				//meteoState = data[14];
                switch(mInfo.stt)
                {
                    case TRACKING:
                    {
                        bServ.IsEnabled = (true);
                        bServ.Background = Brushes.Green;
                        bServ.Content = "TRACKING" + '\n' + "STARTED";
                        bAlarm.Background = (Brushes.Green);
                        bAlarm.Content = "ALARM" + '\n' + "INACTIVE";
                    }break;

                    case STOPPED:
                    {
                        bServ.IsEnabled = true;
                        bServ.Background = (Brushes.Red);
                        bServ.Content = "TRACKING" + '\n' + "STOPPED";
                        bAlarm.Background = (Brushes.Green);
                        bAlarm.Content = "ALARM" + '\n' + "INACTIVE";
                    }break;

                    case ALARM:
                    case MANUAL_ALARM:
                    {
                        bServ.IsEnabled = (false);
                        bAlarm.Content = "ALARM" + '\n' + "ACTIVATED";
                        bAlarm.Background = (Brushes.Red);
                    }break;
                }
				
				
			}
			//else
			{
				//=====================================
				cX = (int)compass.ActualWidth / 2;
				cY = (int)compass.ActualHeight/ 2;

				int lineLength = (int)((mInfo.elev/100) * cX/(90));

				double angle =(mInfo.azim / 100);
				angle = 180 - angle;
				angle = angle * Math.PI / 180;

				line.X1 = cX;
				line.Y1 = cY;
				line.X2 = cX + lineLength * Math.Sin(angle);
				line.Y2 = cY + lineLength * Math.Cos(angle);

				bola.SetValue(Canvas.LeftProperty, (double)line.X2 - 15); //set x
				bola.SetValue(Canvas.TopProperty, (double)line.Y2 - 15); //set y
				
				if(first) {getMeteoCfg(); first = false;};
				if(cfgIncome != null)
				{
					cfgIncome = null;
					if(show)
					{
						mCfgWin win2 = new mCfgWin();
						win2.ShowDialog();
					}
					else
					{
						show = true;
						lat = ("" + mCfgWin.vals[0]).Replace(',', '.');
						lon = ("" + mCfgWin.vals[1]).Replace(',', '.');
					}
					
				}
				
				for(int i = 0; i < 256; i++)
				{
					if(iInfo[i].ip != 0) 
					{		
						
						if(iInfo[i].ip == 1000)
						{
							iInfo[i].ip = 1;
							items.Add(new SlaveState(){
								          	ip    = i,
								          	pitch = iInfo[i].pitch,
								          	roll  = iInfo[i].roll,
								          	head  = iInfo[i].head,
								          	light = iInfo[i].light,
								          	terms = iInfo[i].terms});
						}
						else
						for(int a = 0; a < items.Count; a++)
						{
							if(items[a].ip == i)
							{
								
								
								items[a].pitch = "" + iInfo[i].pitch;
//								items[a].roll  = "" + iInfo[i].roll;
								items[a].head  = "" + iInfo[i].head;
								items[a].light = "" + iInfo[i].light;
								items[a].terms = "" + iInfo[i].terms;
								
								slavestt = Int32.Parse(iInfo[items[currentSlave].ip].stt);
								
								int ss = Int32.Parse(iInfo[items[a].ip].stt);
								
//								if(((byte)(ss & (byte)0xff) & (byte)0x28) != 0)
//									items[a].roll = "АВАРИЯ";
//								else
//								{
//									if(((byte)(ss & (byte)0xff) & 0x02) == 0)
//										items[a].roll = "АВТОМАТ";
//									else
//										items[a].roll = "РУЧНОЙ";
//								}	
								
								if(((byte)(ss & (byte)0xff) & 0x02) != 0)
									items[a].roll = "РУЧНОЙ";
								else if(((byte)(ss & (byte)0xff) & (byte)0x20) == (byte)0x20)
									items[a].roll = "АВАРИЯ:М";
								else if(((byte)(ss & (byte)0xff) & (byte)0x08) == (byte)0x08)
									items[a].roll = "АВАРИЯ:Д";
								else if(((byte)(ss & (byte)0xff) & 0x02) == 0)
									items[a].roll = "АВТОМАТ";
								
							}
							
							// delete item if device has disapeared
							if(iInfo[items[a].ip].timeOut < 100) 
								iInfo[items[a].ip].timeOut++;
							else
							{
								iInfo[items[a].ip].ip = 0;
								items.Remove(items[a]);
							}
						}						
					}
				}					
			}
		}
		//======================================================================
		static byte[] addr = new byte[4];
		//======================================================================
		public static void Receiver()
        {
            // Создаем UdpClient для чтения входящих данных
            UdpClient receivingUdpClient = new UdpClient(7171);

            IPEndPoint RemoteIpEndPoint = null;

            try
            {
               
                while (true)
                {
                    // Ожидание дейтаграммы
                    byte[] receiveBytes = receivingUdpClient.Receive(
                       ref RemoteIpEndPoint);

                    // Преобразуем и отображаем данные
                    switch(receiveBytes[0])
                    {                    		
                    	case ID_METEO:	                    
	                    	switch(receiveBytes[1])
	                    	{
	                    		case CMD_STATE:
	                    			if(receiveBytes[2] > 1)
	                    			{
	                    				mIp = RemoteIpEndPoint.Address;
	                    				mInfo.date  = receiveBytes[5] + "." + receiveBytes[4] + "." + receiveBytes[3] ;
	                    				mInfo.time  = receiveBytes[6] + ":" + receiveBytes[7] + ":" + receiveBytes[8];
	                    				mInfo.azim  = (int)( receiveBytes[9]  | (receiveBytes[10]) << 8);
	                    				mInfo.elev  = (int)( receiveBytes[11] | (receiveBytes[12]) << 8);
	                    				mInfo.wind  = (int)( receiveBytes[13] | (receiveBytes[14]) << 8);
	                    				mInfo.light = (int)( receiveBytes[15] | (receiveBytes[16]) << 8);
	                    				mInfo.stt =   (int) (receiveBytes[17]);
	                    				
	                    				if(receiveBytes.Length > 19)
	                    				{
		                    				mInfo.fcWind  = (int)(receiveBytes[18] | (receiveBytes[19]) << 8);	                    				
		                    				mInfo.fcTemp  = (int)(short)(receiveBytes[20] | (receiveBytes[21] << 8));	                    				
		                    				mInfo.fcHumid = (int)(receiveBytes[22] | receiveBytes[23] << 8);	                    				
		                    				mInfo.fcIcon = Encoding.ASCII.GetString(new byte[]{ receiveBytes[44], receiveBytes[45], receiveBytes[46] });	                    				
		                    				
		                    				int i;
		                    				for( i = 0; i < 20; i++) {if(receiveBytes[24 + i]== 0) break;}
		                    				
		                    				byte[] tmp = new byte[i];
		                    				for(int a = 0; a < i; a++) tmp[a] = receiveBytes[24 + a];
		                    				mInfo.fcRegion = Encoding.ASCII.GetString(tmp);
	                    				}	                    				
	                    				returnData  = "123";
	                    			}
	                    			
	                    			break;
	                    			
	                    		case (byte) CMD_CFG:
	                    			cfgIncome = "123";
	                    			mCfgWin.vals[0] = (double)( receiveBytes[3]  | (receiveBytes[4])  << 8) / 100;
	                    			mCfgWin.vals[1] = (double)( receiveBytes[5]  | (receiveBytes[6])  << 8) / 100;
	                    			mCfgWin.vals[2] = (double)( receiveBytes[7]  | (receiveBytes[8])  << 8) ;
	                    			mCfgWin.vals[3] = (double)( receiveBytes[9]  | (receiveBytes[10]) << 8);
	                    			mCfgWin.vals[4] = (double)( receiveBytes[11] | (receiveBytes[12]) << 8) ;
	                    			
	                                mCfgWin.vals[5] = (double)( receiveBytes[13] | (receiveBytes[14]) << 8) ;
	                                mCfgWin.vals[6] = (double)( receiveBytes[15] | (receiveBytes[16]) << 8) ;
	                                mCfgWin.vals[7] = (double)( receiveBytes[17] | (receiveBytes[18]) << 8) ;
	                                mCfgWin.vals[8] = (double)( receiveBytes[19] | (receiveBytes[20]) << 8) ;
	                    			
	                    			break;
	                    	}break;
	                    	
	                    	
	                    case ID_SLAVE:	                    	
	                    		switch(receiveBytes[1])
	                    		{
	                    			case CMD_STATE:
	                    				sIp = RemoteIpEndPoint.Address;
	                    				addr = sIp.GetAddressBytes();
	                    				
	                    				double tt = Double.Parse(String.Format("{0,4:N1}", 
	                    				                           ((double)BitConverter.ToInt16(new byte[] { receiveBytes[7], receiveBytes[8] }, 0)/10000) * (180.0 / Math.PI)));
	                    				if(tt < 0) tt += 360;   
	                    				
	                    				iInfo[(int)addr[3]].pitch = "" + Double.Parse(String.Format("{0,4:N1}", ((double)BitConverter.ToInt16(new byte[] { receiveBytes[3], receiveBytes[4] }, 0)/10000) * (180.0 / Math.PI)));
	                    				iInfo[(int)addr[3]].roll  = "" + Double.Parse(String.Format("{0,4:N1}", ((double)BitConverter.ToInt16(new byte[] { receiveBytes[5], receiveBytes[6] }, 0)/10000) * (180.0 / Math.PI)));
	                    				iInfo[(int)addr[3]].head  = "" + tt;
	                    				iInfo[(int)addr[3]].light = "" + BitConverter.ToInt16(new byte[] { receiveBytes[9], receiveBytes[10] }, 0);
	                    				iInfo[(int)addr[3]].terms = "" + receiveBytes[11];
	                    				iInfo[(int)addr[3]].stt   = "" + receiveBytes[12];
	                    				
               				
	                    				if (iInfo[(int)addr[3]].ip == 0) iInfo[(int)addr[3]].ip    = 1000;
	                    				iInfo[(int)addr[3]].timeOut = 0;
	                    				 
	                    				break;
	                    			case CMD_VERSION:
	                    				
	                    				byte [] tmp = new byte[21];
	                    				for(int t = 0; t<21; t++) tmp[t] = receiveBytes[t + 3];
	                    				version =  Encoding.UTF8.GetString(tmp);
	                    				
	                    				break;
	                    		}break;
                    }
                }
            }            
            catch (Exception ex)
            {
               returnData = "Возникло исключение: " + ex.ToString() + "\n  " + ex.Message;
            }
        }
		
		public static IPAddress mIp, sIp, ipToControl;
		//===========================================================================================
		void meteoCfg(object sender, MouseButtonEventArgs e)
		{
			getMeteoCfg();
		}
		
		//===========================================================================================
		void getMeteoCfg()
		{
			if(mIp != null)
			{
				Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
				                         ProtocolType.Udp);		
				IPEndPoint endPoint = new IPEndPoint(mIp, 7171);
				
				byte[] send_buffer = {ID_MASTER, CMD_CFG, 1, 0, (byte) 0xcc, (byte) 0xcc};
				sock.SendTo(send_buffer , endPoint);
			}
		}
		//===========================================================================================
		void bServClick(object sender, RoutedEventArgs e)
		{				
			Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
			                         ProtocolType.Udp);		
			IPEndPoint endPoint = new IPEndPoint(mIp, 7171);
			
			byte[] send_buffer = {ID_MASTER, CMD_SERVICE, 1, 0, (byte) 0xcc, (byte) 0xcc};
			
			if(mInfo.stt == TRACKING) send_buffer[3] = STOPPED;
			else send_buffer[3] = TRACKING;
			
			sock.SendTo(send_buffer , endPoint);
			
			var scope = FocusManager.GetFocusScope(bServ); // elem is the UIElement to unfocus
			FocusManager.SetFocusedElement(scope, null);
			Keyboard.ClearFocus();
		}
		
		//===========================================================================================
		void bAlarmClick(object sender, RoutedEventArgs e)
		{
			Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
			                         ProtocolType.Udp);		
			IPEndPoint endPoint = new IPEndPoint(mIp, 7171);
			
			byte[] send_buffer = {ID_MASTER, CMD_SERVICE, 1, 0, (byte) 0xcc, (byte) 0xcc};
			
			if(mInfo.stt != MANUAL_ALARM) send_buffer[3] = MANUAL_ALARM;
                else send_buffer[3] = STOPPED;
			
			sock.SendTo(send_buffer , endPoint);
			
			var scope = FocusManager.GetFocusScope(bServ); // elem is the UIElement to unfocus
			FocusManager.SetFocusedElement(scope, null);
			Keyboard.ClearFocus();
		}
		//===========================================================================================
		Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
			                         ProtocolType.Udp);
		//===========================================================================================
		void slaveCfgClick(object sender, MouseButtonEventArgs e)
		{
			ListView fe = (ListView)sender;
			currentSlave = fe.SelectedIndex;
			
			
//			Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
//			                         ProtocolType.Udp);				
			
			byte [] adr = new byte[4];
			adr = sIp.GetAddressBytes();
			adr[3] = (byte)items[currentSlave].ip;
			
			ipToControl = new IPAddress(adr);
			
			IPEndPoint endPoint = new IPEndPoint(ipToControl, 7171);
			
			byte[] send_buffer = {ID_MASTER, CMD_VERSION, 0, (byte) 0xcc, (byte) 0xcc};
			version = "";
			
			sock.SendTo(send_buffer , endPoint);
			
			
			
			SlaveCfg winS = new SlaveCfg();
			winS.ShowDialog();
			
		}
		
		//===========================================================================================
		void firmwareCheckClick(object sender, MouseButtonEventArgs e)
		{
			if(MessageBox.Show(
				"Update device firmware?", 
				"Firmware update",
				MessageBoxButton.OKCancel,
				MessageBoxImage.Question) == MessageBoxResult.OK)
			{
				IPEndPoint endPoint = new IPEndPoint(mIp, 7171);
				byte[] buf = new byte[6];
				buf[1]  = (byte)CMD_FWUPDATE;
				sock.SendTo(buf , endPoint);
			}
		}					
		//===========================================================================================
		public class SlaveState: INotifyPropertyChanged
        {
			    public int ip    { get; set; }
			    
			    public string _pitch;
                public string pitch
                {
                	get                	{ return _pitch;}
                	set
                	{
                		_pitch = value;
                		if (PropertyChanged != null)
                			PropertyChanged(this, new PropertyChangedEventArgs("pitch"));
                	}
                }
                
                public string _roll;
                public string roll  {
                	get                	{ return _roll;}
                	set
                	{
                		_roll = value;
                		if (PropertyChanged != null)
                			PropertyChanged(this, new PropertyChangedEventArgs("roll"));
                	}
                }
                
                
                public string _head;
                public string head
                {
                	get                	{ return _head;}
                	set
                	{
                		_head = value;
                		if (PropertyChanged != null)
                			PropertyChanged(this, new PropertyChangedEventArgs("head"));
                	}
                }
                
                public string _light;
                public string light
                {
                	get				{ return _light;}
                	set
                	{
                		_light = value;
                		if (PropertyChanged != null)
                			PropertyChanged(this, new PropertyChangedEventArgs("light"));
                	}
                }
                
//                public string terms { get; set; }
				 public string _terms;
                public string terms
                {
                	get				{ return _terms;}
                	set
                	{
                		_terms = value;
                		if (PropertyChanged != null)
                			PropertyChanged(this, new PropertyChangedEventArgs("terms"));
                	}
                }
                
                
                public event PropertyChangedEventHandler PropertyChanged;
        }
		
		//===========================================================================================
		public void OnWindowClosing(object sender, CancelEventArgs e) 
		{
			dispatcherTimer.Stop();			
		}
		//===========================================================================================
		bool appModeMaster = false;
		//===========================================================================================
		void masterEnableClick(object sender, RoutedEventArgs e)
		{
			if(appModeMaster) 	appModeMaster = false;
			else				appModeMaster = true;
		}
		//===========================================================================================
		void btnSyncClick(object sender, RoutedEventArgs e)
		{
			IPEndPoint endPoint = new IPEndPoint(mIp, 7171);
			
			byte [] tmp = new byte[6];
			
			DateTime now = DateTime.Now;	
			
			
			tmp[0] = (byte)(now.Year - 2000);
			tmp[1] = (byte)(now.Month);
			tmp[2] = (byte)(now.Day);
			
			tmp[3] = (byte)(now.Hour);
			tmp[4] = (byte)(now.Minute);
			tmp[5] = (byte)(now.Second);
			
			byte[] send_buffer = {ID_MASTER, CMD_SYNC, 6,  0,0,0,0,0,0, (byte) 0xcc, (byte) 0xcc};
			for(int i = 0; i < 6; i++) send_buffer[i+3] = tmp[i];
			
			sock.SendTo(send_buffer , endPoint);
			
//			tmp[5] = 0;
//			byte[] buf = new byte[6];
//			buf[1]  = (byte)CMD_FWUPDATE;
//			sock.SendTo(buf , endPoint);
		}		
	}
}
