package com.ampersand.ss;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.ampersand.lcu.gui.component.list.FilesList;

public class MetaStreamServer extends Thread {

	/*
	 * Attributes
	 */
	
	private boolean m_running = false;
	private int m_backlog;
	private int m_port;
	private ServerSocket m_svc_socket;
	
	private FilesList m_media_list;

	/*
	 * Methods
	 */
	
	// CONSTRUCTOR
	
	public MetaStreamServer(int port, int backlog) {
		
		m_backlog = backlog;
		m_port = port;
	}
	
	// ACCESSORS and MUTATORS
	
	public synchronized boolean isRunning() {
		
		return m_running;
	}
	
	public void setMediaList(FilesList media_list) {
		
		m_media_list = media_list;
	}
	
	// RE-IMPLEMENTED METHODS
	
	@Override
	public void run() {
		
		if (!m_running) {
			
			try {
				
				m_svc_socket = new ServerSocket(m_port, m_backlog);
				System.out.println("Serveur connecté!");
				
				m_running = true;
				
				while (m_running) {
					
					System.out.println("Serveur en attente de connexion...");
					Socket clnt_socket = m_svc_socket.accept();
					System.out.println("Client connecté!");
					
					new Service(clnt_socket).start();
				} 
			} 
			catch (IOException e) {
				
				shutDown();
			}
		}
	}
	
	// IMPLEMENTED METHODS
	
	public synchronized void shutDown() {
		
		if (m_running) {
			
			try {
				
				m_svc_socket.close();
				
				System.out.println("Arrêt du serveur...");
			} 
			catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
		interrupt();
		
		m_running = false;
	}
	
	// IMPLEMENTED CLASSES
	
	class Service extends Thread {
		
		/*
		 * Methods
		 */
		
		public Service(Socket client_socket) {
			
			m_clnt_socket = client_socket;
		}
		
		@Override
		public void run() {
			
			try {
				
				DataInputStream data_in_stream = new DataInputStream(m_clnt_socket.getInputStream());
				
				System.out.println("Le serveur attend la requête...");
				
				int request_id = data_in_stream.readInt();
				
				System.out.println("Requête reçue!");
				
				switch (request_id) {
				
				case ClientRequest.MEDIA_LIST:
					
					ObjectOutputStream obj_out_stream = new ObjectOutputStream(m_clnt_socket.getOutputStream());

					try {
						
						obj_out_stream.writeObject(m_media_list);
						obj_out_stream.flush();	
						
						System.out.println("Objet envoyé!");
					} 
					catch (IOException e) {
						
						e.printStackTrace();
					}
					
					break;
				}
			} 
			catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
		/*
		 * Attributes
		 */
	
		private Socket m_clnt_socket;
	}
	
	// IMPLEMENTED CLASSES
	
	public abstract class ClientRequest {
			
		public static final int MEDIA_LIST = 0;
	}
	}
