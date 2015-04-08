package com.newcrawler.plugin.urlfetch.js;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URLEncodedUtils;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.StringWebResponse;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

public class UrlFetchNoErrorLogWebConnection implements WebConnection{
	
	/** Logging support. */
    private static final Log LOG = LogFactory.getLog(UrlFetchNoErrorLogWebConnection.class);

    private static final String[] GAE_URL_HACKS = {"http://gaeHack_javascript/", "http://gaeHack_data/",
        "http://gaeHack_about/"};

    private final WebClient webClient_;

    /**
     * Creates a new web connection instance.
     * @param webClient the WebClient that is using this connection
     */
    public UrlFetchNoErrorLogWebConnection(final WebClient webClient) {
        webClient_ = webClient;
    }

    /**
     * {@inheritDoc}
     */
    public WebResponse getResponse(final WebRequest webRequest) throws IOException {
        final long startTime = System.currentTimeMillis();
        final URL url = webRequest.getUrl();
        if (LOG.isTraceEnabled()) {
            LOG.trace("about to fetch URL " + url);
        }

        // hack for JS, about, and data URLs.
        final WebResponse response = produceWebResponseForGAEProcolHack(url);
        if (response != null) {
            return response;
        }

        // this is a "normal" URL
        try {
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setUseCaches(false);
            connection.setConnectTimeout(4000);//连接超时4秒
            connection.setReadTimeout(webClient_.getOptions().getTimeout());
            
            connection.addRequestProperty("User-Agent", webClient_.getBrowserVersion().getUserAgent());
            connection.setInstanceFollowRedirects(false);

            // copy the headers from WebRequestSettings
            for (final Entry<String, String> header : webRequest.getAdditionalHeaders().entrySet()) {
                connection.addRequestProperty(header.getKey(), header.getValue());
            }
            addCookies(connection);

            final HttpMethod httpMethod = webRequest.getHttpMethod();
            connection.setRequestMethod(httpMethod.name());
            if (HttpMethod.POST == httpMethod || HttpMethod.PUT == httpMethod) {
                connection.setDoOutput(true);
                final String charset = webRequest.getCharset();
                connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                final OutputStream outputStream = connection.getOutputStream();
                try {
                    final List<NameValuePair> pairs = webRequest.getRequestParameters();
                    final org.apache.http.NameValuePair[] httpClientPairs = NameValuePair.toHttpClient(pairs);
                    final String query = URLEncodedUtils.format(Arrays.asList(httpClientPairs), charset);
                    outputStream.write(query.getBytes(charset));
                    if (webRequest.getRequestBody() != null) {
                        IOUtils.write(webRequest.getRequestBody().getBytes(charset), outputStream);
                    }
                }
                finally {
                    outputStream.close();
                }
            }

            final int responseCode = connection.getResponseCode();
            if (LOG.isTraceEnabled()) {
                LOG.trace("fetched URL " + url);
            }

            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            for (final Map.Entry<String, List<String>> headerEntry : connection.getHeaderFields().entrySet()) {
                final String headerKey = headerEntry.getKey();
                if (headerKey != null) { // map contains entry like (null: "HTTP/1.1 200 OK")
                    final StringBuilder sb = new StringBuilder();
                    for (final String headerValue : headerEntry.getValue()) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(headerValue);
                    }
                    headers.add(new NameValuePair(headerKey, sb.toString()));
                }
            }

            final InputStream is = responseCode < 400 ? connection.getInputStream() : connection.getErrorStream();
            final byte[] byteArray;
            try {
                byteArray = IOUtils.toByteArray(is);
            }
            finally {
                is.close();
            }
            final long duration = System.currentTimeMillis() - startTime;
            final WebResponseData responseData = new WebResponseData(byteArray, responseCode,
                connection.getResponseMessage(), headers);
            saveCookies(url.getHost(), headers);
            return new WebResponse(responseData, webRequest, duration);
        }
        catch (final IOException e) {
        	//LOG.error("Exception while tyring to fetch " + url, e);
            throw new RuntimeException(e);
        }
    }

    private void addCookies(final HttpURLConnection connection) {
        final StringBuilder cookieHeader = new StringBuilder();
        final Set<Cookie> cookies = webClient_.getCookieManager().getCookies();
        if (cookies.isEmpty()) {
            return;
        }

        int cookieNb = 1;
        for (Cookie cookie : webClient_.getCookieManager().getCookies()) {
            cookieHeader.append(cookie.getName()).append('=').append(cookie.getValue());
            if (cookieNb < cookies.size()) {
                cookieHeader.append("; ");
            }
            cookieNb++;
        }
        connection.setRequestProperty("Cookie", cookieHeader.toString());
    }

    private void saveCookies(final String domain, final List<NameValuePair> headers) {
        for (final NameValuePair nvp : headers) {
            if ("Set-Cookie".equalsIgnoreCase(nvp.getName())) {
                final Set<Cookie> cookies = parseCookies(domain, nvp.getValue());
                for (Cookie cookie : cookies) {
                    webClient_.getCookieManager().addCookie(cookie);
                }
            }
        }
    }

    private WebResponse produceWebResponseForGAEProcolHack(final URL url) {
        final String externalForm = url.toExternalForm();
        for (String pattern : GAE_URL_HACKS) {
            final int index = externalForm.indexOf(pattern);
            if (index == 0) {
                String contentString = externalForm.substring(pattern.length());
                if (contentString.startsWith("'") && contentString.endsWith("'")) {
                    contentString = contentString.substring(1, contentString.length() - 1);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("special handling of URL, returning (" + contentString + ") for URL " + url);
                }
                return new StringWebResponse(contentString, url);
            }
        }
        return null;
    }

    /**
     * Parses the given string into cookies.
     * Very limited implementation.
     * All created cookies apply to all paths, never expire and are not secure.
     * Will not work when there's a comma in the cookie value (because there's a bug in the Url Fetch Service)
     * @see "http://code.google.com/p/googleappengine/issues/detail?id=3379"
     * @param cookieHeaderString The cookie string to parse
     * @param domain the domain of the current request
     * @return The parsed cookies
     */
    static Set<Cookie> parseCookies(final String domain, final String cookieHeaderString) {
        final Set<Cookie> cookies = new HashSet<Cookie>();
        final String[] cookieStrings = cookieHeaderString.split(",");
        for (int i = 0; i < cookieStrings.length; i++) {
            final String[] nameAndValue = cookieStrings[i].split(";")[0].split("=");
            if (nameAndValue.length > 1) {
                cookies.add(new Cookie(domain, nameAndValue[0], nameAndValue[1]));
            }
        }
        return cookies;
    }
}
