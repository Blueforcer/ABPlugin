package com.ab.abplugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
//import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BA.Author;
import anywheresoftware.b4a.BA.DesignerName;
import anywheresoftware.b4a.BA.RaisesSynchronousEvents;
import anywheresoftware.b4a.BA.ShortName;
import anywheresoftware.b4a.BA.Version;

@DesignerName("Build 20200210")                                    
@Version(1.26F)                                
@Author("Alain Bailleul")
@ShortName("ABPlugin") 

public class ABPlugin {	
	protected BA _ba;
	protected String _event;
	private String pluginsDir;
	private Map<String, ABPluginDefinition> plugins = new LinkedHashMap<String, ABPluginDefinition>(); 
	protected boolean mPluginIsRunning=false;
	protected boolean mIsLoading=false;
	protected ScheduledExecutorService service = null;
	protected Future<?> future = null;
	protected long CheckForNewIntervalMS=0;
	private static ClassLoader parentClassLoader;
	private static String AllowedKey="";
	private static boolean PluginOK=false;
	
	private static ABPluginDefinition def=null;
	
	public boolean AllowOtherKeys=false;
	
	public void Initialize(BA ba, String eventName, String pluginsDir, String allowedKey) {
		this._ba = ba;
		this._event = eventName.toLowerCase(BA.cul);
		this.pluginsDir = pluginsDir;
		AllowedKey=allowedKey;
		parentClassLoader = Thread.currentThread().getContextClassLoader();
		File f = new File(pluginsDir);
		if (!f.exists()) {
			try {
				Files.createDirectories(Paths.get(pluginsDir));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}				
	}	
	
	
	@RaisesSynchronousEvents
	public anywheresoftware.b4a.objects.collections.List CheckForNewPlugins() {
		Map<String, Boolean> toRemove = new LinkedHashMap<String,Boolean>();
		List<String> toAdd = new ArrayList<String>();
		for (Entry<String,ABPluginDefinition> entry: plugins.entrySet()) {
			toRemove.put(entry.getKey(), true);
		}	
		File dh = new File(pluginsDir);
		for (File f: dh.listFiles()) {
			if (f.getName().endsWith(".jar")) {
				String pluginName = f.getName().substring(0, f.getName().length()-4).toLowerCase();
				toRemove.remove(pluginName);
				if (!plugins.containsKey(pluginName)) {
					toAdd.add(f.getAbsolutePath());    						
				} else {
					ABPluginDefinition def = plugins.get(pluginName);
					if (f.lastModified()!=def.lastModified) {
						toRemove.put(pluginName, true);    							 
					}
				}
			}
		}
		if (toRemove.size()>0) {
			toAdd = new ArrayList<String>();
			for (Entry<String,ABPluginDefinition> entry: plugins.entrySet()) {
				entry.getValue().objectClass = null;
			}
			plugins = new LinkedHashMap<String, ABPluginDefinition>();
			for (File f: dh.listFiles()) {
				if (f.getName().endsWith(".jar")) {
					toAdd.add(f.getAbsolutePath());        					
				}
			}
		}
		
		for (int i=0;i<toAdd.size();i++) {	    			
			File f = new File(toAdd.get(i));
			ABPluginDefinition def = new ABPluginDefinition();
			def.lastModified = f.lastModified();
			if (loadJarFile(pluginsDir, f, parentClassLoader, def)) {
				PluginOK=false;
				RunInitialize(def);
				if (PluginOK ){
					Object ret;
					if (def.Methods.containsKey("_getnicename")) {			
						try {
							ret = def.Methods.get("_getnicename").invoke(def.object, new Object[] {});
							mPluginIsRunning=false;				
							def.NiceName = (String)ret;
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							e.printStackTrace();
						}				
					} else {
						Class<?> clazz = def.objectClass;
						while (clazz != null) {
							java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
						    for (java.lang.reflect.Method method : methods) {		     
						        if (method.getName().equals("_getnicename")) {
						        	method.setAccessible(true);
						        	def.Methods.put("_getnicename", method);
						        	try {
										ret = method.invoke(def.object, new Object[] {});
										mPluginIsRunning=false;
										clazz = null;
										def.NiceName = (String)ret;
									} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
										e.printStackTrace();
									}						
						        }
						    }
						    if (clazz!=null) {
						    	clazz = clazz.getSuperclass();
						    }
						}
					}
					plugins.put(def.Name.toLowerCase(), def); 						
				} else {
					if (AllowOtherKeys) {
						Object ret;
						if (def.Methods.containsKey("_getnicename")) {			
							try {
								ret = def.Methods.get("_getnicename").invoke(def.object, new Object[] {});
								mPluginIsRunning=false;				
								def.NiceName = (String)ret;
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								e.printStackTrace();
							}				
						} else {
							Class<?> clazz = def.objectClass;
							while (clazz != null) {
								java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
							    for (java.lang.reflect.Method method : methods) {		     
							        if (method.getName().equals("_getnicename")) {
							        	method.setAccessible(true);
							        	def.Methods.put("_getnicename", method);
							        	try {
											ret = method.invoke(def.object, new Object[] {});
											mPluginIsRunning=false;
											clazz = null;
											def.NiceName = (String)ret;
										} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
											e.printStackTrace();
										}						
							        }
							    }
							    if (clazz!=null) {
							    	clazz = clazz.getSuperclass();
							    }
							}
						}
						plugins.put(def.Name.toLowerCase(), def); 
					}
				}
			}
		}    
		
		BA.Log("Active Apps: " + toAdd.size());	
			//_ba.raiseEvent(this, _event + "_pluginschanged", new Object[] {});
		anywheresoftware.b4a.objects.collections.List ret = new anywheresoftware.b4a.objects.collections.List();
		ret.Initialize();
		for (Entry<String,ABPluginDefinition> entry : plugins.entrySet()) {
			ret.Add(entry.getValue().NiceName);
		}
		return ret;
	}
	
	private void RunInitialize(ABPluginDefinition def) {	
	    java.lang.reflect.Method m;	
	    try {	
			m = def.objectClass.getMethod("_initialize", new Class[]{anywheresoftware.b4a.BA.class});
			m.setAccessible(true);
			boolean ret = (AllowedKey==(String) m.invoke(def.object, new Object[] {_ba}));
			PluginOK = ret;
			return;	
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return;
		
	}
	
	public Object RunPlugin(String pluginNiceName, String tag, anywheresoftware.b4a.objects.collections.Map params) {		
		mPluginIsRunning=true;
		def=null;
		for (Entry<String,ABPluginDefinition> entry: plugins.entrySet()) {
			if (entry.getValue().NiceName.equalsIgnoreCase(pluginNiceName)) {
				def = entry.getValue();
				break;
			}
		}		
		if (def==null) {
			BA.Log("No App found with name: '" + pluginNiceName + "'");
			mPluginIsRunning=false;
			return null;
		}
		Object ret;
		if (def.Methods.containsKey("_run")) {			
			try {
				ret = def.Methods.get("_run").invoke(def.object, new Object[] {tag, params});
				def=null;
				mPluginIsRunning=false;				
				return ret;
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}				
		} else {
			Class<?> clazz = def.objectClass;
			while (clazz != null) {
				java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
			    for (java.lang.reflect.Method method : methods) {		     
			        if (method.getName().equals("_run")) {
			        	method.setAccessible(true);
			        	def.Methods.put("_run", method);
			        	try {
							ret = method.invoke(def.object, new Object[] {tag, params});
							def=null;
							clazz = null;
							mPluginIsRunning=false;
							return ret;
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							e.printStackTrace();
						}						
			        }
			    }
			    clazz = clazz.getSuperclass();
			}
		}
		return null;
	}	
		
	private boolean loadJarFile(String directoryName, File pluginFile, ClassLoader parentClassLoader, ABPluginDefinition def) {
		BA.Log("AWTRIX App " + pluginFile.getName() + " found");
        URL url = null;
        try {
            url = new URL("jar:file:" + directoryName + "/" + pluginFile.getName() + "!/");
        } catch (MalformedURLException e) {
            BA.Log("URL '" + url + "': " + e);
            return false;
        }
        URL[] urls = { url };
 
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(pluginFile);
        } catch (IOException e) {
            BA.Log("JAR file '" + pluginFile.getName() + "': " + e);
            return false;
        }
        def.Name = pluginFile.getName().substring(0, pluginFile.getName().length()-4);
 
        // go through JAR file contents
        List<String> classes = new ArrayList<String>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = (JarEntry) entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }
            classes.add(entry.getName().replace(".class", "").replace('/', '.'));            
        }
        
        def.objectClass=null;
        
        
        try (URLClassLoader classLoader = new URLClassLoader(urls, parentClassLoader)) {
        	for (int i=0;i<classes.size();i++) {            		
        		if (classes.get(i).toLowerCase().endsWith(def.Name.toLowerCase())) {
        			def.objectClass = classLoader.loadClass(classes.get(i));
        			def.object = def.objectClass.newInstance();
        		} else {
        			classLoader.loadClass(classes.get(i));
        		}
        	}
        } catch (ClassNotFoundException | IOException | InstantiationException | IllegalAccessException e) {
            BA.Log("" + e);
            try {
                jarFile.close();
            } catch (IOException eClose) {
            	BA.Log("Attempting to close JAR file: " + eClose);
            	return false;
            }
        }
              
 
        try {
            jarFile.close();
        } catch (IOException e) {
        	BA.Log("Attempting to close JAR file: " + e);
        	return false;
        }
        BA.Log("Loading Apps finished");
        return true;
	}

}
