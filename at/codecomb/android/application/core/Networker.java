package at.codecomb.android.application.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import at.codecomb.android.application.listener.ApplicationListener;
import at.codecomb.util.thread.PausableThread;

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
 * This abstract patterns allows a simple implementation of a thread performing given networking
 * tasks while only implementing one method, {@link #executeRequest(RequestType, HashMap)
 * executeRequest}. Parameters will be packed in a HashMap by implementing own static wrapper
 * methods.
 * 
 * @author David Riedl (Code Comb)
 * @version 1.0
 */
abstract public class Networker {
	private static Core mCore;

	protected static void setCore(final Core core) {
		mCore = core;
	}

	private PausableThread mNetworkingThread;
	private final Object mRequestLock = new Object();
	private List<Tupling<RequestType, HashMap<String, Object>>> mRequests;

	public Networker() {
		mRequests = new ArrayList<Tupling<RequestType, HashMap<String, Object>>>();
		setupThread();
	}

	private void setupThread() {
		mNetworkingThread = new PausableThread(true) {
			@Override
			public void work() {
				if (hasRequest()) {
					Tupling<RequestType, HashMap<String, Object>> request = getRequest();
					executeRequest(request.f, request.s);
				}
			}
		};
		mNetworkingThread.pauseThread();
		mNetworkingThread.start();
	}

	/* ------------------------------------- public methods ------------------------------------- */

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
	public static void network(final ApplicationListener listener, final RequestType requestType, HashMap<String, Object> parameters) {
		mCore.network(listener, requestType, parameters);
	}

	protected void _network(final RequestType requestType, HashMap<String, Object> parameters) {
		addRequest(requestType, parameters);
		mNetworkingThread.resumeThread();
	}

	/* ------------------------------------- private methods ------------------------------------- */

	private boolean hasRequest() {
		synchronized (mRequestLock) {
			return !mRequests.isEmpty();
		}
	}

	private void addRequest(final RequestType requestType, final HashMap<String, Object> parameters) {
		synchronized (mRequestLock) {
			mRequests.add(new Tupling<RequestType, HashMap<String, Object>>(requestType, parameters));
		}
	}

	private Tupling<RequestType, HashMap<String, Object>> getRequest() {
		synchronized (mRequestLock) {
			return mRequests.remove(0);
		}
	}

	/**
	 * can be called from {@link Database} and {@link Networker} to send messages to the main-thread
	 * 
	 * @param requestType
	 *            the requestType of the operation performed
	 */
	protected void sendMessage(final RequestType requestType) {
		mCore.sendMessage(requestType);
	}

	/**
	 * can be called from {@link Database} and {@link Networker} to send messages to the main-thread
	 * 
	 * @param requestType
	 *            the requestType of the operation performed
	 * @param requestValue
	 *            the value obtained by performing the requested operation
	 */
	protected void sendMessage(final RequestType requestType, final Object requestValue) {
		storeRequestValue(requestType, requestValue);
		mCore.sendMessage(requestType);
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

	abstract protected void executeRequest(final RequestType requestType, final HashMap<String, Object> parameters);
}
