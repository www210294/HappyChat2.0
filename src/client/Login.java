package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;


public class Login {
	Socket socket;												//和服务器端连接的套接字
	DataOutputStream dos; 										//和socket联系的输出流
	DataInputStream dis;										//和socket联系的输出流
	Scanner sc = new Scanner(System.in);						//键盘输入
	boolean checked = false;									//用户的登录状态，true为已通过后台验证
	List<String> rooms = new ArrayList<String>();				//本用户在此次连接中进入的聊天室计划
	String temp = null;
	
	public static void main(String[] args) {
		new Login().start();
	}
	
	public static String getTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		long start = System.currentTimeMillis();  
		return "	---" + sdf.format(start);
	}
	
	public void  check() throws IOException{				//验证登录或注册信息
		boolean success = false;
		while(!success) {
			String nameAndPwd = sc.nextLine();
			if(nameAndPwd.contains("$")) {
				System.out.println("用户名含有非法字符！");
				continue;
			}
			String[] arr = nameAndPwd.split(" ");
			if(arr.length != 2) {
				System.out.println("格式不正确，请重新输入：");
				continue;
			}
			send("$login " + nameAndPwd);
			String ans = dis.readUTF();
			if(ans.equals("DUPLICATED_NAME")) {
				System.out.println("注册用户名已存在，或者登录密码错误!");
			} else if(ans.equals("DUPLICATED_LOGIN")) {
				System.out.println("请不要重复登录！");
			} else {
				success = true;
				checked = true;
				System.out.println(arr[0] + ",您好！欢迎进入服务大厅" + getTime());
					if(ans.contains("$roomList")) {
						ans = ans.substring(9);
						int i = 1;
						String[] list = ans.split(" ");
						if(list.length > 1) {
							System.out.println("聊天室列表：");
							for(int j = 1; j < list.length; j++) {
								System.out.println(i+"." +" " +list[j]);
								i++;
							}
						} else {
							System.out.println("暂无聊天室，请新建。");
						}
					}
				
			}
		}
		
	}
	
	public void send(String str) throws IOException {
			dos.writeUTF(str);
	}
	
	
	public void handle(String string) throws IOException{  //处理传送到服务器数据
		if(!checked) {							//验证是否已经登录。
			System.out.println("请登录！");
			return;
		}
		if(!string.contains("$")) {
			System.out.println("非法输入");
			return;
		}
		String[] arr = string.split(" ");
		//System.out.println(string);
		for(String str : rooms) {                   // 群聊
			if(arr[0].substring(1).equals(str)) {
				if(arr.length > 1) {
					send(string);
				} else {
					System.out.println("消息不能为空！");
				}
				return;
			}
		}
		if(string.contains("$@")) {  //私聊
			send(string);
			return;
		}
		switch (arr[0]) {
		case "$create":					//创建聊天室
			if(arr.length == 3){
				send(string);
				//System.out.println("成功");
			} else {
				System.out.println("创建聊天室格式不正确！");
			}
			break;
		case "$enter":					//进入聊天室
			if(arr.length == 2){
				boolean duplicated = false;           //检验用户是否重复进入同一个聊天室
				for(String str : rooms) {
					if(str.equals(arr[1])) {
						duplicated = true;
						break;
					}
				}
				if(!duplicated) {	
					rooms.add(arr[1]);
					send(string);
				} else {
					System.out.println("请不要重复进入聊天室！");
				}
				//System.out.println("成功");
			} else {
				System.out.println("进入聊天室格式不正确！");
			}
			break;
		case "$exit":					//进入聊天室
			if(arr.length == 2){
				boolean exist = false;           //检验用户是否进入了聊天室
				for(String str : rooms) {
					if(str.equals(arr[1])) {
						rooms.remove(arr[1]);
						exist = true;
						break;
					}
				}
				if(!exist) {	
					System.out.println("无效的退出！");
				} else {
					temp = arr[1];
					send(string);
				}
				//System.out.println("成功");
			} else {
				System.out.println("无效输入！");
			}
			break;
		case "$hongbao":					//发红包： $hongbao 房间号  金额   个数        OR  $hongbao 房间号  金额   个数  拼手气
			boolean exist = false;           //检验用户是否重复进入同一个聊天室
			
			String rexMoney  = "^(0(?:[.](?:[1-9]\\d?|0[1-9]))|[1-9]\\d*(?:[.]\\d{1,2}|$))$";	//校验金额是否有效	
			Pattern p1 = Pattern.compile(rexMoney);
			Matcher matcher1= p1.matcher(arr[2]);
			if (!matcher1.matches()) {
				System.out.println("红包金额输入不正确");
				return;
			}
			
			String rexString  = "^[1-9]*[1-9][0-9]*$";						//校验红包个数是否有效
			Pattern p2 = Pattern.compile(rexString);
			Matcher matcher2 = p2.matcher(arr[3]);
			if (!matcher2.matches()) {
				System.out.println("红包个数输入不正确");
				return ;
			}
			
			double money = Double.parseDouble(arr[2]);						//检验金额是否太低，保证每个红包最低0.01元。
			int count = Integer.parseInt(arr[3]);
			if((int)(money*100) < count) {
				System.out.println("单个红包不可低于0.01元！");
				return;
			}
			
			for(String str : rooms) {								//检验房间号
				if(str.equals(arr[1])) {
					exist = true;
					break;
				}
			}
			if(!exist) {	
				System.out.println("您未进入该聊天室或该聊天室不存在！");
				return;
			} 
			
			if(arr.length == 4){
				send(string);
			} else if(arr.length == 5 && arr[4].equals("拼手气")) {
				send(string);
			} else {
				System.out.println("无效输入！");
			}
			break;
		case "$qiang":	
			if(arr.length == 3){
				boolean exst = false;           //检验用户是否进入了聊天室
				for(String str : rooms) {
					if(str.equals(arr[1])) {
						exst = true;
						break;
					}
				}
				String rex = "^[1-9]\\d*$";
				Pattern p = Pattern.compile(rex);
				Matcher matcher3 = p.matcher(arr[2]);
				if(!matcher3.matches()) {
					System.out.println("无效的抢红包！++");
					return;
				}
				if(!exst) {	
					System.out.println("无效的抢红包！");
					return;
				} else {
					temp = arr[1];
					send(string);
				}
				
				//System.out.println("成功");
			} else {
				System.out.println("无效输入！");
			}
			break;
			
		default:
			System.out.println("非法输入!");
			break;
		}
		
	}
	
	public void back(String string) throws IOException{  //处理服务器回传的数据
		String[] arr = string.split(" ");
		switch (arr[0]) {
		case "DUPLICATED_ROOMNAME":
			System.out.println("此房间已存在！");
			break;
		case "ROOM_NOT_EXIST":
			System.out.println("此房间不存在！");
			break;
		case "$roomList":
			break;
		case "DUPLICATED_NAME":
			break;
		case "DUPLICATED_LOGIN":
			break;
		case "DUPLICATED_ENTERED":
			System.out.println("请不要重复进入聊天室");
			break;
		case "OFF_LINE":
			System.out.println("用户不在线或不存在。");
			break;
		case "SUCCESS_EXIT":
			rooms.remove(temp);
			System.out.println("成功退出了聊天室 "+ temp);
			break;
		default:
			System.out.println(string);
			break;
		}
		
	}
	
	public void start() {
		try {
			socket = new Socket("127.0.0.1", 9999);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			System.out.println("成功连接服务器，请输入用户名和密码(用户名和密码不能有空格，两者用空格隔开)：");
			check();
			
			new Thread(new Runnable() {							//监听键盘输入
				public void run() {
					while(true) {
						String output = sc.nextLine();
						try {
							handle(output);
						} catch (IOException e) {
							close();
						}
					}
				}
			}).start();
			
			while(true) {										//监听后台服务器返回数据
				String string = dis.readUTF();
				back(string);
				//System.out.println(string);
			}
			
		} catch (IOException e) {
			System.out.println("连接服务器失败，请重试！");
			close();
		}
	}
	
	public void close() {
		try {
			if(dos != null) dos.close();
			if(dis != null) dis.close();
			if(socket != null) socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
