package com.newcrawler.plugin.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.newcrawler.plugin.urlfetch.js.UrlFetchPluginService;
import com.soso.plugin.bo.HttpCookieBo;
import com.soso.plugin.bo.UrlFetchPluginBo;

public abstract class Test {

	public static void main(String[] args) throws IOException{
		Map<String, String> properties=new HashMap<String, String>(); 
		/*properties.put(PROXY_IP, "127.0.0.1");
		properties.put(PROXY_PORT, String.valueOf(8888));
		properties.put(PROXY_TYPE, "http");*/
		//{proxy.ip=, js.filter.regexs=, proxy.password=, timeout.connection=10000, js.cache.regexs=, js.filter.type=, proxy.username=, proxy.type=, timeout.javascript=5000, proxy.port=}
		//properties.put(PROPERTIES_JS_FILTER_TYPE, "include");
		//properties.put(PROPERTIES_JS_FILTER_REGEXS, "http://static.360buyimg.com/*|$|http://item.jd.com/*");
		
		properties.put("timeout.connection", "10000");
		properties.put("timeout.javascript", "5000");
		
		
		Map<String, String> headers=new HashMap<String, String>(); 
		String crawlUrl="http://item.jd.com/4206468.html"; 
		String method="GET"; 
		List<HttpCookieBo> cookieList=null;
		String userAgent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.36 Safari/535.7"; 
		String encoding="GB2312";
		UrlFetchPluginService urlFetchPluginService=new UrlFetchPluginService();
		UrlFetchPluginBo urlFetchPluginBo=new UrlFetchPluginBo(properties, headers, crawlUrl, method, cookieList, userAgent, encoding);
		
		Map<String, Object> map =urlFetchPluginService.execute(urlFetchPluginBo);
		System.out.println(map.get(UrlFetchPluginService.RETURN_DATA_KEY_CONTENT));
	}

}
