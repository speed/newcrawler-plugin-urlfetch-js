package com.newcrawler.plugin.urlfetch.js;

import static com.gargoylesoftware.htmlunit.BrowserVersionFeatures.PROTOCOL_DATA;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.DownloadedContent;
import com.gargoylesoftware.htmlunit.HttpWebConnection;
import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.TextUtil;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import com.gargoylesoftware.htmlunit.protocol.data.DataUrlDecoder;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.gargoylesoftware.htmlunit.util.UrlUtils;

public class WebClientWithFilter extends WebClient {

	private static final long serialVersionUID = 2775940366514081586L;

	private static Log logger=LogFactory.getLog(WebClientWithFilter.class);
	private String[] jsFilterRegexs;
	private List<String> jsList;
	private String jsFilterType;
	private String[] jsCacheRegexs;
	private String crawlUrl;
	private List<String> exceptionURL;

	public WebClientWithFilter(String crawlUrl, BrowserVersion browserVersion, String jsFilterType, String[] jsFilterRegexs, List<String> jsList, String[] jsCacheRegexs, List<String> exceptionURL) {
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
		if ("about".equals(protocol)) {
			response = makeWebResponseForAboutUrl(webRequest.getUrl());
		} else if ("file".equals(protocol)) {
			response = makeWebResponseForFileUrl(webRequest);
		} else if ("data".equals(protocol)) {
			if (getBrowserVersion().hasFeature(PROTOCOL_DATA)) {
				response = makeWebResponseForDataUrl(webRequest);
			} else {
				throw new MalformedURLException("Unknown protocol: data");
			}
		} else {
			if ("include".equals(jsFilterType)) {
				response = includeFilter(webRequest, jsFilterRegexs, jsList, crawlUrl, exceptionURL, urlString);
			} else {
				response = excludeFilter(webRequest, jsFilterRegexs, jsList, crawlUrl, exceptionURL, urlString);
			}
			if(jsCacheRegexs!=null){
				for (String regex : jsCacheRegexs) {
					Pattern p = Pattern.compile(regex);
					if (p.matcher(urlString).find()) {
						response.getWebRequest().getAdditionalHeaders().put("isCache", "true");
						break;
					}
				}
			}
		}
		return response;

	}
	private final WebResponse excludeFilter(final WebRequest request, final String[] regexs, final List<String> jsList, final String crawlUrl, List<String> exceptionURL, final String urlString){
		if (regexs != null) {
			for (String regex : regexs) {
				Pattern p = Pattern.compile(regex);
				if (p.matcher(urlString).find()) {
					// Return an empty response body.
					logger.debug("exclude:" + urlString);
					return new StringWebResponse("", request.getUrl());
				}
			}
		}
		return includeUrl(request, urlString);
	}
	
	private final WebResponse includeFilter(final WebRequest request, final String[] regexs, final List<String> jsList, final String crawlUrl, final List<String> exceptionURL, final String urlString) {
		if(regexs==null){
			return includeUrl(request, urlString);
		}else{
			for (String regex : regexs) {
				Pattern p = Pattern.compile(regex);
				if (p.matcher(urlString).find()) {
					return includeUrl(request, urlString);
				}
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

	private WebResponse makeWebResponseForAboutUrl(final URL url) {
		final String urlWithoutQuery = StringUtils.substringBefore(url.toExternalForm(), "?");
		if (!"blank".equalsIgnoreCase(StringUtils.substringAfter(urlWithoutQuery, "about:"))) {
			throw new IllegalArgumentException(url + " is not supported, only about:blank is supported now.");
		}
		return new StringWebResponse("", URL_ABOUT_BLANK);
	}

	private WebResponse makeWebResponseForFileUrl(final WebRequest webRequest) throws IOException {
		URL cleanUrl = webRequest.getUrl();
		if (cleanUrl.getQuery() != null) {
			// Get rid of the query portion before trying to load the file.
			cleanUrl = UrlUtils.getUrlWithNewQuery(cleanUrl, null);
		}
		if (cleanUrl.getRef() != null) {
			// Get rid of the ref portion before trying to load the file.
			cleanUrl = UrlUtils.getUrlWithNewRef(cleanUrl, null);
		}

		final File file = new File(cleanUrl.toExternalForm().substring(5));
		if (!file.exists()) {
			// construct 404
			final List<NameValuePair> compiledHeaders = new ArrayList<NameValuePair>();
			compiledHeaders.add(new NameValuePair("Content-Type", "text/html"));
			final WebResponseData responseData = new WebResponseData(TextUtil.stringToByteArray("File: " + file.getAbsolutePath()), 404, "Not Found", compiledHeaders);
			return new WebResponse(responseData, webRequest, 0);
		}

		final String contentType = guessContentType(file);

		final DownloadedContent content = new DownloadedContent.OnFile(file, false);
		final List<NameValuePair> compiledHeaders = new ArrayList<NameValuePair>();
		compiledHeaders.add(new NameValuePair("Content-Type", contentType));
		final WebResponseData responseData = new WebResponseData(content, 200, "OK", compiledHeaders);
		return new WebResponse(responseData, webRequest, 0);
	}

	private WebResponse makeWebResponseForDataUrl(final WebRequest webRequest) throws IOException {
		final URL url = webRequest.getUrl();
		final List<NameValuePair> responseHeaders = new ArrayList<NameValuePair>();
		DataUrlDecoder decoder;
		try {
			decoder = DataUrlDecoder.decode(url);
		} catch (final DecoderException e) {
			throw new IOException(e.getMessage());
		}
		responseHeaders.add(new NameValuePair("content-type", decoder.getMediaType() + ";charset=" + decoder.getCharset()));
		final DownloadedContent downloadedContent = HttpWebConnection.downloadContent(url.openStream());
		final WebResponseData data = new WebResponseData(downloadedContent, 200, "OK", responseHeaders);
		return new WebResponse(data, url, webRequest.getHttpMethod(), 0);
	}

}
