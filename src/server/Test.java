package server;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Random;

import sun.net.www.http.KeepAliveCache;

public class Test {
	public static void main(String[] args) {
		String s = "ab";
		String[] arr = s.split(" ");
		System.out.println(s.length());
		for(String str : arr) {
			System.out.println(str);
		}
		System.out.println(arr.length);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		long start = System.currentTimeMillis();  
		System.out.println(sdf.format(start));
		System.out.println("hh".equals(null));
		
		String string = "aa";
		System.out.println(string+"\n" + "bb");
		
		final Gift gift = new Gift(121, 10, 5, false);
		for(int i = 0; i < 10; i++) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {	
						if(!gift.isEmpty()) {
							//double get = gift.grabGift();
							double get = 0.0;
							if(get > 0) {
								System.out.println(Thread.currentThread().getName() + " get gift : " + get);	
							} else {
								System.out.println(Thread.currentThread().getName() + " "+gift.getId()+"红包抢完啦！");
							}
							
						} else {
							System.out.println(Thread.currentThread().getName() + " "+gift.getId()+"红包抢完啦！");
						}
										
				}
			}).start();		
		}
		
		
		PriorityQueue<Double> gets = new PriorityQueue<Double>(new Comparator<Double>() {
			public int compare(Double o1, Double o2) {
				return o2.doubleValue() - o1.doubleValue() > 0 ? 1 : -1;
			};
		});
		gets.add(9.88);
		gets.add(0.33);
		gets.add(0.96);
		gets.add(7.23);
		gets.add(2.36);
		System.out.println("++++++++++++++++++++"+gets.peek());
	}
	
	
}
