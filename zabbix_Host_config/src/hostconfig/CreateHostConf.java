package hostconfig;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class CreateHostConf {
	private String host_name;
	private String ip;
	private List<Can> cans;
	
	public CreateHostConf(String host_name,String ip,List<Can> cans){
		this.host_name = host_name;
		this.ip = ip;
		this.cans = cans;
	}
	
	public Document getDocument() {
		// 生成一个节点
		Document document = DocumentHelper.createDocument();
		// root
		Element root = document.addElement("zabbix_export");
		root.addAttribute("version", "1.0");
		root.addAttribute("date", "09.08.13");
		root.addAttribute("time", "15.55");
		// 2
		Element hosts = root.addElement("hosts");
		root.addElement("dependencies");
		// 3 host
		Element host = hosts.addElement("host");
		host.addAttribute("name", host_name);
		// 4
		host.addElement("proxy_hostid").addText("0");
		host.addElement("useip").addText("1");
		host.addElement("dns");
		host.addElement("ip").addText(ip);
		host.addElement("port").addText("10050");
		host.addElement("status").addText("0");	// 0-开启
		host.addElement("useipmi").addText("0");
		host.addElement("ipmi_ip");
		host.addElement("ipmi_port").addText("623");
		host.addElement("ipmi_authtype").addText("-1");
		host.addElement("ipmi_privilege").addText("2");
		host.addElement("ipmi_username");
		host.addElement("ipmi_password");
		host.addElement("groups").addElement("group").addText("Discovered hosts");
		Element triggers = host.addElement("triggers");
		Element items = host.addElement("items");
		Element templates = host.addElement("templates");
		Element graphs = host.addElement("graphs");
		Element macros = host.addElement("macros");
		// 5
		// item can 1can/5items;trigger
		for(Can c:cans){
			createCanItem("volume",items,c);
			createCanItem("height", items, c);
			createCanItem("water", items, c);
			createCanItem("watervolume", items, c);
			createCanItem("temp", items, c);
			// 0-超高，1-超低，不同级别告警
			createCanHightTrigger(0,0,triggers,c);
			createCanHightTrigger(0,1,triggers,c);
			createCanHightTrigger(0,2,triggers,c);
			createCanHightTrigger(1,0,triggers,c);
			createCanHightTrigger(1,1,triggers,c);
			createCanHightTrigger(1,2,triggers,c);
			// 损益告警
			createPLTrigger(triggers,c);
			// 卸油告警
			createUnloadTrigger(triggers,c);
			// item gun
			for(Gun g:c.getGuns()){
				createGunItem("volume",items,g);
				createGunItem("pump", items, g);
				createTransactionItem(items,g);
				createPumpSpeedItem(items,g);
			}
			// item sale_profit_loss
			createProfitLossItem(items,c);
		}
		// cardno 在 items中
		createCardnoItem(items);
		// macro
		createMacros(macros);
		return document;
	}

	private void createMacros(Element macros) {
		Element macro = macros.addElement("macro");
		macro.addElement("value").addText("900");
		macro.addElement("name").addText("{$HIGHT_HIGH1}");
		macro = macros.addElement("macro");
		macro.addElement("value").addText("1000");
		macro.addElement("name").addText("{$HIGHT_HIGH2}");
		macro = macros.addElement("macro");
		macro.addElement("value").addText("250");
		macro.addElement("name").addText("{$HIGHT_LOW1}");
		macro = macros.addElement("macro");
		macro.addElement("value").addText("200");
		macro.addElement("name").addText("{$HIGHT_LOW2}");		
	}

	private void createUnloadTrigger(Element triggers, Can c) {
		Element trigger = triggers.addElement("trigger");
		String desc = host_name+"_罐"+c.getId()+"卸油异常";
		// {TanGu:can1.volume.delta(7200)}>1000  两小时增加1000l
		String exp = "{"+host_name+":can"+c.getId()+".volume.delta(7200)}>1000";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText("3");
		trigger.addElement("comments");
	}

	private void createPLTrigger(Element triggers, Can c) {
		Element trigger = triggers.addElement("trigger");
		String desc = host_name+"_罐"+c.getId()+"损益告警 当前={ITEM.LASTVALUE}";
		String exp = "{"+host_name+":can"+c.getId()+".sale_profit_loss.last(0)}<-50";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText("2");
		trigger.addElement("comments");
	}

	private void createCanHightTrigger(int i, int level, Element triggers,Can c) {
		Element trigger = triggers.addElement("trigger");
		String levels[] = {"警告","一般严重","严重"};
		String priority = getPriority(level);		
		// 2
		String param;
		String exp;	//表达式
		if(i==0){
			param = "超高";
			exp = getHightTriggerExp(level,i,c);
		}else{
			param = "超低";
			exp = getHightTriggerExp(level,i,c);
		}
		String desc = host_name+"_罐"+c.getId()+"液位"+param+"-"+levels[level]+" 当前={ITEM.LASTVALUE}";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText(priority);
		trigger.addElement("comments");
	}

	private String getHightTriggerExp(int level, int i,Can c) {
	// 超高超低警告表达式，根据级别
		String exp;
		if(i==0){
			if(level==0){
				// ({TanGu:can1.height.last(0)}>{$HIGHT_HIGH1}&{TanGu:can1.height.last(0)}<{$HIGHT_HIGH2})|{TanGu:can1.height.last(0)}={$HIGHT_HIGH2}
				exp = "({"+host_name+":can"+c.getId()+".height.last(0)}>{$HIGHT_HIGH1}&{"
						+host_name+":can"+c.getId()+".height.last(0)}<{$HIGHT_HIGH2})|{"
						+host_name+":can"+c.getId()+".height.last(0)}={$HIGHT_HIGH2}";	// 注意>等不用人为转义
			}else if(level==1){
				// {TanGu:can1.height.last(0)}>{$HIGHT_HIGH2}
				exp = "{"+host_name+":can"+c.getId()+".height.last(0)}>{$HIGHT_HIGH2}";
			}else{
				// {TanGu:can1.height.min(#10)}>{$HIGHT_HIGH2}
				exp = "{"+host_name+":can"+c.getId()+".height.min(#10)}>{$HIGHT_HIGH2}";
			}
		}else{
			if(level==0){
				// ({TanGu:can1.height.last(0)}<{$HIGHT_LOW1}&{TanGu:can1.height.last(0)}>{$HIGHT_LOW2})|{TanGu:can1.height.last(0)}={$HIGHT_LOW2}
				exp = "({"+host_name+":can"+c.getId()+".height.last(0)}<{$HIGHT_LOW1}&{"
						+host_name+":can"+c.getId()+".height.last(0)}>{$HIGHT_LOW2})|{"
						+host_name+":can"+c.getId()+".height.last(0)}={$HIGHT_LOW2}";
			}else if(level==1){
				// {TanGu:can1.height.last(0)}<{$HIGHT_LOW2}
				exp = "{"+host_name+":can"+c.getId()+".height.last(0)}<{$HIGHT_LOW2}";
			}else{
				// {TanGu:can1.height.max(#10)}<{$HIGHT_LOW2}
				// 采集的10次数据(5min)最大小于200
				exp = "{"+host_name+":can"+c.getId()+".height.max(#10)}<{$HIGHT_LOW2}";
			}
		}
		return exp;
	}

	private String getPriority(int level) {
	// trigger 的 警告级别
		String priority;
		if(level==0){
			priority = "2";
		}else if(level==1){
			priority = "3";
		}else{
			priority = "4";
		}
		return priority;
	}

	private void createPumpSpeedItem(Element items, Gun g){
		Element item = items.addElement("item");
		item.addAttribute("type", "15");	// calculated
		String key = "gun"+g.getId()+".pumpspeed";
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");
		String desc = host_name+"_罐"+g.getCan().getId()+"_枪"+g.getId()+"的出油速度";
		item.addElement("description").addText(desc);
		addOtherChilds(item);
		item.element("params").setText("change(can"+g.getCan().getId()
				+".gun"+g.getId()+".pump)");
		Element app = item.addElement("applications").addElement("application");
		app.addText(host_name+"_Gun");
	}
	
	private void createTransactionItem(Element items, Gun g) {
		Element item = items.addElement("item");
		item.addAttribute("type", "2");	// trapper
		String key = "gun"+g.getId()+".transaction";
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");
		String desc = host_name+"_罐"+g.getCan().getId()+"_枪"+g.getId()+"的交易记录";
		item.addElement("description").addText(desc);
		addOtherChilds(item);
		item.element("delay").setText("60");
		Element app = item.addElement("applications").addElement("application");
		app.addText(host_name+"_Gun");
	}

	private void createCardnoItem(Element items) {
		Element item = items.addElement("item");
		item.addAttribute("type", "2");	// trapper
		item.addAttribute("key","cardno");
		item.addAttribute("value_type", "1");
		item.addElement("description").addText("卡号");
		addOtherChilds(item);
		item.element("delay").setText("60");	// 没有主动设置这个，应该是默认配置
		item.addElement("applications");
	}

	private void createProfitLossItem(Element items, Can c) {
		Element item = items.addElement("item");
		item.addAttribute("type", "15");	// calculated
		String key = "can"+c.getId()+".sale_profit_loss";
		item.addAttribute("key", key);
		item.addAttribute("value_type", "0");	// 数据类型是float
		// 2
		String desc = host_name+"_罐"+c.getId()+"的付油损益";
		item.addElement("description").addText(desc);
		addOtherChilds(item);		
		item.element("delay").setText("1800");
		String params = createProfitLossParams(c);
		item.element("params").setText(params);
		item.element("units").setText("L");
		Element app = item.addElement("applications").addElement("application");
		app.addText(host_name+"_Can"+c.getId());
	}

	/**
	 *  计算损益时的表达式
	 * @param c
	 * @return
	 */
	private String createProfitLossParams(Can c) {
		StringBuilder sb = new StringBuilder();
		for(Gun g:c.getGuns()){
			sb.append("delta(can"+c.getId()+".gun"+g.getId()+".pump,3600)+");
		}
		sb.deleteCharAt(sb.length()-1); // 去掉最后的+
		sb.append("-delta(can"+c.getId()+".volume,3600)");
		return sb.toString();
	}

	private void createGunItem(String param, Element items, Gun g) {
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
		String desc = host_name+"_罐"+g.getCan().getId()+"_枪"+g.getId()+"的"+param_zn;
		item.addElement("description").addText(desc);
		addOtherChilds(item);	
		Element app = item.addElement("applications").addElement("application");
		app.addText(host_name+"_Can"+g.getCan().getId());
	}

	private void createCanItem(String param, Element items,Can c) {
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
		String desc = host_name+"_罐"+c.getId()+"的"+param_zn;
		item.addElement("description").addText(desc);
		addOtherChilds(item);
		// 分别放入指定app
		Element app = item.addElement("applications").addElement("application");
		app.addText(host_name+"_Can"+c.getId());
	}

	/**
	 * can or gun items中共同的部分
	 * @param item
	 */
	private void addOtherChilds(Element item) {
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
			System.out.println("write to xml completed");
		} catch (IOException e) {
			System.out.println("文件没有找到");
			e.printStackTrace();
		}
	}
}
