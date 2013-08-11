package hostconfig.conf;

import hostconfig.Can;
import hostconfig.Gun;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Configuration {
	private String host_name;
	private String ip;
	private List<Can> cans;
	
	public Configuration(){}
	
	public void load() throws Exception{
		// src下
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("hostconfig/conf/host.properties");
		Properties pro = new Properties();
		pro.load(in);
		host_name = pro.getProperty("host_name");
		ip = pro.getProperty("ip");
		String can_gun = pro.getProperty("can_gun");
		cans = parseCan_Gun(can_gun);
	}

	private List<Can> parseCan_Gun(String can_gun) {
		List<Can> cans = new ArrayList<Can>();
		// 1:2,3,8,13/3:1,4,5,6,11,15/4:9,12,14
		String cgs[] = can_gun.split("/");
		for(int i=0;i<cgs.length;i++){
			String cg[] = cgs[i].split(":");
			Can c = new Can(Integer.parseInt(cg[0]));
			List<Gun> gunList = new ArrayList<Gun>();
			String gs[] = cg[1].split(",");
			for(int j=0;j<gs.length;j++){
				Gun g = new Gun(Integer.parseInt(gs[j]));
				gunList.add(g);
			}
			c.setGuns(gunList);
			cans.add(c);
		}
		return cans;
	}

	public String getHost_name() {
		return host_name;
	}

	public void setHost_name(String host_name) {
		this.host_name = host_name;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public List<Can> getCans() {
		return cans;
	}

	public void setCans(List<Can> cans) {
		this.cans = cans;
	}
	
}
