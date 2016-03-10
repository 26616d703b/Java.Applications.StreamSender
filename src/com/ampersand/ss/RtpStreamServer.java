package com.ampersand.ss;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;


public class RtpStreamServer extends Thread {

	/*
	 * Attributes
	 */
	
	private boolean m_running = false;
	
	private String m_media_path;
	private String m_target_address;
	private int m_target_port;
	private long m_start_time;
	private long m_stop_time;
	
	private String m_options;
	
	private MediaPlayer m_media_player;

	/*
	 * Methods
	 */
	
	// CONSTRUCTOR
	
	public RtpStreamServer(String media_path, String target_address, int target_port) {
		
		 // Initialise LibX pour réduire les opportunités de crash
		 LibXUtil.initialise();
		 
		 // Ajout du chemin de la librairie vlc, puis chargement de cette dernière
		 NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), nativeLibrarySearchPath());
		 Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
		 
		 m_media_path = media_path;
		 m_target_address = target_address;
		 m_target_port = target_port;
	     
	     m_media_player = new MediaPlayerFactory().newHeadlessMediaPlayer();
	     m_media_player.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
			 
			 @Override
			public void error(MediaPlayer mediaPlayer) {
				
				 //
			}
		 });
	}
	
	public RtpStreamServer(String media_path, String server_address, int server_port, long start_time, long stop_time) {
		
		 // Initialise LibX pour réduire les opportunités de crash
		 LibXUtil.initialise();
		 
		 // Ajout du chemin de la librairie vlc, puis chargement de cette dernière
		 NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), nativeLibrarySearchPath());
		 Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
		 
		 m_media_path = media_path;
		 m_target_address = server_address;
		 m_target_port = server_port;
		 m_start_time = start_time;
		 m_stop_time = stop_time;
	     
	     m_media_player = new MediaPlayerFactory().newHeadlessMediaPlayer();
	     m_media_player.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
			 
			 @Override
			public void error(MediaPlayer mediaPlayer) {
				
				 
			}
		 });
	}
	
	// RE-IMPLEMENTED METHODS
	
	@Override
	public void run() {
		
		if (!m_running) {
			
			if (m_start_time == 0 && m_stop_time == 0) {
				
				m_options = formatRtpStream(m_target_address, m_target_port);
			}
			else {
				
				m_options = formatRtpStream(m_target_address, 
						m_target_port, 
						m_start_time, 
						m_stop_time);
			}
		
			m_running = m_media_player.playMedia(m_media_path,
		            
					m_options,
		            ":no-sout-rtp-sap",
		            ":no-sout-standard-sap",
		            ":sout-all",
		            ":sout-keep"
					);
			
			// Pour empêcher la déconnexion
				
			try {
				
				join();
			} 
			catch (InterruptedException e) {
				
				Thread.currentThread().interrupt();
			}
		}
	}
	
	public void shutDown() {
		
		if (m_running) {
			
			m_media_player.stop();
			m_media_player.release();
			
			interrupt();
		}
		
		m_running = false;
	}
	
	// ACCESSORS and MUTATORS
	
	public boolean isRunning() {
		
		return m_running;
	}
	
	public String getAddress() {
		
		return m_target_address;
	}
	
	public String getMediaPath() {
		
		return m_media_path;
	}
	
	public void setMediaPath(String path) {
		
		m_media_path = path;
	}
	
	public void setAddress(String address) {
		
		m_target_address = address;
	}
	
	public int getPort() {
		
		return m_target_port;
	}
	
	public void setPort(int port) {
		
		m_target_port = port;
	}
	
	public long getStartTime() {
		
		return m_start_time;
	}
	
	public void setStartTime(long time) {
		
		m_start_time = time;
	}
	
	public long getStopTime() {
		
		return m_start_time;
	}
	
	public void setStopTime(long time) {
		
		m_start_time = time;
	}
	
	public MediaPlayer getMediaPlayer() {
		
		return m_media_player;
	}
	
	// IMPLEMENTED METHODS
	
	private static String formatRtpStream(String server_address, int server_port) {
        
		StringBuilder string_builder = new StringBuilder(60);
		/*string_builder.append(":sout=#transcode{vcodec=h264,vb=800,scale=0.25,fps=20,acodec=mp3,ab=128,channels=2,samplerate=44100}");
		string_builder.append(":rtp{dst=");*/
		string_builder.append(":sout=#rtp{dst=");
		string_builder.append(server_address);
		string_builder.append(",port=");
		string_builder.append(server_port);
		string_builder.append(",mux=ts}");
        
        return string_builder.toString();
    }
	
	private static String formatRtpStream(String server_address, int server_port, long start_time, long stop_time) {
        
		StringBuilder string_builder = new StringBuilder(60);
		string_builder.append(":sout=#rtp{dst=");
		string_builder.append(server_address);
		string_builder.append(",port=");
		string_builder.append(server_port);
		string_builder.append(",mux=ts}");
		string_builder.append(":start-time=");
		string_builder.append(start_time);
		string_builder.append(":stop-time=");
		string_builder.append(stop_time);
        
        return string_builder.toString();
    }
	
	private static String nativeLibrarySearchPath() {
		
		// Detecter l'OS de l'utilisateur pour améliorer la portabilité
		String native_library_search_path = null;
		String user_os = System.getProperty("os.name");
		String user_os_arch = System.getProperty("os.arch");
		 
		if (user_os.startsWith("Windows")) {
			 
			 if (user_os_arch.contains("32")) {
				 
				 native_library_search_path = "natives/x86/win";
			 }
			 else {
				 
				 native_library_search_path = "natives/x64/win";
			 }
		 }
		 else if (user_os.startsWith("Linux")) {
			 
			 if (user_os_arch.contains("32")) {
				 
				 native_library_search_path = "natives/x86/linux";
			 }
			 else {
				 
				 native_library_search_path = "natives/x64/linux";
			 }
		 }
		 else if (user_os.startsWith("Mac")) {
			 
			 if (user_os_arch.contains("32")) {
				 
				 native_library_search_path = "natives/x86/mac";
			 }
			 else {
				 
				 native_library_search_path = "natives/x64/mac";
			 }
		 }
		
		return native_library_search_path;
	}
}
