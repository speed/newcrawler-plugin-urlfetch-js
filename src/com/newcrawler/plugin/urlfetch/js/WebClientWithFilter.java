package com.newcrawler.plugin.urlfetch.js;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class WebClientWithFilter extends WebClient {

	private static final long serialVersionUID = 2775940366514081586L;

	private static Log logger=LogFactory.getLog(WebClientWithFilter.class);
	private String jsFilterRegexs;
	private List<String> jsList;
	private String jsFilterType;
	private String jsCacheRegexs;
	private String crawlUrl;
	private List<String> exceptionURL;

	public WebClientWithFilter(String crawlUrl, BrowserVersion browserVersion, String jsFilterType, String jsFilterRegexs, List<String> jsList, String jsCacheRegexs, List<String> exceptionURL) {
		super(browserVersion);
		this.jsFilterRegexs = jsFilterRegexs;
		this.jsFilterType = jsFilterType;
		this.jsList = jsList;
		this.jsCacheRegexs=jsCacheRegexs;
		this.crawlUrl=crawlUrl;
		this.exceptionURL=exceptionURL;
	}

	@Override
	public WebResponse loadWebResponse(final WebRequest webRequest) throws IOException {
		final WebResponse response;
		String urlString = webRequest.getUrl().toExternalForm();
		if(crawlUrl.equals(urlString)){
			try {
				WebResponse  webResponse =super.loadWebResponse(webRequest);
				Set<Cookie> cookieSet = this.getCookieManager().getCookies();
				
				return webResponse;
			} catch (Exception e) {
				String log="URL:"+urlString+", "+e.getMessage();
				exceptionURL.add(log);
				logger.error(log);
				
				
				WebResponse  webResponse =new StringWebResponse("", webRequest.getUrl());
				webResponse.getWebRequest().getAdditionalHeaders().put("isCache", "true");
				getCache().cacheIfPossible(webRequest, webResponse, null);
				return  webResponse;
			}
		}
		final String protocol = webRequest.getUrl().getProtocol();
		if ("about".equals(protocol) || "file".equals(protocol) || "data".equals(protocol)) {
			response =super.loadWebResponse(webRequest);
		} else {
			if ("include".equals(jsFilterType)) {
				response = includeFilter(webRequest, jsFilterRegexs, jsList, crawlUrl, exceptionURL, urlString);
			} else {
				response = excludeFilter(webRequest, jsFilterRegexs, jsList, crawlUrl, exceptionURL, urlString);
			}
			if(jsCacheRegexs!=null){
				Pattern p = Pattern.compile(jsCacheRegexs);
				if (p.matcher(urlString).find()) {
					response.getWebRequest().getAdditionalHeaders().put("isCache", "true");
				}
			}
		}
		return response;

	}
	private final WebResponse excludeFilter(final WebRequest request, final String regexs, final List<String> jsList, final String crawlUrl, List<String> exceptionURL, final String urlString){
		if (regexs != null) {
			Pattern p = Pattern.compile(regexs);
			if (p.matcher(urlString).find()) {
				// Return an empty response body.
				logger.debug("exclude:" + urlString);
				return new StringWebResponse("", request.getUrl());
			}
		}
		return includeUrl(request, urlString);
	}
	
	private final WebResponse includeFilter(final WebRequest request, final String regexs, final List<String> jsList, final String crawlUrl, final List<String> exceptionURL, final String urlString) {
		if(regexs==null || "".equals(regexs)){
			return includeUrl(request, urlString);
		}else{
			Pattern p = Pattern.compile(regexs);
			if (p.matcher(urlString).find()) {
				return includeUrl(request, urlString);
			}
		}
		logger.debug("exclude:" + urlString);
		return new StringWebResponse("", request.getUrl());
	}
	
	private final WebResponse includeUrl(final WebRequest request, String urlString){
		// Return an empty response body.
		logger.debug("include:" + urlString);
		String result=urlString;
		if(urlString.indexOf("?")!=-1){
			result=urlString.substring(0, urlString.indexOf("?"))+"*";
		}
		if(!jsList.contains(result)){
			jsList.add(result);
		}
		try {
			WebResponse  webResponse =super.loadWebResponse(request);
			Set<Cookie> cookieSet = this.getCookieManager().getCookies();
			
			return webResponse;
		} catch (Exception e) {
			String log="URL:"+urlString+", "+e.getMessage();
			exceptionURL.add(log);
			logger.error(log);
			WebResponse  webResponse =new StringWebResponse("", request.getUrl());
			webResponse.getWebRequest().getAdditionalHeaders().put("isCache", "true");
			getCache().cacheIfPossible(request, webResponse, null);
			return  webResponse;
		}
	}

}
