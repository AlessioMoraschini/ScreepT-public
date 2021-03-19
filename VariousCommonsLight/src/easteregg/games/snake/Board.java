package easteregg.games.snake;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Board extends JPanel implements ActionListener {
	private static final long serialVersionUID = 3669580814590491106L;

	private final int B_WIDTH = 500;
    private final int B_HEIGHT = 500;
    private final int DOT_SIZE = 10;
    private final int ALL_DOTS = 900;
    private final int RAND_POS = 29;
    private Level level = Level.THREE;
    public int score = 0;

    private final int x[] = new int[ALL_DOTS];
    private final int y[] = new int[ALL_DOTS];

    private int dots;
    private int apple_x;
    private int apple_y;

    private boolean leftDirection;
    private boolean rightDirection;
    private boolean upDirection;
    private boolean downDirection;
    private boolean inGame;

    private Timer timer;
    private Image ball;
    private Image apple;
    private Image head;
    
    private boolean waitingForStart;
    private boolean gameOver;
    public Snake parent;

    public Board(Snake parent) {
    	
    	this.parent = parent != null ? parent : new Snake(null);
    	
        addKeyListener(new TAdapter());
        setBackground(Color.black);
        setFocusable(true);

        setMinimumSize(new Dimension(B_WIDTH, B_HEIGHT));
        setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));
        loadImages();
        initGame();
    }

    private void loadImages() {

        ImageIcon iid = new ImageIcon(getClass().getResource("/res/imgs/snake/GreenDot.png"));
        ball = iid.getImage();

        ImageIcon iia = new ImageIcon(getClass().getResource("/res/imgs/snake/apple.png"));
        apple = iia.getImage();

        ImageIcon iih = new ImageIcon(getClass().getResource("/res/imgs/snake/HeadDot.png"));
        head = iih.getImage();
    }

    private void initGame() {
    	
    	gameOver = true;
    	waitingForStart = true;
    	
    	score = 0;
    	
    	leftDirection = false;
    	rightDirection = true;
    	upDirection = false;  
    	downDirection = false;
    	inGame = true; 

        dots = 3;

        for (int z = 0; z < dots; z++) {
            x[z] = 50 - z * 10;
            y[z] = 50;
        }
    }
    
    private void startGame() {
    	locateApple();
    	gameOver = false;
        timer = new Timer(level.delay, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        doDrawing(g);
    }
    
    private void doDrawing(Graphics g) {
    	
    	if(waitingForStart) {
    		printString("Welcome to snake, press spacebar to start!", g, B_HEIGHT / 2);
    		waitingForStart = false;
    	}
        
        if (inGame) {

            g.drawImage(apple, apple_x-2, apple_y-4, this);

            for (int z = 0; z < dots; z++) {
                if (z == 0) {
                    g.drawImage(head, x[z], y[z], this);
                } else {
                    g.drawImage(ball, x[z], y[z], this);
                }
            }

            Toolkit.getDefaultToolkit().sync();

        } else {

            gameOver(g);
        }        
    }

    private void gameOver(Graphics g) {
        
        String msg = "SCORE: " + score + " (Level " + level.name() + ")";
        String msg2 = "Game Over  - (Press Ctrl+R to restart game, Esc to exit)";
        
        printString(msg, g, B_HEIGHT / 2);
        printString(msg2, g, B_HEIGHT / 2 + 25);
        
        gameOver = true;
    }
    
    private void printString(String msg, Graphics graphics, int y) {
    	Font small = new Font("Helvetica", Font.BOLD, 14);
        FontMetrics metr = getFontMetrics(small);
        
        Graphics g = graphics != null ? graphics : getGraphics();
        g.setColor(Color.white);
        g.setFont(small);
        g.drawString(msg, (B_WIDTH - metr.stringWidth(msg)) / 2, y);
    }

    private void checkApple() {

        if ((x[0] == apple_x) && (y[0] == apple_y)) {

            dots++;
            score += level.scoreModifier * 10;
            parent.updateTitle("Score: " + score);
            locateApple();
        }
    }

    private void move() {

        for (int z = dots; z > 0; z--) {
            x[z] = x[(z - 1)];
            y[z] = y[(z - 1)];
        }

        if (leftDirection) {
            x[0] -= DOT_SIZE;
        }

        if (rightDirection) {
            x[0] += DOT_SIZE;
        }

        if (upDirection) {
            y[0] -= DOT_SIZE;
        }

        if (downDirection) {
            y[0] += DOT_SIZE;
        }
    }

    private void checkCollision() {

        for (int z = dots; z > 0; z--) {

            if ((z > 4) && (x[0] == x[z]) && (y[0] == y[z])) {
                inGame = false;
            }
        }

        if (y[0] >= B_HEIGHT) {
            inGame = false;
        }

        if (y[0] < 0) {
            inGame = false;
        }

        if (x[0] >= B_WIDTH) {
            inGame = false;
        }

        if (x[0] < 0) {
            inGame = false;
        }
        
        if(!inGame) {
            timer.stop();
        }
    }

    private void locateApple() {

        int r = (int) (Math.random() * RAND_POS);
        apple_x = ((r * DOT_SIZE));

        r = (int) (Math.random() * RAND_POS);
        apple_y = ((r * DOT_SIZE));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (inGame) {

            checkApple();
            checkCollision();
            move();
        }

        repaint();
    }

    private class TAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {

            int key = e.getKeyCode();

            if ((key == KeyEvent.VK_LEFT) && (!rightDirection)) {
                leftDirection = true;
                upDirection = false;
                downDirection = false;
            
            } else if ((key == KeyEvent.VK_RIGHT) && (!leftDirection)) {
                rightDirection = true;
                upDirection = false;
                downDirection = false;
           
            } else if ((key == KeyEvent.VK_UP) && (!downDirection)) {
                upDirection = true;
                rightDirection = false;
                leftDirection = false;

            } else if ((key == KeyEvent.VK_DOWN) && (!upDirection)) {
                downDirection = true;
                rightDirection = false;
                leftDirection = false;
           
            } else if ((key == KeyEvent.VK_SPACE) && (gameOver)) {
            	initGame();
            	startGame();
           
            } else if ((key == KeyEvent.VK_R) && e.isControlDown() && (gameOver)) {
            	initGame();
            	repaint();
           
            } else if ((key == KeyEvent.VK_ESCAPE) && (gameOver)) {
            	parent.dispose();
            }
        }
    }
    
    public void setLevel(Level level) {
    	this.level = level;
    	if(timer != null) {
    		timer.setDelay(level.delay);
    	}
    }
    
    public enum Level{
    	ONE(160, 0.8f),
    	TWO(140, 0.9f),
    	THREE(120, 1f),
    	FOUR(100, 1.2f),
    	FIVE(80, 1.3f),
    	SIX(60, 1.4f),
    	SEVEN(50, 1.6f),
    	EIGHT(40, 1.8f),
    	NINE(30, 2f);
    	
    	public int delay;
    	public float scoreModifier;
    	
    	private Level(int delay, float scoreModifier) {
    		this.delay = delay;
    		this.scoreModifier = scoreModifier;
    	}
    }
}