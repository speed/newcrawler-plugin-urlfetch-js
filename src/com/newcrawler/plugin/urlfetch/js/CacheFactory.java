package com.newcrawler.plugin.urlfetch.js;



public class CacheFactory {

	private volatile static CacheFactory cacheFactory;
	private static final int _maxSize=10000;
	private static CustomCache _cache;
	
	private CacheFactory(){
		if(_cache==null){
			System.out.println("初始化 JAVASCRIPT 缓存。");
			_cache = new CustomCache();
			_cache.setMaxSize(_maxSize);
		}
	}
	public final static CacheFactory getInstance(){
		if(cacheFactory==null){
			synchronized(CacheFactory.class){
				if(cacheFactory==null){
					cacheFactory=new CacheFactory();
				}
			}
		}
		return cacheFactory;
	}

	public CustomCache getCache() {
		return _cache;
	}
	
	
}
