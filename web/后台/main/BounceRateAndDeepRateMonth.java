/**
 * 
 * 该Servlet实现跳出率/深访率月数据处理
 * @author Victors
 * 
 */
package com.victors.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.client.transport.TransportClient;
import org.json.JSONObject;

import com.victors.tools.GetDayBaseYearAndMonth;
import com.victors.tools.GetESData;
import com.victors.tools.ProcessNumber;

public class BounceRateAndDeepRateMonth extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/html;charset=utf-8");
		PrintWriter out = response.getWriter();
		ServletContext servletContext = this.getServletContext();
		TransportClient transportClient = (TransportClient)servletContext.getAttribute("transportClient");
		final int MAX_X = 12;//表示横坐标最大的显示数目,修改时不要超过24
		String index_name;
		String[] index = {"res-"};
		String[] type = {"month"};
		ArrayList<Float> the_bounce_rate = new ArrayList<Float>();//跳出率
		ArrayList<Float> the_deep_rate = new ArrayList<Float>();//深访率
		ArrayList<String> time = new ArrayList<String>();
		String s_time;//用于临时存放时间
		Calendar system_time = Calendar.getInstance();//用于获取当前系统时间
		int year;//系统时间年
		int month;//系统时间月
		int day;//系统时间日
		int hour;//系统时间小时
		int start_year;//开始时间年
		int start_month;//开始时间月
		int start_day;//开始时间日
		int start_hour;//开始时间小时
		String host = "192.168.1.104";//ElasticSearch的IP
		int port = 9300;//端口号
		String show_start_time;//展示开始时间
		String show_end_time;//展示结束时间
		//下面为展示开始时间的临时变量
		int temp_start_year;
		int temp_start_month;
		//依次获取系统时间
		year = system_time.get(Calendar.YEAR); 
		month = system_time.get(Calendar.MONTH) + 1; 
		day = system_time.get(Calendar.DATE); 
		hour = system_time.get(Calendar.HOUR_OF_DAY);
		//初始开始时间
		start_year = year;
		start_hour = 0;
		start_day = 1;
		//如果日为1，则处理
		if(day == 1)
		{
			//如果时为0，则上月数据不可获取
			if(hour == 0)
			{
				start_month = month - MAX_X;
			}
			else
			{
				start_month = month - MAX_X + 1;
				month++;
				if(month > 12)
				{
					month = 1;
					year++;
				}
			}
		}
		else
		{
			start_month = month - MAX_X +1;
			month++;
			if(month > 12)
			{
				month = 1;
				year++;
			}
		}
		//处理非正数月
		if(start_month <= 0)
		{
			start_year--;
			start_month = 12 + start_month;
		}
		//处理展示开始时间
		temp_start_year = start_year;
		temp_start_month = start_month - 1;
		if(temp_start_month <= 0)
		{
			temp_start_year--;
			temp_start_month = 12 + temp_start_month;
		}
		show_start_time = temp_start_year + "-" + ProcessNumber.processNumber(temp_start_month);
		int temp_year = start_year;//用于循环临时年
		int temp_month = start_month;//用于循环临时月
		//下列为展示显示时间的变量
		int show_year;
		int show_month;
		//循环进行数据获取处理
		while(true)
		{
			//组合索引名
			index_name = "res-" + temp_year + "." + ProcessNumber.processNumber(temp_month) + "." + ProcessNumber.processNumber(start_day) + "_" + ProcessNumber.processNumber(start_hour);
			index[0] = index_name;
			show_year = temp_year;
			show_month = temp_month - 1;
			if(show_month <= 0)
			{
				show_year--;
				show_month = 12 + show_month;
			}
			show_end_time = show_year + "-" + ProcessNumber.processNumber(show_month);
			//组合时间
			s_time = show_year + "." + ProcessNumber.processNumber(show_month);
			time.add(s_time);
			//String data = GetESData.select_data(host, port, index, type);
			String data = GetESData.selectData(transportClient, index, type);
			String bounce_rate;//跳出率
			String deep_rate;//深访率
			if(data.equals(""))
			{
				bounce_rate = "0";
				deep_rate = "0";
			}
			else
			{
				JSONObject dataJson = new JSONObject(data);//创建一个包含json串的json对象
				bounce_rate = dataJson.getString("Bounce rate");//跳出率
				deep_rate = dataJson.getString("Deep rate");//深访率
			}
			the_bounce_rate.add(Float.parseFloat(bounce_rate));
			the_deep_rate.add(Float.parseFloat(deep_rate));
			//处理下一个时间
			temp_month++;
			if(temp_month > 12)
			{
				temp_month = 1;
				temp_year++;
			}
			if(temp_year == year && temp_month == month)
			{
				break;
			}
		}
		//创建并处理
		JSONObject new_json_obj = new JSONObject();
		new_json_obj.accumulate("bounce_rate", the_bounce_rate);
		new_json_obj.accumulate("deep_rate", the_deep_rate);
		new_json_obj.accumulate("time", time);
		new_json_obj.accumulate("show_start_time", show_start_time);
		new_json_obj.accumulate("show_end_time", show_end_time);
		String new_s_json_obj = new_json_obj.toString();
		out.println(new_s_json_obj);
	}
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doGet(request, response);
	}

}
