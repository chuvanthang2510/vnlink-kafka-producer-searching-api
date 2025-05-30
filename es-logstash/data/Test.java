/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vnlink.api.thread;

import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

/**
 *
 * @author truongnv
 */
public class Test
{
	private static final String URL = "http://localhost:8080/api/orders/batch"; // giả sử API nhận batch là /batch
	private static final int BATCH_SIZE = 1000;
	private static final int TOTAL_RECORDS = 1000;
	private static final int THREAD_POOL_SIZE = 10; // điều chỉnh phù hợp server

	private static Gson gson = new Gson();

	public static void main(String[] args) throws InterruptedException
	{
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
		Random random = new Random();

		List<OrderLog> batch = new ArrayList<>(BATCH_SIZE);
		for(int i = 1; i <= TOTAL_RECORDS; i++)
		{
			String id = UUID.randomUUID().toString();
			batch.add(new OrderLog(
					id,
					randomPhone(random),
					"user" + id + "@example.com",
					"ORD" + id,
					"Customer " + id,
					randomTimestamp(random)
			));

			if(i % BATCH_SIZE == 0)
			{
				List<OrderLog> toSend = new ArrayList<>(batch);
				batch.clear();
				executor.submit(() ->
				{
					try
					{
						sendBatch(toSend);
					}
					catch(Exception e)
					{
						e.printStackTrace();
						// Có thể retry nếu muốn
					}
				});
			}
		}

		// gửi batch cuối nếu còn
		if(!batch.isEmpty())
		{
			executor.submit(() ->
			{
				try
				{
					sendBatch(batch);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			});
		}

		executor.shutdown();
		executor.awaitTermination(1, TimeUnit.HOURS); // chờ hết tất cả thread hoàn thành
		System.out.println("Finished sending all records");
	}

	private static void sendBatch(List<OrderLog> logs) throws Exception
	{
		String jsonPayload = gson.toJson(logs);
		String res = getResponseBodyAPIWithPostMethod(URL, jsonPayload, 30000);
		// In ra hoặc log res nếu cần
//		 System.out.println("Response: " + res);
	}

	public static String getResponseBodyAPIWithPostMethod(String url, String input, int timeout) throws Exception
	{
		HttpClient client = new HttpClient();
		client.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
		client.getHttpConnectionManager().getParams().setSoTimeout(timeout);
		PostMethod method = new PostMethod(url);
		method.setRequestHeader("Content-Type", "application/json");
		method.setRequestEntity(new StringRequestEntity(input, "application/json", "UTF-8"));
		try
		{
			client.executeMethod(method);
			return method.getResponseBodyAsString();
		}
		finally
		{
			method.releaseConnection();
		}
	}

	// Hàm tạo random phone ví dụ
	private static String randomPhone(Random r)
	{
		return String.format("%010d", r.nextInt(1000000000));
	}

	private static String randomTimestamp(Random r)
	{
		long startMillis = 1609459200000L; // 2021-01-01
		long endMillis = 1684550400000L;   // 2023-05-20
		long randomMillis = startMillis + (long) (r.nextDouble() * (endMillis - startMillis));

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new java.util.Date(randomMillis));
	}

	// DTO OrderLog
	public static class OrderLog
	{
		private String id, phone, email, orderCode, customerName, createdTime;

		public OrderLog(String id, String phone, String email, String orderCode, String customerName, String createdTime)
		{
			this.id = id;
			this.phone = phone;
			this.email = email;
			this.orderCode = orderCode;
			this.customerName = customerName;
			this.createdTime = createdTime;
		}
	}
}
