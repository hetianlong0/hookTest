package com.lingcloud.apptrace.sdk;

import java.util.concurrent.LinkedBlockingQueue;


public class TaskQueue {
	private LinkedBlockingQueue<String> _queue = new LinkedBlockingQueue<String>();
	
	private static class SingletonHolder {
		static final TaskQueue instance = new TaskQueue();
	}

	/**
	 * Returns the DclingCloudAgent singleton.
	 */
	public static TaskQueue getInstance() {
		return SingletonHolder.instance;
	}

	public void addTask(String task) {
		try {
			_queue.put(task);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String takeTask() {
		String ret = null;
		try {
			ret = _queue.take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}
	
	public int getCount() {
		int count = _queue.size();
		return count;
	}
}
