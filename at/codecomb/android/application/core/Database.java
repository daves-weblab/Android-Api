package at.codecomb.android.application.core;

import java.util.ArrayList;
import java.util.List;

import android.os.Message;
import at.codecomb.android.application.listener.ApplicationListener;
import at.codecomb.util.thread.PausableThread;

/*
 * Copyright (c) 2013, All Rights Reserved, file = Database.java
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
 * This abstract pattern allows a simple implementation of a database withing two methods,
 * {@link #executeLoadingRequest(RequestType, Object) load} and
 * {@link #executeStoringRequest(RequestType, Object) store}. What technology will be used to store
 * is still free to implement this is only a abstract wrapper which takes over the part of using
 * threads and listening patterns to perform the given tasks.
 * 
 * @author David Riedl (Code Comb)
 * @version 1.0
 */
abstract public class Database {
	private static Core mCore;

	protected static void setCore(final Core core) {
		mCore = core;
	}

	private PausableThread mStoringThread;
	private final Object mStoringRequestLock = new Object();
	private List<Tupling<RequestType, Object>> mStoringRequests;

	private PausableThread mLoadingThread;
	private final Object mLoadingRequestLock = new Object();
	private List<Tupling<RequestType, Object>> mLoadingRequests;

	public Database() {
		mStoringRequests = new ArrayList<Tupling<RequestType, Object>>();
		mLoadingRequests = new ArrayList<Tupling<RequestType, Object>>();
		setupThread();
	}

	private void setupThread() {
		mStoringThread = new PausableThread(true) {
			@Override
			public void work() {
				if (hasStoringRequest()) {
					Tupling<RequestType, Object> request = getStoringRequest();
					executeStoringRequest(request.f, request.s);
				}
			}
		};
		mStoringThread.pauseThread();
		mStoringThread.start();

		mLoadingThread = new PausableThread(true) {
			@Override
			public void work() {
				if (hasLoadingRequest()) {
					Tupling<RequestType, Object> request = getLoadingRequest();
					executeLoadingRequest(request.f, request.s);
				}
			}
		};
		mLoadingThread.pauseThread();
		mLoadingThread.start();
	}

	/* ------------------------------------- static Wrapper ------------------------------------- */

	/**
	 * stores an object in the local database provided by the class extending {@link Database}
	 * 
	 * @param requestType
	 *            defines what the Database should do
	 * @param content
	 *            the content to be stored
	 */
	public static <T> void store(final RequestType requestType, final T content) {
		mCore.store(requestType, content);
	}

	/**
	 * loads an object from the local database provided by the class extending {@link Database}
	 * 
	 * @param listener
	 *            the listener which will be informed once the object is loaded
	 * @param requestType
	 *            defines what the Database should do
	 */
	public static void load(final ApplicationListener listener, final RequestType requestType) {
		mCore.load(listener, requestType);
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
	public static void load(final ApplicationListener listener, final RequestType requestType, final Object reference) {
		mCore.load(listener, requestType, reference);
	}

	/**
	 * returns the achieved value by performing a request for the given listener
	 * 
	 * @param listener
	 *            the listener who asked for the request to be performed
	 * @return the achieved value
	 */
	public static Object getRequestValue(final ApplicationListener listener, final RequestType requestType) {
		return mCore.getRequestValue(listener, requestType);
	}

	/* ------------------------------------- public methods ------------------------------------- */

	protected <T> void _store(final RequestType requestType, final T content) {
		addStoringRequest(requestType, content);
	}

	protected void _load(final RequestType requestType) {
		addLoadingRequest(requestType, null);
	}

	protected void _load(final RequestType requestType, final Object reference) {
		addLoadingRequest(requestType, reference);
	}

	/* ------------------------------------- private methods ------------------------------------- */

	private boolean hasStoringRequest() {
		synchronized (mStoringRequestLock) {
			return !mStoringRequests.isEmpty();
		}
	}

	private boolean hasLoadingRequest() {
		synchronized (mLoadingRequestLock) {
			return !mLoadingRequests.isEmpty();
		}
	}

	private Tupling<RequestType, Object> getStoringRequest() {
		synchronized (mStoringRequestLock) {
			return mStoringRequests.remove(0);
		}
	}

	private Tupling<RequestType, Object> getLoadingRequest() {
		synchronized (mLoadingRequestLock) {
			return mLoadingRequests.remove(0);
		}
	}

	private void addStoringRequest(final RequestType requestType, final Object content) {
		synchronized (mStoringRequestLock) {
			mStoringRequests.add(new Tupling<RequestType, Object>(requestType, content));
			mStoringThread.resumeThread();
		}
	}

	private void addLoadingRequest(final RequestType requestType, final Object content) {
		synchronized (mLoadingRequestLock) {
			mLoadingRequests.add(new Tupling<RequestType, Object>(requestType, content));
			mLoadingThread.resumeThread();
		}
	}

	/**
	 * can be called from {@link Database} and {@link Networker} to send messages to the main-thread
	 * 
	 * @param message
	 *            the message which will be handled
	 */
	protected void sendMessage(final Message message) {
		mCore.sendMessage(message);
	}

	/**
	 * stores a value achieved by completing a request will be stored with the Listener as key
	 * 
	 * @param requestValue
	 *            the achieved value
	 */
	protected <T> void storeRequestValue(final RequestType requestType, final T requestValue) {
		mCore.storeRequestValue(requestType, requestValue);
	}

	abstract protected void executeStoringRequest(final RequestType requestType, final Object object);

	abstract protected void executeLoadingRequest(final RequestType requestType, final Object reference);
}
