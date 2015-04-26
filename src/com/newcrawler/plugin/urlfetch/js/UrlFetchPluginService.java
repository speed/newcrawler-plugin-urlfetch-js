package com.newcrawler.plugin.urlfetch.js;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Cache;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebWindow;
import com.gargoylesoftware.htmlunit.gae.GAEUtils;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.soso.plugin.UrlFetchPlugin;

public class UrlFetchPluginService implements UrlFetchPlugin {
	private static Log logger=LogFactory.getLog(UrlFetchPluginService.class);
	
	public static final String PROPERTIES_TIMEOUT_JAVASCRIPT = "timeout.javascript";
	public static final String PROPERTIES_TIMEOUT_CONNECTION = "timeout.connection";
	public static final String PROPERTIES_JS_FILTER_REGEXS = "js.filter.regexs";
	public static final String PROPERTIES_JS_FILTER_TYPE = "js.filter.type";
	public static final String PROPERTIES_JS_CACHE_REGEXS = "js.cache.regexs";
	
	public static final String PROXY_IP = "proxy.ip";
	public static final String PROXY_PORT = "proxy.port";
	public static final String PROXY_USER = "proxy.username";
	public static final String PROXY_PASS = "proxy.password";
	public static final String PROXY_TYPE = "proxy.type";
	
	private static final String DEFAULT_JS_FILTER_TYPE = "include";
	private Cache _cache;

	public static void main(String[] args) throws IOException{
		Map<String, String> properties=new HashMap<String, String>(); 
		properties.put(PROXY_IP, "127.0.0.1");
		properties.put(PROXY_PORT, String.valueOf(6666));
		properties.put(PROXY_TYPE, "socks5");
		
		Map<String, String> headers=new HashMap<String, String>(); 
		String crawlUrl="http://item.jd.com/832705.html"; 
		String method=null; 
		String cookie=null; 
		String userAgent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.36 Safari/535.7"; 
		String encoding="GB2312";
		UrlFetchPluginService urlFetchPluginService=new UrlFetchPluginService();
		//urlFetchPluginService.execute(properties, crawlUrl, method, cookie, userAgent, encoding);
		
		crawlUrl="http://www.newcrawler.com/header"; 
		Map<String, Object> map =urlFetchPluginService.execute(properties, headers, crawlUrl, method, cookie, userAgent, encoding);
		System.out.println(map.get(RETURN_DATA_KEY_CONTENT));
		
		crawlUrl="http://www.google.com"; 
		map =urlFetchPluginService.execute(properties, headers, crawlUrl, method, cookie, userAgent, encoding);
		System.out.println(map.get(RETURN_DATA_KEY_CONTENT));
	}
	
	public UrlFetchPluginService() {
		_cache = CacheFactory.getInstance().getCache();
	}

	@Override
	public Map<String, Object> execute(Map<String, String> properties, Map<String, String> headers, String urlString, String method, String cookie, String userAgent, String encoding) throws IOException {
		String jsFilterRegexs = null;
		String jsFilterType = DEFAULT_JS_FILTER_TYPE;
		String jsCacheRegexs = null;
		int timeoutConnection=15000;
		int timeoutJavascript=8000;
		
		String proxyIP=null;
		int proxyPort=-1;
		String proxyUsername=null;
		String proxyPassword=null;
		String proxyType=null;
		
		if (properties != null) {
			if (properties.containsKey(PROPERTIES_JS_FILTER_REGEXS) && properties.get(PROPERTIES_JS_FILTER_REGEXS)!=null 
					&& !"".equals(properties.get(PROPERTIES_JS_FILTER_REGEXS).trim())) {
				jsFilterRegexs = properties.get(PROPERTIES_JS_FILTER_REGEXS).trim();
			}
			
			if (properties.containsKey(PROPERTIES_JS_CACHE_REGEXS) && properties.get(PROPERTIES_JS_CACHE_REGEXS)!=null 
					&& !"".equals(properties.get(PROPERTIES_JS_CACHE_REGEXS).trim())) {
				jsCacheRegexs = properties.get(PROPERTIES_JS_CACHE_REGEXS).trim();
			}

			if (properties.containsKey(PROPERTIES_JS_FILTER_TYPE) && properties.get(PROPERTIES_JS_FILTER_TYPE)!=null 
					&& !"".equals(properties.get(PROPERTIES_JS_FILTER_TYPE).trim())) {
				jsFilterType = properties.get(PROPERTIES_JS_FILTER_TYPE).trim();
			}
			
			if (properties.containsKey(PROPERTIES_TIMEOUT_JAVASCRIPT) && properties.get(PROPERTIES_TIMEOUT_JAVASCRIPT)!=null 
					&& !"".equals(properties.get(PROPERTIES_TIMEOUT_JAVASCRIPT).trim())) {
				timeoutJavascript = Integer.parseInt(properties.get(PROPERTIES_TIMEOUT_JAVASCRIPT).trim());
			}
			if (properties.containsKey(PROPERTIES_TIMEOUT_CONNECTION) && properties.get(PROPERTIES_TIMEOUT_CONNECTION)!=null 
					&& !"".equals(properties.get(PROPERTIES_TIMEOUT_CONNECTION).trim())) {
				timeoutConnection = Integer.parseInt(properties.get(PROPERTIES_TIMEOUT_CONNECTION).trim());
			}
			
			if (properties.containsKey(PROXY_IP) && properties.get(PROXY_IP)!=null
					&& !"".equals(properties.get(PROXY_IP).trim())) {
				proxyIP = properties.get(PROXY_IP).trim();
			}
			if (properties.containsKey(PROXY_PORT) && properties.get(PROXY_PORT)!=null
					&& !"".equals(properties.get(PROXY_PORT).trim())) {
				proxyPort = Integer.parseInt(properties.get(PROXY_PORT).trim());
			}

			if (properties.containsKey(PROXY_USER) && properties.get(PROXY_USER)!=null
					&& !"".equals(properties.get(PROXY_USER).trim())) {
				proxyUsername = properties.get(PROXY_USER).trim();
			}
			
			if (properties.containsKey(PROXY_PASS) && properties.get(PROXY_PASS)!=null
					&& !"".equals(properties.get(PROXY_PASS).trim())) {
				proxyPassword = properties.get(PROXY_PASS).trim();
			}
			if (properties.containsKey(PROXY_TYPE) && properties.get(PROXY_TYPE)!=null
					&& !"".equals(properties.get(PROXY_TYPE).trim())) {
				proxyType = properties.get(PROXY_TYPE).trim();
			}
		}

		String[] filterRegexs = null;
		if (jsFilterRegexs != null && !"".equals(jsFilterRegexs.trim())) {
			String[] regexs = jsFilterRegexs.split("\\Q|$|\\E");
			int len = regexs.length;
			filterRegexs = new String[len];
			for (int i = 0; i < len; i++) {
				String regex = regexs[i];
				regex = regex.trim();
				regex = "^\\Q"+regex+"\\E$";
				if(regex.indexOf("*")!=-1){
					regex = regex.replaceAll("\\*", "\\\\E.*\\\\Q");
				}
				filterRegexs[i] = regex;
			}
		}
		String[] cacheRegexs = null;
		if (jsCacheRegexs != null && !"".equals(jsCacheRegexs.trim())) {
			String[] regexs = jsCacheRegexs.split("\\Q|$|\\E");
			int len = regexs.length;
			cacheRegexs = new String[len];
			for (int i = 0; i < len; i++) {
				String regex = regexs[i];
				regex = regex.trim();
				regex = "^\\Q"+regex+"\\E$";
				if(regex.indexOf("*")!=-1){
					regex = regex.replaceAll("\\*", "\\\\E.*\\\\Q");
				}
				cacheRegexs[i] = regex;
			}
		}
		if(headers==null){
			headers = new HashMap<String, String>();
		}
		if(StringUtils.isNoneBlank(cookie)){
			headers.put("Cookie", cookie);
		}
		if(StringUtils.isNoneBlank(userAgent)){
			headers.put("User-Agent", userAgent);
		}
		
		List<String> exceptionURL=new ArrayList<String>();
		List<String> jsList = new ArrayList<String>();
		UrlFetchResponse urlFetchResponse = null;
		
		urlFetchResponse = readHtml(proxyIP, proxyPort, proxyUsername, proxyPassword, proxyType, urlString, encoding, headers, jsFilterType, filterRegexs, jsList, cacheRegexs,
				timeoutConnection, timeoutJavascript, exceptionURL);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(RETURN_DATA_KEY_CONTENT, urlFetchResponse.getContent());
		map.put(RETURN_DATA_KEY_REALURL, urlFetchResponse.getRealURL());
		map.put(RETURN_DATA_KEY_INCLUDE_JS, jsList);
		map.put(RETURN_DATA_KEY_COOKIES, urlFetchResponse.getCookies());
		map.put(RETURN_DATA_KEY_EXCEPTION_URL, exceptionURL);
		return map;
	}

	private UrlFetchResponse readHtml(String proxyIP, int proxyPort, final String proxyUsername, final String proxyPassword, final String proxyType, String urlString, String encoding, final Map<String, String> headers, String jsFilterType, 
			String[] jsFilterRegexs, List<String> jsList, String[] jsCacheRegexs,
			int timeoutConnection, int timeoutJavascript, List<String> exceptionURL) throws IOException {
		WebRequest request = new WebRequest(new URL(urlString));
		request.setCharset(encoding);
		request.setAdditionalHeaders(headers);

		WebClientWithFilter webClient = getWebClient(proxyIP, proxyPort, proxyUsername, proxyPassword, proxyType, urlString, jsFilterType, jsFilterRegexs, jsList, jsCacheRegexs,
				timeoutConnection, timeoutJavascript, exceptionURL);
		Page page = null;
		String pageAsText = null;
		String realURL = urlString;
		Map<String, String> cookies=new HashMap<String, String>();;
		try {
			page = webClient.getPage(request);
			pageAsText = getPageContent(page);
			realURL = page.getUrl().toString();
			
			CookieManager cookieManager =webClient.getCookieManager();
			Set<Cookie> cookieSet = cookieManager.getCookies();
			for (Cookie cookie : cookieSet) {
				cookies.put(cookie.getName(), cookie.getValue());
			}
		} catch (StackOverflowError overflowError) {
			pageAsText = overflowError.getMessage();
			logger.error(urlString, overflowError);
		} catch (OutOfMemoryError outOfMemoryError) {
			pageAsText = outOfMemoryError.getMessage();
			logger.error(urlString, outOfMemoryError);
		} catch (Exception e) {
			logger.error(urlString, e);
		} finally {
			closeWindows(webClient);
			if (page != null) {
				page.cleanUp();
				page = null;
			}
			webClient = null;
		}

		UrlFetchResponse urlFetchResponse = new UrlFetchResponse();
		urlFetchResponse.setContent(pageAsText);
		urlFetchResponse.setRealURL(realURL);
		urlFetchResponse.setCookies(cookies);
		return urlFetchResponse;
	}
	private WebConnection createWebConnection(WebClientWithFilter webClient) {
        if (GAEUtils.isGaeMode()) {
            return new UrlFetchNoErrorLogWebConnection(webClient);
        }

        return new NoRetryHttpWebConnection(webClient);
    }
	
	public WebClientWithFilter getWebClient(String proxyIP, int proxyPort, final String proxyUsername, final String proxyPassword, final String proxyType, String urlString, String jsFilterType, String[] jsFilterRegexs, List<String> jsList, String[] jsCacheRegexs,
			int timeoutConnection, int timeoutJavascript, List<String> exceptionURL) {
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
		
		WebClientWithFilter webClient = new WebClientWithFilter(urlString, BrowserVersion.INTERNET_EXPLORER_11, jsFilterType, jsFilterRegexs, jsList, jsCacheRegexs, exceptionURL);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setAppletEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setTimeout(timeoutConnection);
		webClient.setJavaScriptTimeout(timeoutJavascript);
		webClient.setCache(_cache);
		webClient.setJavaScriptEngine(new LogJavaScriptEngine(webClient));
		webClient.getOptions().setActiveXNative(false);
		webClient.getOptions().setPopupBlockerEnabled(true);
		webClient.getOptions().setDoNotTrackEnabled(true);
		
		
		if(proxyIP!=null){
			ProxyConfig proxyConfig = new ProxyConfig(proxyIP,proxyPort);
			if("socks5".equals(proxyType)){
				proxyConfig.setSocksProxy(true);
			}
			webClient.getOptions().setProxyConfig(proxyConfig);
			
			if(proxyUsername!=null && proxyPassword!=null){
				final DefaultCredentialsProvider credentialsProvider = new DefaultCredentialsProvider();
				credentialsProvider.addCredentials(proxyUsername, proxyPassword);
				webClient.setCredentialsProvider(credentialsProvider);
			}
		}
		
		webClient.setWebConnection(createWebConnection(webClient));
		webClient.setIncorrectnessListener(new IncorrectnessListener() {
			@Override
			public void notify(String arg0, Object arg1) {
				// TODO Auto-generated method stub
			}
		});
		webClient.setCssErrorHandler(new ErrorHandler() {
			@Override
			public void warning(CSSParseException exception) throws CSSException {
				// TODO Auto-generated method stub
			}
			@Override
			public void fatalError(CSSParseException exception) throws CSSException {
				// TODO Auto-generated method stub
			}
			@Override
			public void error(CSSParseException exception) throws CSSException {
				// TODO Auto-generated method stub
			}
		});
		webClient.setJavaScriptErrorListener(new JavaScriptErrorListener() {
			@Override
			public void timeoutError(HtmlPage arg0, long arg1, long arg2) {
				// TODO Auto-generated method stub
			}
			@Override
			public void scriptException(HtmlPage arg0, ScriptException arg1) {
				// TODO Auto-generated method stub
			}
			@Override
			public void malformedScriptURL(HtmlPage arg0, String arg1, MalformedURLException arg2) {
				// TODO Auto-generated method stub
			}
			@Override
			public void loadScriptError(HtmlPage arg0, URL arg1, Exception arg2) {
				// TODO Auto-generated method stub
			}
		});
		webClient.setHTMLParserListener(new HTMLParserListener() {
			@Override
			public void error(String message, URL url, String html, int line, int column, String key) {
				// TODO Auto-generated method stub
			}
			@Override
			public void warning(String message, URL url, String html, int line, int column, String key) {
				// TODO Auto-generated method stub
			}
		});
		return webClient;
	}

	private void closeWindows(WebClient webClient) {
		webClient.getJavaScriptEngine().shutdown();
		List<WebWindow> windows = webClient.getWebWindows();
		if (windows != null && !windows.isEmpty()) {
			for (WebWindow window : windows) {
				try {
					window.getJobManager().removeAllJobs();
					window.getJobManager().shutdown();
					Page page = window.getEnclosedPage();
					if (page != null) {
						if (page instanceof HtmlPage) {
							HtmlPage htmlPage = (HtmlPage) page;
							htmlPage.remove();
						}
						window.getEnclosedPage().getEnclosingWindow().setScriptObject(null);
					}
					webClient.deregisterWebWindow(window);
				} catch (Exception e) {
					// Ignore Exception
				}
			}
		}
		webClient.getJavaScriptEngine().getWebClient().closeAllWindows();
		webClient.closeAllWindows();
	}

	private String getPageContent(Page page) {
		String content = null;
		if (page instanceof HtmlPage && ((HtmlPage) page).getBody() != null) {
			content = ((HtmlPage) page).asXml();
		} else if (page instanceof TextPage) {
			content = (((TextPage) page).getContent());
		}
		return content;
	}

	final class LogJavaScriptEngine extends JavaScriptEngine {
		public LogJavaScriptEngine(WebClient webClient) {
			super(webClient);
		}

		protected void handleJavaScriptException(ScriptException scriptException, boolean triggerOnError) {
			//logger.info("Caught script exception " + scriptException.getMessage());
		}
	}

	final class UrlFetchResponse {
		private String realURL;
		private String content;
		private Map<String, String> cookies;
		
		public String getRealURL() {
			return realURL;
		}

		public void setRealURL(String realURL) {
			this.realURL = realURL;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public Map<String, String> getCookies() {
			return cookies;
		}

		public void setCookies(Map<String, String> cookies) {
			this.cookies = cookies;
		}
	}

	@Override
	public void destory() {
		_cache.clear();
	}
}
