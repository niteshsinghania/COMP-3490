import javax.swing.*;

import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.glu.*;

public class LuxoLamp implements GLEventListener, KeyListener {
	public static final boolean TRACE = false;

	public static final String WINDOW_TITLE = "A3Q2: [.awNitesh]";
	public static final int INITIAL_WIDTH = 640;
	public static final int INITIAL_HEIGHT = 640;

	private static GLU glu = new GLU();

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

	private int projection = 0;
	private int cameraAngle = 0;
	private boolean viewChanged = true;
	private String keysDown = "";
	private int baseRotate = 90;
	private int baseAngle = 0;
	private int midAngle = 90;
	private int headAngle = 0;
	private int headRotate = 180;
	private float t;
	private float INTERVAL;



	private float[][] colours = {
			{1.0f, 1.0f, 1.0f}, //White
			{0.0f, 0.0f, 1.0f}, //Blue
			{0.0f, 0.5f, 0.0f}, //Green
			{0.0f, 0.5f, 0.5f}, //BlueViolet
			{0.4f, 0.4f, 0.4f}, //Gray
			{0.75f, 0.0f, 0.75f}, //Purple
			{0.6f, 0.6f, 0.3f}, //Khakhi
			{1.0f, 1.0f, 0.0f}, //Yellow
			{1.0f, 0.0f, 0.0f}
	};
	
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

	public void setup(final GLCanvas canvas) {
		// Called for one-time setup
		if (TRACE)
			System.out.println("-> executing setup()");

		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (t >= 1) {
					t = 0;
				} else {
					t += INTERVAL;
				}
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

		t = 0.0f;
		INTERVAL = 0.005f; // inc/dec t by this amount 60 times a second
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		// gl.glEnable(GL2.GL_CULL_FACE);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		// Draws the display
		if (TRACE)
			System.out.println("-> executing display()");

		final GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		if (keysDown.length() > 0) {
			for (int i = 0; i < keysDown.length(); i++) {
				switch (keysDown.charAt(i)) {
				case 'z':
					baseRotate = (baseRotate + 1) % 360;
					break;
				case 'x':
					baseRotate = (baseRotate + 359) % 360;
					break;
				case 'c':
					baseAngle = Math.max(baseAngle - 1, -60);
					break;
				case 'v':
					baseAngle = Math.min(baseAngle + 1, 60);
					break;
				case 'q':
					midAngle = Math.max(midAngle - 1, 45);
					break;
				case 'a':
					midAngle = Math.min(midAngle + 1, 135);
					break;
				case 'w':
					headAngle = Math.max(headAngle - 1, -45);
					break;
				case 's':
					headAngle = Math.min(headAngle + 1, 45);
					break;
				case 'e':
					headRotate = Math.max(headRotate - 1, 135);
					break;
				case 'd':
					headRotate = Math.min(headRotate + 1, 225);
					break;
				}
			}

			//System.out.printf("baseRotate=%d baseAngle=%d midAngle=%d headAngle=%d headRotate=%d\n", baseRotate, baseAngle, midAngle, headAngle, headRotate);
		}

		if (viewChanged) {
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();

			if (projection == 0) {
				gl.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 10.0f);
			}else {
				gl.glFrustumf(-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 10.0f); // [-1.0 to -10]
			}
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			
			if (cameraAngle == 0) {
				if (projection == 0) {
					glu.gluLookAt(0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
				}else {
					glu.gluLookAt(0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0);
				}
			}else if(cameraAngle == 1) {
				if (projection == 0) {
					glu.gluLookAt(0.0, 1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
				}else {
					glu.gluLookAt(-1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
				}
				
			}else{
				if (projection == 0) {
					glu.gluLookAt(-2.0, 1.0, 7.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
				}else {
					glu.gluLookAt(3.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0);
				}
				
			}

			viewChanged = false;
		}

		drawObject(gl);
		drawDesk(gl);
		drawLamp(gl);
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

		// The stuff that usually happens here is done in display()
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// FIX: added second condition to prevent the key bounce in Windows
		if ("qweasdzxcv".contains(""+e.getKeyChar()) && !keysDown.contains("" + e.getKeyChar())) {
			keysDown += e.getKeyChar();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		int i;

		// FIX: shouldn't be necessary but double-check key bounce
		do {
			i = keysDown.indexOf(e.getKeyChar());
			if (i >= 0) {
				keysDown = keysDown.substring(0, i) + keysDown.substring(i+1);
			}
		} while (i >= 0);
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		if (e.getKeyChar() == ' ') {
			cameraAngle++;
			if (cameraAngle == 3) {
				cameraAngle = 0;
				projection = (projection + 1) % 2;
			}
			System.out.println("Pressed space: camera = " + cameraAngle + ", projection = " + projection);
			viewChanged = true;
			((GLCanvas)e.getSource()).repaint();
		}
	}

	public void drawLamp (GL2 gl){

		//draw lamp base
		gl.glPushMatrix();	
		gl.glTranslatef(-0.25f, -0.90f, 0.0f);
		gl.glTranslatef(0.0f, 0.0f, -1.75f);
		gl.glRotatef(baseRotate, 0.0f, 1.0f, 0.0f);
		gl.glScalef(1.0f, 0.25f, 1.0f);
		gl.glTranslatef(0.0f, 0.0f, 1.75f);
		gl.glColor3f(colours[1][0], colours[1][1], colours[1][2]);
		drawPrimitive (gl);
		
		gl.glTranslatef(0.0f, 0.0f, -1.75f);
		gl.glScalef(1.0f/1.0f, 1.0f/0.25f, 1.0f/1.0f); //Reverse scaling
		gl.glRotatef(baseAngle, 1.0f, 0.0f, 0.0f);
		gl.glScalef(0.15f, 0.75f, 0.1f);
		gl.glTranslatef(0.0f, 0.30f, 1.75f);
		gl.glColor3f(colours[3][0], colours[3][1], colours[3][2]);
		drawPrimitive (gl);
		
		//green joint to the bar from the base
		gl.glTranslatef(0.0f, 0.25f, 0.0f);
		gl.glTranslatef(0.0f, 0.0f, -1.75f);
		gl.glScalef(1.0f/0.15f, 1.0f/0.75f, 1.0f/0.1f); //Reverse scaling
		gl.glRotatef(midAngle, 1.0f, 0.0f, 0.0f);
		gl.glScalef(0.25f, 0.25f, 0.25f);
		gl.glTranslatef(0.0f, 0.0f, 1.75f);
		gl.glColor3f(colours[2][0], colours[2][1], colours[2][2]);
		drawPrimitive (gl);
		
		//second bar from green joint
		gl.glTranslatef(0.0f, 0.5f, 0.0f);
		gl.glTranslatef(0.0f, 0.0f, -1.75f);
		gl.glScalef(1.0f/0.25f, 1.0f/0.25f, 1.0f/0.25f); //Reverse scaling
		gl.glScalef(0.15f, 0.5f, 0.1f);
		gl.glTranslatef(0.0f, 0.0f, 1.75f);
		gl.glColor3f(colours[3][0], colours[3][1], colours[3][2]);
		drawPrimitive (gl);
		
		//yellow joint 
		gl.glTranslatef(0.0f, 0.30f, 0.0f);
		gl.glTranslatef(0.0f, 0.0f, -1.75f);
		gl.glScalef(1.0f/0.15f, 1.0f/0.5f, 1.0f/0.1f); //Reverse scaling
		gl.glRotatef(headAngle, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(headRotate, 0.0f, 1.0f, 0.0f);
		gl.glScalef(0.25f, 0.20f, 0.20f);
		gl.glTranslatef(0.0f, 0.0f, 1.75f);
		gl.glColor3f(colours[7][0], colours[7][1], colours[7][2]);
		drawPrimitive (gl);
		
		//lamp head
		gl.glTranslatef(0.0f, 0.0f, -0.75f);
		gl.glTranslatef(0.0f, 0.0f, -1.75f);
		gl.glScalef(1.0f/0.25f, 1.0f/0.20f, 1.0f/0.20f); //Reverse scaling
		gl.glScalef(0.25f, 0.25f, 0.50f);
		gl.glTranslatef(0.0f, 0.0f, 1.75f);
		gl.glColor3f(colours[8][0], colours[8][1], colours[8][2]);
		drawPrimitive (gl);
		gl.glPopMatrix();


	}

	public void drawObject (GL2 gl){

		//draw object bottom
		gl.glPushMatrix();
		gl.glTranslatef(0.50f, -0.80f, -0.25f);
		gl.glTranslatef(0.0f, 0.0f, -1.75f);
		gl.glRotatef(40, 0.0f, 1.0f, 0.0f);
		gl.glScalef(0.1f, 0.6f, 0.1f);
		gl.glTranslatef(0.0f, 0.0f, 1.75f);
		gl.glColor3f(colours[5][0], colours[5][1], colours[5][2]);
		drawPrimitive (gl);
		
		gl.glTranslatef(0.0f, 0.25f, 0.0f);
		gl.glTranslatef(0.0f, 0.0f,-1.75f);
		gl.glScalef(1.0f/0.1f, 1.0f/0.6f, 1.0f/0.1f); //Reverse scaling
		gl.glRotatef(-360 * t*2, 0.0f, 1.0f, 0.0f);
		gl.glScalef(0.4f, 0.175f, 0.4f);
		gl.glTranslatef(0.0f, 0.0f, 1.75f);
		gl.glColor3f(colours[6][0], colours[6][1], colours[6][2]);
		drawPrimitive (gl);
		gl.glPopMatrix();

	}
	
	public void drawDesk (GL2 gl){

		//draw object bottom
		gl.glPushMatrix();
		gl.glTranslatef(0.0f, -1.75f, 0.0f);
		gl.glTranslatef(0.0f, 1.0f, -1.75f);
//		gl.glRotatef(5, 0.0f, 1.0f, 0.0f);
		gl.glScalef(3.0f, 0.25f, 3.0f);
		gl.glTranslatef(0.0f, -1.0f, 1.75f);
		gl.glColor3f(colours[4][0], colours[4][1], colours[4][2]);
		drawPrimitive (gl);
		gl.glPopMatrix();

	}

	public void drawPrimitive (GL2 gl){
		float x, y, z;
		int index;

		gl.glBegin(GL2.GL_QUADS);	
		for (int i = 0; i < objectQuads.length; i++) {
			for (int j = 0; j < objectQuads[i].length; j++) {
				index = objectQuads[i][j];
				x = objectVertices[index][0];
				y = objectVertices[index][1];
				z = objectVertices[index][2];
				gl.glVertex3f( x,y,z);
			}
		}
		gl.glEnd();

		gl.glColor3f(colours[0][0],colours[0][1],colours[0][2]);
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

}
