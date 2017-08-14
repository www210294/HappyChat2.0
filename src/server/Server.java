package server;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.org.apache.bcel.internal.generic.NEW;

import sun.security.krb5.internal.crypto.crc32;



public class Server implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ServerSocket ss;											//服务器套接字
	private List<Client> clients = new ArrayList<Client>();				//与服务器保持连接的
	private HashMap<String, String> users = null;						//保存用户信息，包括用户名和密码
	private Socket s;													//当前或即将连到服务器的客户套接字
	private List<ChatRoom> rooms = new ArrayList<ChatRoom>();			//用户已经建立的聊天室
	

	public static void main(String[] args) {
		new Server().start();
	}

	private void init() {												//反序列化读取历史用户和聊天室记录
		File file = new File("./store/users.obj");
		File file1 = new File("./store/rooms.obj");
		if (file.exists()) {
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(new FileInputStream(file));
				users = (HashMap)ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (ois != null)
						ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			
			users = new HashMap<String, String>();
		}
		
		if(!users.containsKey("系统消息")) {                   //防止有人注册这个名字冒充系统乱发消息。
			users.put("系统消息", "sadasd2121312asd12eqwasda");
		}
		
		if (file1.exists()) {
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(new FileInputStream(file1));
				rooms = (ArrayList) ois.readObject();
				for(ChatRoom cr : rooms) {        //由于ChatRoom 的persons属性是 transient,反序列后persons为null,需要单独初始化
					cr.persons = new CopyOnWriteArrayList<Client>();
					cr.gifts = new HashMap<Integer, Gift>();
//					cr.gets = new PriorityQueue<Double>(new Comparator<Double>() {
//						public int compare(Double o1, Double o2) {
//							return o2.doubleValue() - o1.doubleValue() > 0 ? 1 : -1;
//						};
//					});
					
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (ois != null)
						ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			rooms = new ArrayList<ChatRoom>();
		}
	}

	private void store() {									//序列化用户和聊天室记录进行存盘
		File file = new File("./store/users.obj");
		File file1 = new File("./store/rooms.obj");
		if(!file.exists()) {	
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("./store/users.obj"));
			out.writeObject(users); 
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(!file1.exists()) {	
			try {
				file1.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("./store/rooms.obj"));
			out.writeObject(rooms); 
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void start() {												//服务器启动函数
		init();															//反序列化读取历史信息记录
		try {
			ss = new ServerSocket(9999);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (true) {
			try {
				s = ss.accept();
				Client client = new Client(s);
				//clients.add(client);
				new Thread(client).start();								//为每一个客户连接新建线程处理业务
			} catch (IOException e) {
				try {
					if (s != null) {
						s.close();
					}
					if (ss != null) {
						ss.close();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} finally {
				store();												//对服务器的数据进行序列化存盘
			}
		}
	}
	
	public String getTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		long start = System.currentTimeMillis();  
		return "	---" + sdf.format(start);
	}

	class Client implements Runnable {
		private Socket s;												//服务器和客户端的套接字
		private DataInputStream dis;									//套接字的输入流
		private DataOutputStream dos;									//套接字的输出流
		private String name;											//客户名字
		private String pwd;												//客户密码
		private List<ChatRoom> room = new ArrayList<ChatRoom>();		//该客户目前加入的聊天室

		public Client(Socket s) {
			this.s = s;
		}
		
		public String getName() {
			return name;
		}
		
		public void send(String str) {
			try {
				dos.writeUTF(str);
			} catch (IOException e) {
				close();
			}
		}

		public void close() {
			System.out.println(name + " disconnected");
			try {
				if (dos != null)
					dos.close();
				if (dis != null)
					dis.close();
				if (s != null)
					s.close();
				clients.remove(this);
				for(ChatRoom cr : room) {
					cr.removeClient(this);
					String msg = this.name +"退出聊天室  ";
					cr.sendMsg("系统消息", msg);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}finally {
				store();
			}
		}

		public void handle(String string) {  								// 处理客户端来信
			String[] arr = string.split(" ");
			for(ChatRoom cr : room) {										//群聊
				if(arr[0].equals("$" + cr.getName())) {
					cr.sendMsg(this, string.substring(arr[0].length()));;
					return;
				}
			}
			
			if(string.contains("$@")) {										//私聊
				String fname = arr[0].substring(2);
				String msg = string.substring(arr[0].length());
				boolean find = false;
				for(Client c : clients) {
					if(c.name.equals(fname)) {
						find = true;
						msg = "[私信]"+ this.name +" to "+c.name+": " + msg + getTime();
						c.send(msg);
						this.send(msg);
					}
				}
				if(!find) {
					send("OFF_LINE");
				}
				return;
			}
			
			switch (arr[0]) {
			case "$login":													// 处理用户登录信息
				name = arr[1];
				pwd = arr[2];
				boolean success = false;
				if (users.containsKey(name)) {
					if (!pwd.equals(users.get(name))) {
						send("DUPLICATED_NAME");
						name = null;
						pwd = null;
					} else {												//验证是否重复登录
						boolean logined = false;
						for(Client client : clients) {
							if(client.name.equals(name) && this != client) {
								send("DUPLICATED_LOGIN");
								logined = true;
								return;
							}
						}
						System.out.println(name + " login !");
						success = true;
						//send("PASSED");
					}
				} else {
					//send("REGISTERED");
					success = true;
					users.put(name, pwd);
					System.out.println(name + " login !");
				}
				
				if(success) {                     							//登录成功，向客户端发送聊天室列表。
					clients.add(this);
					String msg = "$roomList";
					for(ChatRoom chatRoom : rooms) {
						msg = msg + " " + chatRoom.getName();
					}
					msg.trim();
					//System.out.println(msg);
					send(msg);
					//System.out.println(msg);
				}
				break;
				
			case "$create":													//创建聊天室
				boolean established = true;
				if(getRoom(arr[1]) != null) {
					established = false;
					send("DUPLICATED_ROOMNAME");
				}
				if(established) {                                  			// 通知所有在线用户来新聊天室聊天
					rooms.add(new ChatRoom(arr[1], arr[2]));
					for(Client client :clients) {
						client.send("[大厅消息]:有新聊天室"+arr[1] + " 建立,快来聊聊吧" + getTime());
					}
						
				}
				break;
				
			case "$enter":													//进入聊天室
				boolean entered = false;
				ChatRoom troom = getRoom(arr[1]);
				if(troom != null) {
					entered = true;
				}
				if(entered) {                                  				// 通知所有在线用户来新聊天室聊天
					if(this.room.contains(troom)) {
						send("DUPLICATED_ENTERED");
					} else {
						troom.addClient(this);
						room.add(troom);
						if(troom.persons.size() > 0) {
							String msg = "["+troom.getName()+"]系统消息:  有新聊友 "+this.name + " 加入群聊,快来和TA打个招呼吧            ";
							troom.stroe(msg);
							for(Client client : troom.persons) {
								if(client == this) {
									client.send(troom.getHistory());
									client.send("您已经加入 "+troom.getName()+" 聊天室，以上是历史记录。快和大家打个招呼！" + getTime());
								} else {
									client.send(msg);
								}
							}
						}
					}
						
				} else {
					send("此聊天室尚不存在！");
				}
				break;
				
			case "$exit":													//退出聊天室
				boolean out = false;
				ChatRoom target = getRoom(arr[1]);
				if(target != null) {
					out = true;
					send("SUCCESS_EXIT");
				}
				if(out) {                                  					// 通知所有聊天室用户有人退出
					target.removeClient(this);
					room.remove(target);
					target.sendMsg("系统消息", this.name+ " 已经退出了聊天室");
				}
				break;
			case "$hongbao":												//处理红包业务
				ChatRoom cRoom = getRoom(arr[1]);
				Random random = new Random();
				int id = random.nextInt(65536);
				synchronized (cRoom.gifts) {
					while(cRoom.gifts.keySet().contains(id)) {
						id = new Random().nextInt(65536);
					}
					
				}
				Gift gift = null;
				if(arr.length == 4) {										//公平红包
					gift = new Gift(id, Double.parseDouble(arr[2]), Integer.parseInt(arr[3]), true);		
				} else {													//拼手气红包
					gift = new Gift(id, Double.parseDouble(arr[2]), Integer.parseInt(arr[3]), false);
				}
				cRoom.gifts.put(id, gift);
				String msg = "["+cRoom.getName()+"]系统消息: 各位亲，大佬" +name + "给大家发了个" +arr[2]+ "元红包，编号为  "+id+" 速速来抢！" ;
				cRoom.stroe(msg);
				for(Client client : cRoom.persons) {
					client.send(msg);
				}
				break;
			case "$qiang":													//处理抢红包业务
				ChatRoom chatRoom = getRoom(arr[1]);
				if(chatRoom == null) {
					return;
				}
				Gift gf = null;
				Integer gid = Integer.parseInt(arr[2]);
				if(chatRoom.gifts.containsKey(gid)) {
					gf = chatRoom.gifts.get(gid);
				} else {
					return;
				}
				if(gf.isEmpty()) {
					send("["+arr[1]+"]系统消息: "+gid + " 号红包已经被抢光啦！");
					return;
				}
				double money = gf.grabGift(name);
				if(money < 0) {
					send("您已经抢过"+gid+" 号红包了。");
					return;
				}
				send("["+arr[1]+"]系统消息: 恭喜你抢到"+ money +"元！");
				Entry<String, Double> max = gf.getMax();
				if(max != null) {
					String str = gid + " 号红包已经被抢光啦！恭喜 "+max.getKey()+" 手气最佳抢到 "+max.getValue()+ "元";
					chatRoom.sendMsg("系统消息", str);
					return;
				}
				if(!(gf.remain() > 0)) {
					chatRoom.sendMsg("系统消息", ""+ gid +"号红包已经被抢光");
					return;
				}
				break;	
			default:
				break;
			}
			
		}
		
		private ChatRoom getRoom(String name) {
			for(ChatRoom cr : rooms) {
				if(cr.getName().equals(name)) {
					return cr;
				}
			}
			return null;
		}
		
		public void run() {
			try {
				dis = new DataInputStream(s.getInputStream());
				dos = new DataOutputStream(s.getOutputStream());
				while (true) {
					String string = dis.readUTF();
					handle(string);											//处理用户端的消息
					System.out.println(string);
				}

			} catch (IOException e) {

			} finally {
				close();
			}
		}
	}
	

}
