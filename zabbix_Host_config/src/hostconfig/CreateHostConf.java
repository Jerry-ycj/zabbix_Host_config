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
	
	public CreateHostConf(String host_name,String ip){
		this.host_name = host_name;
		this.ip = ip;
	}
	
	public Document getDocument(List<Can> cans) {
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
		// item can 1can/5items
		for(Can c:cans){
			createCanItem("volume",items,c);
			createCanItem("height", items, c);
			createCanItem("water", items, c);
			createCanItem("watervolume", items, c);
			createCanItem("temp", items, c);
			// 0-超高，1-超低
			createCanTrigger(0,triggers,c);
			createCanTrigger(1,triggers,c);
			// item gun
			for(Gun g:c.getGuns()){
				createGunItem("volume",items,g);
				createGunItem("pump", items, g);
				// item transaction/gun
				createTransactionItem(items,g);
			}
			// item sale_profit_loss
			createProfitLossItem(items,c);
			
		}
		// cardno 在 items中
		createCardnoItem(items);		
		return document;
	}

	private void createCanTrigger(int i, Element triggers,Can c) {
		Element trigger = triggers.addElement("trigger");
		// 2
		String param;
		String exp;	//表达式
		if(i==0){
			param = "超高";
			exp = "{"+host_name+":can"+c.getId()+".height.last(0)}>900";	// 注意>等不用人为转义
		}else{
			param = "超低";
			exp = "{"+host_name+":can"+c.getId()+".height.last(0)}<250";
		}
		String desc = host_name+"_罐"+c.getId()+"液位"+param+"告警";
		trigger.addElement("description").addText(desc);
		trigger.addElement("type").addText("0");
		trigger.addElement("expression").addText(exp);
		trigger.addElement("url");
		trigger.addElement("status").addText("0");
		trigger.addElement("priority").addText("2");
		trigger.addElement("comments");
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
	}

	private void createCardnoItem(Element items) {
		Element item = items.addElement("item");
		item.addAttribute("type", "2");	// trapper
		item.addAttribute("key","cardno");
		item.addAttribute("value_type", "1");
		item.addElement("description").addText("卡号");
		addOtherChilds(item);
		item.element("delay").setText("60");	// 没有主动设置这个，应该是默认配置
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
		String params = createCalculatedParams(c);
		item.element("params").setText(params);
	}

	/**
	 *  计算损益时的表达式
	 * @param c
	 * @return
	 */
	private String createCalculatedParams(Can c) {
		StringBuilder sb = new StringBuilder();
		for(Gun g:c.getGuns()){
			sb.append("delta(can"+c.getId()+".gun"+g.getId()+".pump,3600)+");
		}
		sb.deleteCharAt(sb.length()-1);
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
