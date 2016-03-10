package com.ampersand.ss;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.ampersand.lcu.gui.GUIFactory;
import com.ampersand.lcu.gui.color.ColorPalette;
import com.ampersand.lcu.gui.component.button.HighlightButton;
import com.ampersand.lcu.gui.component.field.TextValidationField;
import com.ampersand.lcu.gui.component.list.FilesList;
import com.ampersand.lcu.validator.Validator;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.manager.MediaManager;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;

public class VODStreamSender extends JFrame {

	/*
	 * Attributes:
	 */
	private static final long serialVersionUID = -1194086608658190616L;
	
	public static final String VOD_MUX = "mp2t";
	
	//
	
	private VODStreamSender m_instance = this;
	
	private SystrayListener m_systray_listener;
	private FileListener m_file_listener;
	private SettingsListener m_settings_listener;
	private MediaListListener m_media_list_listener;
	private HelpListener m_help_listener;
	
	private Vector<File> m_media_list_files;
	
	private String m_address = "127.0.0.1";
	private int m_port = 5555;
    private MediaManager m_media_manager;
	private MetaStreamServer m_meta_server;

	// GUI
	
	private SystemTray m_tray;
	private MenuItem m_show_item;
	private MenuItem m_exit_item;
	private TrayIcon m_tray_icon;
	
	private JMenuBar m_menu_bar;

	private JMenu m_file_menu;
	private JMenuItem m_open;
	private JMenuItem m_exit;
	
	private JMenu m_settings_menu;
	private JMenuItem m_network;
	
	private JMenu m_playlist_menu;
	private JMenuItem m_show_playlist_playlist;

	private JMenu m_help_menu;
	private JMenuItem m_about;
	
	private HighlightButton m_connect_button;
	
	/*
	 * Methods:
	 */

	// CONSTRUCTOR

	public VODStreamSender() {
		
		// Initialise LibX pour réduire les opportunités de crash
		LibXUtil.initialise();
				 
		// Ajout du chemin de la librairie vlc, puis chargement de cette dernière
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), nativeLibrarySearchPath());
		Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);

		try {
			
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (ClassNotFoundException e) {
			
			e.printStackTrace();
		} 
		catch (InstantiationException e) {
			
			e.printStackTrace();
		} 
		catch (IllegalAccessException e) {
		
			e.printStackTrace();
		} 
		catch (UnsupportedLookAndFeelException e) {
			
			e.printStackTrace();
		}
		
		m_media_list_files = new Vector<File>();
		
		m_media_manager = new MediaPlayerFactory("--rtsp-host=" + m_address, "--rtsp-port=" + m_port).newMediaManager();
		m_meta_server = new MetaStreamServer(4444, 100);
		
		// Window properties
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent e) {
				
				m_media_manager.release();
			}
			
			@Override
			public void windowIconified(WindowEvent e) {

				if (SystemTray.isSupported()) {
					
					m_tray_icon.displayMessage("En cours...", "Le serveur est toujours en exécution!", MessageType.INFO);
					
					setVisible(false);
				}
			}
		});
		
		setSize(450, 200);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setIconImage(new ImageIcon(getClass().getResource("res/icons/application.png")).getImage());
		setTitle("StreamSender - VOD");
		setLocationRelativeTo(null);

		initialize();
	}

	// INITIALIZATIONS:

	public void initialize() {

		if (SystemTray.isSupported()) {
		
			initTray();
		}
		
		initMenu();
		
		m_connect_button = new HighlightButton(new ImageIcon(getClass().getResource("res/icons/menu/online-64.png")), 
				ColorPalette.WHITE,
				ColorPalette.LIGHT_GRAY);
		
		m_connect_button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent event) {
				
				if (m_connect_button.getToolTipText().equals("Connecter")) {
					
					if (m_media_list_files.isEmpty()) {
						
						JOptionPane.showMessageDialog(m_instance, 
								"Vous devez d'abord sélectionner des fichiers. \nPour cela, rendez-vous dans Fichier->Ouvrir...", 
								"Liste de lécture vide", 
								JOptionPane.WARNING_MESSAGE,
								new ImageIcon(getClass().getResource("res/icons/menu/caution.png")));
						
						return;
					}
					
					new Thread() {
						
						public void run() {
							
							for (File file : m_media_list_files) {
								
								m_media_manager.play(file.getName().substring(0, file.getName().lastIndexOf(".")));
							}
							
							try {
								
								join();
							} 
							catch (InterruptedException e) {
								
								Thread.currentThread().interrupt();
							}
						};
						
					}.start();
					
					m_meta_server.start();
					
					// GUI
					
					if (SystemTray.isSupported()) {
					
						m_tray_icon.displayMessage("Statut", "Le serveur est prêt!", TrayIcon.MessageType.INFO);
					}
					
					m_open.setEnabled(false);
					
					m_connect_button.setToolTipText("Déconnecter");
					m_connect_button.setIcon(new ImageIcon(getClass().getResource("res/icons/menu/shutdown-64.png")));
					
					m_network.setEnabled(false);
				}
				else {
					
					m_media_manager.release();
					
					System.exit(0);
				}
			}
		});
		
		m_connect_button.setToolTipText("Connecter");
		
		add(m_connect_button);
	}
	
	public void initTray() {
		
		m_systray_listener = new SystrayListener();
		
		m_show_item = new MenuItem("Afficher");
		m_show_item.addActionListener(m_systray_listener);
		
		m_exit_item = new MenuItem("Quitter");
		m_exit_item.addActionListener(m_systray_listener);
		
		PopupMenu popup = new PopupMenu();
		popup.add(m_show_item);
		popup.addSeparator();
		popup.add(m_exit_item);
		
		m_tray_icon = new TrayIcon(new ImageIcon(getClass().getResource("res/icons/application.png")).getImage());
		m_tray_icon.addActionListener(m_systray_listener);
		m_tray_icon.setImageAutoSize(true);
		m_tray_icon.setPopupMenu(popup);
		
		m_tray = SystemTray.getSystemTray();
		 
		try {
			
			m_tray.add(m_tray_icon);
		} 
		catch (AWTException e) {
			
			e.printStackTrace();
		}
	}

	public void initMenu() {

		// FILE
		
		m_file_listener = new FileListener();

		m_open = new JMenuItem("Ouvrir", new ImageIcon(getClass().getResource("res/icons/menu/folder.png")));
		m_open.addActionListener(m_file_listener);
		m_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		m_open.setMnemonic('o');

		m_exit = new JMenuItem("Quitter", new ImageIcon(getClass().getResource("res/icons/menu/shutdown.png")));
		m_exit.addActionListener(m_file_listener);
		m_exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
		m_exit.setMnemonic('q');

		m_file_menu = new JMenu("Fichier");
		m_file_menu.add(m_open);
		m_file_menu.addSeparator();
		m_file_menu.add(m_exit);
		m_file_menu.setMnemonic('f');
		
		// SETTINGS
		
		m_settings_listener = new SettingsListener();
		
		m_network = new JMenuItem("Réseau", new ImageIcon(getClass().getResource("res/icons/menu/network.png")));
		m_network.addActionListener(m_settings_listener);
		m_network.setMnemonic('r');
		
		m_settings_menu = new JMenu("Paramètres");
		m_settings_menu.add(m_network);
		m_settings_menu.setMnemonic('p');
		
		// PLAYLIST
		
		m_media_list_listener = new MediaListListener();
		
		m_show_playlist_playlist = new JMenuItem("Afficher", new ImageIcon(getClass().getResource("res/icons/menu/list.png")));
		m_show_playlist_playlist.addActionListener(m_media_list_listener);
		m_show_playlist_playlist.setMnemonic('a');
		
		m_playlist_menu = new JMenu("Liste de lecture");
		m_playlist_menu.add(m_show_playlist_playlist);
		m_playlist_menu.setMnemonic('l');

		// HELP
		
		m_help_listener = new HelpListener();

		m_about = new JMenuItem("À propos", new ImageIcon(getClass().getResource("res/icons/menu/info.png")));
		m_about.addActionListener(m_help_listener);
		m_about.setAccelerator(KeyStroke.getKeyStroke("F1"));
		m_about.setMnemonic('p');

		m_help_menu = new JMenu("?");
		m_help_menu.add(m_about);
		m_help_menu.setMnemonic('?');

		// Menu

		m_menu_bar = new JMenuBar();
		m_menu_bar.add(m_file_menu);
		m_menu_bar.add(m_settings_menu);
		m_menu_bar.add(m_playlist_menu);
		m_menu_bar.add(m_help_menu);
		
		setJMenuBar(m_menu_bar);
	}

	// LISTENERS
	
	public class SystrayListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource().equals(m_show_item)) {
				
				setVisible(true);
				setState(Frame.NORMAL);
			}
			else if (event.getSource().equals(m_exit_item)) {
				
				m_media_manager.release();
				
				System.exit(0);
			}
			else if (event.getSource().equals(m_tray_icon)) {
				
				setVisible(true);
				setState(Frame.NORMAL);
			}	
		}
	}

	public class FileListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource().equals(m_open)) {

				JFileChooser file_chooser = new JFileChooser();
				file_chooser.setApproveButtonText("Sélectionner");
				file_chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				
				int state = file_chooser.showOpenDialog(m_instance);
				
				if (state == JOptionPane.OK_OPTION) {
					
					File selected_file = file_chooser.getSelectedFile();
					
					if (selected_file.isDirectory()) {
						
						File[] list_of_files = selected_file.listFiles(new FilenameFilter() {
							
							@Override
							public boolean accept(File file, String file_name) {
								
								if (file_name.toLowerCase().endsWith(".aac") 
										|| file_name.toLowerCase().endsWith(".m2a") 
										|| file_name.toLowerCase().endsWith(".m4a")
										|| file_name.toLowerCase().endsWith(".mp2") 
										|| file_name.toLowerCase().endsWith(".mp3")
										|| file_name.toLowerCase().endsWith(".ogg")
										|| file_name.toLowerCase().endsWith(".wav") 
										|| file_name.toLowerCase().endsWith(".wma") 
										
										|| file_name.toLowerCase().endsWith(".3gp")
										|| file_name.toLowerCase().endsWith(".avi")
										|| file_name.toLowerCase().endsWith(".flv")
										|| file_name.toLowerCase().endsWith(".mov")
										|| file_name.toLowerCase().endsWith(".mp4")
										|| file_name.toLowerCase().endsWith(".mpeg") 
										|| file_name.toLowerCase().endsWith(".mpg")) {
									
									return true;
								}
								
								return false;
							}
						});
						
						for (int i = 0; i < list_of_files.length; i++) {
							
							m_media_list_files.add(list_of_files[i]);
						}
					}
					else if (selected_file.isFile()) {
						
						String file_path = selected_file.getAbsolutePath().toLowerCase();
						
						if (file_path.endsWith(".aac") 
								|| file_path.endsWith(".m2a") 
								|| file_path.endsWith(".m4a")
								|| file_path.endsWith(".mp2") 
								|| file_path.endsWith(".mp3")
								|| file_path.endsWith(".ogg")
								|| file_path.endsWith(".wav") 
								|| file_path.endsWith(".wma") 
								
								|| file_path.endsWith(".3gp")
								|| file_path.endsWith(".avi")
								|| file_path.endsWith(".flv")
								|| file_path.endsWith(".mov")
								|| file_path.endsWith(".mp4")
								|| file_path.endsWith(".mpeg") 
								|| file_path.endsWith(".mpg")) {
							
							m_media_list_files.add(selected_file);
						}
					}
					
					FilesList files_list = new FilesList(m_media_list_files);
					files_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					
					for (File file : m_media_list_files) {
						
						m_media_manager.addVideoOnDemand(formatStreamId(file.getName()), file.getAbsolutePath(), true, VOD_MUX);
					}
					
					m_meta_server.setMediaList(files_list);
				}
			} 
			else if (event.getSource().equals(m_exit)) {

				m_media_manager.release();
				
				System.exit(0);
			}
		}
	}
	
	public class SettingsListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource().equals(m_network)) {

				final JDialog dialog = GUIFactory.createDialog(m_instance, "Paramètres", 100, 200);
				dialog.getContentPane().setLayout(new GridLayout(3, 1));
				
				final TextValidationField address_field = new TextValidationField(Validator.IP_ADDRESS);
				address_field.setText(m_address);
				
				final TextValidationField port_field = new TextValidationField(Validator.PORT_NUMBER);
				port_field.setText(String.valueOf(m_port));
				
				HighlightButton validate_button = new HighlightButton(new ImageIcon(getClass().getResource("res/icons/menu/accept.png")), 
																	ColorPalette.WHITE, 
																	ColorPalette.LIGHT_GRAY);
				validate_button.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent event) {
					
						if (Validator.IP_ADDRESS.isValid(address_field.getText()) 
								&& Validator.PORT_NUMBER.isValid(port_field.getText())) {
							
							m_address = address_field.getText();
							m_port = Integer.valueOf(port_field.getText());
							
							m_media_manager = new MediaPlayerFactory("--rtsp-host=" + m_address, "--rtsp-port=" + m_port).newMediaManager();
							
							dialog.dispose();
						}
					}
				});
				
				dialog.add(address_field);
				dialog.add(port_field);
				dialog.add(validate_button);
				
				dialog.setVisible(true);
			} 
		}
	}
	
	public class MediaListListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource().equals(m_show_playlist_playlist)) {
				
				FilesList files_list = new FilesList(m_media_list_files);
				
				JDialog dialog = GUIFactory.createDialog(m_instance, "La liste de lecture", 450, 600, false);
				dialog.setContentPane(new JScrollPane(files_list));
				dialog.setVisible(true);
			}
		}
	}
	
	public class HelpListener implements ActionListener {
		
		@Override
		public void actionPerformed(ActionEvent event) {
	
			if (event.getSource().equals(m_about)) {
				
				JOptionPane.showMessageDialog(m_instance,
						"StreamSender - VOD est un projet réalisé pour le fun!",
						"À propos", JOptionPane.INFORMATION_MESSAGE,
						new ImageIcon(getClass().getResource("res/icons/application.png")));
			}
		}
	}
	
	// IMPLEMENTED METHODS
	
	private static String formatStreamId(String fname) {
		
		return fname.substring(0, fname.lastIndexOf(".")).replace(" ", "_").replace("(", "[").replace(")", "]").replace(".", "_");
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
