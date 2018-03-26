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
using System.Threading;
using System.Windows.Threading;

namespace SOLAR_APP
{
	/// <summary>
	/// Interaction logic for SlaveCfg.xaml
	/// </summary>
	public partial class SlaveCfg : Window
	{
		
		DispatcherTimer dispatcherTimer;
		
		public SlaveCfg()
		{
			InitializeComponent();
			
			bManual.Background = Brushes.AliceBlue;
			
			dispatcherTimer = new DispatcherTimer();
			dispatcherTimer.Tick += new EventHandler(dispatcherTimer_Tick);
			dispatcherTimer.Interval = TimeSpan.FromMilliseconds(100);//new TimeSpan(0, 0, 1);
			dispatcherTimer.Start();

		}
		//======================================================================
		private void dispatcherTimer_Tick(object sender, EventArgs e)
		{
			bManual.Content = "" + Window1.slavestt;
		}
	}
}