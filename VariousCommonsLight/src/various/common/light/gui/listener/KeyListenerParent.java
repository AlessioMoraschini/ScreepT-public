package various.common.light.gui.listener;

import java.awt.event.KeyEvent;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import various.common.light.om.LimitedConcurrentList;
import various.common.light.utility.log.SafeLogger;

public class KeyListenerParent {
	
	// TextFileWriter logger creation
	protected static SafeLogger logger = new SafeLogger(KeyListenerParent.class);
	
	protected Vector<Integer> currentPressed = new Vector<>();
	
	public static long MAX_INTERVAL_MS = 600;
	public LimitedConcurrentList<ClickUpdate> lastNClicks = new LimitedConcurrentList<>(8);
	
	public KeyListenerParent() {
		currentPressed = new Vector<>();
	}
	

	public boolean dispatchKeyEvent(final KeyEvent e) {
		
		updateKeyCode(e);
		lastNClicks.add(new ClickUpdate(e.getKeyCode(), e.isControlDown(), e.isAltDown(), e.isShiftDown()));
		
		return false;
	}
	

	protected void updateKeyCode(KeyEvent keyEvent) {
		if (keyEvent.getID() == KeyEvent.KEY_PRESSED && !currentPressed.contains(keyEvent.getKeyCode())) {
			currentPressed.add(keyEvent.getKeyCode());
		} else if (keyEvent.getID() == KeyEvent.KEY_RELEASED && currentPressed.contains(keyEvent.getKeyCode())){
			currentPressed.remove(Integer.valueOf(keyEvent.getKeyCode()));
		}
	}
	
	public boolean isOtherPressed(int keyCode) {
		return currentPressed.contains(keyCode);
	}
	
	public boolean isArrowLR(int keyCode) {
		return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_KP_LEFT || keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_KP_RIGHT;
	}

	public boolean isShortcutKey(KeyEvent e) {
		return (e.isAltDown() || e.isControlDown() || e.isShiftDown());
	}
	
	public boolean repeatedClicksInLimitedTime(KeyEvent e, int repeats, boolean ctrl, boolean alt, boolean shift, long maxInterval) {
		boolean combinationFound = false;

		if(lastNClicks.isFull() || lastNClicks.getSize() >= repeats) {
			
			if(e.isControlDown() == ctrl && e.isAltDown() == alt && e.isShiftDown() == shift) {
				int nLastClicks = lastNClicks.getSize();
				int limit = nLastClicks-1-repeats >= 0 ? nLastClicks-1-repeats : 0;
				
				ClickUpdate lastClickData = lastNClicks.getLast();
				long lastTime = lastClickData.timestamp;
				boolean inARowMatched = false;
				
				for(int i = nLastClicks-1; i > limit; i--) {
					inARowMatched = lastTime - lastClickData.timestamp < maxInterval;
					lastTime = lastClickData.timestamp;
					inARowMatched = inARowMatched 
							&& lastClickData.ctrlPressed.get() == ctrl
							&& lastClickData.altPressed.get() == alt
							&& lastClickData.shiftPressed.get() == shift
							&& lastClickData.keyCode == e.getKeyCode();
					
					if(!inARowMatched) {
						inARowMatched = false;
						break;
					}
					
					if (i-1 >= 0) {
						lastClickData = lastNClicks.getList().get(i - 1);
					}
				}
				
				combinationFound = inARowMatched;
			}
			
			if(combinationFound) {
				for (int i = 0; i < repeats; i++) {
					lastNClicks.removeLast();
				}
			}
		}
		
		return combinationFound;
	}

	
	public class ClickUpdate {
		long timestamp = 0;
		int keyCode = KeyEvent.VK_UNDEFINED;
		
		AtomicBoolean ctrlPressed;
		AtomicBoolean altPressed;
		AtomicBoolean shiftPressed;
		
		public ClickUpdate(int keyCode, boolean ctrl, boolean alt, boolean shift) {
			ctrlPressed = new AtomicBoolean(); 
			altPressed = new AtomicBoolean();  
			shiftPressed = new AtomicBoolean();
			
			this.keyCode = keyCode;
			ctrlPressed.set(ctrl);
			altPressed.set(alt);
			shiftPressed.set(shift);
			timestamp = System.currentTimeMillis();
		}
	}
}
