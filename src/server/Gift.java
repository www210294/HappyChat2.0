package server;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Gift {
	private int id;										//红包编号
	private double money;								//红包金额
	private int count;									//红包个数
	private  double remainMoney;						//红包剩余金额
	private int remainCount;							//红包剩余个数
	private boolean fair;								//是否需要拼手气
	private boolean empty = false;						//红包是否为空
	private HashMap<String, Double> gets;				//那些用户抢了这个红包以及分别抢了多少
	private ChatRoom cr;								//红包属于哪个聊天室
	
	public Gift(int id, double money, int count, boolean fair) {
		this.id = id;
		this.money = money;
		this.count = count;
		this.remainMoney = money;
		this.remainCount = count;
		this.fair = fair;
		this.gets = new HashMap<String, Double>();
	}
	
	public int remain() {
		return  count - gets.size();
	}
	
	public double keep(double val) {     // 保留两位小数
		return (int)(val*100)/100.0;
	}
	
	public synchronized double grabGift(String name) {
		if(gets.containsKey(name)) {					//该用户已经抢过了。
			return -1.0;
		}
		double get = 0.0;
		if(empty || remainCount < 1 || remainMoney <= 0.0) {
			return 0.0;
		}
		if(remainCount == 1) {
			empty = true;
			get = keep(remainMoney);
			remainCount = 0;
			remainMoney = 0;
			gets.put(name, get);
			return get;
		}
		if(fair) {							//公平抢红包
			get= keep(remainMoney/remainCount);
			remainCount--;
			remainMoney -= get;
		} else {							//不公平抢红包
			double MIN = 0.01;
			//double MAX = remainMoney - 0.01 * (remainCount - 1);
			double v1 = remainMoney - 0.01 * (remainCount - 1);
			double v2 = (remainMoney/remainCount)*2;
			double MAX = v1 < v2 ? v1 : v2;
			Random random = new Random();
			get = keep(MIN + 0.01 * random.nextInt((int)((MAX - MIN)/0.01) + 1));
			remainCount--;
			remainMoney -= get;
		}
		if(remainCount <= 0) {
			empty = true;
		}
		gets.put(name, get);
		return get;
	}
	
	public  Entry<String, Double> getMax() {     //计算最佳手气玩家
		Entry<String, Double> max = null;
		double temp = 0.0;
		if(count == gets.size() && !fair) {
			for(Entry<String, Double> entry : gets.entrySet()) {
				if(temp < entry.getValue()) {
					max = entry;
					temp = entry.getValue();
				}
			}
		}
		return max;
	}
	
	public double getRemainMoney() {
		return remainMoney;
	}

	public void setRemainMoney(double remainMoney) {
		this.remainMoney = remainMoney;
	}

	public int getRemainCount() {
		return remainCount;
	}

	public void setRemainCount(int remainCount) {
		this.remainCount = remainCount;
	}

	public boolean isFair() {
		return fair;
	}

	public void setFair(boolean fair) {
		this.fair = fair;
	}

	public boolean isEmpty() {
		return empty;
	}

	public void setEmpty(boolean empty) {
		this.empty = empty;
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	public double getMoney() {
		return money;
	}
	public void setMoney(double money) {
		this.money = money;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	
}