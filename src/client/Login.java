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
	Socket socket;												//�ͷ����������ӵ��׽���
	DataOutputStream dos; 										//��socket��ϵ�������
	DataInputStream dis;										//��socket��ϵ�������
	Scanner sc = new Scanner(System.in);						//��������
	boolean checked = false;									//�û��ĵ�¼״̬��trueΪ��ͨ����̨��֤
	List<String> rooms = new ArrayList<String>();				//���û��ڴ˴������н���������Ҽƻ�
	String temp = null;
	
	public static void main(String[] args) {
		new Login().start();
	}
	
	public static String getTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		long start = System.currentTimeMillis();  
		return "	---" + sdf.format(start);
	}
	
	public void  check() throws IOException{				//��֤��¼��ע����Ϣ
		boolean success = false;
		while(!success) {
			String nameAndPwd = sc.nextLine();
			if(nameAndPwd.contains("$")) {
				System.out.println("�û������зǷ��ַ���");
				continue;
			}
			String[] arr = nameAndPwd.split(" ");
			if(arr.length != 2) {
				System.out.println("��ʽ����ȷ�����������룺");
				continue;
			}
			send("$login " + nameAndPwd);
			String ans = dis.readUTF();
			if(ans.equals("DUPLICATED_NAME")) {
				System.out.println("ע���û����Ѵ��ڣ����ߵ�¼�������!");
			} else if(ans.equals("DUPLICATED_LOGIN")) {
				System.out.println("�벻Ҫ�ظ���¼��");
			} else {
				success = true;
				checked = true;
				System.out.println(arr[0] + ",���ã���ӭ����������" + getTime());
					if(ans.contains("$roomList")) {
						ans = ans.substring(9);
						int i = 1;
						String[] list = ans.split(" ");
						if(list.length > 1) {
							System.out.println("�������б�");
							for(int j = 1; j < list.length; j++) {
								System.out.println(i+"." +" " +list[j]);
								i++;
							}
						} else {
							System.out.println("���������ң����½���");
						}
					}
				
			}
		}
		
	}
	
	public void send(String str) throws IOException {
			dos.writeUTF(str);
	}
	
	
	public void handle(String string) throws IOException{  //�����͵�����������
		if(!checked) {							//��֤�Ƿ��Ѿ���¼��
			System.out.println("���¼��");
			return;
		}
		if(!string.contains("$")) {
			System.out.println("�Ƿ�����");
			return;
		}
		String[] arr = string.split(" ");
		//System.out.println(string);
		for(String str : rooms) {                   // Ⱥ��
			if(arr[0].substring(1).equals(str)) {
				if(arr.length > 1) {
					send(string);
				} else {
					System.out.println("��Ϣ����Ϊ�գ�");
				}
				return;
			}
		}
		if(string.contains("$@")) {  //˽��
			send(string);
			return;
		}
		switch (arr[0]) {
		case "$create":					//����������
			if(arr.length == 3){
				send(string);
				//System.out.println("�ɹ�");
			} else {
				System.out.println("���������Ҹ�ʽ����ȷ��");
			}
			break;
		case "$enter":					//����������
			if(arr.length == 2){
				boolean duplicated = false;           //�����û��Ƿ��ظ�����ͬһ��������
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
					System.out.println("�벻Ҫ�ظ����������ң�");
				}
				//System.out.println("�ɹ�");
			} else {
				System.out.println("���������Ҹ�ʽ����ȷ��");
			}
			break;
		case "$exit":					//����������
			if(arr.length == 2){
				boolean exist = false;           //�����û��Ƿ������������
				for(String str : rooms) {
					if(str.equals(arr[1])) {
						rooms.remove(arr[1]);
						exist = true;
						break;
					}
				}
				if(!exist) {	
					System.out.println("��Ч���˳���");
				} else {
					temp = arr[1];
					send(string);
				}
				//System.out.println("�ɹ�");
			} else {
				System.out.println("��Ч���룡");
			}
			break;
		case "$hongbao":					//������� $hongbao �����  ���   ����        OR  $hongbao �����  ���   ����  ƴ����
			boolean exist = false;           //�����û��Ƿ��ظ�����ͬһ��������
			
			String rexMoney  = "^(0(?:[.](?:[1-9]\\d?|0[1-9]))|[1-9]\\d*(?:[.]\\d{1,2}|$))$";	//У�����Ƿ���Ч	
			Pattern p1 = Pattern.compile(rexMoney);
			Matcher matcher1= p1.matcher(arr[2]);
			if (!matcher1.matches()) {
				System.out.println("���������벻��ȷ");
				return;
			}
			
			String rexString  = "^[1-9]*[1-9][0-9]*$";						//У���������Ƿ���Ч
			Pattern p2 = Pattern.compile(rexString);
			Matcher matcher2 = p2.matcher(arr[3]);
			if (!matcher2.matches()) {
				System.out.println("����������벻��ȷ");
				return ;
			}
			
			double money = Double.parseDouble(arr[2]);						//�������Ƿ�̫�ͣ���֤ÿ��������0.01Ԫ��
			int count = Integer.parseInt(arr[3]);
			if((int)(money*100) < count) {
				System.out.println("����������ɵ���0.01Ԫ��");
				return;
			}
			
			for(String str : rooms) {								//���鷿���
				if(str.equals(arr[1])) {
					exist = true;
					break;
				}
			}
			if(!exist) {	
				System.out.println("��δ����������һ�������Ҳ����ڣ�");
				return;
			} 
			
			if(arr.length == 4){
				send(string);
			} else if(arr.length == 5 && arr[4].equals("ƴ����")) {
				send(string);
			} else {
				System.out.println("��Ч���룡");
			}
			break;
		case "$qiang":	
			if(arr.length == 3){
				boolean exst = false;           //�����û��Ƿ������������
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
					System.out.println("��Ч���������++");
					return;
				}
				if(!exst) {	
					System.out.println("��Ч���������");
					return;
				} else {
					temp = arr[1];
					send(string);
				}
				
				//System.out.println("�ɹ�");
			} else {
				System.out.println("��Ч���룡");
			}
			break;
			
		default:
			System.out.println("�Ƿ�����!");
			break;
		}
		
	}
	
	public void back(String string) throws IOException{  //����������ش�������
		String[] arr = string.split(" ");
		switch (arr[0]) {
		case "DUPLICATED_ROOMNAME":
			System.out.println("�˷����Ѵ��ڣ�");
			break;
		case "ROOM_NOT_EXIST":
			System.out.println("�˷��䲻���ڣ�");
			break;
		case "$roomList":
			break;
		case "DUPLICATED_NAME":
			break;
		case "DUPLICATED_LOGIN":
			break;
		case "DUPLICATED_ENTERED":
			System.out.println("�벻Ҫ�ظ�����������");
			break;
		case "OFF_LINE":
			System.out.println("�û������߻򲻴��ڡ�");
			break;
		case "SUCCESS_EXIT":
			rooms.remove(temp);
			System.out.println("�ɹ��˳��������� "+ temp);
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
			System.out.println("�ɹ����ӷ��������������û���������(�û��������벻���пո������ÿո����)��");
			check();
			
			new Thread(new Runnable() {							//������������
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
			
			while(true) {										//������̨��������������
				String string = dis.readUTF();
				back(string);
				//System.out.println(string);
			}
			
		} catch (IOException e) {
			System.out.println("���ӷ�����ʧ�ܣ������ԣ�");
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
