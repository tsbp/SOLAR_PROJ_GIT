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
                
               
			public event PropertyChangedEventHandler PropertyChanged;
        }
		
		DispatcherTimer dispatcherTimer;
		public User us
		{get; private set;}
		
		public ObservableCollection<SlaveState> items = new ObservableCollection<SlaveState>();
		
		public Window1()
		{
			InitializeComponent();
			this.DataContext = this;
			// Создаем поток для прослушивания
                Thread tRec = new Thread(new ThreadStart(Receiver));
                tRec.Start();
                
			dispatcherTimer = new DispatcherTimer();
			dispatcherTimer.Tick += new EventHandler(dispatcherTimer_Tick);
			dispatcherTimer.Interval = TimeSpan.FromMilliseconds(1000);//new TimeSpan(0, 0, 1);
			dispatcherTimer.Start();
			
			us= new User();
			lvSlave.ItemsSource = items;
		}
		//======================================================================
		static string returnData;
		struct itemInfo
		{
			public int ip;
			public int pitch;
			public int roll;
			public int head;
			public int light;
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
		//======================================================================
		private void dispatcherTimer_Tick(object sender, EventArgs e)
		{
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

				bola.SetValue(Canvas.LeftProperty, (double)line.X2 - 10); //set x
				bola.SetValue(Canvas.TopProperty, (double)line.Y2 - 10); //set y
				
				for(int i = 0; i < 256; i++)
				{
					if(iInfo[i].ip != 0) 
					{
						if(items.Count == 0) 
							items.Add(new SlaveState(){
								          	ip    = i,
								          	pitch = iInfo[i].pitch,
								          	roll  = iInfo[i].roll,
								          	head  = iInfo[i].head,
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
                    if(receiveBytes.Length > 15)
                    {
                    	mInfo.date  = receiveBytes[5] + "." + receiveBytes[4] + "." + receiveBytes[3] ;
                    	mInfo.time  = receiveBytes[6] + ":" + receiveBytes[7] + ":" + receiveBytes[8];
                    	mInfo.azim  = (int)( receiveBytes[9]  | (receiveBytes[10]) << 8);
                    	mInfo.elev  = (int)( receiveBytes[11] | (receiveBytes[12]) << 8);
                    	mInfo.wind  = (int)( receiveBytes[13] | (receiveBytes[14]) << 8);
                    	mInfo.light = (int)( receiveBytes[15] | (receiveBytes[16]) << 8);	
                    	returnData  = "123";
                    }
                   
                   
                    if(receiveBytes[0] == (byte)0x3c)
                    {
                    	addr = RemoteIpEndPoint.Address.GetAddressBytes();
                    	iInfo[(int)addr[3]].ip    = 1;
                    	iInfo[(int)addr[3]].pitch = (int)( receiveBytes[3] | ( receiveBytes[4])  << 8);
                    	iInfo[(int)addr[3]].roll  = (int)( receiveBytes[5] | ( receiveBytes[6])  << 8);
                    	iInfo[(int)addr[3]].head  = (int)( receiveBytes[7] | ( receiveBytes[8])  << 8);
                    	iInfo[(int)addr[3]].light = (int)( receiveBytes[9] | ( receiveBytes[10]) << 8);
                    	iInfo[(int)addr[3]].terms = receiveBytes[11];
                    }
                }
            }
            catch (Exception ex)
            {
               returnData = "Возникло исключение: " + ex.ToString() + "\n  " + ex.Message;
            }
        }
		
		//===========================================================================================
		public class SlaveState
        {
			    public int ip    { get; set; }
                public int pitch { get; set; }
                public int roll  { get; set; }
                public int head  { get; set; }
                public int light { get; set; }
                public int terms { get; set; }
        }
		//===========================================================================================
		
	}
}