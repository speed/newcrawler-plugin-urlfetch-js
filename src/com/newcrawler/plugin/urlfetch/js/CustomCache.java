package com.newcrawler.plugin.urlfetch.js;

import java.util.Date;

import com.gargoylesoftware.htmlunit.Cache;
import com.gargoylesoftware.htmlunit.WebResponse;

public class CustomCache extends Cache {

	private static final long serialVersionUID = -4491431390629862371L;

	protected boolean isDynamicContent(final WebResponse response) {
		final Date lastModified = parseDateHeader(response, "Last-Modified");
		final Date expires = parseDateHeader(response, "Expires");

		final long delay = 10 * org.apache.commons.lang3.time.DateUtils.MILLIS_PER_MINUTE;
		final long now = getCurrentTimestamp();

		
		Boolean isCache=false;
		Object obj=response.getWebRequest().getAdditionalHeaders().get("isCache");
		if(obj!=null){
			isCache=Boolean.valueOf(obj.toString());
		}
		
		final boolean cacheableContent = (isCache || 
				(expires != null && (expires.getTime() - now > delay))
				|| (expires == null && lastModified != null && (now - lastModified.getTime() > delay))
				);
		
		
		return !cacheableContent;
	}

}
