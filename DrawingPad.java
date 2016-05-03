import javax.swing.*;

import java.awt.Polygon;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;

public class DrawingPad implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {
	public static final boolean TRACE = false;

	public static final String WINDOW_TITLE = "A2Q2: [.Nitesh]"; 
	public static final int INITIAL_WIDTH = 800;
	public static final int INITIAL_HEIGHT = 600;
	public static final float SELECTION_DISTANCE = 7.0f;

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
			canvas.addMouseMotionListener((MouseMotionListener)self);
			canvas.addKeyListener((KeyListener)self);
			canvas.addMouseListener((MouseListener)self);
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

	float[][] colours = new float[][] {
			{ 0.0f, 0.0f, 1.0f },
			{ 0.0f, 1.0f, 0.0f },
			{ 1.0f, 0.0f, 0.0f },
			{ 0.0f, 1.0f, 1.0f },
			{ 1.0f, 0.0f, 1.0f },
			{ 1.0f, 1.0f, 0.0f },
			{ 0.0f, 0.5f, 0.5f },
			{ 0.5f, 0.0f, 0.5f },
			{ 0.5f, 0.5f, 0.0f },
			{ 0.5f, 0.5f, 0.5f }
	};
	private float[] matrix = new float[]{
			1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1
	};
	private static final int tX = 12, tY = 13;
	private static final int rX0 = 0, rX1 = 1, rY0 = 4, rY1 = 5;
	int width, height;
	float colourPanelHeight = 1;
	ArrayList<Point2D> mousePressForColourPanel;
	ArrayList<Point2D> mousePressForTriangles;
	ArrayList<Point2D> mouseMovedLocation;
	ArrayList<Point2D[]> colourBoxes;
	ArrayList<Point2D[]> triangles;
	ArrayList<Polygon> colourBoxPolygons;
	ArrayList<Polygon> trianglesPolygons;
	ArrayList<float[]> triangleColours;
	Point2D selectedPoint;
	Point2D triangleDraggedBy;
	Point2D pointDraggedBy;
	Point2D lastDragPos;
	int colorBoxindex;
	int selectedTriangle;
	float panelWidth;
	float panelHeight;
	float translateViewXBy;
	float translateViewYBy;
	float scaleViewBy;

	public void setup(final GLCanvas canvas) {
		// Called for one-time setup
		if (TRACE)
			System.out.println("-> executing setup()");

		mousePressForColourPanel = new ArrayList<Point2D>();
		mousePressForTriangles = new ArrayList<Point2D>();
		mouseMovedLocation = new ArrayList<Point2D>();
		colourBoxes = new ArrayList<Point2D[]>();
		triangles = new ArrayList<Point2D[]>();
		colourBoxPolygons = new ArrayList<Polygon>();
		trianglesPolygons = new ArrayList<Polygon>();
		triangleColours = new ArrayList<float[]>();
		pointDraggedBy  = null;
		selectedPoint = null;
		lastDragPos = null;
		triangleDraggedBy = null;
		selectedTriangle = -1;
		colorBoxindex = 0;
		panelWidth = 0;
		panelHeight = 0;
		translateViewXBy = 0;
		translateViewYBy = 0;
		scaleViewBy = 1;
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// Called when the canvas is (re-)created - use it for initial GL setup
		if (TRACE)
			System.out.println("-> executing init()");

		final GL2 gl = drawable.getGL().getGL2();

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glEnable(GL2.GL_BLEND);
		gl.glEnable(GL2.GL_POINT_SMOOTH);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
	}


	
	@Override
	public void display(GLAutoDrawable drawable) {
		float[] matrixViewTransformation = Arrays.copyOf(matrix,matrix.length);
		panelWidth = width/10f;
		panelHeight = height/colours.length;


		// Draws the display
		if (TRACE)
			System.out.println("-> executing display()");

		final GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		colourBoxes(gl, panelWidth,panelHeight);
		processMousePressForColourPanel(gl);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		matrixViewTransformation = transformView(translateViewXBy,translateViewYBy,scaleViewBy);
		gl.glLoadMatrixf(matrixViewTransformation, 0);
		processMousePressForTriangles(gl, panelHeight);

		if (selectedPoint != null && pointDraggedBy != null) {
			processPointDragged(panelHeight);
		}else if (selectedTriangle != -1 && triangleDraggedBy != null) {
			processTriangleDragged();
		}


		if (triangles.size() > 0) {
			drawPoint(gl);
			drawTriangles(gl);

			if (selectedTriangle != -1) {
				selectTriangle(gl);
			}
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// Called when the canvas is destroyed (reverse anything from init) 
		if (TRACE)
			System.out.println("-> executing dispose()");
		mousePressForColourPanel.clear();
		mousePressForTriangles.clear();
		mouseMovedLocation.clear();
		colourBoxes.clear();
		triangles.clear();
		colourBoxPolygons.clear();
		triangles.clear();
		colourBoxPolygons.clear();
		trianglesPolygons.clear();
		triangleColours.clear();

	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// Called when the canvas has been resized
		// Note: glViewport(x, y, width, height) has already been called so don't bother if that's what you want
		if (TRACE)
			System.out.println("-> executing reshape(" + x + ", " + y + ", " + width + ", " + height + ")");

		final GL2 gl = drawable.getGL().getGL2();

		// The projection places 0,0 in the bottom-left corner ABOVE the control panel
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(0.0f, width, 0.0f, height, 0.0f, 1.0f);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		this.width = width;
		this.height = height;
	}

	@Override
	public void mouseClicked(MouseEvent event) {
		((GLCanvas)event.getSource()).repaint();

	}

	@Override
	public void mouseEntered(MouseEvent event) {
	}

	@Override
	public void mouseExited(MouseEvent event) {
		mouseMovedLocation.clear();
		mousePressForTriangles.clear();
		pointDraggedBy = null;
		selectedPoint = null;

		((GLCanvas)event.getSource()).repaint();
	}

	@Override
	public void mousePressed(MouseEvent event) {
		Point2D point = new Point2D.Float(event.getX(), ((GLCanvas)(event.getSource())).getHeight() - event.getY() - 1);
		float[] matrixViewTransformation = null;
		float scale = 1;

		matrixViewTransformation = transformView(-translateViewXBy,-translateViewYBy,scale);
		point = transformVertice(matrixViewTransformation,new float[] {(float) point.getX()/scaleViewBy, (float) point.getY()/scaleViewBy, 0, 1});

		mousePressForColourPanel.add(point);
		mousePressForTriangles.add(point);

		if (selectedTriangle != -1) {
			lastDragPos = point;
		}

		((GLCanvas)event.getSource()).repaint();
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		selectedPoint = null;
		triangleDraggedBy = null;
		pointDraggedBy = null;

		if (lastDragPos != null) {
			lastDragPos = null;
		}
		
		((GLCanvas)event.getSource()).repaint();
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		// NOTE: remove/comment out the following if you are not using this event!
		Point2D point = new Point2D.Float(event.getX(), ((GLCanvas)(event.getSource())).getHeight() - event.getY() - 1);
		float[] matrixViewTransformation = null;
		float scale = 1;
		int index = selectedTriangle;

		matrixViewTransformation = transformView(-translateViewXBy,-translateViewYBy,scale);
		point = transformVertice(matrixViewTransformation,new float[] {(float) point.getX()/scaleViewBy, (float) point.getY()/scaleViewBy, 0, 1});

		if (selectedPoint != null) {
			pointDraggedBy = point;
		}else if (selectedTriangle != -1) {
			calculationForTriangleDraggedBy(index, point);
		}
		
		((GLCanvas)event.getSource()).repaint();
	}

	@Override
	public void mouseMoved(MouseEvent event) {
		Point2D point = new Point2D.Float(event.getX(), ((GLCanvas)(event.getSource())).getHeight() - event.getY() - 1);
		float[] matrixViewTransformation = null;
		float scale = 1;
		
		matrixViewTransformation = transformView(-translateViewXBy,-translateViewYBy,scale);
		point = transformVertice(matrixViewTransformation,new float[] {(float) point.getX()/scaleViewBy, (float) point.getY()/scaleViewBy, 0, 1});
		mouseMovedLocation.add(point);
		((GLCanvas)event.getSource()).repaint();
	}

	@Override
	public void keyPressed(KeyEvent event) {
	}

	@Override
	public void keyReleased(KeyEvent event) {
	}

	@Override
	public void keyTyped(KeyEvent event) {

		float scaleBy;
		float rotateby;

		switch (event.getKeyChar()) {
		case 'w':
			processKeysForTranslation(0,2.5f);
			break;
		case 'a':
			processKeysForTranslation(-2.5f,0);
			break;
		case 's':
			processKeysForTranslation(0,-2.5f);
			break;
		case 'd':
			processKeysForTranslation(2.5f,0);
			break;
		case 'c':
			scaleBy = 1;
			scaleBy -= 0.1f;
			if (selectedTriangle == -1 && scaleViewBy >= 0) {
				scaleViewBy -= 0.1f;
			}
			processKeysForScaling(scaleBy);
			break;
		case 'v':
			scaleBy = 1;
			scaleBy += 0.1f;
			if (selectedTriangle == -1 && scaleViewBy <= 2) {
				scaleViewBy += 0.1f;
			}
			processKeysForScaling(scaleBy);
			break;
		case 'z':
			translateViewXBy = 0;
			translateViewYBy = 0;
			scaleViewBy = 1;
			break;
		case 'e':
			rotateby = (float) (Math.PI/48);
			processKeysForRotation(rotateby);
			break;
		case 'r':
			rotateby = -1*(float) (Math.PI/48);
			processKeysForRotation(rotateby);
			break;
		}

		((GLCanvas)event.getSource()).repaint();
	}

	public void processTriangleDragged (){

		float x = 0, y = 0;
		int index = selectedTriangle;

		if (triangleDraggedBy != null) {

			for (int i = 0; i < triangles.get(index).length; i++) {
				if (triangleDraggedBy!=null && selectedTriangle != -1) {

					x = (float) (triangles.get(index)[i].getX() + triangleDraggedBy.getX());
					y = (float) (triangles.get(index)[i].getY() + triangleDraggedBy.getY());

					triangles.get(index)[i].setLocation(x, y);

				}
			}
			drawTrianglePolygons();
		}
	}

	public void processPointDragged(float panelHeight) {

		isClickInColourBox(panelHeight);


		if (selectedPoint != null && pointDraggedBy != null) {
			for (int i = 0; i < triangles.size(); i++) {
				for (int j = 0; j < triangles.get(i).length; j++) {

					if (triangles.get(i)[j].equals(selectedPoint)) {
						triangles.get(i)[j] = pointDraggedBy;
						drawTrianglePolygons();
					}
				}
			}
			selectedPoint = pointDraggedBy;
			pointDraggedBy = null;
		}
	}
	
	public void drawTrianglePolygons (){
		Polygon trianglePolygon;
		trianglesPolygons.clear();
		//add modified polygon
		for (int j2 = 0; j2 < triangles.size(); j2++) {
			trianglePolygon = new Polygon();
			for (int k = 0; k < triangles.get(j2).length; k++) {
				trianglePolygon.addPoint((int) triangles.get(j2)[k].getX(), (int) triangles.get(j2)[k].getY());
			}
			trianglesPolygons.add(trianglePolygon);
		}
	}

	public void processMousePressForColourPanel(GL2 gl ){
		if (mousePressForColourPanel.size()  == 0) {
			selectColorBox(gl, colorBoxindex);
		}else {
			colorBoxindex = colourPanelSelected(gl, colorBoxindex);
		}
	}

	public void processMousePressForTriangles(GL2 gl, float panelHeight){

		float distance = 0;

		outerloop:
			for (int i = 0; i < mousePressForTriangles.size(); i++) {
				for (int j = 0; j < triangles.size(); j++) {
					for (int j2 = 0; j2 < triangles.get(j).length; j2++) {

						distance = (float) triangles.get(j)[j2].distance(mousePressForTriangles.get(i));

						if (distance < SELECTION_DISTANCE && distance > 0 && mousePressForTriangles.size() > 1) {

							mousePressForTriangles.set(i, triangles.get(j)[j2]);
							break outerloop;
						}
					}
				}
			}

		isClickInColourBox(panelHeight);


		if (mousePressForTriangles.size() == 3) {
			addTriangle(gl);	
		}else if  (mousePressForTriangles.size() > 0) {

			if (mousePressForTriangles.size() == 1) {
				selectedPoint = isPointSelected(gl);
				selectedTriangle = isTriangleSelected();
			}

			//if triangle is not selected than move on to creating a triangle
			if (mousePressForTriangles.size() < 3 && mousePressForTriangles.size() > 0) {
				drawUnprocessedTriangle(gl);
			}else {
				mousePressForTriangles.clear();

			}
		}
	}

	public void selectTriangle(GL2 gl){
		Point2D[] triangleCoordinates = triangles.get(selectedTriangle);
		
		gl.glLineWidth(3f);
		gl.glBegin(GL2.GL_LINE_LOOP);
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		for (int i = 0; i < triangleCoordinates.length; i++) {
			gl.glVertex2f((float) triangleCoordinates[i].getX(), (float) triangleCoordinates[i].getY());
		}				
		gl.glEnd();
		//make smooth outlines
		gl.glPointSize(SELECTION_DISTANCE);
		gl.glBegin(GL2.GL_POINTS);
		for (int i = 0; i < triangleCoordinates.length; i++) {
			gl.glVertex2f((float) triangleCoordinates[i].getX(), (float) triangleCoordinates[i].getY());
		}				
		gl.glEnd();

	}

	public Point2D isPointSelected (GL2 gl){

		float distance = 0;
		Point2D pointSelected = null;

		outerloop:
			for (int i = 0; i < mousePressForTriangles.size(); i++) {
				for (int j = 0; j < triangles.size(); j++) {
					for (int j2 = 0; j2 < triangles.get(j).length; j2++) {

						distance = (float) triangles.get(j)[j2].distance(mousePressForTriangles.get(i));

						if (distance < SELECTION_DISTANCE && distance >= 0) {
							pointSelected = triangles.get(j)[j2];
							mousePressForTriangles.clear();
							mouseMovedLocation.clear();
							break outerloop;
						}else {
							pointSelected = null;
						}
					}
				}
			}
		return pointSelected;

	}

	public void isClickInColourBox(float panelHeight){

		for (int i = 0; i < mousePressForTriangles.size(); i++) {

			if (mousePressForTriangles.get(i).getY() < panelHeight) {
				mousePressForTriangles.clear();
				mouseMovedLocation.clear();
			}
		}

		if (pointDraggedBy != null) {
			if (pointDraggedBy.getY() < panelHeight)  {
				pointDraggedBy = null;
				selectedPoint = null;
			}
		}
	}

	public void drawTriangles(GL2 gl) {


		for (int i = 0; i < triangles.size(); i++) {
			gl.glBegin(GL2.GL_TRIANGLES);
			gl.glColor3f(triangleColours.get(i)[0], triangleColours.get(i)[1], triangleColours.get(i)[2]);
			for (int j = 0; j < triangles.get(i).length; j++) {

				gl.glVertex2f((float)triangles.get(i)[j].getX(), (float)triangles.get(i)[j].getY());
			}
			gl.glEnd();
			gl.glLineWidth(1f);
			gl.glBegin(GL2.GL_LINE_LOOP);
			gl.glColor3f(1.0f, 1.0f, 1.0f);
			for (int j = 0; j < triangles.get(i).length; j++) {
				gl.glVertex2f((float)triangles.get(i)[j].getX(), (float)triangles.get(i)[j].getY());
			}
			gl.glEnd();

		}
	}

	public void addTriangle(GL2 gl){

		Point2D[] triangle = new Point2D[3];
		if (mousePressForTriangles.size() == 3) {
			for (int i = 0; i < mousePressForTriangles.size(); i++) {
				triangle[i] = mousePressForTriangles.get(i);
			}
			triangles.add(triangle);
			triangleColours.add(colours[colorBoxindex]);
			drawTrianglePolygons();
			mousePressForTriangles.clear();
			mouseMovedLocation.clear();
		}

	}

	public void drawUnprocessedTriangle(GL2 gl) {
		gl.glLineWidth(2.5f);
		gl.glBegin(GL2.GL_LINES);
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		if (mousePressForTriangles.size() > 1) {
			gl.glVertex2f((float) mousePressForTriangles.get(0).getX(), (float) mousePressForTriangles.get(0).getY());
			gl.glVertex2f((float) mousePressForTriangles.get(1).getX(), (float) mousePressForTriangles.get(1).getY());

		}
		gl.glEnd();
		drawPoint(gl);


	}

	public void rubberBanding(GL2 gl) {
		ArrayList<Point2D> currMouseLocations = new ArrayList<Point2D>();

		currMouseLocations.addAll(mousePressForTriangles);

		//if there a move location than take the the most recent moved location
		if (mouseMovedLocation.size() > 0) {
			currMouseLocations.add(mouseMovedLocation.get(mouseMovedLocation.size()-1));
		}
		gl.glLineWidth(2.5f);
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glBegin(GL2.GL_LINE_LOOP);
		for (int i = 0; i < currMouseLocations.size(); i++) {
			gl.glVertex2f((float) currMouseLocations.get(i).getX(), (float) currMouseLocations.get(i).getY());
		}
		gl.glEnd();
		mouseMovedLocation.clear();

	}

	public void drawPoint(GL2 gl) {
		//make smooth outlines
		gl.glPointSize(5f);
		gl.glBegin(GL2.GL_POINTS);
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		for (int i = 0; i < mousePressForTriangles.size(); i++) {
			gl.glVertex2f((float) mousePressForTriangles.get(i).getX(), (float) mousePressForTriangles.get(i).getY());
		}
		if (selectedPoint != null) {
			gl.glVertex2f((float) selectedPoint .getX(), (float) selectedPoint.getY());
		}
		gl.glEnd();
		rubberBanding(gl);
	}

	public void colourBoxes (GL2 gl, float panelWidth, float panelHeight){
		float boxX0 = 0, boxY0 = 0, boxX1 = 0, boxY1 = panelHeight, boxX2 = panelWidth, boxY2 = panelHeight, boxX3 = panelWidth, boxY3 = 0;
		Point2D[] quad;
		Polygon colourBoxPolygon;
		colourBoxes.clear();
		colourBoxPolygons.clear();


		for (int i = 0; i < 10; i++) {
			quad = new Point2D[4];
			colourBoxPolygon = new Polygon();

			quad[0] = new Point2D.Float(boxX0,boxY0);
			quad[1] = new Point2D.Float(boxX1,boxY1);
			quad[2] = new Point2D.Float(boxX2,boxY2);
			quad[3] = new Point2D.Float(boxX3,boxY3);
			colourBoxes.add(quad);

			colourBoxPolygon.addPoint((int)quad[0].getX(), (int) quad[0].getY());
			colourBoxPolygon.addPoint((int)quad[1].getX(), (int) quad[1].getY());
			colourBoxPolygon.addPoint((int)quad[2].getX(), (int) quad[2].getY());
			colourBoxPolygon.addPoint((int)quad[3].getX(), (int) quad[3].getY());
			colourBoxPolygons.add(colourBoxPolygon);

			boxX0 += panelWidth;
			boxX1 += panelWidth;
			boxY1 = panelHeight;
			boxX2 += panelWidth;
			boxY2 = panelHeight;
			boxX3 += panelWidth;
		}
		drawColourBoxes(gl);
	}

	public void drawColourBoxes(GL2 gl){
		for (int i = 0; i < colourBoxes.size(); i++) {
			gl.glBegin(GL2.GL_QUADS);
			gl.glColor3f(colours[i][0], colours[i][1], colours[i][2]);
			for (int j = 0; j < colourBoxes.get(i).length ; j++) {
				gl.glVertex2f((float) colourBoxes.get(i)[j].getX(), (float) colourBoxes.get(i)[j].getY());    
			}
			gl.glEnd();
		}
	}

	public void selectColorBox(GL2 gl, int index){

		Point2D[] quadCoordinates = colourBoxes.get(index);

		gl.glLineWidth(2.5f);
		gl.glBegin(GL2.GL_LINE_LOOP);
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		for (int i = 0; i < quadCoordinates.length; i++) {
			gl.glVertex2f((float) quadCoordinates[i].getX(), (float) quadCoordinates[i].getY());
		}				
		gl.glEnd();

		//make smooth outlines
		gl.glPointSize(2.5f);
		gl.glBegin(GL2.GL_POINTS);
		for (int i = 0; i < quadCoordinates.length; i++) {
			gl.glVertex2f((float) quadCoordinates[i].getX(), (float) quadCoordinates[i].getY());
		}				
		gl.glEnd();

	}

	public int colourPanelSelected(GL2 gl, int index) {
		int indexReturned = -1;

		for (int i = 0; i < mousePressForColourPanel.size(); i++) {
			indexReturned = isColourBoxSelected(mousePressForColourPanel.get(i));
			if (indexReturned != -1) {
				selectColorBox(gl, indexReturned);
				index = indexReturned;
			}else {
				selectColorBox(gl, index);
			}
			mousePressForColourPanel.remove(i);
		}
		return index;
	}

	public int isColourBoxSelected(Point2D point){

		int index = -1;

		for (int i = 0; i < colourBoxPolygons.size(); i++) {

			if (colourBoxPolygons.get(i).contains(point)) {
				index = i;
			}
		}
		return index;
	}	

	public int isTriangleSelected (){
		int index = -1;

		if (mousePressForTriangles.size() > 0) {

			outerloop:
				for (int i = 0; i < mousePressForTriangles.size(); i++) {
					for (int j = 0; j < trianglesPolygons.size(); j++) {

						if (trianglesPolygons.get(j).contains(mousePressForTriangles.get(i))) {
							index = j;
							mousePressForTriangles.clear();
							mouseMovedLocation.clear();
							break outerloop;
						}
					}
				}
		}

		return index;
	}

	public void processKeysForTranslation(float x, float y){
		if (selectedTriangle != -1) {
			triangleDraggedBy = new Point2D.Float(x,y);
			processTriangleDragged();
			triangleDraggedBy = null;
		}else {
			translateViewXBy += x;
			translateViewYBy += y;
		}
	}

	public void processKeysForScaling(float scaleBy){

		float[] matrixScaling = Arrays.copyOf(matrix,matrix.length);
		float[] matrixTranslation = Arrays.copyOf(matrix,matrix.length);
		float[] matrixResult = null;
		Point2D centre = null;
		Point2D newLocation = null;
		Point2D oldLocation = null;

		if (selectedTriangle != -1) {
			centre = triangleMidpoint(selectedTriangle);

			for (int i = 0; i < triangles.get(selectedTriangle).length; i++) {

				if (selectedTriangle != -1) {
					matrixTranslation[tX] = (float) (-1*centre.getX());
					matrixTranslation[tY] = (float) (-1*centre.getY());
					matrixScaling[rX0] = scaleBy;
					matrixScaling[rY1] = scaleBy;
					matrixResult = multiply(matrixScaling, matrixTranslation);
					matrixTranslation[tX] = (float) centre.getX();
					matrixTranslation[tY] = (float) centre.getY();
					matrixResult = multiply(matrixTranslation, matrixResult);
					oldLocation =  new Point2D.Float((float) (triangles.get(selectedTriangle)[i].getX()),(float) (triangles.get(selectedTriangle)[i].getY()));
					newLocation = transformVertice(matrixResult, new float[]{(float) oldLocation.getX(), (float) oldLocation.getY(), 0, 1});
					triangles.get(selectedTriangle)[i].setLocation(newLocation.getX(),newLocation.getY());
					drawTrianglePolygons();
				}
			}
		}
	}

	public void processKeysForRotation(float rotateBy){
		float[] matrixRotation = Arrays.copyOf(matrix,matrix.length);
		float[] matrixTranslation = Arrays.copyOf(matrix,matrix.length);
		float[] matrixResult = null;
		Point2D centre = null;
		Point2D newLocation = null;
		Point2D oldLocation = null;


		if (selectedTriangle != -1) {
			centre = triangleMidpoint(selectedTriangle);


			for (int i = 0; i < triangles.get(selectedTriangle).length; i++) {

				if (selectedTriangle != -1) {
					matrixTranslation[tX] = (float) (-1*centre.getX());
					matrixTranslation[tY] = (float) (-1*centre.getY());
					matrixRotation = rotate(matrixRotation, rotateBy);
					matrixResult = multiply(matrixRotation, matrixTranslation);
					matrixTranslation[tX] = (float) centre.getX();
					matrixTranslation[tY] = (float) centre.getY();
					matrixResult = multiply(matrixTranslation, matrixResult);
					oldLocation =  new Point2D.Float((float) (triangles.get(selectedTriangle)[i].getX()),(float) (triangles.get(selectedTriangle)[i].getY()));
					newLocation = transformVertice(matrixResult, new float[]{(float) oldLocation.getX(), (float) oldLocation.getY(), 0, 1});
					triangles.get(selectedTriangle)[i].setLocation(newLocation.getX(),newLocation.getY());
					drawTrianglePolygons();
				}
			}
		}
	}

	public void calculationForTriangleDraggedBy(int index, Point2D point){

		Point2D midPoint = null;
		Point2D newPoint = null;
		float distanceX = 0, distanceY =0;
		if (lastDragPos != null) {

			midPoint =  triangleMidpoint(index);			
			if (midPoint != null) {
				// calculate the change in mouse position
				distanceX = (float) (point.getX() - midPoint.getX());
				distanceY = (float) (point.getY() - midPoint.getY());
				newPoint = new Point2D.Float(distanceX,distanceY);
				triangleDraggedBy = newPoint;
			}
			lastDragPos = point;
		}
	}

	public Point2D  triangleMidpoint(int index){
		Point2D midPoint = null;
		float x = 0, y = 0;

		for (int i = 0; i < triangles.get(index).length ; i++) {
			if (selectedTriangle != -1 ) {
				x += triangles.get(selectedTriangle)[i].getX();
				y += triangles.get(selectedTriangle)[i].getY();
				midPoint = new Point2D.Float(x/3,y/3);
			}
		}
		return midPoint;
	}

	public Point2D transformVertice(float[] a, float[] b){

		Point2D result = null;
		float x;
		float y;

		x = ((a[rX0]*b[0])+(a[rY0]*b[1])+(a[tX]*b[3]));
		y = ((a[rX1]*b[0])+(a[rY1]*b[1])+(a[tY]*b[3]));

		result = new Point2D.Float(x,y);

		return result;

	}

	public float[] multiply(float[] a, float[] b) {
		float[] result = new float[16];

		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				for (int k = 0; k < 4; k++)
					result[i + 4 * j] += a[i + 4 * k] * b[4 * j + k];

		return result;
	}

	public float[] rotate(float[] matrixTransformation, float rotateby){

		float rotateX0;
		float rotateY0;
		float rotateX1;
		float rotateY1;

		rotateX0 = (float) Math.cos(rotateby);
		rotateY0 = ((float) Math.sin(rotateby)*-1);
		rotateX1 = (float) Math.sin(rotateby);
		rotateY1 = (float) Math.cos(rotateby);
		matrixTransformation[rX0] = rotateX0;
		matrixTransformation[rY0] = rotateY0;
		matrixTransformation[rX1] = rotateX1;
		matrixTransformation[rY1] = rotateY1;

		return matrixTransformation;
	}
	
	public float[] transformView(float x, float y, float scale) {
		float[] matrixViewTransformation = Arrays.copyOf(matrix,matrix.length);
		float[] matrixViewTransformationScaling = Arrays.copyOf(matrix,matrix.length);
		float[] matrixViewTransformationTranslation = Arrays.copyOf(matrix,matrix.length);


		matrixViewTransformationTranslation[tX] = x;
		matrixViewTransformationTranslation[tY] = y;
		matrixViewTransformationScaling[rX0] = scale;
		matrixViewTransformationScaling[rY1] = scale;
		matrixViewTransformation = multiply(matrixViewTransformationTranslation, matrixViewTransformationScaling);
		
		return matrixViewTransformation;
	}
}
