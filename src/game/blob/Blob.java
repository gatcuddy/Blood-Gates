package game.blob;

import cnge.core.CCD;
import cnge.core.CNGE;
import cnge.core.Loop;
import cnge.graphics.Transform;
import game.Map;
import game.GameAssets;

import static org.lwjgl.glfw.GLFW.*;

abstract public class Blob {

	public static class ColPackage {
		public CCD.Collision bestCollision;
		public Map.Line bestWall;
		public double wallAngle;
		public double bestT;

		public ColPackage() {
			bestCollision = null;
			bestWall = null;
			wallAngle = 0;
			bestT = 2;
		}
	}

	public static float FRICTION = 8f;

	//core components of blobs
	public float x, y;
	public float radius;

	public Transform transform;
	public float speed;
	protected float angle;

	public Blob(float o, float a, float r) {
		x = o;
		y = a;
		radius = r;
		transform = new Transform();
	}

	public boolean blobCollision(Blob blob2) {
		CCD.Vector v = new CCD.Vector(blob2.x - x, blob2.y - y);
		double dist = v.length();
		return dist < (radius + blob2.radius);
	}

	public float calculateArea() {
		return radius * radius * CNGE.PI;
	}

	public float radiusToAdd(float area) {
		return (float)((-2 * Math.PI * radius + Math.sqrt(4*Math.PI*Math.PI*radius*radius + 4*Math.PI*area)) / (2 * Math.PI));
	}

	public void cameraOnThis() {

		double constant = CNGE.gameWidth / 6;

		double zoom = CNGE.gameWidth / (constant * radius);

		Transform ct = CNGE.camera.getTransform();

		ct.setScale((float)zoom, (float)zoom);
		ct.setCenter(x, y);
	}

	protected ColPackage movement(Map map, float dx, float dy) {
		CCD.Line move = new CCD.Line(x, y, x + dx, y + dy);

		ColPackage colPackage = new ColPackage();

		int[] moveBounds = map.getBoundsUnsorted(x, y, x + dx, y + dy);

		for (int i = moveBounds[0]; i <= moveBounds[1]; ++i) {
			for (int j = moveBounds[2]; j <= moveBounds[3]; ++j) {
				if (map.zoneInRange(i, j)) {
					Map.Line[] lines = map.getLineZones()[i][j];
					int numLines = lines.length;
					for (int k = 0; k < numLines; ++k) {
						Map.Line l = lines[k];
						CCD.Line line = l.line;

						CCD.Vector wv = new CCD.Vector(line);
						double tempWallAngle = wv.getAngle();

						CCD.Vector off = new CCD.Vector(radius, 0);
						if (CCD.normalSide(l.line, x, y)) {
							off.rotate(tempWallAngle - CNGE.PI / 2);
						} else {
							off.rotate(tempWallAngle + CNGE.PI / 2);
						}

						CCD.Line colLine = new CCD.Line(line.x0 + off.x, line.y0 + off.y, line.x1 + off.x, line.y1 + off.y);

						boolean colFound = false;
						double effectiveT = 2;

						double circleStartT = CCD.circleCollision(move, line.x0, line.y0, radius);
						if (CCD.inline(circleStartT) && circleStartT < effectiveT) {
							effectiveT = circleStartT;
							colFound = true;
						}

						double circleEndT = CCD.circleCollision(move, line.x1, line.y1, radius);
						if (CCD.inline(circleEndT) && circleEndT < effectiveT) {
							effectiveT = circleEndT;
							colFound = true;
						}

						CCD.Collision col = CCD.result(move, colLine);
						if (col.collision() && col.t_ < effectiveT) {
							effectiveT = col.t_;
							colFound = true;
						}

						if (colFound && effectiveT < colPackage.bestT) {
							colPackage.bestCollision = col;
							colPackage.wallAngle = tempWallAngle;
							colPackage.bestWall = l;
							colPackage.bestT = col.t_;
						}
					}
				}
			}
		}

		if (colPackage.bestCollision != null) {
			double colX = CCD.xAlong(colPackage.bestT - 0.01f, move);
			double colY = CCD.yAlong(colPackage.bestT - 0.01f, move);

			dx = (float) (colX - x);
			dy = (float) (colY - y);

			angle = (float) (2 * (colPackage.wallAngle) - angle);
			speed *= 0.75f;
		}

		x += dx;
		y += dy;

		return colPackage;
	}

	abstract public void update(Map map);

	abstract public void render();

}