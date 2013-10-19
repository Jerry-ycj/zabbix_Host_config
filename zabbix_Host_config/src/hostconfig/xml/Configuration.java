package hostconfig.xml;

import hostconfig.Can;
import hostconfig.Gun;
import hostconfig.xml.bean.Block;
import hostconfig.xml.bean.City;
import hostconfig.xml.bean.DBconf;
import hostconfig.xml.bean.Province;
import hostconfig.xml.bean.Station;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

/**
 * 解析xml配置文件
 * @author ycj
 *
 */
public class Configuration {
	private String filePath;
	private DBconf dbc; 
	private Province p;
	
	public Configuration(){}
	
	public void load(String filename) throws Exception{
		// src下
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);
		// 因为流默认读取iso-8859-1，可以用reader
		InputStreamReader in_utf8= new InputStreamReader(in, "utf-8");
		// 解析xml文件
		SAXReader reader = new SAXReader();
		Document doc = reader.read(in_utf8);
		Element root = doc.getRootElement();
		
		Element fp = root.element("file_path");
		filePath = fp.getTextTrim();
		Element db = root.element("db");
		dbc = new DBconf();
		dbc.setGroup(db.element("group").getTextTrim());
		dbc.setIp(db.element("ip").getTextTrim());
		dbc.setName(db.attributeValue("name"));
		Element pro = root.element("province");
		p = new Province();
		p.setGroup(pro.elementTextTrim("group"));
		p.setName(pro.attributeValue("name"));	
		parse_citys(pro);
//		System.out.println(p.getCitys().get(0).getBlocks().size());
	}

	private void parse_citys(Element pro) {
		for(Iterator iter = pro.elementIterator("city");iter.hasNext();){
			Element city = (Element)iter.next();
			City c = new City();
			c.setProvince(p);
			c.setGroups(city.elementTextTrim("group"));
			c.setName(city.attributeValue("name"));
			parse_blocks(city,c);
		}
		
	}

	private void parse_blocks(Element city,City c) {
		for(Iterator iter = city.elementIterator("block");iter.hasNext();){
			Element block = (Element)iter.next();
			Block b = new Block();
			b.setCity(c);
			b.setGroups(block.elementTextTrim("group"));
			b.setName(block.attributeValue("name"));
			parse_stations(block,b);
		}
		
	}

	private void parse_stations(Element block, Block b) {
		for(Iterator iter = block.elementIterator("station");iter.hasNext();){
			Element station = (Element)iter.next();
			Station s = new Station();
			s.setBlock(b);
			s.setGroups(station.elementTextTrim("group"));
			s.setName(station.attributeValue("name"));
			s.setIp(station.elementTextTrim("ip"));
			s.setName_cn(station.elementTextTrim("name_cn"));
			s.setCans(parseCan_Gun(station.elementTextTrim("can_gun")));
			parseCan_sort(s,station.elementTextTrim("can_sort"));

//			for(int i=0;i<s.getGroups().length;i++){
//				System.out.println(s.getGroups()[i]);
//			}
//			for(int i=0;i<s.getCans().size();i++){
//				System.out.println(s.getCans().get(i).getId());
//			}
//			System.out.println("------------------");
		}
		
	}

	private void parseCan_sort(Station s, String str) {
		// 1:97/3:93/4:0
		List<String> can97 = new ArrayList<String>();
		List<String> can93 = new ArrayList<String>();
		List<String> can0 = new ArrayList<String>();
		Map<String, List<String>> all = new HashMap<String, List<String>>();
		all.put("97", can97);
		all.put("93", can93);
		all.put("0", can0);
		String[] tmp = str.split("/");
		for(int i=0;i<tmp.length;i++){
			String[] tmp2 = tmp[i].split(":");
			all.get(tmp2[1]).add(tmp2[0]);
		}
		s.setSort_can(all);
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

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public DBconf getDbc() {
		return dbc;
	}

	public void setDbc(DBconf dbc) {
		this.dbc = dbc;
	}

	public Province getP() {
		return p;
	}

	public void setP(Province p) {
		this.p = p;
	}

	public static void main(String[] args) throws Exception {
		new Configuration().load("hostconfig/xml/hebei.xml");
	}
	
}
