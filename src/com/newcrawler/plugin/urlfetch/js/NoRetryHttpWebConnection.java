package com.newcrawler.plugin.urlfetch.js;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import com.gargoylesoftware.htmlunit.HttpWebConnection;
import com.gargoylesoftware.htmlunit.WebClient;

public class NoRetryHttpWebConnection extends HttpWebConnection{
	private static final String HACKED_COOKIE_POLICY = "mine";
	
	public NoRetryHttpWebConnection(WebClient webClient) {
		super(webClient);
	}
	
	protected HttpClientBuilder createHttpClient() {
        final HttpClientBuilder builder = super.createHttpClient();
        
        builder.disableAutomaticRetries().setMaxConnTotal(10);
        
        configureTimeout(builder, getTimeout());
        return builder;
    }
	
	private void configureTimeout(final HttpClientBuilder builder, final int timeout) {
        final RequestConfig.Builder requestBuilder = createRequestConfigBuilder(timeout);
        builder.setDefaultRequestConfig(requestBuilder.build());
        builder.setDefaultSocketConfig(createSocketConfigBuilder(timeout).build());
    }
	
	 private SocketConfig.Builder createSocketConfigBuilder(final int timeout) {
	        final SocketConfig.Builder socketBuilder = SocketConfig.custom()
	                // timeout
	                .setSoTimeout(timeout);
	        return socketBuilder;
	    }
	private RequestConfig.Builder createRequestConfigBuilder(final int timeout) {
        final RequestConfig.Builder requestBuilder = RequestConfig.custom()
                .setCookieSpec(HACKED_COOKIE_POLICY)
                .setRedirectsEnabled(false)

                // timeout
                .setConnectTimeout(4000)//连接超时4秒
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout);
        return requestBuilder;
    }

}
