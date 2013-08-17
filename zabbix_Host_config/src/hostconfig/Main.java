package hostconfig;

import hostconfig.conf.Configuration;

import org.dom4j.Document;

/**
 * 生成 zabbix_server1.8.7 的host配置文件 for 加油站项目
 * v1 版本：一个油站作为一个host，罐枪都是items
 * 
 * history:
 * v1.1 - 增加properties配置文件，设置罐枪关系
 * v1.2 - 业务更新；完善item、trigger、macro内容；配置文件内容微调；
 * v1.2.1 - 增加graph配置。
 * v1.2.2 - 
 * 		业务更新，组名、trigger name等尽可能中文化;
 * 		trigger卸油异常表达式更改
 * 		trigger卸油开始、卸油结束
 * 		item volumespeed
 * 
 * @author ycj
 *
 */
public class Main {
	public static void main(String[] args) {
		String config_file= "hostconfig/conf/fenglin_host.properties"; 
		// init
		Configuration conf = new Configuration();
		try {
			conf.load(config_file);
		} catch (Exception e) {
			System.out.println("初始化失败");
			e.printStackTrace();
			return;
		}
		
		String filepath = conf.getFilePath();
		// create xml
		CreateHostConf factory = new CreateHostConf(conf);
		Document root = factory.getDocument();
		factory.writeDocument(root, filepath);
	}
}
