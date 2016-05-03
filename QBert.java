import javax.swing.*;

import java.awt.Font;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.glu.*;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class QBert implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {
	public static final boolean TRACE = false;
	public static final String WINDOW_TITLE = "A4: [.Nitesh]";
	public static final int INITIAL_WIDTH = 640;
	public static final int INITIAL_HEIGHT = 640;
	public static final String[] TEXTURE_FILES = { "texture1.png", "texture2.png", "texture3.png", "texture4.png", "texture5.png", "texture6.png"};


	private static final GLU glu = new GLU();
	private static final GLUT glut = new GLUT();
	private static final String TEXTURE_PATH = "content/";


	public static void main(String[] args) {
		final JFrame frame = new JFrame(WINDOW_TITLE);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (TRACE)
					System.out.println("closing window '" + ((JFrame)e.getWindow()).getTitle() + "'");
				System.exit(0);
			}
		});

		final GLProfile profile = GLProfile.get(GLProfile.GL2);
		final GLCapabilities capabilities = new GLCapabilities(profile);
		final GLCanvas canvas = new GLCanvas(capabilities);
		try {
			Object self = self().getConstructor().newInstance();
			self.getClass().getMethod("setup", new Class[] { GLCanvas.class }).invoke(self, canvas);
			canvas.addGLEventListener((GLEventListener)self);
			canvas.addKeyListener((KeyListener)self);
			canvas.addMouseListener((MouseListener)self);
			canvas.addMouseMotionListener((MouseMotionListener)self);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		canvas.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);

		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setVisible(true);

		canvas.requestFocusInWindow();

		if (TRACE)
			System.out.println("-> end of main().");
	}

	private static Class<?> self() {
		// This ugly hack gives us the containing class of a static method 
		return new Object() { }.getClass().getEnclosingClass();
	}

	/*** Instance variables and methods ***/

	private String direction;
	private Texture[] textures;
	private boolean isometric = true;
	private float t = 0.0f;
	private float INTERVAL = 0.01f; // inc/dec t by this amount 60 times a second
	private float LERPINTERVAL = 0.025f;
	private int currQBitAngle = 90;
	private int prevQBitAngle = 90;
	private int angleAfterMode = 90;
	private float lerpingAngle = 0.0f;
	private float cameraAngleX = 0.0f;
	private float cameraAngleY = 0.0f;
	private float currPMotionY = 0.0f;
	private float currQBitX = 0.0f; 
	private float currQBitY = 0.0f; 
	private float currQBitZ = 0.0f; 
	private float prevQBitX = 0.0f; 
	private float prevQBitY = 0.0f; 
	private float prevQBitZ = 0.0f;
	private float ar = 1;
	private boolean startLerping = false;
	private int enemyACurrQBitAngle = 0;
	private int enemyAPrevQBitAngle = 0;
	private float enemyALerpingAngle = 0.0f;
	private float enemyACurrPMotionY = 0.0f;
	private float enemyACurrQBitY = 0.0f; 
	private float enemyACurrQBitZ = 0.0f; 
	private float enemyAPrevQBitY = 0.0f; 
	private float enemyAPrevQBitZ = 0.0f;
	private boolean enemyAStartLerping = true;
	private boolean enemyAReverse = false;
	private int enemyBCurrQBitAngle = 0;
	private int enemyBPrevQBitAngle = 0;
	private float enemyBLerpingAngle = 0.0f;
	private float enemyBCurrPMotionY = 0.0f;
	private float enemyBCurrQBitX = 0.0f; 
	private float enemyBCurrQBitY = 0.0f; 
	private float enemyBPrevQBitX = 0.0f; 
	private float enemyBPrevQBitY = 0.0f; 
	private boolean enemyBStartLerping = true;
	private boolean enemyBReverse = false;
	private Point2D startPoint = null;
	private Point2D endPoint = null;
	private Point2D movedBy = null;
	private float[] colour = new float[] { 0.2f, 0.2f, 0.2f, 0.0f };
	private float start = 7.0f;
	private float end = 10.0f;
	private final int[] LIGHTS = { GL2.GL_LIGHT0, GL2.GL_LIGHT1, GL2.GL_LIGHT2, GL2.GL_LIGHT3, GL2.GL_LIGHT4, GL2.GL_LIGHT5 };
	private String[] lightDescriptions = { "", "", "", "", "", ""};
	private boolean modeChange = true;
	private boolean lights[] = new boolean[10];
	private boolean smoothShading = false;
	private int lives = 3;
	private TextRenderer renderer;

	private float[][] objectVertices = {
			{ 0.25f,  0.25f, -1.5f},
			{-0.25f,  0.25f, -1.5f},
			{-0.25f, -0.25f, -1.5f},
			{ 0.25f, -0.25f, -1.5f},
			{ 0.25f, -0.25f, -2.0f},
			{-0.25f, -0.25f, -2.0f},
			{-0.25f,  0.25f, -2.0f},
			{ 0.25f,  0.25f, -2.0f}
	};

	private int[][] objectQuads = {
			{7, 0, 3, 4},
			{1, 6, 5, 2},
			{4, 5, 6, 7},
			{0, 1, 2, 3},
			{3, 2, 5, 4},// Bottom
			{7, 6, 1, 0} //Top
	};

	private int[][] texCoord = {
			{1,1},
			{0,1},
			{0,0},
			{1,0}
	};
	
	public void setup(final GLCanvas canvas) {
		// Called for one-time setup
		if (TRACE)
			System.out.println("-> executing setup()");
		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				t += INTERVAL;
				if (t > 1.0f)
					t = 0.0f;
				canvas.repaint();
			}
		}, 1000, 1000/60);
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// Called when the canvas is (re-)created - use it for initial GL setup
		if (TRACE)
			System.out.println("-> executing init()");

		final GL2 gl = drawable.getGL().getGL2();

		textures = new Texture[TEXTURE_FILES.length];
		try {
			for (int i = 0; i < TEXTURE_FILES.length; i++) {
				File infile = new File(TEXTURE_PATH + TEXTURE_FILES[i]); 
				BufferedImage image = ImageIO.read(infile);
				ImageUtil.flipImageVertically(image);
				textures[i] = TextureIO.newTexture(AWTTextureIO.newTextureData(gl.getGLProfile(), image, false));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL2.GL_CULL_FACE);
		gl.glShadeModel(GL2.GL_SMOOTH);
		gl.glEnable(GL2.GL_NORMALIZE);
		gl.glShadeModel(GL2.GL_FLAT);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, new float[] { 0.2f, 0.2f, 0.2f, 1.0f }, 0); // this is the default
		gl.glEnable(GL2.GL_COLOR_MATERIAL);
		gl.glColorMaterial(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE);
		gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, new float[] { 1.0f, 1.0f, 1.0f, 1.0f }, 0);
		gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 32.0f);

		lightDescriptions[0] = "ambient only: (0.3, 0.3, 0.3)";
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, new float[] { 0.1f, 0.1f, 0.1f, 1.0f }, 0);
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, new float[] { 0.0f, 0.0f, 0.0f, 1.0f }, 0);  // LIGHT0 sets these to 1, 1, 1, 1
		gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, new float[] { 0.0f, 0.0f, 0.0f, 1.0f }, 0); // LIGHT0 sets these to 1, 1, 1, 1

		lightDescriptions[1] = "diffuse only: (1.0, 1.0, 1.0), directed from (1.0, 1.0, 1.0)";
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, new float[] { 1.0f, 1.0f, 1.0f, 0.0f }, 0); // directional
		gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, new float[] { 1.0f, 1.0f, 1.0f, 1.0f }, 0);

		lightDescriptions[2] = "diffuse (0.5, 0.5, 1.0) specular (1.0, 1.0, 0.3), positioned at (1.0, 1.0, 1.0) and attenuated by 0.1 + 0.2d + 0.5d^2";
		gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_POSITION, new float[] {1.0f, 1.0f, 1.0f, 0.0f }, 0); // positional
		gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_DIFFUSE, new float[] { 0.5f, 0.5f, 1.0f, 1.0f }, 0);    // blue matter
		gl.glLightfv(GL2.GL_LIGHT2, GL2.GL_SPECULAR, new float[] { 1.0f, 1.0f, 0.3f, 1.0f }, 0);   // yellow shiny
		gl.glLightf(GL2.GL_LIGHT2, GL2.GL_CONSTANT_ATTENUATION, 0.1f);
		gl.glLightf(GL2.GL_LIGHT2, GL2.GL_LINEAR_ATTENUATION, 0.2f);
		gl.glLightf(GL2.GL_LIGHT2, GL2.GL_QUADRATIC_ATTENUATION, 0.5f);

		lightDescriptions[3] = "specular (0.0, 0.0, 1.0), directed from (0, 0, 1)";
		gl.glLightfv(GL2.GL_LIGHT3, GL2.GL_POSITION, new float[] { 0.0f, 0.0f, 1.0f, 0.0f }, 0); // directional
		gl.glLightfv(GL2.GL_LIGHT3, GL2.GL_SPECULAR, new float[] { 0.0f, 0.0f, 1.0f, 1.0f }, 0); // blue shiny

		//spot light will follow Qbit
		lightDescriptions[4] = "blue spotlight at (0, 0, 0, 1) directed at (0, 2, -4.2)";
		gl.glLightfv(GL2.GL_LIGHT4, GL2.GL_POSITION, new float[] {0, 0, 0, 1.0f }, 0); // positional
		gl.glLightfv(GL2.GL_LIGHT4, GL2.GL_DIFFUSE, new float[] { 0.75f, 0.75f, 1.0f, 1.0f  }, 0);
		gl.glLightfv(GL2.GL_LIGHT4, GL2.GL_SPECULAR, new float[] { 0.5f, 0.5f, 0.5f, 1.0f }, 0);
		gl.glLightfv(GL2.GL_LIGHT4, GL2.GL_SPOT_DIRECTION, new float[] { 0.0f, 2.0f, -4.2f }, 0);
		gl.glLightf(GL2.GL_LIGHT4, GL2.GL_SPOT_EXPONENT, 1.0f);
		gl.glLightf(GL2.GL_LIGHT4, GL2.GL_SPOT_CUTOFF, 10.0f); 

		lightDescriptions[5] = "orange spotlight at (0, 0, 0, 1) directed at (-0.5, 0, -5)";

		lights[4] = true;
		lights[5] = true;
		
		renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		// Draws the display
		if (TRACE)
			System.out.println("-> executing display()");

		final GL2 gl = drawable.getGL().getGL2();

		if (modeChange) {
			System.out.println();
			for (int i = 0; i < LIGHTS.length; i++) {
				if (lights[i]) {
					gl.glEnable(LIGHTS[i]);
					System.out.print("Enabling light " + i);
					if (lightDescriptions[i].length() > 0) {
						System.out.println(": " + lightDescriptions[i]);
					} else {
						System.out.println();
					}
				} else {
					gl.glDisable(LIGHTS[i]);
					System.out.println("Disabling light " + i);
				}
			}
			if (smoothShading) {
				gl.glShadeModel(GL2.GL_SMOOTH);
				System.out.println("smooth shading");
			} else {
				gl.glShadeModel(GL2.GL_FLAT);
				System.out.println("flat shading");
			}
			modeChange = false;

		}

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(80, ar, 1.0f, 10.0f);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		if (lives > 0) {
			renderer.beginRendering(INITIAL_WIDTH, INITIAL_HEIGHT);
			renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
			renderer.draw("Lives : "+lives, INITIAL_WIDTH/2 - 75, INITIAL_HEIGHT-30);
			renderer.endRendering();

			if (isometric) {
				startPoint = null;
				endPoint = null;
				gl.glPushMatrix();
				gl.glTranslatef(0.0f, 0.0f, -3.75f);
				gl.glRotatef(25, 1, 0, 0);
				gl.glTranslatef(0.0f, 0.0f, 3.75f);
				gl.glTranslatef(-1.0f, 0.0f, -3.5f);
				gl.glRotatef(-47.5f, 0.0f, 1.0f, 0.0f);
				gl.glTranslatef(1.0f, 0.0f, 3.5f);

				//spot light will follow Qbit
				lightDescriptions[5] = "orange spotlight at (0, 0, 0, 1) directed at (-0.5, 0, -5)";
				gl.glLightfv(GL2.GL_LIGHT5, GL2.GL_POSITION, new float[] {prevQBitX, prevQBitY, prevQBitZ, 1.0f }, 0); // positional
				gl.glLightfv(GL2.GL_LIGHT5, GL2.GL_DIFFUSE, new float[] { 1.0f, 0.5f, 0.0f, 0.6f }, 0);
				gl.glLightfv(GL2.GL_LIGHT5, GL2.GL_SPECULAR, new float[] { 1.0f, 0.5f, 0.0f, 1.0f }, 0);
				gl.glLightfv(GL2.GL_LIGHT5, GL2.GL_SPOT_DIRECTION, new float[] { -0.5f, 0.0f, -5.0f }, 0);
				gl.glLightf(GL2.GL_LIGHT5, GL2.GL_SPOT_EXPONENT, 3.0f);
				gl.glLightf(GL2.GL_LIGHT5, GL2.GL_SPOT_CUTOFF, 4.0f); 
				drawWorld(gl);
				gl.glPopMatrix();

			}else {
				cameraAngleX = 0;
				cameraAngleY = 0;
				if (endPoint != null && startPoint != null) {
					movedBy = new Point2D.Float((float)(endPoint.getX()-startPoint.getX()),(float)(endPoint.getY()-startPoint.getY()));
					cameraAngleX = (float) (movedBy.getX()/5);
					cameraAngleY = (float) (movedBy.getY()/5);
				}
				gl.glPushMatrix();
				gl.glEnable(GL2.GL_FOG);
				gl.glFogfv(GL2.GL_FOG_COLOR, colour, 0);
				gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
				gl.glFogf(GL2.GL_FOG_START, start);
				gl.glFogf(GL2.GL_FOG_END, end);
				gl.glScalef(4, 4, 4);
				gl.glTranslatef(0.0f, 0.0f, -0.25f);
				gl.glRotatef(15-cameraAngleY, 1, 0, 0);
				gl.glRotatef(180-prevQBitAngle-cameraAngleX,  0, 1, 0);
				gl.glTranslatef(0.5f, 0.0f, 4.2f);
				gl.glTranslatef(-prevQBitX, -prevQBitY, -prevQBitZ);

				//spot light will follow Qbit
				lightDescriptions[5] = "orange spotlight at (0, 0, 0, 1) directed at (-0.5, -0.5, -5)";
				gl.glLightfv(GL2.GL_LIGHT5, GL2.GL_POSITION, new float[] {prevQBitX, prevQBitY, prevQBitZ, 1.0f }, 0); // positional
				gl.glLightfv(GL2.GL_LIGHT5, GL2.GL_DIFFUSE, new float[] { 1.0f, 0.5f, 0.0f, 0.6f }, 0);
				gl.glLightfv(GL2.GL_LIGHT5, GL2.GL_SPECULAR, new float[] { 1.0f, 0.5f, 0.0f, 1.0f }, 0);
				gl.glLightfv(GL2.GL_LIGHT5, GL2.GL_SPOT_DIRECTION, new float[] { -0.5f, -0.5f, -5.0f }, 0);
				gl.glLightf(GL2.GL_LIGHT5, GL2.GL_SPOT_EXPONENT, 3.0f);
				gl.glLightf(GL2.GL_LIGHT5, GL2.GL_SPOT_CUTOFF, 4.0f);
				drawWorld(gl);

				gl.glTranslatef(0.0f, -1.5f, 17.0f);
				gl.glTranslatef(-0.0f, 0.0f, -3.0f);
				gl.glRotatef(180, 1, 0, 0);
				gl.glScalef(14f, 0.1f, 14f);
				gl.glTranslated(0.0f, 0.0f, 3.0f);
				gl.glColor3f(1, 1, 1);
				drawPrimitive(gl);
				gl.glDisable(GL2.GL_FOG);
				gl.glPopMatrix();

			}
		}else{
			renderer.beginRendering(INITIAL_WIDTH, INITIAL_HEIGHT);
			renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
			renderer.draw("GAME OVER!", 200, 350);
			renderer.draw("Press Enter to Play Again", 115, 320);
			renderer.endRendering();
		}

		gl.glFlush();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// Called when the canvas is destroyed (reverse anything from init) 
		if (TRACE)
			System.out.println("-> executing dispose()");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// Called when the canvas has been resized
		// Note: glViewport(x, y, width, height) has already been called so don't bother if that's what you want
		if (TRACE)
			System.out.println("-> executing reshape(" + x + ", " + y + ", " + width + ", " + height + ")");

		final GL2 gl = drawable.getGL().getGL2();
		ar = (float)width / (height == 0 ? 1 : height);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(80, ar, 1.0f, 10.0f);

	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {

		direction = null;
		prevQBitAngle = currQBitAngle;
		if (e.getKeyChar() >= '0' && e.getKeyChar() <= '9') {
			lights[e.getKeyChar() - '0'] = !lights[e.getKeyChar() - '0'];
			modeChange = true;
		}if (e.getKeyChar() == 'f') {
			smoothShading = !smoothShading;
			modeChange = true;
		}else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyChar() == 'a'){
			if (!startLerping) {
				direction = "left";
				currQBitAngle -= 90;
				angleAfterMode = currQBitAngle%360;
			}
		}else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyChar() == 'd'){
			if (!startLerping) {
				direction = "right";
				currQBitAngle += 90;
				angleAfterMode = currQBitAngle%360;
			}
		}else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyChar() == 'w'){
			if (!startLerping) {
				direction = "up";
				startLerping = true;
				prevQBitX = currQBitX;
				prevQBitY = currQBitY;
				if (angleAfterMode == 90 || angleAfterMode  == -270) {
					currQBitX += 0.5f;
					currQBitY -= 0.5f;
				}else if (angleAfterMode == -90 || angleAfterMode  == 270) {
					currQBitX -= 0.5f;
					currQBitY += 0.5f;
				}else if(angleAfterMode == 0){
					currQBitY -= 0.5;
					currQBitZ += 0.5;
				}else if (Math.abs(angleAfterMode) == 180) {
					currQBitY += 0.5;
					currQBitZ -= 0.5;
				}
			}

		}else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyChar() == 's'){
			if (!startLerping) {
				direction = "down";
				startLerping = true;
				prevQBitX = currQBitX;
				prevQBitY = currQBitY;
				if (angleAfterMode == 90 || angleAfterMode  == -270) {
					currQBitX -= 0.5f;
					currQBitY += 0.5f;
				}else if (angleAfterMode == -90 || angleAfterMode  == 270) {
					currQBitX += 0.5f;
					currQBitY -= 0.5f;
				}else if(angleAfterMode == 0) {
					currQBitY += 0.5;
					currQBitZ -= 0.5;
				}else if (Math.abs(angleAfterMode) == 180) {
					currQBitY -= 0.5;
					currQBitZ += 0.5;
				}
			}
		}else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			lives = 3;
		}
		if (direction != null) {
			System.out.println("Direction key pressed: " + direction);
			((GLCanvas)e.getSource()).repaint();
		}

		if (e.getKeyChar() == ' ') {
			isometric = !isometric;
			// anything else...
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		//		System.out.println("drag: (" + e.getX() + "," + e.getY() + ") at " + e.getWhen());
		endPoint = new Point2D.Float(e.getX(), e.getY());
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		//		System.out.println("press: (" + e.getX() + "," + e.getY() + ") at " + e.getWhen());
		startPoint = new Point2D.Float(e.getX(), e.getY());
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}

	public void drawPrimitive (GL2 gl){
		float x, y, z;
		int index;
		for (int i = 0; i < objectQuads.length; i++) {
			textures[i].bind(gl);
			textures[i].enable(gl);
			gl.glBegin(GL2.GL_QUADS);	

			for (int j = 0; j < objectQuads[i].length; j++) {
				index = objectQuads[i][j];
				x = objectVertices[index][0];
				y = objectVertices[index][1];
				z = objectVertices[index][2];
				gl.glTexCoord2f(texCoord[j][0], texCoord[j][1]);
				gl.glVertex3f( x,y,z);
			}

			gl.glEnd();
			textures[i].disable(gl);
		}

		gl.glColor3f(1, 1, 1);
		gl.glLineWidth(1.25f);
		for (int i = 0; i < objectQuads.length; i++) {
			gl.glBegin(GL2.GL_LINE_LOOP);	
			for (int j = 0; j < objectQuads[i].length; j++) {
				index = objectQuads[i][j];
				x = objectVertices[index][0];
				y = objectVertices[index][1];
				z = objectVertices[index][2];
				gl.glVertex3f( x,y,z);
			}
			gl.glEnd();
		}
	}

	public void drawWorld(GL2 gl) {

		float numberOfColumns = 7;
		float numberOfRows = 7;
		float moveInX = -1.5f;
		float moveInY = -1.5f;
		float moveInZ = -0.5f;

		gl.glPushMatrix();	

		for (int i = 0; i < numberOfRows; i++) {
			gl.glPushMatrix();	
			gl.glTranslatef(moveInX, moveInY, moveInZ);
			gl.glColor3f(1, 1, 1);
			drawPrimitive(gl);
			for (int j = 1; j < numberOfColumns; j++) {

				if (i == 0 && j == numberOfColumns-1) { //animated the top block
					gl.glTranslatef(0.0f, 0.5f,-0.5f);
					gl.glTranslatef(0.0f, 0.0f,-1.75f);
					gl.glRotatef(360.0f* t, 0, 1, 0);
					gl.glTranslatef(0.0f, 0.0f, 1.75f);
					gl.glColor3f(1, 1, 1);
					drawPrimitive(gl);
				}else {
					gl.glTranslatef(0.0f, 0.5f,-0.5f);
					gl.glColor3f(1, 1, 1);
					drawPrimitive(gl);
				}
			}

			gl.glPopMatrix();
			numberOfColumns--;
			moveInX += 0.5f;
			moveInZ -= 0.5f;
		}
		moveQBit(gl);
		moveEnemyAQBit(gl);
		moveEnemyBQBit(gl);
		gl.glPopMatrix();
	}

	public void moveQBit(GL2 gl){

		if ((currQBitY == enemyAPrevQBitY+1.5 && currQBitZ == enemyAPrevQBitZ-0.5) || (currQBitY == enemyBPrevQBitY-1 && currQBitX == enemyBPrevQBitX+2) || currQBitY > 2 || currQBitY < -1 || currQBitX > 2 || currQBitX < -1 || currQBitZ < -1.0) { //respain the QBert when it dies
			currQBitX = 0;
			currQBitY = 0;
			currQBitZ = 0;
			prevQBitX = 0;
			prevQBitY = 0;
			prevQBitZ = 0;
			lerpingAngle = 0.0f;
			currPMotionY = 0.0f;
			prevQBitX = 0;
			prevQBitY = 0;
			prevQBitZ = 0;
			prevQBitAngle -= 360;
			startLerping = true;
			enemyACurrQBitAngle = 0;
			enemyAPrevQBitAngle = 0;
			enemyALerpingAngle = 0.0f;
			enemyACurrPMotionY = 0.0f;
			enemyACurrQBitY = 0.0f; 
			enemyACurrQBitZ = 0.0f; 
			enemyAPrevQBitY = 0.0f; 
			enemyAPrevQBitZ = 0.0f;
			enemyAStartLerping = true;
			enemyAReverse = false;
			enemyBCurrQBitAngle = 0;
			enemyBPrevQBitAngle = 0;
			enemyBLerpingAngle = 0.0f;
			enemyBCurrPMotionY = 0.0f;
			enemyBCurrQBitX = 0.0f; 
			enemyBCurrQBitY = 0.0f; 
			enemyBPrevQBitX = 0.0f; 
			enemyBPrevQBitY = 0.0f; 
			enemyBStartLerping = true;
			enemyBReverse = false;
			lives--;
		}

		if (currQBitAngle != prevQBitAngle) { //lerp QBert when turning
			startLerping = true;
			if (prevQBitAngle < currQBitAngle) {
				prevQBitAngle += 10;
			}else {
				prevQBitAngle -= 10;
			}
		}else {
			startLerping = false;
		}

		prevQBitX = moveQBit(prevQBitX, currQBitX);
		prevQBitY = moveQBitUp(prevQBitY, currQBitY);
		prevQBitZ = moveQBit(prevQBitZ, currQBitZ);

		if (isometric) {
			drawQBit(gl);
		}
	}

	public void drawQBit(GL2 gl){
		int stacks = 30;
		int slices = 30;
		float sphereRadius = 0.20f;
		float coneBase = 0.1f;
		float coneHeight = 0.5f;
		float innerRadius = 0.05f;
		float outerRadius = 0.01f;

		gl.glPushMatrix();
		gl.glTranslatef(-0.5f, 0.0f, -4.2f);//set QBert starting position
		gl.glTranslatef(prevQBitX, prevQBitY, prevQBitZ);
		gl.glRotatef(prevQBitAngle, 0, 1, 0);
		gl.glColor3f(1.0f,0.0f,0.0f);
		glut.glutSolidSphere(sphereRadius, slices, stacks);
		gl.glColor3f(0.0f,1.0f,0.0f);
		glut.glutSolidCone(coneBase, coneHeight, slices, stacks);
		gl.glPushMatrix();
		gl.glColor3f(0.0f,0.0f,1.0f);
		gl.glTranslatef(0.1f, 0.10f, 0.1f);
		glut.glutSolidTorus(innerRadius, outerRadius, slices, stacks);
		gl.glPopMatrix();    
		gl.glPushMatrix();
		gl.glTranslatef(-0.1f, 0.10f, 0.1f);
		glut.glutSolidTorus(innerRadius, outerRadius, slices, stacks);
		gl.glPopMatrix();
		gl.glPopMatrix();
	}

	public float moveQBit(float prev, float curr){

		if (prev != curr) {
			if (prev < curr) {
				prev += LERPINTERVAL;
				prev = (float) (Math.round(prev*1000.0)/1000.0);
			}else{
				prev -= LERPINTERVAL;
				prev = (float) (Math.round(prev*1000.0)/1000.0);
			}
		}		
		return prev;
	}

	public float moveQBitUp(float prev, float curr){
		prev -= currPMotionY;
		prev = (float) (Math.round(prev*1000.0)/1000.0);

		if (prev != curr) {
			startLerping = true;
			lerpingAngle += 180.0f/20.0f;
			currPMotionY = (float) Math.sin(Math.toRadians(lerpingAngle))*0.5f;
			if (prev < curr) {
				prev += LERPINTERVAL;
			}else{
				prev -= LERPINTERVAL;
			}
			prev += currPMotionY;
			prev = (float) (Math.round(prev*1000.0)/1000.0);
		}else {
			currPMotionY = 0.0f;
			lerpingAngle = 0.0f;
			startLerping = false;
		}
		return prev;
	}

	public void moveEnemyAQBit(GL2 gl){

		if (enemyAPrevQBitY == -2.5 || enemyAPrevQBitZ == 2.5 ) { //respain the QBert when it dies
			enemyACurrQBitAngle = 180;
			enemyAReverse = true;

		}else if (enemyAPrevQBitY == 0 || enemyAPrevQBitZ == 0) {
			enemyACurrQBitAngle = 0;
			enemyAReverse = false;
		}

		if (enemyACurrQBitAngle != enemyAPrevQBitAngle) { //lerp QBert when turning
			if (enemyAPrevQBitAngle < enemyACurrQBitAngle) {
				enemyAPrevQBitAngle += 10;
			}else {
				enemyAPrevQBitAngle -= 10;
			}
			enemyAStartLerping = false;
		}else {
			enemyAStartLerping = true;
		}

		if (enemyAStartLerping) {
			enemyAPrevQBitY = moveEnemyAQBitUp(enemyAPrevQBitY , enemyACurrQBitY);
			enemyAPrevQBitZ = moveQBit(enemyAPrevQBitZ, enemyACurrQBitZ);
		}

		gl.glPushMatrix();
		gl.glTranslatef(-1.0f, 1.5f, -0.5f);//set QBert starting position
		drawEnemyAQBit(gl);
		gl.glPopMatrix();

	}

	public void drawEnemyAQBit(GL2 gl){
		int stacks = 30;
		int slices = 30;
		float coneBase = 0.1f;
		float coneHeight = 0.5f;

		gl.glPushMatrix();
		gl.glTranslatef(-0.5f, 0.0f, -4.2f);//set QBert starting position
		gl.glTranslatef(0, enemyAPrevQBitY, enemyAPrevQBitZ);
		gl.glRotatef(enemyAPrevQBitAngle, 0, 1, 0);
		gl.glColor3f(1.0f, 0.0f,0.75f);
		glut.glutSolidCube(0.3f);
		gl.glPushMatrix();
		gl.glColor3f(1.0f,1.0f,0.0f);
		gl.glRotatef(-90*t, 1, 0, 0);
		glut.glutSolidCone(coneBase, coneHeight, slices, stacks);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glColor3f(1.0f,1.0f,0.0f);
		gl.glRotatef(90, 1, 0, 0);
		glut.glutSolidCone(coneBase, coneHeight, slices, stacks);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glColor3f(1.0f,1.0f,0.0f);
		gl.glRotatef(90, 0, 1, 0);
		glut.glutSolidCone(coneBase, coneHeight, slices, stacks);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glColor3f(1.0f,1.0f,0.0f);
		gl.glRotatef(-90, 0, 1, 0);
		glut.glutSolidCone(coneBase, coneHeight, slices, stacks);
		gl.glPopMatrix();
		gl.glPopMatrix();
	}

	public float moveEnemyAQBitUp(float prev, float curr){
		prev -= enemyACurrPMotionY;
		prev = (float) (Math.round(prev*1000.0)/1000.0);

		if (prev != curr) {
			enemyALerpingAngle += 180.0f/20.0f;
			enemyACurrPMotionY = (float) Math.sin(Math.toRadians(enemyALerpingAngle))*0.5f;
			if (prev < curr) {
				prev += LERPINTERVAL;
			}else{
				prev -= LERPINTERVAL;
			}
			prev += enemyACurrPMotionY;
			prev = (float) (Math.round(prev*1000.0)/1000.0);
		}else {
			enemyACurrPMotionY = 0.0f;
			enemyALerpingAngle = 0.0f;
			if (enemyAReverse) {
				enemyACurrQBitY += 0.5f;
				enemyACurrQBitZ -= 0.5f;
			}else {
				enemyACurrQBitY -= 0.5f;
				enemyACurrQBitZ += 0.5f;
			}
		}
		return prev;
	}

	public void moveEnemyBQBit(GL2 gl){

		if (enemyBPrevQBitX == -2.5 || enemyBPrevQBitY == -2.5 ) { //respain the QBert when it dies
			enemyBCurrQBitAngle = 180;
			enemyBReverse = true;
		}else if (enemyBPrevQBitX == 0 || enemyBPrevQBitY == 0) {
			enemyBCurrQBitAngle = 0;
			enemyBReverse = false;
		}

		if (enemyBCurrQBitAngle != enemyBPrevQBitAngle) { //lerp QBert when turning
			if (enemyBPrevQBitAngle < enemyBCurrQBitAngle) {
				enemyBPrevQBitAngle += 10;
			}else {
				enemyBPrevQBitAngle -= 10;
			}
			enemyBStartLerping = false;
		}else {
			enemyBStartLerping = true;
		}

		if (enemyBStartLerping) {
			enemyBPrevQBitX = moveQBit(enemyBPrevQBitX, enemyBCurrQBitX);
			enemyBPrevQBitY = moveEnemyBQBitUp(enemyBPrevQBitY , enemyBCurrQBitY);
		}

		gl.glPushMatrix();
		gl.glTranslatef(2.0f, -1.0f, -1.0f);//set QBert starting position
		drawEnemyBQBit(gl);
		gl.glPopMatrix();

	}

	public void drawEnemyBQBit(GL2 gl){
		int stacks = 30;
		int slices = 30;
		float coneBase = 0.1f;
		float coneHeight = 0.5f;

		gl.glPushMatrix();
		gl.glTranslatef(-0.5f, 0.0f, -4.2f);//set QBert starting position
		gl.glTranslatef(enemyBPrevQBitX, enemyBPrevQBitY, 0);
		gl.glRotatef(90, 1, 0, 0);
		gl.glRotatef(enemyBPrevQBitAngle, 0, 1, 0);
		gl.glColor3f(0.25f, 0.25f,0.75f);
		glut.glutSolidTorus(0.1, 0.2, stacks, slices);;
		gl.glPushMatrix();
		gl.glColor3f(0.25f,0.9f,0.2f);
		glut.glutSolidCone(coneBase, coneHeight, slices, stacks);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glRotatef(180, 0, 1, 0);
		gl.glColor3f(0.9f,0.9f,0.2f);
		glut.glutSolidCone(coneBase, coneHeight, slices, stacks);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glRotatef(90, 0, 1, 0);
		gl.glRotatef(360*t, 1, 0, 0);
		gl.glColor3f(0.9f,0.9f,0.2f);
		glut.glutSolidCone(coneBase, coneHeight, slices, stacks);
		gl.glPopMatrix();
		gl.glPopMatrix();
	}

	public float moveEnemyBQBitUp(float prev, float curr){
		prev -= enemyBCurrPMotionY;
		prev = (float) (Math.round(prev*1000.0)/1000.0);

		if (prev != curr) {
			enemyBLerpingAngle += 180.0f/20.0f;
			enemyBCurrPMotionY = (float) Math.sin(Math.toRadians(enemyBLerpingAngle))*0.5f;
			if (prev < curr) {
				prev += LERPINTERVAL;
			}else{
				prev -= LERPINTERVAL;
			}
			prev += enemyBCurrPMotionY;
			prev = (float) (Math.round(prev*1000.0)/1000.0);
		}else {
			enemyBCurrPMotionY = 0.0f;
			enemyBLerpingAngle = 0.0f;
			if (enemyBReverse) {
				enemyBCurrQBitY -= 0.5f;
				enemyBCurrQBitX += 0.5f;
			}else {
				enemyBCurrQBitY += 0.5f;
				enemyBCurrQBitX -= 0.5f;
			}
		}
		return prev;
	}

}
