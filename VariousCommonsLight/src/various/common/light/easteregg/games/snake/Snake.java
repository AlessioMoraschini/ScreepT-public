package various.common.light.easteregg.games.snake;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import various.common.light.easteregg.games.snake.Board.Level;

public class Snake extends JFrame {
	private static final long serialVersionUID = -2575763564390459298L;

	public static Snake instance;
	
	Board board;
	JMenuBar menuBar;
	
	String title = "Snake";

	public Snake(String title) {
		init();
		setVisible(true);
		this.title = title != null ? title : "Snake";
		setTitle(this.title);
		instance = this;
    }
	
	public void init() {
		
		menuBar = new JMenuBar();
		initMenuBar(menuBar);
		setJMenuBar(menuBar);
        
		board = new Board(this);
		add(board);
		
        setResizable(false);
        pack();
        
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        setAlwaysOnTop(true);
        
        repaint();
	}
	
	public void initMenuBar(JMenuBar menuBar) {
		JMenu levelMenu = new JMenu("Level");
		menuBar.add(levelMenu);
		
		for(Level level : Level.values()) {
			JMenuItem levelChooser = new JMenuItem(level.name());
			levelChooser.addActionListener((e)->{
				board.setLevel(level);
			});

			levelMenu.add(levelChooser);
		}
	}
	
	public void updateTitle(String toAdd) {
		setTitle(title + " - " + toAdd);
	}
	
    public static void main(String[] args) {
        
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {                
                JFrame snakeRunner = new Snake("Snake Game");
                snakeRunner.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });
    }
    
    public static Snake getInstance(String title) {
    	Snake instance = Snake.instance != null ? Snake.instance : new Snake(title);
    	Snake.instance.setVisible(true);
    	instance.toFront();
    	
    	return instance;
    }
}