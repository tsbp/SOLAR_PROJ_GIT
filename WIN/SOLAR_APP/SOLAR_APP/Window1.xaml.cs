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
		public const byte CMD_AZIMUTH		=	(0x11);
		public const byte CMD_SET_POSITION=	(0x12);

		public const byte CMD_LEFT		=	(0x20);
		public const byte CMD_RIGHT		=	(0x21);
		public const byte CMD_UP	    	=	(0x22);
		public const byte CMD_DOWN		=	(0x23);
		public const byte CMD_GOHOME		=	(0x24);

		public const byte CMD_STATE		=	(0xA0);

		public const byte CMD_SYNC		=	(0xE0);
		public const byte CMD_SERVICE		=	(0xE1);

		public const byte CMD_CFG			=	(0xC0);
		public const byte CMD_WIFI		=	(0xC1);

		//======================================================================
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
		DispatcherTimer dispatcherTimer;
		public User us
		{get; private set;}
		
		bool first = true, show = false;
		
		public ObservableCollection<SlaveState> items = new ObservableCollection<SlaveState>();
		
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
			dispatcherTimer.Interval = TimeSpan.FromMilliseconds(200);//new TimeSpan(0, 0, 1);
			dispatcherTimer.Start();
			
			us= new User();
			lvSlave.ItemsSource = items;
			
			
		}
		//======================================================================
		static string returnData, cfgIncome;
		struct itemInfo
		{
			public int ip;
			public short pitch;
			public short roll;
			public short head;
			public short light;
			public byte terms;
		};
		
		struct masterInfo
		{
			public string date;
			public string time;
			public int azim;
			public int elev;
			public int wind;
			public int light;
		};
		static masterInfo mInfo;
		
		static itemInfo []iInfo = new itemInfo[256];		
		int cX, cY;
		
		public static string lat = "48.5";
		public static string lon = "32.24";
		//======================================================================
		private void dispatcherTimer_Tick(object sender, EventArgs e)
		{
			
			us.url = "https://maps.googleapis.com/maps/api/staticmap?center=" +
				lat +
				"," +
				lon +
				"&zoom=12&size=300x300&path=weight:3%7Ccolor:blue%7Cenc:{coaHnetiVjM??_SkM??~R" +
				"&key=AIzaSyD_D1xWxD7orZOlcgizFhepXfGFacQMXck";
			
			if(returnData != null) 
			{
				lblDate.Content  = mInfo.date + ", " + mInfo.time;				 
				lblAzim.Content  = String.Format("{0}°", (double)mInfo.azim / 100);
				lblElev.Content  = String.Format("{0}°", (double)mInfo.elev / 100);
				lblWind.Content  = mInfo.wind;
				lblLight.Content = mInfo.light;
				returnData = null;
				
				
				string str = mInfo.date + ", " + mInfo.time + '\n' +
					"Azimith: "  + mInfo.azim + '\n' +
					"Elevation: "  + mInfo.elev + '\n' +
					"Wind: "  + mInfo.wind+ '\n' +
					"Light: "  + mInfo.light + '\n'; 
				returnData = null;				
				us.mState = str;
				
				
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
						 //String.Format("{0,4:N1}", 19 + slTemp.Value);
						double tt = Double.Parse(String.Format("{0,4:N1}", ((double)iInfo[i].head/10000) * (180.0 / Math.PI)));
						if(tt < 0) tt += 360;
						
						if(items.Count == 0) 
							items.Add(new SlaveState(){
								          	ip    = i,
								          	pitch = Double.Parse(String.Format("{0,4:N1}", ((double)iInfo[i].pitch/10000) * (180.0 / Math.PI))),
								          	roll  = Double.Parse(String.Format("{0,4:N1}", ((double)iInfo[i].roll/10000) * (180.0 / Math.PI))),
								          	head  = tt,
								          	light = iInfo[i].light,
								          	terms = iInfo[i].terms});
						else
						for(int a = 0; a < items.Count; a++)
						{
							if(items[a].ip == i)
							{
								items[a].pitch = iInfo[i].pitch;
								items[a].roll  = iInfo[i].roll;
								items[a].head  = iInfo[i].head;
								items[a].light = iInfo[i].light;
								items[a].terms = iInfo[i].terms;
							}
							else
							{
								items.Add(new SlaveState(){
								          	ip    = i,
								          	pitch = iInfo[i].pitch,
								          	roll  = iInfo[i].roll,
								          	head  = iInfo[i].head,
								          	light = iInfo[i].light,
								          	terms = iInfo[i].terms});
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
	                    			break;
	                    	}break;
	                    	
	                    	
	                    case ID_SLAVE:	                    	
	                    		switch(receiveBytes[1])
	                    		{
	                    			case CMD_STATE:
	                    				mIp = RemoteIpEndPoint.Address;
	                    				addr = mIp.GetAddressBytes();
	                    				iInfo[(int)addr[3]].ip    = 1;	                    					                    				
	                    				iInfo[(int)addr[3]].pitch = BitConverter.ToInt16(new byte[] { receiveBytes[3], receiveBytes[4] }, 0);
	                    				iInfo[(int)addr[3]].roll  = BitConverter.ToInt16(new byte[] { receiveBytes[5], receiveBytes[6] }, 0);
	                    				iInfo[(int)addr[3]].head  = BitConverter.ToInt16(new byte[] { receiveBytes[7], receiveBytes[8] }, 0);
	                    				iInfo[(int)addr[3]].light = BitConverter.ToInt16(new byte[] { receiveBytes[9], receiveBytes[10] }, 0);
	                    				iInfo[(int)addr[3]].terms =        receiveBytes[11];
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
		
		public static IPAddress mIp;
		//===========================================================================================
		void meteoCfg(object sender, MouseButtonEventArgs e)
		{
			getMeteoCfg();
		}
		
		//===========================================================================================
		void getMeteoCfg()
		{
			Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
			                         ProtocolType.Udp);		
			IPEndPoint endPoint = new IPEndPoint(mIp, 7171);
			
			byte[] send_buffer = {ID_MASTER, CMD_CFG, 1, 0, (byte) 0xcc, (byte) 0xcc};
			sock.SendTo(send_buffer , endPoint);
		}
		
		
		//===========================================================================================
		public class SlaveState
        {
			    public int ip    { get; set; }
                public double pitch { get; set; }
                public double roll  { get; set; }
                public double head  { get; set; }
                public int light { get; set; }
                public int terms { get; set; }
        }
		//===========================================================================================
		
	}
}