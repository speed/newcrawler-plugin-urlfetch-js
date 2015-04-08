package com.newcrawler.plugin.urlfetch.js;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonUtil<T> {
	final Type type;
	protected JsonUtil() {
	    this.type = getSuperclassTypeParameter(getClass());
	}
	/**
	 * 从JSON到对象
	 * @param <T>
	 * @param json
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T fromJsonToObject(String json){
		Gson gson = new Gson();
		return (T)gson.fromJson(json, type);
	}
	
	@SuppressWarnings("unchecked")
	public T fromJsonToObject(String json, GsonBuilder gsonBuilder){
		Gson gson = gsonBuilder.create();
		return (T)gson.fromJson(json, type);
	}
	
	public static Object fromJsonToObject(String json, Type type){
		Gson gson = new Gson();
		return gson.fromJson(json, type);
	}
	
	/**
	 * 从对象到JSON
	 * @param <T>
	 * @param t
	 * @return
	 */
	public static String fromObjectToJson(Object obj){
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.serializeNulls();
		Gson gson = gsonBuilder.create();
		return gson.toJson(obj);
	}
	/**
	 * GsonBuilder gson = new GsonBuilder();
	 * gson.registerTypeAdapter(MyType2.class, new MyTypeAdapter());
	 * gson.registerTypeAdapter(MyType.class, new MySerializer());
	 * gson.registerTypeAdapter(MyType.class, new MyDeserializer());
	 * gson.registerTypeAdapter(MyType.class, new MyInstanceCreator());
	 * @param <T>
	 * @param t
	 * @param gsonBuilder
	 * @return
	 */
	public static String fromObjectToJson(Object obj, GsonBuilder gsonBuilder){
		Gson gson = gsonBuilder.create();
		return gson.toJson(obj);
	}
	
	static Type getSuperclassTypeParameter(Class<?> subclass) {
		Type superclass = subclass.getGenericSuperclass();
		ParameterizedType parameterized = (ParameterizedType) superclass;
		return parameterized.getActualTypeArguments()[0];
	}
}
