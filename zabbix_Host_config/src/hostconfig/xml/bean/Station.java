package hostconfig.xml.bean;

import hostconfig.Can;
import java.util.List;
import java.util.Map;

public class Station {
	private String ip;
	private String[] groups = new String[4];
	private String name_cn;
	private List<Can> cans;
	private String name;
	private Block block;
	private Map<String, List<String>> sort_can;
	
	public void setGroups(String g) {
		int i=0;
		for(;i<groups.length-1;i++){
			groups[i] = block.getGroups()[i];
		}
		groups[i] = g;
	}
	public Block getBlock() {
		return block;
	}
	public void setBlock(Block block) {
		this.block = block;
		block.setStation(this);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String[] getGroups() {
		return groups;
	}
	
	public String getName_cn() {
		return name_cn;
	}
	public void setName_cn(String name_cn) {
		this.name_cn = name_cn;
	}
	public List<Can> getCans() {
		return cans;
	}
	public void setCans(List<Can> cans) {
		this.cans = cans;
	}
	public Map<String, List<String>> getSort_can() {
		return sort_can;
	}
	public void setSort_can(Map<String, List<String>> sort_can) {
		this.sort_can = sort_can;
	}
	
}
