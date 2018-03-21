/*
 * Created by SharpDevelop.
 * User: Voodoo
 * Date: 03/19/2018
 * Time: 10:29
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
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Net;
using System.Net.Sockets;

namespace SOLAR_APP
{
	/// <summary>
	/// Interaction logic for Window2.xaml
	/// </summary>
	/// 	
		
	public partial class mCfgWin : Window
	{
		
		public ObservableCollection<cfgData> items = new ObservableCollection<cfgData>();
		
		string[] names = {"Latitude", "Longitude", "Time zone", "Wind", "Light"};
		public static double [] vals = {0, 0, 0, 0, 0};
		
		public mCfgWin()
		{
			InitializeComponent();
			lvConfigs.ItemsSource = items;
			
			for(int i = 0; i < names.Length; i++)
			{
				items.Add(new cfgData(i){
				          	name = names[i],
				          	data = vals[i]});				
			}
			this.DataContext = this;
		}	
		
		
		//=========================================================================
		void okBtnClick(object sender, RoutedEventArgs e)
		{
			this.Close();
			Socket sock = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
			                         ProtocolType.Udp);		
			IPEndPoint endPoint = new IPEndPoint(Window1.mIp, 7171);
			
			byte[] buf = new byte[16]; //{(byte) 0x7e, (byte) 0xc0, (byte) 0x01, (byte) 0x00, (byte) 0xcc, (byte) 0xcc};
			
			buf[0]  = (byte) 0x7e;
			buf[1]  = (byte) 0xc0;
			buf[2]  = (byte) 0x01;
			buf[3]  = (byte) 0x01; //set
			buf[4]  = (byte) ((int)(vals[0] * 100) &  0xff);
			buf[5]  = (byte) ((int)(vals[0] * 100) >> 8);
			buf[6]  = (byte) ((int)(vals[1] * 100) & 0xff);
			buf[7]  = (byte) ((int)(vals[1] * 100) >> 8);
			buf[8]  = (byte) ((int)(vals[2]) & 0xff);
			buf[9]  = (byte) ((int)vals[2] >> 8);
			buf[10] = (byte) ((int)(vals[3]) & 0xff);
			buf[11] = (byte) ((int)vals[3] >> 8);
			buf[12] = (byte) ((int)(vals[4]) & 0xff);
			buf[13] = (byte) ((int)vals[4] >> 8);
			buf[14] = (byte) 0xcc;
			buf[15] = (byte) 0xcc;
			
			sock.SendTo(buf , endPoint);
		}
				
		//=========================================================================
		public class cfgData : INotifyPropertyChanged
        {
			public int  Index
			{ get; private set;}
			
			    public string name {get; set;}
			    
			    private double _data; //{get; set;}
			    public double data
			    {
			    	get
			    	{ 			    		
			    		return _data;
			    	}
			    	
			    	set
			    	{
			    		_data = value;
			    		vals[Index] = value;
			    		
			    		if (PropertyChanged != null)
			    			PropertyChanged(this, new PropertyChangedEventArgs("data"));
			    	}
			    }
			    
			    public cfgData(int index)
			    {
			    	this.Index = index;
			    }
			    
			    public event PropertyChangedEventHandler PropertyChanged;
        }
	}
}