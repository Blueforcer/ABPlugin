package com.ab.abplugin;

import java.util.LinkedHashMap;
import java.util.Map;

import anywheresoftware.b4a.BA.Hide;

@Hide
public class ABPluginDefinition {
	protected Class<?> objectClass=null;
	protected Object object;
	protected String Name="";
	protected String NiceName="";
	protected long lastModified=0;
	
	public Map<String,java.lang.reflect.Method> Methods = new LinkedHashMap<String,java.lang.reflect.Method>();
}
