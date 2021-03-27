package gui.commons.frameutils.frame.panels.common;

import javax.swing.JButton;

import gui.commons.frameutils.frame.panels.arch.ParentPanel;
import net.miginfocom.swing.MigLayout;
import resources.IconsPathConfigurator;

public class PlayStopPausePanel extends ParentPanel {
	private static final long serialVersionUID = -3789652661940094712L;
	
	Runnable playAction = () -> {};
	Runnable stopAction = () -> {};
	Runnable pauseAction = () -> {};
	
	boolean isPlaying = false;
	boolean isPaused = false;
	boolean isStopped = false;
	
	private JButton pauseButton;
	private JButton stopButton;
	private JButton playButton;
	
	public PlayStopPausePanel() {
		this(() -> {},() -> {},() -> {});
	}
	
	public PlayStopPausePanel(Runnable playAction, Runnable stopAction, Runnable pauseAction) {
		this.playAction = playAction != null ? playAction : () -> {};
		this.stopAction = stopAction != null ? stopAction : () -> {};
		this.pauseAction = pauseAction != null ? pauseAction : () -> {};
		
		isPlaying = false;  
		isPaused = false;   
		isStopped = true;  
	
		super.init();
	}
	
	@Override
	public void initGui() {
		setLayout(new MigLayout("fill, insets 1, hidemode 2", "[::33px,grow][::33px,grow][::33px,grow]", "[::9px,grow]"));
		
		playButton = getButton("", null, null, IconsPathConfigurator.ICON_PLAY, true);
		add(playButton, "cell 0 0,alignx left,aligny center");
		
		pauseButton = getButton("", null, null, IconsPathConfigurator.ICON_PAUSE, true);
		add(pauseButton, "cell 1 0,alignx left,aligny center");
		
		stopButton = getButton("", null, null, IconsPathConfigurator.ICON_STOP, true);
		add(stopButton, "cell 2 0,alignx left,aligny center");
		
		initButtons();
	}
	
	public void initButtons() {
		setButtonsEnabled(true, false, false);
		setPlayStopPause(false, false, true);
	}
	
	@Override
	public void addHandlers() {
		playButton.addActionListener((e) -> {
			if(isStopped || isPaused || isPlaying) {
				setButtonsEnabled(false, true, true);
				setPlayStopPause(true, false, false);
				playAction.run();
			}
		});
		
		pauseButton.addActionListener((e) -> {
			if(isPlaying) {
				setButtonsEnabled(true, false, true);
				setPlayStopPause(false, true, false);
				pauseAction.run();
			}
		});
		
		stopButton.addActionListener((e) -> {
			if(isPlaying) {
				setButtonsEnabled(true, false, false);
				setPlayStopPause(false, false, true);
				stopAction.run();
			}
		});
	}
	
	@Override
	protected void addKeyListeners() {
	}

	public void setPlayStopPause(boolean playing, boolean paused, boolean stopped) {
		isPaused = paused;
		isPlaying = playing;
		isStopped = stopped;
	}
	
	public void setButtonsEnabled(boolean playEnabled, boolean pauseEnabled, boolean stopEnabled) {
		playButton.setEnabled(playEnabled);
		stopButton.setEnabled(pauseEnabled);
		pauseButton.setEnabled(stopEnabled);
	}
	
	public void setPauseVisible(boolean pauseVisible) {
		pauseButton.setVisible(pauseVisible);
	}
	public void setPlayVisible(boolean playVisible) {
		playButton.setVisible(playVisible);
	}
	public void setStopVisible(boolean stopVisible) {
		stopButton.setVisible(stopVisible);
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	public boolean isPaused() {
		return isPaused;
	}

	public boolean isStopped() {
		return isStopped;
	}

	public JButton getPauseButton() {
		return pauseButton;
	}

	public JButton getStopButton() {
		return stopButton;
	}

	public JButton getPlayButton() {
		return playButton;
	}
	
}
