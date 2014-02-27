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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;

public class MountManager {
	private final Context _context;
	
	private List<Mount> _mounts = null;
	private MountManagerListener _delegate = null;
	
	public MountManager(Context context) {
		this._context = context;
	}
	
	public void setFoundMountPointListener(MountManagerListener delegate) {
		_delegate = delegate;
	}
	
	public void getMountPoints() {
		(new RuntimeHelper("mounts")).execute();
	}
	
	public void getDebugInfo() {
		(new RuntimeHelper("debug")).execute();		
	}
	
	private void generateMountPoints() {
		if (_delegate != null) {
			_delegate.hasFoundMountPoints(_mounts);
		}
	}
	
	// - Threaded Asynchronous Task to handle runtime processes.
	private class RuntimeHelper extends AsyncTask<String, Void, List<Mount>> {
		private final String _tag;
		
		RuntimeHelper(String tag) {
			_tag = tag;
		}
		
		@Override
		protected List<Mount> doInBackground(String... params) {
			List<Mount> results = new ArrayList<Mount>();
			
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
			    BufferedReader br = new BufferedReader(isr);
			    String line;
			    try {
			        while ((line = br.readLine()) != null) {
			        	results.add(new Mount(_context, line));
			        }
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
		    }
		    
			return results;
		}
		
		protected void onPostExecute(List<Mount> result) {
			if (_tag == "mounts") {
				List<Mount> cleanedResults = new ArrayList<Mount>();
				Iterator<Mount> mounts = result.iterator();
				
				while(mounts.hasNext()) {
					Mount mount = mounts.next();
					mount.generateName();
					
					if (mount.name != null) {
						mount.getSpaceInformation();
						cleanedResults.add(mount);
					}
				}
				
				_mounts = cleanedResults;
				generateMountPoints();
			}
			else {
				if (_delegate != null) {
					_delegate.hasFoundMountPointsDebugInfo(System.getenv("EXTERNAL_STORAGE"), System.getenv("SECONDARY_STORAGE"), result);
				}
			}
		}
	}
}
