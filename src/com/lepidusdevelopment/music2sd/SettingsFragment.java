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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import eu.chainfire.libsuperuser.Shell;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, MountManagerListener, OnItemClickListener {
	private Map<String, File> _externalLocations = new HashMap<String, File>();
	private List<String> _mountpoints = new ArrayList<String>();
	private Preference pref;
	private AlertDialog _currentDialog = null;
	
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
		
		((Preference)findPreference("music2sd")).setOnPreferenceClickListener(this);
		((Preference)findPreference("xposed")).setOnPreferenceClickListener(this);
		((Preference)findPreference("sdcard")).setOnPreferenceClickListener(this);
		((Preference)findPreference("support")).setOnPreferenceClickListener(this);
		
		if (android.os.Build.VERSION.SDK_INT >= 19) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.error);
			builder.setMessage(R.string.kitkat_above);
			builder.setCancelable(false);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					getActivity().finish();
				}
			});
			builder.show();
		}
		else {
			(new RootHelper()).execute();
		}
	}
	
	// - OnPreferenceClickListener Methods
	@Override
    public boolean onPreferenceClick(Preference arg0) {
		if (arg0.getKey().equalsIgnoreCase("path")) {
			_externalLocations.clear();
			_mountpoints.clear();
			
			MountManager mm = new MountManager(this.getActivity().getBaseContext());
			mm.setFoundMountPointListener(this);
			mm.getMountPoints();
		}
		else if (arg0.getKey().equalsIgnoreCase("music2sd")) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/showthread.php?t=2414467"));
			startActivity(browserIntent);
		}
		else if (arg0.getKey().equalsIgnoreCase("xposed")) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://forum.xda-developers.com/showthread.php?t=1574401"));
			startActivity(browserIntent);
		}
		else if (arg0.getKey().equalsIgnoreCase("sdcard")) {
			MountManager mm = new MountManager(this.getActivity().getBaseContext());
			mm.setFoundMountPointListener(this);
			mm.getDebugInfo();
		}
		else if (arg0.getKey().equalsIgnoreCase("support")) {
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("message/rfc822");
			i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"steven@lepidusdevelopment.com"});
			i.putExtra(Intent.EXTRA_SUBJECT, "[MUSIC2SD] Support");
			try {
			    startActivity(Intent.createChooser(i, "Send mail..."));
			} catch (android.content.ActivityNotFoundException ex) {
			    Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
			}
		}
		
		return true;
	}
	
	// - OnItemClickListener Methods
	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
		File path = _externalLocations.get(_mountpoints.get(pos));
		
		if (path != null) {
			path = new File(path, "Android/data/com.google.android.music/files");
			if (!path.exists()) {
				(new RootHelper()).execute(path.getAbsolutePath());
				(new RootHelper()).execute((new File(path, "artwork")).getAbsolutePath());
				(new RootHelper()).execute((new File(path, "artwork2")).getAbsolutePath());
				(new RootHelper()).execute((new File(path, "artwork2/folder")).getAbsolutePath());
				(new RootHelper()).execute((new File(path, "music")).getAbsolutePath());
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
		
		if (_currentDialog != null) {
			_currentDialog.dismiss();
		}
	}
	
	// - MountManagerListener Methods
	@Override
	public void hasFoundMountPoints(List<Mount> mounts) {
		for (int i = 1; i <= 4; i++) {
			Iterator<Mount> mountIterator = mounts.iterator();
			while (mountIterator.hasNext()) {
				Mount mount = mountIterator.next();
				if (mount.priority == i) {
					_externalLocations.put(mount.name + ":" + mount.mount + ":" + mount.freeSpace + ":" + mount.totalSpace, mount.file);
					_mountpoints.add(mount.name + ":" + mount.mount + ":" + mount.freeSpace + ":" + mount.totalSpace);
				}
			}
		}
		
		ListView mountList = new ListView(this.getActivity().getBaseContext());
		ListViewAdapter adapter = new ListViewAdapter(this.getActivity().getBaseContext(), R.layout.list_view, _mountpoints.toArray(new String[_mountpoints.size()]));
		mountList.setAdapter(adapter);
		mountList.setOnItemClickListener(this);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.pref_path);
		builder.setView(mountList);
		_currentDialog = builder.show();
	}
	
	@Override
	public void hasFoundMountPointsDebugInfo(String external, String secondary, List<Mount> mounts) {
		StringBuilder body = new StringBuilder();
		body.append("OS Version:\r\n");
		body.append("\t" + System.getProperty("os.version") + "\r\n\r\n");
		body.append("API Level:\r\n");
		body.append("\t" + Build.VERSION.SDK_INT + "\r\n\r\n");
		body.append("Device:\r\n");
		body.append("\t" + Build.DEVICE + "\r\n\r\n");
		body.append("Model:\r\n");
		body.append("\t" + Build.MODEL + "\r\n\r\n");
		body.append("Product:\r\n");
		body.append("\t" + Build.PRODUCT + "\r\n\r\n");
		body.append("EXTERNAL_STORAGE Environment Variable:" + "\r\n");
		body.append("\t" + external + "\r\n\r\n");
		body.append("SECONDARY_STORAGE Environment Variable:" + "\r\n");
		body.append("\t" + secondary + "\r\n\r\n");
		body.append("MOUNT Command:" + "\r\n");
		
		Iterator<Mount> mountIterator = mounts.iterator();
		while (mountIterator.hasNext()) {
			Mount mount = mountIterator.next();
			body.append("\t" + mount.mountLine + "\r\n");
		}
		
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"steven@lepidusdevelopment.com"});
		i.putExtra(Intent.EXTRA_SUBJECT, "[MUSIC2SD] Support");
		i.putExtra(Intent.EXTRA_TEXT, body.toString());
		try {
		    startActivity(Intent.createChooser(i, "Send mail..."));
		} catch (android.content.ActivityNotFoundException ex) {
		    Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
		}
	}
	
	// - Private Methods
	private boolean isPackageExists(String targetPackage) {
		PackageManager pm = this.getActivity().getPackageManager();
		try{pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);}
		catch (NameNotFoundException e){return false;}
		return true;
	}

	// - Threaded Asynchronous Task to handle SuperUser tasks.
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
			if (result == 1) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
	}

	private class ListViewAdapter extends ArrayAdapter<String> {
		private final String[] _items;
		
		public ListViewAdapter(Context context, int textViewResourceId, String[] items) {
			super(context, textViewResourceId, items);
			_items = items;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			
			if (v == null) {
				LayoutInflater vi = LayoutInflater.from(getContext());
				v = vi.inflate(R.layout.list_view, null);
			}
			
			String[] info = _items[position].split(":");
			
			TextView nameTextView = (TextView) v.findViewById(R.id.list_name);
			TextView pathTextView = (TextView) v.findViewById(R.id.list_path);
			TextView diskSpaceTextView = (TextView) v.findViewById(R.id.list_diskspace);
			
			nameTextView.setText(info[0]);
			pathTextView.setText(info[1]);
			if (info[2].equalsIgnoreCase("unk") || info[3].equalsIgnoreCase("unk")) {
				diskSpaceTextView.setText("");
			}
			else {
				diskSpaceTextView.setText(info[2] + "/" + info[3]);
			}
			
			return v;
		}
	}
}
