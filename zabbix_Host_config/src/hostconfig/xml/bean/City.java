package hostconfig.xml.bean;

import java.util.ArrayList;
import java.util.List;

public class City {
	private String[] groups = new String[2];
	private List<Block> blocks;
	private String name;
	private Province province;
	
	public void setGroups(String g){
		groups[0] = province.getGroup();
		groups[1] = g;
	}
	public Province getProvince() {
		return province;
	}
	public void setProvince(Province province) {
		this.province = province;
		province.setCity(this);
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
	public List<Block> getBlocks() {
		return blocks;
	}
	public void setBlock(Block block) {
		if(blocks == null){
			blocks = new ArrayList<Block>();
		}
		blocks.add(block);
	}

}
