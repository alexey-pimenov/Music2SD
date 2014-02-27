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

import android.content.Context;
import android.os.Environment;
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
		if (lpparam.packageName.equals("com.google.android.music")) {
			ClassLoader classLoader = lpparam.classLoader;
			
			XC_MethodReplacement CacheUtils_getCacheDirectory = new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					Context paramContext = ((Context) param.args[0]);
					String paramString = ((String) param.args[1]);

					File path = getSDCardPath(paramContext, paramString);
					XposedBridge.log("MUSIC2SD: " + path.getAbsolutePath());
					
					return path;
				}
		    };
		    			
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheLocationManager", classLoader, "deviceHasExternalStorage", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable  {
					param.setResult(true);
				}
		    });
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "getExternalCacheDirectory", Context.class, String.class, CacheUtils_getCacheDirectory);		
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "getInternalCacheDirectory", Context.class, String.class, CacheUtils_getCacheDirectory);		
		    
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheUtils", classLoader, "getSelectedVolumeMusicCacheDirectory", Context.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable  {
					Context paramContext = ((Context) param.args[0]);
					String paramString = "music";

					File path = getSDCardPath(paramContext, paramString);
					param.setResult(path);
				}
		    });		

		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheLocation", classLoader, "getCacheFile", String.class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable  {
					String pathString = prefs.getString("path", "");
					String paramString = (String) param.args[0];
					if (!pathString.equalsIgnoreCase("")) {
						param.setResult(new File(pathString, paramString));
					}
				}
		    });		
		    XposedHelpers.findAndHookMethod("com.google.android.music.download.cache.CacheLocation", classLoader, "getPath", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable  {
					String pathString = prefs.getString("path", "");
					if (!pathString.equalsIgnoreCase("")) {
						param.setResult(new File(pathString));
					}
				}
		    });		
		}
//		else if (lpparam.packageName.equals("com.google.android.gsf")) {
//			ClassLoader classLoader = lpparam.classLoader;
//			
//			 XposedHelpers.findAndHookMethod("com.google.android.gsf.Gservices", classLoader, "getBoolean", ContentResolver.class, String.class, Boolean.class, new XC_MethodHook() {
//				@Override
//				protected void afterHookedMethod(MethodHookParam param) throws Throwable  {
//					if (((String) param.args[1]).equalsIgnoreCase("music_enable_secondary_sdcards")) {
//						param.setResult(true);
//					}
//				}
//		    });	
//		}
	}
	
	private File getSDCardPath(Context paramContext, String paramString) {
		File path = null;
		String pathString = prefs.getString("path", "");
		
		// if path is empty then use Internal Storage.
		if (!pathString.isEmpty()) {
			path = new File(pathString);
			
			if (path.exists()) {
				if (paramString != null)
					path = new File(pathString, paramString);
				
				return path;
			}
			// Something is wrong lets try Device Storage and if that doesn't work fallback to Internal Storage.
			else {
				if ("mounted".equals(Environment.getExternalStorageState())) {
					path = paramContext.getExternalFilesDir(null);

					if (path != null) {
						if (paramString != null)
							path = new File(path, paramString);

						return path;
					}
				}
			}
		}

		path = paramContext.getFilesDir();
		
		if (path != null) {
			if (paramString != null)
				path = new File(path, paramString);

			return path;
		}
		
		return null;
	}	
}
