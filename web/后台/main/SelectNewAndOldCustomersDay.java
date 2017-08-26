/**
 * 
 * 该servlet实现查询新老顾客数据（按日计）
 * @author Victors
 */
package com.victors.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

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

public class SelectNewAndOldCustomersDay extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/html;charset=utf-8");
		PrintWriter out = response.getWriter();
		ServletContext servletContext = this.getServletContext();
		TransportClient transportClient = (TransportClient)servletContext.getAttribute("transportClient");
		String index_name;
		String[] index = {"res-"};
		String[] type = {"day"};
		ArrayList<Integer> the_new_customers = new ArrayList<Integer>();//新顾客
		ArrayList<Integer> the_old_customers = new ArrayList<Integer>();//老顾客
		ArrayList<String> time = new ArrayList<String>();
		String s_time;//用于临时存放时间
		int start_year = Integer.parseInt(request.getParameter("start_year"));
		int start_month = Integer.parseInt(request.getParameter("start_month"));
		int start_day = Integer.parseInt(request.getParameter("start_day"));
		int start_hour = 0;
		int end_year = Integer.parseInt(request.getParameter("end_year"));
		int end_month = Integer.parseInt(request.getParameter("end_month"));
		int end_day = Integer.parseInt(request.getParameter("end_day"));
		String show_start_time;//展示开始时间
		String show_end_time;//展示结束时间
		show_end_time = end_year + "-" + ProcessNumber.processNumber(end_month) + "-" + ProcessNumber.processNumber(end_day);
		show_start_time = start_year + "-" + ProcessNumber.processNumber(start_month) + "-" + ProcessNumber.processNumber(start_day);
		//开始时间加1
		start_day++;
		if(start_day > GetDayBaseYearAndMonth.getDay(start_year, start_month))
		{
			start_day = 1;
			start_month++;
			if(start_month > 12)
			{
				start_month = 1;
				start_year++;
			}
		}
		//处理结束时间
		for(int i = 0; i < 2; i++)
		{
		    end_day++;
		    if(end_day > GetDayBaseYearAndMonth.getDay(end_year, end_month))
		    {
			    end_day = 1;
			    end_month++;
			    if(end_month > 12)
			    {
			    	end_month = 1;
			    	end_year++;
			    }
		    }
		}
		int temp_year = start_year;//用于循环临时年
		int temp_month = start_month;//用于循环临时月
		int temp_day = start_day;//用于循环临时日
		//下列为展示显示时间的变量
		int show_year;
		int show_month;
		int show_day;
		//循环进行数据获取处理
		while(true)
		{
			//组合索引名
			index_name = "res-" + temp_year + "." + ProcessNumber.processNumber(temp_month) + "." + ProcessNumber.processNumber(temp_day) + "_" + ProcessNumber.processNumber(start_hour);
			index[0] = index_name;
			show_year = temp_year;
			show_month = temp_month;
			show_day = temp_day - 1;
			if(show_day <= 0)
			{
				show_month = show_month - 1;
				if(show_month == 0)
				{
					show_month = 12;
					show_year = show_year - 1;
				}
				show_day = GetDayBaseYearAndMonth.getDay(show_year, show_month) + show_day;
			}
			//组合时间
			s_time = show_year + "." + ProcessNumber.processNumber(show_month) + "." + ProcessNumber.processNumber(show_day);
			time.add(s_time);
			String data = GetESData.selectData(transportClient, index, type);
			String new_customer;//新顾客
			String old_customer;//老顾客
			if(data.equals(""))
			{
				new_customer = "0";
				old_customer = "0";
			}
			else
			{
				JSONObject dataJson = new JSONObject(data);//创建一个包含json串的json对象
				JSONObject new_and_old_customers = dataJson.getJSONObject("The new and old customers");//新老顾客
				new_customer = new_and_old_customers.getString("new");//新顾客
				old_customer = new_and_old_customers.getString("old");//老顾客
			}
			the_new_customers.add(Integer.parseInt(new_customer));
			the_old_customers.add(Integer.parseInt(old_customer));
			//处理下一个时间
			temp_day++;
			if(temp_day > GetDayBaseYearAndMonth.getDay(temp_year, temp_month))
			{
				temp_day = 1;
				temp_month++;
				if(temp_month > 12)
				{
					temp_month = 1;
					temp_year++;
				}
			}		
			if(temp_year == end_year && temp_month == end_month && temp_day == end_day)
			{
				break;
			}
		}
		//创建并处理
		JSONObject new_json_obj = new JSONObject();
		new_json_obj.accumulate("new_customers", the_new_customers);
		new_json_obj.accumulate("old_customers", the_old_customers);
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
