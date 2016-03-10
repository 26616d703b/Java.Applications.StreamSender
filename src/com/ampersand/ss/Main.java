package com.ampersand.ss;

import javax.swing.SwingUtilities;

public class Main {

	public static void main(String[] args) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				LiveStreamSender window = new LiveStreamSender();	
				//VODStreamSender window = new VODStreamSender();
				
				window.setVisible(true);
			}
		});	
	}
}
