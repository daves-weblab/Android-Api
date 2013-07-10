package at.ac.uibk.thread;

import android.util.Log;

/*
 * Copyright (c) 2013, All Rights Reserved, file = PausableThread.java
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
 * Since creating a Thread costs a lot of resources and the DalvikVM's Garbage Collector Calls
 * are even more expansive, this Thread can be used instead of using simple threads.
 * This thread can be paused and resumed when needed again, the method which does the work can be overriden
 * when creating a PausableThread since it's being abstract.
 * @author David Riedl (Code Comb)
 *
 */
abstract public class PausableThread extends Thread {
	/* Lock for Thread-Safety */
	private Object mPauseLock;
	/* true --> the thread autopauses itself after each call of work() */
	private boolean mAutoPause;
	/* true --> the thread is actually paused */
	private boolean mPaused;
	/* true --> thread is done, so it can run out */
	private boolean mFinished;
	/* true --> actually did one step and can be resumed (safety) */
	private boolean mStarted;

	public PausableThread() {
		mPauseLock = new Object();
		mPaused = false;
		mFinished = false;
	}

	@Override
	public void run() {
		/* run until mFinished == true */
		while (!mFinished) {
			/* get the lock */
			synchronized (mPauseLock) {
				/* if being paused */
				while (mPaused) {
					try {
						/* if it is the first cycle set mStarted to true */
						if(!mStarted) {
							mStarted = true;
						}
						/* wait till resumeThread() or done() are being called */
						mPauseLock.wait();
					} catch (InterruptedException e) {
						Log.i("DEBUG", PausableThread.class.toString() + " InterruptedException occured!");
					}
				}
			}

			/* if not finished yet call work() */
			if (!mFinished) {
				work();
			}
			/* if autopause is activated, the thread pauses itself */
			if (mAutoPause) {
				mPaused = true;
			}
		}
	}

	/**
	 * pauses the thread, and prevents work() from being called 
	 */
	public void pauseThread() {
//		/* get the lock and set paused to true */
		synchronized (mPauseLock) {
			mPaused = true;
		}
	}

	 /**
	  * resume the thread again
	  */
	public void resumeThread() {
		/* get the lock  and set paused to false */
		synchronized (mPauseLock) {
			mPaused = false;
		}
		
		/* if the thread hasn't started yet (safety matter) */
		if (mStarted) {
			/* actually wait till the thread is really waiting or the notify is lost */
			while (this.getState() != Thread.State.WAITING)
				;

			/* get the lock again and notify the waiting thread */
			synchronized (mPauseLock) {
				mPauseLock.notifyAll();
			}
		}
	}

	/**
	 * tells the thread that he can stop calling work() and run out
	 */
	public void done() {
		/* wait till the thread is waiting or the notify is lost */
		while(this.getState() != Thread.State.WAITING)
			;
		
		/* get the lock set finished to true and notify the thread */
		synchronized (mPauseLock) {
			mFinished = true;
			mPauseLock.notifyAll();
		}
	}

	/**
	 * activates or deactivates autopause, which makes the thread pause itself after each call of work()
	 * @param autoPause true = activated, false = deactivated
	 */
	public void setAutoPause(boolean autoPause) {
		mAutoPause = autoPause;
	}

	/**
	 * this is the actual work the thread will perform each cycle
	 */
	abstract public void work();
}
