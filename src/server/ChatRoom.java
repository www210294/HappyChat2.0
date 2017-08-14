package server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;



import client.Login;
import server.Server.Client;

class ChatRoom implements Serializable{
 	private static final long serialVersionUID = 1L;
	private String name;															//聊天室名字
	private String introduction;													//聊天室说明
	private String history = "";													//聊天室历史记录
	public transient Map<Integer,Gift> gifts = new HashMap<Integer, Gift>();		//本聊天室在服务器启动期间内产生的红包
	transient CopyOnWriteArrayList<Client> persons = new CopyOnWriteArrayList<Client>(); //本聊天室在服务器启动期间内在线的用户
	
	
	ChatRoom(String name, String introduction) {
		this.name = name;
		this.introduction = introduction;
	}
	public void addClient(Client c) {
		persons.add(c);
	}
	public void removeClient(Client c) {
		persons.remove(c);
	}
	
	
	
	public void sendMsg(Client c, String str) {										//单个用户向本聊天室群发消息
		str = "[" + name + "]" + c.getName() + ":" + str + "        " + Login.getTime();
		history += (str +"\n");
		for(Client client : persons) {
			client.send(str);
		}
	}
	public void sendMsg(String s, String str) {										//系统向聊天室发送的消息
		str = "[" + name + "]" + s + ":   " + str + "        " + Login.getTime();
		history += (str +"\n");
		for(Client client : persons) {
			client.send(str);
		}
	}
	
	public void stroe(String msg) {													//将相关信息追加到聊天室的历史记录中
		msg = msg  + "        " + Login.getTime() + "\n"; 
		history += msg;
	}
	
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getIntroduction() {
		return introduction;
	}
	
	public void setIntroduction(String introduction) {
		this.introduction = introduction;
	}
	
	public String getHistory() {
		return history;
	}
	
	public void setHistory(String history) {
		this.history = history;
	}
	
		
}