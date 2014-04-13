package com.lepidusdevelopment.music2sd;

/**
 * Copyright (c) 2014, Lepidus Development LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 *   Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 *   Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.File;
import java.util.UUID;

import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Music2SDPatch implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	public static final String PACKAGE_NAME = Music2SDPatch.class.getPackage().getName();
	public static final String TAG = "Music2SDPatch";
	private static XSharedPreferences prefs;
	
	@Override
    public void initZygote(StartupParam startupParam) throws Throwable {
		prefs = new XSharedPreferences(PACKAGE_NAME);
	}
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("com.google.android.music") && android.os.Build.VERSION.SDK_INT < 19) {
			ClassLoader classLoader = lpparam.classLoader;
			
		    // Move all of our directories to the new location.
			XC_MethodReplacement CacheUtils_getCacheDirectory = new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					Context paramContext = ((Context) param.args[0]);
					String paramString = ((String) param.args[1]);
					return getSDCardPath(paramContext, paramString);
//					File debugFile = getSDCardPath(paramContext, paramString);
//					XposedBridge.log("CacheUtils_getCacheDirectory: " + debugFile.getAbsolutePath());
//					
//					return debugFile;
				}
		    };
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "getInternalCacheDirectory", Context.class, String.class, CacheUtils_getCacheDirectory);
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "getInternalCacheDirectory_Old", Context.class, String.class, CacheUtils_getCacheDirectory);
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "getExternalCacheDirectory", Context.class, String.class, CacheUtils_getCacheDirectory);
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "getExternalCacheDirectory_Old", Context.class, String.class, CacheUtils_getCacheDirectory);
		    
		    
		    // Weird methods that may or may not be using the above methods. Best to play it safe and overwrite it.
		    XC_MethodReplacement CacheUtils_musicCacheDirectory = new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					Context paramContext = ((Context) param.args[0]);
					return getSDCardPath(paramContext, "music");
//					File debugFile = getSDCardPath(paramContext, "music");
//					XposedBridge.log("CacheUtils_musicCacheDirectory: " + debugFile.getAbsolutePath());
//					
//					return debugFile;
				}
		    };
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "getMusicCacheDirectoryById", Context.class, UUID.class, CacheUtils_musicCacheDirectory);
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "getSelectedVolumeMusicCacheDirectory", Context.class, CacheUtils_musicCacheDirectory);
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "resolveMusicPath", Context.class, String.class, int.class, UUID.class, CacheUtils_musicCacheDirectory);
		    
		    
		    // Forces External Storage type.
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheLocationManager", classLoader, "deviceHasExternalStorage", XC_MethodReplacement.returnConstant(true));
		    Class<?> storageTypeClass = XposedHelpers.findClass("com.google.android.music.download.cache.CacheUtils.StorageType", classLoader);
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "getSchemaValueForStorageType", storageTypeClass, XC_MethodReplacement.returnConstant(2));
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "isExternalStorageMounted",  XC_MethodReplacement.returnConstant(true));
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "isVolumeMounted", UUID.class, Context.class,  XC_MethodReplacement.returnConstant(true));
		    		    
		    
		    // Modify any cache locations that may slip through.
		    XC_MethodHook CacheLocation_getFile = new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					File oldPath = (File) param.getResult();
					File newPath = null;
					String oldPathString = oldPath.getAbsolutePath();
					String newPathString = prefs.getString("path", "");
					
					if (!newPathString.isEmpty()) {
						newPath = new File(newPathString);
						
						if (newPath.exists() && oldPathString.indexOf(newPathString) == -1) {
							int pos = oldPathString.indexOf("/Android/data/com.google.android.music/files") + 44;

							if (pos != oldPathString.length()) {
								String revisedPathString = newPathString + oldPathString.substring(pos);
								newPath = new File(revisedPathString);
							}
							
//							XposedBridge.log("CacheLocation_getFile: newPath - " + newPath.getAbsolutePath());
							param.setResult(newPath);
							return;
						}
					}
					
//					XposedBridge.log("CacheLocation_getFile: oldPath - " + oldPath.getAbsolutePath());
					param.setResult(oldPath);
					return;
				}
		    };
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheLocation", lpparam.classLoader, "getCacheFile", String.class, CacheLocation_getFile);
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheLocation", lpparam.classLoader, "getPath", CacheLocation_getFile);
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheLocation", lpparam.classLoader, "hasMusicFiles", XC_MethodReplacement.returnConstant(true));	
		}
	}
	
	private File getSDCardPath(Context paramContext, String paramString) {
		File path = null;
		String pathString = prefs.getString("path", "");
		
		// If path is empty then use Internal Storage.
		if (!pathString.isEmpty()) {
			path = new File(pathString);
			
			if (path.exists()) {
				if (paramString != null)
					path = new File(pathString, paramString);
				
				return path;
			}
		}

		path = paramContext.getFilesDir();
		
		// If for some reason that fails then panic!
		if (path != null) {
			if (paramString != null)
				path = new File(path, paramString);

			return path;
		}
		
		return null;
	}	
}
