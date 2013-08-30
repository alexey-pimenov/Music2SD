package com.lepidusdevelopment.music2sd;

/**
 * Copyright (c) 2013, Lepidus Development LLC
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import eu.chainfire.libsuperuser.Shell;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, OnClickListener {
	private Map<String, File> _externalLocations = new HashMap<String, File>();
	private List<String> _mountpoints = new ArrayList<String>();
	private Preference pref;
	private Activity _activity;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.preferences);
		
		pref = ((Preference)findPreference("path"));
		pref.setOnPreferenceClickListener(this);
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		pref.setSummary(sharedPref.getString("path", "Internal Storage"));
		
		_activity = this.getActivity();
		(new RootHelper()).execute();
	}
	
	@Override
    public boolean onPreferenceClick(Preference arg0) {
		if (arg0.getKey().equalsIgnoreCase("path")) {
			buildExternalLocations();
									
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.pref_path);
			builder.setItems(_mountpoints.toArray(new CharSequence[_mountpoints.size()]), this);
			builder.show();
		}
		
		return true;
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		File path = _externalLocations.get(_mountpoints.get(arg1));
		
		if (path != null) {
			path = new File(path, "Android/data/com.google.android.music");
			if (!path.exists()) {
				(new RootHelper()).execute(path.getAbsolutePath());
			}
			
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
			Editor editor = sharedPref.edit();
			editor.putString("path", path.getAbsolutePath());
			editor.commit();
			
			pref.setSummary(path.getAbsolutePath());
		}
		else {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
			Editor editor = sharedPref.edit();
			editor.remove("path");
			editor.commit();
			
			pref.setSummary("Internal Storage");
		}
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
	}
	
	private boolean isPackageExists(String targetPackage) {
		PackageManager pm = this.getActivity().getPackageManager();
		
		try {
			PackageInfo info = pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return false;
		}
		
		return true;
	}
	
	private void buildExternalLocations() {
		_externalLocations.clear();
		_mountpoints.clear();
		
		if ("mounted".equals(Environment.getExternalStorageState())) {
			if (Environment.isExternalStorageRemovable()) {
				_externalLocations.put("Internal Storage", null);
				_externalLocations.put("Device Storage", Environment.getExternalStorageDirectory());
				
				_mountpoints.add("Internal Storage");
				_mountpoints.add("Device Storage");
			}
			else {
				List<String> externalstorage_path = getPathToExternalStorage();
				
				if (externalstorage_path.size() != 0) {
					_externalLocations.put("Internal Storage", null);
					_externalLocations.put("Device Storage", Environment.getExternalStorageDirectory());

					_mountpoints.add("Internal Storage");
					_mountpoints.add("Device Storage");
					
					ListIterator<String> it = externalstorage_path.listIterator();
					int index = 1;
					while (it.hasNext()) {
						File ext_storage = new File(it.next());

						if (ext_storage.exists() && !_externalLocations.containsValue(ext_storage)) {
							_externalLocations.put("External Storage " + index, ext_storage);
							_mountpoints.add("External Storage " + index);
							
							index++;
						}
					}
				}
				else {
					_externalLocations.put("Internal Storage", null);
					_externalLocations.put("Device Storage", Environment.getExternalStorageDirectory());
					
					_mountpoints.add("Internal Storage");
					_mountpoints.add("Device Storage");
				}
			}
		}
		else {
			_externalLocations.put("Internal Storage", null);
			
			_mountpoints.add("Internal Storage");
		}
	}
	
	private List<String> getPathToExternalStorage() {
		List<String> sdcardPath = new ArrayList<String>();
	    Runtime runtime = Runtime.getRuntime();
	    Process proc = null;
	    try {
	        proc = runtime.exec("mount");
	    } catch (IOException e1) {
	        e1.printStackTrace();
	    }
	    
	    if (proc != null) {
		    InputStream is = proc.getInputStream();
		    InputStreamReader isr = new InputStreamReader(is);
		    String line;
		    BufferedReader br = new BufferedReader(isr);
		    try {
		        while ((line = br.readLine()) != null) {
		            if (line.contains("fat") && line.contains("rw")) {
		                String columns[] = line.split(" ");
		                if (columns != null && columns.length > 1) {
		                	sdcardPath.add(columns[1]);
		                }
		            }
		        }
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
	    }
	    else {
	    	Toast.makeText(getActivity(), R.string.unable_search, Toast.LENGTH_SHORT).show();
	    }
	    
	    return sdcardPath;
	}
	
	private void showDialog(Integer result) {
		if (result == 1) {
			AlertDialog.Builder builder = new AlertDialog.Builder(_activity);
			builder.setTitle(R.string.error);
			builder.setMessage(R.string.root_access);
			builder.setCancelable(false);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					getActivity().finish();
				}
			});
			builder.show();
		}
		else if (result == 2) {
			AlertDialog.Builder builder = new AlertDialog.Builder(_activity);
			builder.setTitle(R.string.error);
			builder.setMessage(R.string.xposed_framework);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.download_xposed, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/showthread.php?t=1574401"));
					startActivity(browserIntent);
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					getActivity().finish();
				}
			});
			builder.show();
		}
	}
	
	private class RootHelper extends AsyncTask<String, Void, Integer> {
		@Override
		protected Integer doInBackground(String... params) {
			int count = params.length;
			
			// Copy folders.
			if (count == 2) {
				Shell.SU.run("cp -r " + params[0] + " " + params[1]);
			}
			// Create Directories
			else if (count == 1) {
				Shell.SU.run("mkdir -p " + params[0]);
			}
			// Check for Root
			else {
				if (!Shell.SU.available()) {
					return 1;
				}
				else {
					if (!isPackageExists("de.robv.android.xposed.installer")) {
						return 2;
					}
				}
			}

			return 0;
		}
		
		protected void onPostExecute(Integer result) {
			showDialog(result);
		}
	}
}
