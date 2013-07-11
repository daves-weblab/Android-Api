package at.codecomb.android.application.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import at.codecomb.android.application.listener.ApplicationListener;


/*
 * Copyright (c) 2013, All Rights Reserved, file = Core.java
 * 
 * This source is subject to Code Comb. 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * This is a abstract pattern to create a class playing the role as {@link Application}. It uses
 * {@link Database} and {@link Networker} to perform storing and loading operations on a database
 * implemented in {@link Database} and networking operations implemented in {@link Networker}.
 * 
 * @author David Riedl (Code Comb)
 * @version 1.0
 */
@SuppressLint("HandlerLeak")
abstract public class Core extends Application {
	private static final String REQUEST_TYPE = "REQUEST_TYPE";

	private Database mDatabase;
	private Networker mNetworker;

	private static ListenerQueue mListenerQueue;

	private TreeMap<RequestType, ApplicationListener> mListener;
	private static Handler mHandler;

	@Override
	public void onCreate() {
		super.onCreate();
		mListener = new TreeMap<RequestType, ApplicationListener>();
		mListenerQueue = new ListenerQueue();
		setupHandler();

		setupCore();
		Database.setCore(this);
		Networker.setCore(this);
	}

	private void setupHandler() {
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				RequestType requestType = null;
				if (msg != null && (requestType = (RequestType) msg.getData().getSerializable(Core.REQUEST_TYPE)) != null) {
					ApplicationListener listener = getListener(requestType);
					if (listener != null) {
						listener.requestCompleted(requestType);
					}
				}
			}
		};
	}

	/* ------------------------------------- public methods ------------------------------------- */

	/**
	 * returns the achieved value by performing a request for the given listener
	 * 
	 * @param listener
	 *            the listener who asked for the request to be performed
	 * @return the achieved value
	 */
	public static Object getRequestValue(final ApplicationListener listener, final RequestType requestType) {
		ListenerValue<?> returnValue = mListenerQueue.get(listener, requestType);
		if (returnValue != null) {
			return returnValue.getRequestValue();
		}
		return null;
	}
	
	/**
	 * stores an object in the local database provided by the class extending {@link Database}
	 * 
	 * @param requestType
	 *            defines what the Database should do
	 * @param content
	 *            the content to be stored
	 */
	public <T> void store(final RequestType requestType, final T content) {
		mDatabase._store(requestType, content);
	}

	/**
	 * loads an object from the local database provided by the class extending {@link Database}
	 * 
	 * @param listener
	 *            the listener which will be informed once the object is loaded
	 * @param requestType
	 *            defines what the Database should do
	 */
	public void load(final ApplicationListener listener, final RequestType requestType) {
		addListener(requestType, listener);
		mDatabase._load(requestType);
	}

	/**
	 * loads an object from the local database provided by the class extending {@link Database}
	 * 
	 * @param listenerthe
	 *            listener which will be informed once the object is loaded
	 * @param requestType
	 *            defines what the Database should do
	 * @param reference
	 *            a reference object if needed
	 */
	public void load(final ApplicationListener listener, final RequestType requestType, final Object reference) {
		addListener(requestType, listener);
		mDatabase._load(requestType, reference);
	}

	/**
	 * performes a networking operation in the networking thread provided by the class extending
	 * {@link Networker}
	 * 
	 * @param listener
	 *            listener which will be informed once the operation is completed
	 * @param requestType
	 *            defines what the Networker should do
	 * @param parameters
	 *            the parameters for the given operation
	 */
	public void network(final ApplicationListener listener, final RequestType requestType, HashMap<String, Object> parameters) {
		addListener(requestType, listener);
		mNetworker._network(requestType, parameters);
	}

	/**
	 * can be called from {@link Database} and {@link Networker} to send messages to the main-thread
	 * 
	 * @param message
	 *            the message which will be handled
	 */
	public void sendMessage(final RequestType requestType) {
		Message message = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putSerializable(Core.REQUEST_TYPE, requestType);
		message.setData(bundle);
		
		mHandler.sendMessage(message);
	}

	/**
	 * stores a value achieved by completing a request will be stored with the Listener as key
	 * 
	 * @param requestValue
	 *            the achieved value
	 */
	public <T> void storeRequestValue(final RequestType requestType, final T requestValue) {
		mListenerQueue.add(getListenerReference(requestType), new ListenerValue<T>(requestType, requestValue));
	}

	/* ------------------------------------- Baseconstruct ------------------------------------- */

	private synchronized void addListener(final RequestType requestType, final ApplicationListener listener) {
		if (listener != null) {
			mListener.put(requestType, listener);
		}
	}

	private synchronized ApplicationListener getListenerReference(final RequestType requestType) {
		if (mListener.size() == 0) {
			return null;
		} else {
			return mListener.get(requestType);
		}
	}

	private synchronized ApplicationListener getListener(final RequestType requestType) {
		if (mListener.size() == 0) {
			return null;
		} else {
			return mListener.remove(requestType);
		}
	}

	protected void setDatabase(final Database database) {
		mDatabase = database;
	}

	protected void setNetworker(final Networker networker) {
		mNetworker = networker;
	}

	/* ------------------------------------- setup process ------------------------------------- */

	private void setupCore() {
		mDatabase = getDatabase();
		mNetworker = getNetworker();
	}

	/**
	 * defines the object to be used as database needs to extend {@link Database}
	 * 
	 * @return the database object
	 */
	abstract protected Database getDatabase();

	/**
	 * defines the object to be used as networker needs to extend {@link Networker}
	 * 
	 * @return the networker object
	 */
	abstract protected Networker getNetworker();

	/* ------------------------------------- value retrieving ------------------------------------- */

	private class ListenerQueue {
		private static final int MAX_QUEUE_LENGTH = 20;
		private HashMap<ApplicationListener, List<ListenerValue<?>>> mRequestValues;

		public ListenerQueue() {
			mRequestValues = new HashMap<ApplicationListener, List<ListenerValue<?>>>();
		}

		private void removeOldestValue() {
			List<ListenerValue<?>> correspondingList = null;
			ListenerValue<?> oldestEntry = null;
			for (Entry<ApplicationListener, List<ListenerValue<?>>> entry : mRequestValues.entrySet()) {
				for (ListenerValue<?> value : entry.getValue()) {
					if (oldestEntry == null) {
						oldestEntry = value;
						correspondingList = entry.getValue();
					} else if (oldestEntry.getTimestamp() > value.getTimestamp()) {
						oldestEntry = value;
						correspondingList = entry.getValue();
					}
				}
			}
			correspondingList.remove(oldestEntry);
		}

		public synchronized void add(final ApplicationListener listener, final ListenerValue<?> value) {
			if (mRequestValues.size() > MAX_QUEUE_LENGTH) {
				removeOldestValue();
			}

			List<ListenerValue<?>> values = null;
			if ((values = mRequestValues.get(listener)) == null) {
				values = new ArrayList<ListenerValue<?>>();
				values.add(value);
				mRequestValues.put(listener, values);
			} else {
				values.add(value);
			}
		}

		public synchronized ListenerValue<?> get(final ApplicationListener listener, final RequestType requestType) {
			List<ListenerValue<?>> values = mRequestValues.remove(listener);
			for (ListenerValue<?> value : values) {
				if (value.requestType.equals(requestType)) {
					return value;
				}
			}
			return null;
		}
	}

	private class ListenerValue<T> {
		private RequestType requestType;
		private T requestValue;
		private Date timestamp;

		public ListenerValue(final RequestType requestType, final T requestValue) {
			this.requestType = requestType;
			this.requestValue = requestValue;
			timestamp = new Date();
		}

		public T getRequestValue() {
			return this.requestValue;
		}

		public long getTimestamp() {
			return timestamp.getTime();
		}
	}
}
