package hostconfig;

import hostconfig.xml.Configuration;
import hostconfig.xml.bean.Block;
import hostconfig.xml.bean.City;
import hostconfig.xml.bean.Province;
import hostconfig.xml.bean.Station;

import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class CreateHostConf {
	private Configuration conf;
	private static String COLOR[]={"0000DD","00DD00","DD0000","00DDDD",
		"DD00DD","DDDD00","777777","550000","550055"};
	
	public CreateHostConf(Configuration conf){
		this.conf = conf;
	}
	
	public Document getDocument() {
		Element host;
		Element items;
		Element triggers;
		Element graphs;
		Element macros;
		// 生成一个节点
		Document document = DocumentHelper.createDocument();
		// root
		Element root = document.addElement("zabbix_export");
		root.addAttribute("version", "1.0");
		root.addAttribute("date", "19.10.13");
		root.addAttribute("time", "15.55");
		// 2
		Element hosts = root.addElement("hosts");
		root.addElement("dependencies");
		// 3 host
		//   root_on_db
		host = hosts.addElement("host");
		host.addAttribute("name", conf.getDbc().getName());
		addHostEles(host);
		host.element("ip").setText(conf.getDbc().getIp());
		host.element("groups").addElement("group").addText(conf.getDbc().getGroup());
		//   province
		host = hosts.addElement("host");
		host.addAttribute("name", conf.getP().getName());
		addHostEles(host);
		host.element("ip").setText("127.0.0.1");
		host.element("groups").addElement("group").addText(conf.getP().getGroup());
		items = host.element("items");
		createOtherItem(93,items,conf.getP());
		createOtherItem(97,items,conf.getP());
		createOtherItem(0,items,conf.getP());
		//  city
		for(City c:conf.getP().getCitys()){
			host = hosts.addElement("host");
			host.addAttribute("name", c.getName());
			addHostEles(host);
			host.element("ip").setText("127.0.0.1");
			for(int i=0;i<c.getGroups().length;i++){
				host.element("groups").addElement("group").addText(c.getGroups()[i]);
			}
			items = host.element("items");
			createOtherItem(93,items,c);
			createOtherItem(97,items,c);
			createOtherItem(0,items,c);			
			//  block
			for(Block b:c.getBlocks()){
				host = hosts.addElement("host");
				host.addAttribute("name", b.getName());
				addHostEles(host);
				host.element("ip").setText("127.0.0.1");
				for(int i=0;i<b.getGroups().length;i++){
					host.element("groups").addElement("group").addText(b.getGroups()[i]);
				}
				items = host.element("items");
				createOtherItem(93,items,b);
				createOtherItem(97,items,b);
				createOtherItem(0,items,b);

				// station
				for(Station s:b.getStations()){
					host = hosts.addElement("host");
					host.addAttribute("name", s.getName());
					addHostEles(host);
					host.element("ip").setText(s.getIp());
					for(int i=0;i<s.getGroups().length;i++){
						host.element("groups").addElement("group").addText(s.getGroups()[i]);
					}
					items = host.element("items");
					triggers = host.element("triggers");
					graphs = host.element("graphs");
					macros = host.element("macros");
					// item can 1can/5items;trigger
					for(Can can:s.getCans()){
						createCanItem("volume",items,can,s);
						createCanItem("height", items, can,s);
						createCanItem("water", items, can,s);
						createCanItem("watervolume", items, can,s);
						createCanItem("temp", items, can,s);
						// item sale_profit_loss
						createProfitLossItem(items,can,s);
						// item volumespeed
						createVolumeSpeedItem(items,can,s);		
						// 0-超高，1-超低，不同级别告警1,2,4,5
						createCanHightTrigger(0,2,triggers,can,s);
						createCanHightTrigger(1,4,triggers,can,s);
						// 损溢告警
						createPLTrigger(triggers,can,s);
						// 卸油告警
						createUnloadTrigger(triggers,can,s);
						// 卸油进行中
						createUnloadIngTrigger(triggers,can,s);
						//卸油时付油警告
						createUnloadSaleTrigger(triggers,can,s);
						// item gun
						for(Gun g:can.getGuns()){
							createGunItem("volume",items,g,s);
							createGunItem("pump", items, g,s);
							createTransactionItem(items,g,s);
							createPumpSpeedItem(items,g,s);
						}
					}
					// cardno 在 items中
					createCardnoItem(items);
					// 汽油或柴油库存
					createVolumeOfSort(97,items,s);
					createVolumeOfSort(93,items,s);
					createVolumeOfSort(0,items,s);
					// graphs
					createPLGraph(graphs,s); 	// 损溢曲线-总
					createVolumeGraph(graphs,s);	// 油罐库存_总
					createTmpGraph(graphs,s);	// 油温曲线_总
					createPumpSpeedGraph(graphs,s);	//出油速度
					// macro
					createMacros(macros);
				}
			}
		}
		return document;
	}

	/**
	 * 站的上级 统计用的items
	 * obj = 省、市、区
	 * @param items
	 */
	private void createOtherItem(int n, Element items,Object obj) {
		Element item = items.addElement("item");
		item.addAttribute("type", "15");
		String key;
		String desc;
		String params = "";
		if(n==93){
			key = "volume[93]";
			desc = "93号汽油库存量";
		}else if(n==97){
			key = "volume[97]";
			desc = "97号汽油库存量";
		}else{
			key = "volume[0]";
			desc = "0号柴油库存量";
		}
		if(obj instanceof Province){
			Province p = (Province)obj;
			for(City c:p.getCitys()){
				params = params+"last("+c.getName()+":volume["+n+"])+";
			}
			params = params.substring(0, params.length()-1);
		}else if(obj instanceof City){
			City c = (City)obj;
			for(Block b:c.getBlocks()){
				params = params+"last("+b.getName()+":volume["+n+"])+";
			}
			params = params.substring(0, params.length()-1);
		}else{
			Block b = (Block)obj;
			for(Station s:b.getStations()){
				params = params+"last("+s.getName()+":volume["+n+"])+";
			}
			params = params.substring(0, params.length()-1);
		}
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");	// 数据类型是float
		item.addElement("description").addText(desc);
		addItemOtherEles(item);
		item.element("params").setText(params);
	}

	private void addHostEles(Element host) {
		host.addElement("proxy_hostid").addText("0");
		host.addElement("useip").addText("1");
		host.addElement("dns");
		host.addElement("ip");
		host.addElement("port").addText("10050");
		host.addElement("status").addText("0");	// 0-开启
		host.addElement("useipmi").addText("0");
		host.addElement("ipmi_ip");
		host.addElement("ipmi_port").addText("623");
		host.addElement("ipmi_authtype").addText("-1");
		host.addElement("ipmi_privilege").addText("2");
		host.addElement("ipmi_username");
		host.addElement("ipmi_password");
		host.addElement("groups");
		host.addElement("triggers");
		host.addElement("items");
		host.addElement("templates");
		host.addElement("graphs");
		host.addElement("macros");
	}

	private void createPumpSpeedGraph(Element graphs, Station s) {
		for(Can c:s.getCans()){
			Element g = graphs.addElement("graph");
			g.addAttribute("name", c.getId()+"号罐油枪出油");
			addGraphOtherChild(g);
			g.element("show_triggers").setText("0");
			Element ges =  g.addElement("graph_elements");
			for(int i=0;i<c.getGuns().size();i++){
				Gun gun = c.getGuns().get(i);
				Element ge = ges.addElement("graph_element");
				ge.addAttribute("item", s.getName()+":gun"+gun.getId()+".pumpspeed");
				ge.addElement("drawtype").addText("2");
				ge.addElement("sortorder").addText(new StringBuilder().append(i).toString());
				ge.addElement("color").addText(COLOR[i]);
				ge.addElement("yaxisside").addText("0");
				ge.addElement("calc_fnc").addText("2");
				ge.addElement("type").addText("0");
				ge.addElement("periods_cnt").addText("5");
			}
		}
	}

	private void createTmpGraph(Element graphs, Station s) {
		Element g = graphs.addElement("graph");
		g.addAttribute("name", "油温曲线_总");
		addGraphOtherChild(g);
		g.element("ymin_type").setText("1");
		Element ges =  g.addElement("graph_elements");
		for(int i=0;i<s.getCans().size();i++){
			Can c = s.getCans().get(i);
			Element ge = ges.addElement("graph_element");
			ge.addAttribute("item", s.getName()+":can"+c.getId()+".temp");
			ge.addElement("drawtype").addText("2");
			ge.addElement("sortorder").addText(new StringBuilder().append(i).toString());
			ge.addElement("color").addText(COLOR[i]);
			ge.addElement("yaxisside").addText("0");
			ge.addElement("calc_fnc").addText("2");
			ge.addElement("type").addText("0");
			ge.addElement("periods_cnt").addText("5");
		}
		
	}

	private void createVolumeGraph(Element graphs, Station s) {
		Element g = graphs.addElement("graph");
		g.addAttribute("name", "油罐库存_总");
		addGraphOtherChild(g);
		g.element("show_work_period").setText("1");
		g.element("show_triggers").setText("0");
		Element ges =  g.addElement("graph_elements");
		for(int i=0;i<s.getCans().size();i++){
			Can c = s.getCans().get(i);
			Element ge = ges.addElement("graph_element");
			ge.addAttribute("item", s.getName()+":can"+c.getId()+".volume");
			ge.addElement("drawtype").addText("2");
			ge.addElement("sortorder").addText(new StringBuilder().append(i).toString());
			ge.addElement("color").addText(COLOR[i]);
			ge.addElement("yaxisside").addText("0");
			ge.addElement("calc_fnc").addText("2");
			ge.addElement("type").addText("0");
			ge.addElement("periods_cnt").addText("5");
		}
	}

	private void createPLGraph(Element graphs, Station s) {
		// 损溢曲线-总
		Element g = graphs.addElement("graph");
		g.addAttribute("name", "损溢曲线_总");
		addGraphOtherChild(g);
		Element ges =  g.addElement("graph_elements");
		for(int i=0;i<s.getCans().size();i++){
			Can c = s.getCans().get(i);
			Element ge = ges.addElement("graph_element");
			ge.addAttribute("item", s.getName()+":can"+c.getId()+".sale_profit_loss");
			ge.addElement("drawtype").addText("2");
			ge.addElement("sortorder").addText(new StringBuilder().append(i).toString());
			ge.addElement("color").addText(COLOR[i]);
			ge.addElement("yaxisside").addText("0");
			ge.addElement("calc_fnc").addText("2");
			ge.addElement("type").addText("0");
			ge.addElement("periods_cnt").addText("5");
		}
	}

	private void addGraphOtherChild(Element g){
		g.addAttribute("width", "900");
		g.addAttribute("height", "200");
		g.addElement("ymin_type").addText("0");
		g.addElement("ymax_type").addText("0");
		g.addElement("ymin_item_key");
		g.addElement("ymax_item_key");
		g.addElement("show_work_period").addText("0");
		g.addElement("show_triggers").addText("1");
		g.addElement("graphtype").addText("0");
		g.addElement("yaxismin").addText("0.0000");
		g.addElement("yaxismax").addText("100.0000");
		g.addElement("show_legend").addText("0");
		g.addElement("show_3d").addText("0");
		g.addElement("percent_left").addText("0.0000");
		g.addElement("percent_right").addText("0.0000");
	}
	
	private void createMacros(Element macros) {
		Element macro = macros.addElement("macro");
		macro.addElement("value").addText("2300");
		macro.addElement("name").addText("{$HIGHT_HIGH1}");
		macro = macros.addElement("macro");
		macro.addElement("value").addText("2295");
		macro.addElement("name").addText("{$HIGHT_HIGH2}");
		macro = macros.addElement("macro");
		macro.addElement("value").addText("260");
		macro.addElement("name").addText("{$HIGHT_LOW1}");
		macro = macros.addElement("macro");
		macro.addElement("value").addText("265");
		macro.addElement("name").addText("{$HIGHT_LOW2}");		
	}

	private void createUnloadSaleTrigger(Element triggers,Can c, Station s){
		Element trigger = triggers.addElement("trigger");
		String desc = c.getId()+"号罐卸油时付油告警";
		//(({TanGu:can1.volume.last(0)}-{TanGu:can1.volume.avg(300)})>5|({TRIGGER.VALUE}=1&({TanGu:can1.volume.last(0)}-{TanGu:can1.volume.avg(300)})>-5))&({TanGu:gun2.pumpspeed.last(0)}>5|{TanGu:gun3.pumpspeed.last(0)}>5|{TanGu:gun8.pumpspeed.last(0)}>5|{TanGu:gun13.pumpspeed.last(0)}>5)
		StringBuilder tmp = new StringBuilder();
		for(Gun g:c.getGuns()){
			tmp.append("{"+s.getName()+":gun"+g.getId()+".pumpspeed.last(0)}>5|");
		}
		tmp.deleteCharAt(tmp.length()-1);
		String exp = "(({"+s.getName()+":can"+c.getId()+".volume.last(0)}-{"
				+s.getName()+":can"+c.getId()+".volume.avg(300)})>5|({TRIGGER.VALUE}=1&({"
				+s.getName()+":can"+c.getId()+".volume.last(0)}-{"
				+s.getName()+":can"+c.getId()+".volume.avg(300)})>-5))&("
				+tmp.toString()+")";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText("2");
		trigger.addElement("comments");
	}
	
	private void createUnloadIngTrigger(Element triggers,Can c, Station s){
		Element trigger = triggers.addElement("trigger");
		String desc = c.getId()+"号罐卸油进行中";
		// ({TanGu:can1.volume.last(0)}-{TanGu:can1.volume.avg(240)})>10|({TRIGGER.VALUE}=1&({TanGu:can1.volume.last(0)}-{TanGu:can1.volume.avg(240)})>-5&({TanGu:can1.volume.last(0)}-{TanGu:can1.volume.avg(600)})>5)
		String exp = "({"+s.getName()+":can"+c.getId()+".volume.last(0)}-{"
				+s.getName()+":can"+c.getId()+".volume.avg(240)})>10|({TRIGGER.VALUE}=1&({"
				+s.getName()+":can"+c.getId()+".volume.last(0)}-{"
				+s.getName()+":can"+c.getId()+".volume.avg(240)})>-5&({"
				+s.getName()+":can"+c.getId()+".volume.last(0)}-{"
				+s.getName()+":can"+c.getId()+".volume.avg(600)})>5)";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText("1");
		trigger.addElement("comments");
	}
	
	private void createUnloadEndTrigger(Element triggers,Can c, Station s){
		Element trigger = triggers.addElement("trigger");
		String desc = c.getId()+"号罐卸油结束";
		// ({TanGu:can1.volumespeed.prev(0)}>10|{TanGu:can1.volumespeed.prev(0)}=10)&{TanGu:can1.volumespeed.last(0)}<10
		String exp = "({"+s.getName()+":can"+c.getId()+".volumespeed.prev(0)}>10|{"
				+s.getName()+":can"+c.getId()+".volumespeed.prev(0)}=10)&{"
				+s.getName()+":can"+c.getId()+".volumespeed.last(0)}<10";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText("1");
		trigger.addElement("comments");
	}
	
	private void createUnloadStartTrigger(Element triggers,Can c, Station s){
		Element trigger = triggers.addElement("trigger");
		String desc = c.getId()+"号罐卸油开始";
		// ({TanGu:can1.volumespeed.prev(0)}<10|{TanGu:can1.volumespeed.prev(0)}=10)&{TanGu:can1.volumespeed.last(0)}>10
		String exp = "({"+s.getName()+":can"+c.getId()+".volumespeed.prev(0)}<10|{"
				+s.getName()+":can"+c.getId()+".volumespeed.prev(0)}=10)&{"
				+s.getName()+":can"+c.getId()+".volumespeed.last(0)}>10";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText("1");
		trigger.addElement("comments");
	}
	
	private void createUnloadTrigger(Element triggers, Can c, Station s) {
		Element trigger = triggers.addElement("trigger");
		String desc = c.getId()+"号罐卸油异常";
		// ({TanGu:can1.volume.last(0)}-{TanGu:can1.volume.min(7200)})>1000  两小时增加1000l
		String exp = "({"+s.getName()+":can"+c.getId()+".volume.last(0)}-{"
				+s.getName()+":can"+c.getId()+".volume.min(7200)})>1000";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText("2");
		trigger.addElement("comments");
	}

	private void createPLTrigger(Element triggers, Can c, Station s) {
		Element trigger = triggers.addElement("trigger");
		String desc = c.getId()+"号罐损溢告警-当前={ITEM.LASTVALUE}";
		String exp = "{"+s.getName()+":can"+c.getId()+".sale_profit_loss.last(0)}<-10";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText("2");
		trigger.addElement("comments");
	}

	private void createCanHightTrigger(int i, int p, Element triggers,Can c, Station s) {
		Element trigger = triggers.addElement("trigger");
//		String levels[] = {"一般","严重","紧急"};
		String priority = new StringBuffer().append(p).toString();		
		// 2
		String param;
		String exp;	//表达式
		if(i==0){
			param = "超高";
			exp = getHightTriggerExp(i,c,s);
		}else{
			param = "超低";
			exp = getHightTriggerExp(i,c,s);
		}
		String desc = c.getId()+"号罐"+param;
//		String desc = c.getId()+"号罐液位"+param+" 当前={ITEM.LASTVALUE}";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText(priority);
		trigger.addElement("comments");
	}

	private String getHightTriggerExp(int i,Can c, Station s) {
	// 超高超低警告表达式
		String exp;
		if(i==0){			
			// {TanGu:can1.height.last(0)}>{$HIGHT_HIGH1}|({TRIGGER.VALUE}=1&{TanGu:can1.height.last(0)}>{$HIGHT_HIGH2})
			exp = "{"+s.getName()+":can"+c.getId()+".height.last(0)}>{$HIGHT_HIGH1}|({TRIGGER.VALUE}=1&{"
					+s.getName()+":can"+c.getId()+".height.last(0)}>{$HIGHT_HIGH2})";	// 注意>等不用人为转义		
		}else{		
			// {TanGu:can1.height.last(0)}<{$HIGHT_LOW1}|({TRIGGER.VALUE}=1&{TanGu:can1.height.last(0)}<{$HIGHT_LOW2})
			exp = "{"+s.getName()+":can"+c.getId()+".height.last(0)}<{$HIGHT_LOW1}|({TRIGGER.VALUE}=1&{"
					+s.getName()+":can"+c.getId()+".height.last(0)}<{$HIGHT_LOW2})";
		}
		return exp;
	}

	private void createPumpSpeedItem(Element items, Gun g, Station s){
		Element item = items.addElement("item");
		item.addAttribute("type", "15");	// calculated
		String key = "gun"+g.getId()+".pumpspeed";
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");
		String desc = g.getId()+"号枪出油速度";
		item.addElement("description").addText(desc);
		addItemOtherEles(item);
		item.element("params").setText("change(can"+g.getCan().getId()
				+".gun"+g.getId()+".pump)");
		Element app = item.element("applications").addElement("application");
		app.addText(s.getName_cn()+"-枪");
	}
	
	private void createTransactionItem(Element items, Gun g, Station s) {
		Element item = items.addElement("item");
		item.addAttribute("type", "2");	// trapper
		String key = "gun"+g.getId()+".transaction";
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");
		String desc = g.getId()+"号枪交易记录";
		item.addElement("description").addText(desc);
		addItemOtherEles(item);
		item.element("delay").setText("60");
		Element app = item.element("applications").addElement("application");
		app.addText(s.getName_cn()+"-枪");
	}

	private void createCardnoItem(Element items) {
		Element item = items.addElement("item");
		item.addAttribute("type", "2");	// trapper
		item.addAttribute("key","cardno");
		item.addAttribute("value_type", "1");
		item.addElement("description").addText("卡号");
		addItemOtherEles(item);
		item.element("delay").setText("60");	// 没有主动设置这个，应该是默认配置
	}

	private void createVolumeOfSort(int n, Element items, Station s){
		Element item = items.addElement("item");
		item.addAttribute("type", "15");
		String desc;
		String key;
		String params = "";
		if(n==93){
			key = "volume[93]";
			desc = "93号汽油库存量";
		}else if(n==97){
			key = "volume[97]";
			desc = "97号汽油库存量";
		}else{
			key = "volume[0]";
			desc = "0号柴油库存量";
		}
		for(String can:s.getSort_can().get(""+n)){
			params = params+"last(can"+can+".volume,0)+";
		}
		params = params.substring(0, params.length()-1);
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");
		item.addElement("description").addText(desc);
		addItemOtherEles(item);
		item.element("params").setText(params);
	}
	
	private void createVolumeSpeedItem(Element items,Can c, Station s){
		Element item = items.addElement("item");
		item.addAttribute("type", "15");	// calculated
		String key = "can"+c.getId()+".volumespeed";
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");	// 数据类型是float
		// 2
		String desc = c.getId()+"号罐库存变化";
		item.addElement("description").addText(desc);
		addItemOtherEles(item);
		item.element("params").setText("change(can"+c.getId()+".volume)");
		Element app = item.element("applications").addElement("application");
		app.addText(s.getName_cn()+"-"+c.getId()+"号罐");
	}
	
	private void createProfitLossItem(Element items, Can c, Station s) {
		Element item = items.addElement("item");
		item.addAttribute("type", "15");	// calculated
		String key = "can"+c.getId()+".sale_profit_loss";
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");	// 数据类型是float
		// 2
		String desc = c.getId()+"号罐付油损溢";
		item.addElement("description").addText(desc);
		addItemOtherEles(item);		
		item.element("delay").setText("1800");
		String params = createProfitLossParams(c);
		item.element("params").setText(params);
		item.element("units").setText("L");
		item.element("applications").addElement("application").addText(s.getName_cn()+"-"+c.getId()+"号罐");
	}

	/**
	 *  计算损溢时的表达式
	 * @param c
	 * @return
	 */
	private String createProfitLossParams(Can c) {
		StringBuilder sb = new StringBuilder();
		for(Gun g:c.getGuns()){
			// last(can1.gun2.pump,0)-last(can1.gun2.pump,0,3600)+
			sb.append("last(can"+c.getId()+".gun"+g.getId()+".pump,0)-last(can"
					+c.getId()+".gun"+g.getId()+".pump,0,3600)+");
		}
//		sb.deleteCharAt(sb.length()-1); // 去掉最后的+
		//last(can1.volume,0)-last(can1.volume,0,3600)
		sb.append("last(can"+c.getId()+".volume,0)-last(can"+c.getId()+".volume,0,3600)");
		return sb.toString();
	}

	private void createGunItem(String param, Element items, Gun g, Station s) {
		Element item = items.addElement("item");
		item.addAttribute("type", "7");	// 罐监控都是 zabbix_agent active
		String key = "can"+g.getCan().getId()+".gun"+g.getId()+"."+param;
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");	// 数据类型是float
		// 2
		String param_zn; 
		if("volume".equals(param)){
			param_zn = "出油量";
		}else{
			param_zn = "泵码值";
		}
		String desc = g.getId()+"号枪"+param_zn;
		item.addElement("description").addText(desc);
		addItemOtherEles(item);	
		item.element("applications").addElement("application").addText(s.getName_cn()+"-枪");
	}

	private void createCanItem(String param, Element items,Can c, Station s) {
		Element item = items.addElement("item");
		item.addAttribute("type", "7");	// 罐监控都是 zabbix_agent active
		String key = "can"+c.getId()+"."+param;
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");	// 数据类型是float
		// 2
		String param_zn; 
		if("volume".equals(param)){
			param_zn = "油水总体积";
		}else if("height".equals(param)){
			param_zn = "液位高";
		}else if("water".equals(param)){
			param_zn = "水高";
		}else if("watervolume".equals(param)){
			param_zn = "水体积";
		}else{
			param_zn = "温度";
		}
		String desc = c.getId()+"号罐"+param_zn;
		item.addElement("description").addText(desc);
		addItemOtherEles(item);
		// 分别放入指定app
		Element app = item.element("applications").addElement("application");
		app.addText(s.getName_cn()+"-"+c.getId()+"号罐");
	}

	/**
	 * can or gun items中共同的部分
	 * @param item
	 */
	private void addItemOtherEles(Element item) {
		item.addElement("ipmi_sensor");
		item.addElement("delay").addText("30");
		item.addElement("history").addText("90");
		item.addElement("trends").addText("365");
		item.addElement("status").addText("0");
		item.addElement("data_type").addText("0");
		item.addElement("units");
		item.addElement("multiplier").addText("0");
		item.addElement("delta").addText("0");
		item.addElement("formula").addText("0");
		item.addElement("lastlogsize").addText("0");
		item.addElement("logtimefmt");
		item.addElement("delay_flex");
		item.addElement("authtype").addText("0");
		item.addElement("username");
		item.addElement("password");
		item.addElement("publickey");
		item.addElement("privatekey");
		item.addElement("params");
		item.addElement("trapper_hosts");
		item.addElement("snmp_community");
		item.addElement("snmp_oid");
		item.addElement("snmp_port").addText("161");
		item.addElement("snmpv3_securityname");
		item.addElement("snmpv3_securitylevel").addText("0");
		item.addElement("snmpv3_authpassphrase");
		item.addElement("snmpv3_privpassphrase");
		item.addElement("valuemapid").addText("0");	
		item.addElement("applications");
	}

	/**
	 * 写入xml文件地址
	 * 
	 * @param document
	 *            所属要写入的内容
	 * @param outFile
	 *            文件存放的地址
	 */
	public void writeDocument(Document document, String outFile) {
		System.out.println("writing to "+outFile);
		try {
			// 读取文件
			FileWriter fileWriter = new FileWriter(outFile);
			// 设置文件编码
			OutputFormat xmlFormat = new OutputFormat();
			xmlFormat.setEncoding("utf-8");
			// 创建写文件方法
			XMLWriter xmlWriter = new XMLWriter(fileWriter, xmlFormat);
			// 写入文件
			xmlWriter.write(document);
			// 关闭
			xmlWriter.close();
			System.out.println("Completed");
		} catch (IOException e) {
			System.out.println("文件没有找到");
			e.printStackTrace();
		}
	}
}
