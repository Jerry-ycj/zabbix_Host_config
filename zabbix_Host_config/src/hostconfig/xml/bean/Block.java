package hostconfig.xml.bean;

import java.util.ArrayList;
import java.util.List;

public class Block {
	private String[] groups = new String[3];
	private List<Station> stations;
	private String name;
	private City city;
	
	public void setGroups(String g){
		int i=0;
		for(;i<groups.length-1;i++){
			groups[i] = city.getGroups()[i];
		}
		groups[i] = g;
	}
	public City getCity() {
		return city;
	}
	public void setCity(City city) {
		this.city = city;
		city.setBlock(this);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String[] getGroups() {
		return groups;
	}
	public List<Station> getStations() {
		return stations;
	}
	public void setStation(Station station) {
		if(stations == null){
			stations = new ArrayList<Station>();
		}
		stations.add(station);
	}
	
}
