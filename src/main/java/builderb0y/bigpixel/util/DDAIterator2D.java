package builderb0y.bigpixel.util;

public class DDAIterator2D {

	public double rawX, rawY;
	public double dirX, dirY;
	public double endX, endY;
	public int x, y;

	public DDAIterator2D() {}

	public DDAIterator2D(double startX, double startY, double endX, double endY) {
		this.init(startX, startY, endX, endY);
	}

	public void init(double startX, double startY, double endX, double endY) {
		this.rawX = startX;
		this.rawY = startY;
		this.endX = endX;
		this.endY = endY;
		double dirX = endX - startX;
		double dirY = endY - startY;
		double dirMagnitude = Math.sqrt(dirX * dirX + dirY * dirY);
		this.dirX = dirX / dirMagnitude;
		this.dirY = dirY / dirMagnitude;
		this.x = (int)(Math.floor(startX));
		this.y = (int)(Math.floor(startY));
	}

	public boolean next() {
		double
			relativeX = this.rawX - this.x,
			relativeY = this.rawY - this.y,
			distX = ((this.dirX > 0.0D ? 1.0D : 0.0D) - relativeX) / this.dirX,
			distY = ((this.dirY > 0.0D ? 1.0D : 0.0D) - relativeY) / this.dirY;
		if (distX < distY) {
			this.x += (int)(Math.copySign(1.0D, this.dirX));
			this.rawX += this.dirX * distX;
			this.rawY += this.dirY * distX;
		}
		else {
			this.y += (int)(Math.copySign(1.0D, this.dirY));
			this.rawX += this.dirX * distY;
			this.rawY += this.dirY * distY;
		}
		return (this.endX - this.rawX) * this.dirX + (this.endY - this.rawY) * this.dirY >= 0.0D;
	}
}