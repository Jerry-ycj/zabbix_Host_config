package hostconfig;

/**
 * 油枪
 * @author lenovo
 *
 */
public class Gun {
	private int id;
	private Can can;
	public Gun(int id) {
		super();
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Can getCan() {
		return can;
	}

	public void setCan(Can can) {
		this.can = can;
	}
	
}
