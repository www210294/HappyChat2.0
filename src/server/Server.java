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
	private ServerSocket ss;
	private List<Client> clients = new ArrayList<Client>();
	private HashMap<String, String> users = null;
	private Socket s;
	private List<ChatRoom> rooms = new ArrayList<ChatRoom>();
	

	public static void main(String[] args) {
		new Server().start();
	}

	private void init() {
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
		
		if(!users.containsKey("ϵͳ��Ϣ")) {                   //��ֹ����ע���������ð��ϵͳ�ҷ���Ϣ��
			users.put("ϵͳ��Ϣ", "sadasd2121312asd12eqwasda");
		}
		
		if (file1.exists()) {
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(new FileInputStream(file1));
				rooms = (ArrayList) ois.readObject();
				for(ChatRoom cr : rooms) {        //����ChatRoom ��persons������ transient,�����к�personsΪnull,��Ҫ������ʼ��
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

	private void end() {
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

	private void start() {
		init();
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
				new Thread(client).start();
				end();
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
				end();
			}

		}
	}
	
	public String getTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		long start = System.currentTimeMillis();  
		return "	---" + sdf.format(start);
	}

	class Client implements Runnable {
		private Socket s;
		private DataInputStream dis;
		private DataOutputStream dos;
		private String name;
		private String pwd;
		private List<ChatRoom> room = new ArrayList<ChatRoom>();

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
					String msg = this.name +"�˳�������  ";
					cr.sendMsg("ϵͳ��Ϣ", msg);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}finally {
				end();
			}
		}

		public void handle(String string) {  // ����ͻ�������
			String[] arr = string.split(" ");
			for(ChatRoom cr : room) {		//Ⱥ��
				if(arr[0].equals("$" + cr.getName())) {
					cr.sendMsg(this, string.substring(arr[0].length()));;
					return;
				}
			}
			
			if(string.contains("$@")) {		//˽��
				String fname = arr[0].substring(2);
				String msg = string.substring(arr[0].length());
				boolean find = false;
				for(Client c : clients) {
					if(c.name.equals(fname)) {
						find = true;
						msg = "[˽��]"+ this.name +" to "+c.name+": " + msg + getTime();
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
			case "$login":				// �����û���¼��Ϣ
				name = arr[1];
				pwd = arr[2];
				boolean success = false;
				if (users.containsKey(name)) {
					if (!pwd.equals(users.get(name))) {
						send("DUPLICATED_NAME");
						name = null;
						pwd = null;
					} else {							//��֤�Ƿ��ظ���¼
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
				
				if(success) {                     //��¼�ɹ�����ͻ��˷����������б�
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
				
			case "$create":					//����������
				boolean established = true;
				if(getRoom(arr[1]) != null) {
					established = false;
					send("DUPLICATED_ROOMNAME");
				}
				if(established) {                                  // ֪ͨ���������û���������������
					rooms.add(new ChatRoom(arr[1], arr[2]));
					for(Client client :clients) {
						client.send("[������Ϣ]:����������"+arr[1] + " ����,�������İ�" + getTime());
					}
						
				}
				break;
				
			case "$enter":					//����������
				boolean entered = false;
				ChatRoom troom = getRoom(arr[1]);
				if(troom != null) {
					entered = true;
				}
				if(entered) {                                  // ֪ͨ���������û���������������
					if(this.room.contains(troom)) {
						send("DUPLICATED_ENTERED");
					} else {
						troom.addClient(this);
						room.add(troom);
						if(troom.persons.size() > 0) {
							String msg = "["+troom.getName()+"]ϵͳ��Ϣ:  �������� "+this.name + " ����Ⱥ��,������TA����к���            ";
							troom.stroe(msg);
							for(Client client : troom.persons) {
								if(client == this) {
									client.send(troom.getHistory());
									client.send("���Ѿ����� "+troom.getName()+" �����ң���������ʷ��¼����ʹ�Ҵ���к���" + getTime());
								} else {
									client.send(msg);
								}
							}
						}
					}
						
				} else {
					send("���������в����ڣ�");
				}
				break;
				
			case "$exit":					//�˳�������
				boolean out = false;
				ChatRoom target = getRoom(arr[1]);
				if(target != null) {
					out = true;
					send("SUCCESS_EXIT");
				}
				if(out) {                                  // ֪ͨ�����������û������˳�
					target.removeClient(this);
					room.remove(target);
					target.sendMsg("ϵͳ��Ϣ", this.name+ " �Ѿ��˳���������");
				}
				break;
			case "$hongbao":									//������ҵ��
				ChatRoom cRoom = getRoom(arr[1]);
				Random random = new Random();
				int id = random.nextInt(65536);
				synchronized (cRoom.gifts) {
					while(cRoom.gifts.keySet().contains(id)) {
						id = new Random().nextInt(65536);
					}
					
				}
				Gift gift = null;
				if(arr.length == 4) {		//��ƽ���
					gift = new Gift(id, Double.parseDouble(arr[2]), Integer.parseInt(arr[3]), true);		
				} else {					//ƴ�������
					gift = new Gift(id, Double.parseDouble(arr[2]), Integer.parseInt(arr[3]), false);
				}
				cRoom.gifts.put(id, gift);
				String msg = "["+cRoom.getName()+"]ϵͳ��Ϣ: ��λ�ף�����" +name + "����ҷ��˸�" +arr[2]+ "Ԫ��������Ϊ  "+id+" ����������" ;
				cRoom.stroe(msg);
				for(Client client : cRoom.persons) {
					client.send(msg);
				}
				break;
			case "$qiang":
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
					send("["+arr[1]+"]ϵͳ��Ϣ: "+gid + " �ź���Ѿ�����������");
					return;
				}
				double money = gf.grabGift(name);
				if(money < 0) {
					send("���Ѿ�����"+gid+" �ź���ˡ�");
					return;
				}
				send("��ϲ������"+ money +"Ԫ��");
				Entry<String, Double> max = gf.getMax();
				if(max != null) {
					String str = gid + " �ź���Ѿ�������������ϲ "+max.getKey()+" ����������� "+max.getValue()+ "Ԫ";
					chatRoom.sendMsg("ϵͳ��Ϣ", str);
					return;
				}
				if(!(gf.remain() > 0)) {
					chatRoom.sendMsg("ϵͳ��Ϣ", ""+ gid +"�ź���Ѿ�������");
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
					handle(string);
					System.out.println(string);
				}

			} catch (IOException e) {

			} finally {
				close();
			}
		}
	}
	

}
