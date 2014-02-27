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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

public class Mount {
	private final Context _context;
	public String name;
	public File file;
	public int priority;

	// Mount Command Information
	public String device;
	public String mount;
	public String filesystem;
	public String[] options;
	public String mountLine;
	
	// DF Command Information
	public String totalSpace;
	public String usedSpace;
	public String freeSpace;
	public int blockSize;
	
	public Mount(Context context, String mountLine) {
		this._context = context;
		this.mountLine = mountLine;
		
		if (mountLine.contains(" ")) {
			String[] params = mountLine.split(" ");
			
			if (params.length >= 4) {
				// Mount Commands with "on" nomenclature. 
				if (params[1].equalsIgnoreCase("on")) {
					// Mount Commands with "on" and "type" nomenclature.
					if (params[3].equalsIgnoreCase("type") && params.length >= 6) {
						this.device = params[0];
						this.mount = params[2];
						this.filesystem = params[4];
						
						if (params[5].charAt(0) == '(') {
							this.options = params[5].substring(1, params[5].length() - 2).split(",");
						}
						else {
							this.options = params[5].split(",");
						}
					}
					else if (params.length >= 4) {
						this.device = params[0];
						this.mount = params[2];
						this.filesystem = null;
						
						if (params[3].charAt(0) == '(') {
							this.options = params[3].substring(1, params[3].length() - 2).split(",");
						}
						else {
							this.options = params[3].split(",");
						}
					}
				}
				else {
					// Mount command without "on", but with "type" nomenclature. (Not sure if this will ever happen, but heck why not support it.)
					if (params[2].equalsIgnoreCase("type") && params.length >= 5) {
						this.device = params[0];
						this.mount = params[1];
						this.filesystem = params[3];
						
						if (params[4].charAt(0) == '(') {
							this.options = params[4].substring(1, params[4].length() - 2).split(",");
						}
						else {
							this.options = params[4].split(",");
						}
					}
					// Mount command with Filesystem.
					else if (params.length >= 4) {
						this.device = params[0];
						this.mount = params[1];
						this.filesystem = params[2];
						
						if (params[3].charAt(0) == '(') {
							this.options = params[3].substring(1, params[3].length() - 2).split(",");
						}
						else {
							this.options = params[3].split(",");
						}
					}
					// Mount command without Filesystem.
					else if (params.length == 3) {
						this.device = params[0];
						this.mount = params[1];
						this.filesystem = null;
						
						if (params[2].charAt(0) == '(') {
							this.options = params[2].substring(1, params[2].length() - 2).split(",");
						}
						else {
							this.options = params[2].split(",");
						}
					}
				}
			}
		}
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public boolean getSpaceInformation() {
		try {
			StatFs stats = new StatFs(this.mount);
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) { 
				this.totalSpace = (stats.getTotalBytes() / 1073741824) + "GB";
				this.usedSpace = ((stats.getTotalBytes() - stats.getAvailableBytes()) / 1073741824) + "GB";
				this.freeSpace = (stats.getAvailableBytes() / 1073741824) + "GB";
			}
			else {
				long blockCount = stats.getBlockCount();
				long blockSize = stats.getBlockSize();
				long freeBlocks = stats.getFreeBlocks();
				
				this.totalSpace = (blockCount * blockSize / 1073741824) + "GB";
				this.usedSpace = (((blockCount - freeBlocks) * blockSize) / 1073741824) + "GB";
				this.freeSpace = (freeBlocks * blockSize / 1073741824) + "GB";
			}
		}
		catch (Exception e) {
			this.totalSpace = "unk";
			this.usedSpace = "unk";
			this.freeSpace = "unk";			
		}
		
		return true;
	}
		
	public void generateName() {
		// Is it the data partition?
		if (this.mount.equalsIgnoreCase("/data")) {
			this.name = this._context.getResources().getString(R.string.internal);
			this.priority = 1;
			this.file = null;
			return;
		}
		
		String[] device;
		String[] external;
		
		// Is it the built-in device storage?
		String result = System.getenv("EXTERNAL_STORAGE");
		if (result != null && result.contains(":")) {
			device = result.split(":");
		}
		else {
			if (result == null) {
				device = null;
			}
			else {
				device = new String[1];
				device[0] = result;
			}
		}
		
		if (device != null) {
			for (int i = 0; i < device.length; i++) {
				if (device[i].equalsIgnoreCase(this.mount)) {
					// Sometimes even though it's under external it may not be removable thus it's "Device Storage".
					if (Environment.isExternalStorageRemovable()) {
						this.name = this._context.getResources().getString(R.string.external);
						this.priority = 3;
						this.file = new File(this.mount);
						return;
					}
					else {
						this.name = this._context.getResources().getString(R.string.device);
						this.priority = 2;
						this.file = new File(this.mount);
						return;
					}
				}
			}
		}
		
		// Is it a SD Card or secondary storage?
		result = System.getenv("SECONDARY_STORAGE");
		if (result != null && result.contains(":")) {
			external = result.split(":");
		}
		else {
			if (result == null) {
				external = null;
			}
			else {
				external = new String[1];
				external[0] = result;
			}
		}
		
		if (external != null) {
			for (int i = 0; i < external.length; i++) {
				if (external[i].equalsIgnoreCase(this.mount)) {
					this.name = this._context.getResources().getString(R.string.external);
					this.priority = 3;
					this.file = new File(this.mount);
					return;
				}
			}
		}
		
		if (this.mountLine.matches("^(.*)rw(.*)nodev(.*)noexec(.*)$")) {
			this.name = this._context.getResources().getString(R.string.unknown);
			this.priority = 4;
			this.file = new File(this.mount);
			return;
		}
	}
}
