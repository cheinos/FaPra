package main;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

/**
 * Implements different {@link java.awt.event.WindowListener}, so that the Popup menu is closed probably.
 * @author Florian
 *
 */
public class WindowListener implements WindowFocusListener, ComponentListener{
	
	private MainFrame frame;
	
	public WindowListener(MainFrame frame){
		this.frame = frame;
	}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		frame.getPopupClass().getPopupMenu().setVisible(false);
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
		frame.getPopupClass().getPopupMenu().setVisible(false);
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		frame.getPopupClass().getPopupMenu().setVisible(false);
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		frame.getPopupClass().getPopupMenu().setVisible(false);
	}

	@Override
	public void componentResized(ComponentEvent e) {
		frame.getPopupClass().getPopupMenu().setVisible(false);
	}

	@Override
	public void componentShown(ComponentEvent e) {
		frame.getPopupClass().getPopupMenu().setVisible(false);
	}

}
