package hostconfig;

import hostconfig.conf.Configuration;

import java.util.List;

import org.dom4j.Document;

public class Main {
	public static void main(String[] args) {
		// init
		Configuration conf = new Configuration();
		try {
			conf.load();
		} catch (Exception e) {
			System.out.println("初始化失败");
			e.printStackTrace();
			return;
		}
		String host_name = conf.getHost_name();
		String ip = conf.getIp();
		List<Can> cans = conf.getCans();	
		// create xml
		CreateHostConf factory = new CreateHostConf(host_name, ip);
		Document root = factory.getDocument(cans);
		factory.writeDocument(root, "d:/test2.xml");
			
//		for(Can c:cans){
//			System.out.print(c.getId()+":");
//			for (Gun g : c.getGuns()) {
//				System.out.print(g.getId()+",");
//			}
//			System.out.println();
//		}
		
	}
}
