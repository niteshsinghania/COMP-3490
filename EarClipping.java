import java.awt.Frame;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;

public class EarClipping implements GLEventListener {
	public static final boolean TRACE = true;

	public static final String WINDOW_TITLE = "A1Q3: [.Nitesh]"; 
	public static final int INITIAL_WIDTH = 640;
	public static final int INITIAL_HEIGHT = 640;

	public static int NPOLYS = 53;
	public static long RSEED = 123456789;

	public static void main(String[] args) {
		final Frame frame = new Frame(WINDOW_TITLE);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
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
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		canvas.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		canvas.setAutoSwapBufferMode(true);

		frame.add(canvas);
		frame.pack();
		frame.setVisible(true);

		System.out.println("\nEnd of processing.");
	}

	private static Class<?> self() {
		// This ugly hack gives us the containing class of a static method 
		return new Object() { }.getClass().getEnclosingClass();
	}

	private ArrayList<ArrayList<float[]>> polys;
	private ArrayList<ArrayList<float[]>> triangles;
	private float[][] colours;

	public void setup(final GLCanvas canvas) {
		// Called for one-time setup
		if (TRACE)
			System.out.println("-> executing setup()");

		final int MIN_SIDE = 64;
		final int MAX_SIDE = 128;
		final int MAX_POLY = 20;
		Random gen = new Random(RSEED);
		polys = new ArrayList<ArrayList<float[]>>();
		for (int i = 0; i < NPOLYS; i++) {
			ArrayList<float[]> poly = new ArrayList<float[]>();

			float[] prev = new float[] { gen.nextInt(INITIAL_WIDTH), gen.nextInt(INITIAL_HEIGHT) };
			poly.add(prev);

			// No, don't do this.
			boolean close = false;
			int tries = 0;
			while (true) {
				float[] pt = new float[2];
				double dx, dy, len;
				boolean giveup = false;

				if (poly.size() >= MAX_POLY) {
					giveup = true;
				}

				if (tries > 30)
					giveup = true;

				if (poly.size() <= 2)
					close = false;

				if (close) {
					pt[0] = poly.get(0)[0];
					pt[1] = poly.get(0)[1];
					dx = prev[0] - pt[0];
					dy = prev[1] - pt[1];
					close = dx * dx + dy * dy < MAX_SIDE * MAX_SIDE;
				}

				if (!close) {
					dx = gen.nextDouble() - 0.5;
					dy = gen.nextDouble() - 0.5;
					len = Math.sqrt(dx * dx + dy * dy);
					len = (gen.nextInt(MAX_SIDE - MIN_SIDE) + MIN_SIDE) / len; // breaks if len is 0!
					dx *= len;
					dy *= len;

					pt[0] = prev[0] + (int)dx;
					pt[1] = prev[1] + (int)dy;
				}

				if (poly.size() == 1) {
					for (ArrayList<float[]> p: polys) {
						if (intersect(p, p.size(), prev, pt)) {
							giveup = true;
						}
					}
				}

				if (giveup) {
					prev = new float[] { gen.nextInt(INITIAL_WIDTH), gen.nextInt(INITIAL_HEIGHT) };
					poly.clear();
					poly.add(prev);
					continue;
				}

				if (pt[0] >= 0 && pt[0] < INITIAL_WIDTH && pt[1] >= 0 && pt[1] < INITIAL_HEIGHT && !intersect(poly, poly.size() - 1, prev, pt)) {

					tries = 0;
					if (close) {
						break;
					}
					close = true;
					poly.add(pt);

					prev = pt;
				} else {
					close = false;
				}
			}

			String s = "poly "; for (int j = 0; j < poly.size(); j++) s+= "(" + poly.get(j)[0] + "," + poly.get(j)[1] + ") "; System.out.println(s);

			polys.add(poly);
		}

		colours = new float[NPOLYS][];
		for (int i = 0; i < NPOLYS; i++) {
			colours[i] = new float[] {gen.nextFloat(), gen.nextFloat(), gen.nextFloat()};
		}
	}

	public boolean intersect(ArrayList<float[]> edges, int num, float[] a, float[] b) {
		for (int i = 0; i < num - 1; i++) {
			float v1x = edges.get(i)[0];
			float v1y = edges.get(i)[1];
			float v2x = edges.get(i+1)[0];
			float v2y = edges.get(i+1)[1];
			float v3x = a[0];
			float v3y = a[1];
			float v4x = b[0];
			float v4y = b[1];

			float num1 = (v4x - v3x) * (v1y - v3y) - (v4y - v3y) * (v1x - v3x);
			float num2 = (v2x - v1x) * (v1y - v3y) - (v2y - v1y) * (v1x - v3x);
			float den = (v4y - v3y) * (v2x - v1x) - (v4x - v3x) * (v2y - v1y);

			if (den != 0.0f) {
				float ta, tb;
				ta = num1 / den;
				tb = num2 / den;
				if (ta > 0.0f && ta <= 1.0f && tb >= 0.0f && tb <= 1.0f) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// Called when the canvas is (re-)created - use it for initial GL setup
		if (TRACE)
			System.out.println("-> executing init()");

		final GL2 gl = drawable.getGL().getGL2();

		gl.glClearColor(0.7f, 0.7f, 0.7f, 0.0f);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		// Draws the display
		if (TRACE)
			System.out.println("-> executing display()");

		final GL2 gl = drawable.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		seperateTrianglesFromPolys();
		drawTriangles(gl);
		drawOutlines(gl);

	}

	public void drawTriangles(GL2 gl) {

		for (int i = 0; i < triangles.size(); i++) {

			for (int j = 0; j < (triangles.get(i).size() - 1) ; j++) {

				gl.glColor3f(0, 0, 0);
				gl.glBegin(GL2.GL_LINE_LOOP);
				gl.glVertex2f( triangles.get(i).get(j)[0], triangles.get(i).get(j)[1]);

			}

			gl.glEnd();
			scanLine(gl, triangles.get(i));
		}	
	}
	public void drawOutlines(GL2 gl) {

		float next = 1;
		float curr = 0;
		ArrayList<float[]> poly;

		try {
			for (int i = 0; i < triangles.size(); i++) {
				if (i+1 < triangles.size()-1) {
					curr = triangles.get(i).get(3)[0];
					next = triangles.get(i+1).get(3)[0];

				}


				if(curr == next && curr < triangles.size()){
					poly = new ArrayList<float[]>(polys.get((int)triangles.get(i+1).get(3)[0]));

					for (int j = 0; j < poly.size(); j++) {
						gl.glColor3f(0, 0, 0);
						gl.glBegin(GL2.GL_LINE_LOOP);
						gl.glVertex2f( poly.get(j)[0], poly.get(j)[1]);

					}
					gl.glEnd();
					gl.glColor3f(1, 1, 1);
					gl.glBegin(GL2.GL_LINES);
					gl.glVertex2f( triangles.get(i).get(0)[0], triangles.get(i).get(0)[1]);
					gl.glVertex2f( triangles.get(i).get(2)[0], triangles.get(i).get(2)[1]);
					gl.glEnd();
				}		
			}
		} catch (Exception e) {
		}
	


	}
	public void scanLine (GL2 gl, ArrayList<float[]> triangle){

		float maxX = triangle.get(0)[0]; 
		float minX = triangle.get(0)[0]; 
		float maxY = triangle.get(0)[1];
		float minY = triangle.get(0)[1];
		boolean isInside = false;
		float[] point;

		//calculate min and max 
		for (int i = 1; i < (triangle.size()-1); i++) {

			if (maxX < triangle.get(i)[0]) {
				maxX = triangle.get(i)[0];
			}

			if (minX > triangle.get(i)[0]) {
				minX = triangle.get(i)[0];
			}

			if (maxY < triangle.get(i)[1]) {
				maxY = triangle.get(i)[1];
			}

			if (minY > triangle.get(i)[1]) {
				minY = triangle.get(i)[1];
			}
		}

		//System.out.println("maxX = "+maxX+" minX = "+minX+ " maxY = "+maxY+" minY = "+minY);
		for (float y = maxY; y > minY; y--) {
			for (float x = minX; x < maxX; x++) {
				point = new float[] {x,y};
				isInside = pointIsInsideTriangle(triangle, point);

				if (isInside) {

					gl.glBegin(GL2.GL_POINTS);
					gl.glColor4f(colours[(int)triangle.get(triangle.size()-1)[0]][0], colours[(int) triangle.get(triangle.size()-1)[0]][1], colours[(int)triangle.get(triangle.size()-1)[0]][2], 0.6f);
					if ((x+y) % 2 == 0) {
						gl.glVertex2f(x, y);
					}
					gl.glEnd();	

				}
			}
		}

	}

	public boolean pointIsInsideTriangle (ArrayList<float[]> triangle, float[] point){

		boolean isInside = false;
		boolean isTriangle = true;
		boolean isClockwise = determineWinding (triangle,isTriangle);
		float crossProduct;
		float[] edge;
		float[] vector;
		int cpCount = 0;


		for (int i = 0; i < 3; i++) {

			//ugly calculation to determine magnitude of the vectors
			if (i == 2) {

				edge = new float[] {triangle.get(i)[0] - triangle.get(0)[0], triangle.get(i)[1] - triangle.get(0)[1]};
				vector = new float[] {triangle.get(i)[0] - point[0] , triangle.get(i)[1] - point[1]};

			}else {

				edge = new float[] {triangle.get(i)[0] - triangle.get(i+1)[0], triangle.get(i)[1] - triangle.get(i+1)[1]};
				vector = new float[] {triangle.get(i)[0] - point[0] , triangle.get(i)[1] - point[1]};

			}
			crossProduct = ((edge[0]*vector[1]) - (edge[1]*vector[0]));

			if (isClockwise && crossProduct < 0) {

				cpCount++;

			}else if (!isClockwise && crossProduct > 0) {

				cpCount++;
			}

		}

		if (cpCount == 3)  {
			isInside = true;
		}
		return isInside;

	}

	public boolean determineWinding (ArrayList<float[]> triangle, boolean isTriangle){

		boolean isClockwise = false;
		float area = 0;
		float xDiff = 0;
		float yDiff = 0;


		if (isTriangle) {
			for( int i = 0; i <=  2; i++ ) {

				//if its the last vertice than reference back to the first one
				if (i == 2) {
					xDiff = triangle.get(0)[0] - triangle.get(i)[0];
					yDiff = triangle.get(0)[1] - triangle.get(i)[1];
					area = area + triangle.get(i)[0] * yDiff - triangle.get(i)[1] * xDiff;
				}else {
					xDiff = triangle.get(i+1)[0] - triangle.get(i)[0];
					yDiff = triangle.get(i+1)[1] - triangle.get(i)[1];
					area = area + triangle.get(i)[0] * yDiff - triangle.get(i)[1] * xDiff;
				}
			}
		}else {
			for( int i = 0; i <  triangle.size(); i++ ) {

				//if its the last vertice than reference back to the first one
				if (i+1 == triangle.size()) {
					xDiff = triangle.get(0)[0] - triangle.get(i)[0];
					yDiff = triangle.get(0)[1] - triangle.get(i)[1];
					area = area + triangle.get(i)[0] * yDiff - triangle.get(i)[1] * xDiff;
				}else {
					xDiff = triangle.get(i+1)[0] - triangle.get(i)[0];
					yDiff = triangle.get(i+1)[1] - triangle.get(i)[1];
					area = area + triangle.get(i)[0] * yDiff - triangle.get(i)[1] * xDiff;
				}
			}
		}





		area = (float) (0.5 * area);

		if (area < 0 ) {
			isClockwise = true;
		}
		return isClockwise;
	}

	public void seperateTrianglesFromPolys(){

		triangles = new ArrayList<ArrayList<float[]>>();
		float[] colorIndex;
		ArrayList<float[]> currPoly;
		boolean isClockwise = false;
		boolean isTriangle = false;


		for (int i = 0; i < polys.size(); i++) {

			if (polys.get(i).size() == 3) {

				//in order to keep track of the color index for parallel reference
				colorIndex= new float[] {i};
				currPoly = new ArrayList<float[]>(polys.get(i));	
				currPoly.add(colorIndex);
				triangles.add(currPoly);
			}else{

				currPoly = new ArrayList<float[]>(polys.get(i));
				isClockwise =  determineWinding (currPoly,isTriangle);
				colorIndex= new float[] {i};
				earClipping (currPoly,colorIndex,isClockwise);
			}
		}
	} 

	public void earClipping (ArrayList<float[]> poly, float[] colorIndex, boolean isClockwise){

		ArrayList<float[]> triangle;
		ArrayList<float[]> pointsForTest;
		float[] point;
		int prev = 0 ;
		int next = 0;
		boolean isInside = false;
		boolean toClip = false;

		for (int i = 1; i < poly.size(); i++) {

			triangle = new ArrayList<float[]> ();
			pointsForTest = new ArrayList<float[]> ();
			prev = i-1;
			next = i+1;
			if ((i + 1) >  (poly.size()-1) ) {
				next = 0;
			}
			point = new float[] {poly.get(prev)[0],poly.get(prev)[1]};
			triangle.add(point);
			point = new float[] {poly.get(i)[0],poly.get(i)[1]};
			triangle.add(point);
			point = new float[] {poly.get(next)[0],poly.get(next)[1]};
			triangle.add(point);

			if (poly.size() == 3) {
				triangle.add(colorIndex);
				triangles.add(triangle);
				break;
			}else {

				for (int j = 0; j < poly.size(); j++) {
					if(triangle.get(0)[0] != poly.get(j)[0] && triangle.get(1)[0] != poly.get(j)[0] && triangle.get(2)[0] != poly.get(j)[0]){
						pointsForTest.add(poly.get(j));
					}
				}

				for (int j = 0; j < pointsForTest.size(); j++) {

					//perform point inside test for polygon
					isInside = pointIsInsideTriangle(triangle, pointsForTest.get(j));
					if (isInside) {
						break;
					}
				}

				//all points are outside triangle so now perform angle test
				if (!isInside) {

					toClip = cpSignTest(triangle, isClockwise);
					if (toClip) {

						for (int j = 0; j <poly.size(); j++) {
							if (poly.get(j)[0] == triangle.get(1)[0] && poly.get(j)[1] == triangle.get(1)[1]) {
								poly.remove(j);
								break;
							}
						}
						triangle.add(colorIndex);
						triangles.add(triangle);
						i = 0;
					}
				}
			}

		}
	}

	public boolean cpSignTest(ArrayList<float[]> triangle, boolean isClockwise){


		boolean toClip = false;
		float crossProduct = 0;
		float [] edge1;
		float [] edge2;


		edge1 = new float[] {triangle.get(0)[0] - triangle.get(1)[0],triangle.get(0)[1] - triangle.get(1)[1]};
		edge2 = new float[] {triangle.get(1)[0] - triangle.get(2)[0],triangle.get(1)[1] - triangle.get(2)[1]};


		crossProduct = ((edge1[0]*edge2[1]) - (edge1[1]*edge2[0]));

		if (isClockwise && crossProduct < 0) {
			toClip = true;
		}else if(!isClockwise && crossProduct > 0){
			toClip = true;
		}
		return toClip;
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

		gl.glViewport(x, y, width, height);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(0, width, 0, height, 0.0f, 1.0f);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}
}
