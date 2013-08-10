package hostconfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 油罐
 * @author lenovo
 *
 */
public class Can {
	private int id;
	private List<Gun> guns;
	
	public Can(int id) {
		super();
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Gun> getGuns() {
		return guns;
	}

	public void setGuns(List<Gun> guns) {
		if(guns==null){
			this.guns = new ArrayList<Gun>();
			return;
		}
		this.guns = guns;
		for(int i=0;i<guns.size();i++){
			guns.get(i).setCan(this);
		}
	}
}
