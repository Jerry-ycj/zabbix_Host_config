package hostconfig;

import hostconfig.conf.Configuration;

import java.util.List;
import org.dom4j.Document;

/**
 * 生成 zabbix_server1.8.7 的host配置文件 for 加油站项目
 * v1 版本：一个油站作为一个host，罐枪都是items
 * @author lenovo
 *
 */
public class Main {
	public static void main(String[] args) {
		// init
		Configuration conf = new Configuration();
		try {
			conf.load("hostconfig/conf/fenglin_host.properties");
		} catch (Exception e) {
			System.out.println("初始化失败");
			e.printStackTrace();
			return;
		}
		String host_name = conf.getHost_name();
		String ip = conf.getIp();
		List<Can> cans = conf.getCans();
		String filepath = conf.getFilePath();
		// create xml
		CreateHostConf factory = new CreateHostConf(host_name, ip,cans);
		Document root = factory.getDocument();
		factory.writeDocument(root, filepath);
		
	}
}
