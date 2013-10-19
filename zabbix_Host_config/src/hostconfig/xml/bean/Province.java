package hostconfig.xml.bean;

import java.util.ArrayList;
import java.util.List;

public class Province {
	private String group;
	private List<City> citys;
	private String name;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public List<City> getCitys() {
		return citys;
	}
	public void setCity(City c){
		if(citys==null){
			citys = new ArrayList<City>();
		}
		citys.add(c);
	}
}
