package at.codecomb.sensorfusion;

/*
 *  * Copyright (c) 2013, All Rights Reserved, file = MotionSensor.java
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
 * simple Listener pattern for MotionSensor
 * 
 * @author David Riedl (Code Comb)
 * @version 1.0
 */
public interface MotionSensorListener {
	/**
	 * if a MotionSensorListener is attached to a MotionSensor this method will be invoked on the Listener each and every time new angles have been
	 * successfully calculated
	 * 
	 * @param motionSensor
	 *            MotionSensor which calculated new angles (just in case if someone needs more than one MotionSensor at a time)
	 */
	public void onSensorChanged(final MotionSensor motionSensor);
}
