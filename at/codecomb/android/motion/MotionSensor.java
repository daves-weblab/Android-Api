package at.codecomb.sensorfusion;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import at.ac.uibk.persistence.TAGS;

/*
 * Copyright (c) 2013, All Rights Reserved, file = MotionSensor.java
 * 
 * This source is subject to Code Comb (with thanks to  Paul Lawitzki). 
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
 * a simple Implementation of a MotionSensor for Android. This MotionSensor works in two different ways. First is, if the Android-Phone does not have
 * a physical gyroscope MotionSensor calculates the phones Orientation using only the Accelerometer and the Compass (Magnetic-Field-Sensor). Since
 * this MotionSensor was mainly build to work with a robot using magnetic motors, which generate a lot of magnetic noise, it was relatively inaccurate
 * when used on the robot, thats why in version 2.0 the gyroscope sensor was added for calculating the orientation using the so called SensorFusion,
 * using all three sensors.
 * 
 * @author David Riedl (Code Comb)
 * @version 2.1
 */
@SuppressLint("HandlerLeak")
public class MotionSensor implements SensorEventListener {
	/* MotionSensorListener which will get informed when new angles were calculated */
	private MotionSensorListener mListener;

	/* SensorManager and the three sensors */
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mCompass;
	private Sensor mGyroscope;

	/* calculated angles, while ApproxPitch is only a angle used for a noisless turning angle */
	private int mRoll;
	private int mAzimuth;
	private int mApproxPitch;
	private int mAccuratePitch;

	/*
	 * ---------------------------------------------------------- Constructor
	 */

	/**
	 * simple Constructor, sets the MotionSensorListener to null
	 * 
	 * @param sensorManager
	 *            SensorManager achieved from the activity (getSystemService(SENSOR_SERVICE))
	 * @param packageManager
	 *            PackageManager achieved from the activity (getPackageManager())
	 */
	public MotionSensor(final SensorManager sensorManager, final PackageManager packageManager) {
		this(sensorManager, packageManager, null);
	}

	/**
	 * simple Constructor
	 * 
	 * @param sensorManager
	 *            SensorManager achieved from the activity (getSystemService(SENSOR_SERVICE))
	 * @param packageManager
	 *            PackageManager achieved from the activity (getPackageManager())
	 * @param listener
	 *            MotionSensorListener which's method is invoked each time new angles have been calculated
	 */
	public MotionSensor(final SensorManager sensorManager, final PackageManager packageManager, final MotionSensorListener listener) {
		mSensorManager = sensorManager;
		mListener = listener;

		setupSensors(packageManager);
		setupMotionSensor();
	}

	/*
	 * ---------------------------------------------------------- Life-Cycle Methods
	 */

	/**
	 * this method should be called on the MotionSensor when the Activity hosting it is calling onResume
	 */
	public void onResume() {
		registerSensor(mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		registerSensor(mCompass, SensorManager.SENSOR_DELAY_FASTEST);
		registerSensor(mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
	}

	/**
	 * this method should be called on the MotionSensor when the Activity hosting it is calling onResume
	 */
	public void onPause() {
		mSensorManager.unregisterListener(this, mAccelerometer);
		mSensorManager.unregisterListener(this, mCompass);
		if (mGyroscope != null)
			mSensorManager.unregisterListener(this, mGyroscope);
		mSensorManager.unregisterListener(this);
		mFusionTimer.cancel();
	}

	public void resetListener() {
		mListener = null;
	}

	/*
	 * ---------------------------------------------------------- private Methods
	 */

	/* simple setup of the Motionsensor */
	private void setupMotionSensor() {
		/* set the GyroscopeMatrix to a IdentityMatrix for the first calculation */
		initialiseIdentityMatrix(mGyroscopeMatrix);

		/* if a Gyroscope is available in the phone start a TimerTask to calculate the SensorFusion-Orientation */
		if (mGyroscope != null) {
			mFusionTimer.scheduleAtFixedRate(new FusedOrientationCalculator(), 1000, FusedOrientationCalculator.FREQUENCY_HIGH);
		}
	}

	private void setupSensors(final PackageManager packageManager) {
		if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
			mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}

		if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS)) {
			mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		}

		if (packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
			mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		}
	}

	private void registerSensor(final Sensor sensor, final int sensorDelay) {
		if (sensor != null) {
			mSensorManager.registerListener(this, sensor, sensorDelay);
		}
	}

	/*
	 * ---------------------------------------------------------- Getter
	 */

	/**
	 * gets the current orientation around the y-axes (roll)
	 */
	public int getRoll() {
		return mRoll;
	}

	/**
	 * gets the current orientation around the z-axes (azimuth)
	 */
	public int getAzimuth() {
		return mAzimuth;
	}

	/**
	 * gets the current approximated orientation around the x-axes (pitch)
	 */
	public int getApproxPitch() {
		return mApproxPitch;
	}

	/**
	 * gets the current orientation around the x-axes (pitch)
	 */
	public int getAccuratePitch() {
		return mAccuratePitch;
	}

	/*
	 * ---------------------------------------------------------- SensorEventListener
	 */

	/* first time Gyroscope initialization */
	private boolean mGyroscopeReady = false;
	private boolean mGyroscopeInitialised = false;

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// nothing to do here yet
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		/* accelerometer picked up changes */
		case Sensor.TYPE_ACCELEROMETER:
			/* copy values to the accelerometer vector */
			System.arraycopy(event.values, 0, mAccelerometerVector, 0, 3);
			/* calculate if possible (accelerometer and compass achieved values), the current simple Orientation */
			calculateAccelerometerCompassOrientation();
			break;

		/* compass picked up changes */
		case Sensor.TYPE_MAGNETIC_FIELD:
			/* copy values to the compass vector */
			System.arraycopy(event.values, 0, mCompassVector, 0, 3);
			break;

		/* Gyroscope picked up changes */
		case Sensor.TYPE_GYROSCOPE:
			/* get the values from the gyroscope */
			calculateGyroscopeData(event);
			break;
		}

		if (mListener != null && getAnglesStatus()) {
			setAnglesStatus(false);
			mListener.onSensorChanged(this);
		}
	}

	/* sets the three angles (roll, azimuth and pitch) */
	private void setEulerAngles() {
		Log.i(TAGS.LOGTAG, "MotionSensor: new angles have been calculated setting them now");
		/* simulate Orientation only with Accelerometer and Compass */
		if (mGyroscope == null) {
			setEulerAngles(mAccelerometerCompassOrientation);
		}
		/* Gyroscope available as well, use FusedOrientation for more accuracy */
		else {
			setEulerAngles(mFusedOrientation);
		}

		setAnglesStatus(true);
	}

	private void setEulerAngles(final float[] orientation) {
		mAzimuth = (int) Math.toDegrees(orientation[1]);
		mRoll = (int) (Math.toDegrees(orientation[2]) + 90) % 360;

		double tempPitch = Math.toDegrees(orientation[0]) + 180;
		mAccuratePitch = (int) tempPitch;
		if (!(mApproxPitch - 2 <= tempPitch && tempPitch <= mApproxPitch + 2))
			mApproxPitch = (int) tempPitch;
	}

	/*
	 * ---------------------------------------------------------- Roll/Azimuth/Pitch calculation
	 */

	/* timestamp of the last calculation */
	private float mTimestamp;
	/* time for the FusedOrientation calculation */
	private Timer mFusionTimer = new Timer();

	/* constants used for calculating the gyroscope orientation */
	private static final float EPSILON = 0.000000001f;
	private static final float NS2S = 1.0f / 1000000000.0f;
	private static final float FILTER_COEFFICIENT = 0.98f;

	/* vector for each sensor */
	private float[] mGyroscopeVector = new float[3];
	private float[] mAccelerometerVector = new float[3];
	private float[] mCompassVector = new float[3];

	/* orientation for only using accelerometer and compass */
	private float[] mAccelerometerCompassOrientation = new float[3];
	/* orientation calculated from the gyroscope's values */
	private float[] mGyroscopeOrientation = new float[3];
	/* orientation calculated by fusing Accelerometer-Compass-Gyroscope */
	private float[] mFusedOrientation = new float[3];

	/* RotationMatrices */
	private float[] mRotationMatrix = new float[9];
	private float[] mGyroscopeMatrix = new float[9];

	/**
	 * initialise a given matrix as a identity matrix
	 * 
	 * @param matrix
	 *            the matrix which will be initialised as a identity matrix
	 */
	private void initialiseIdentityMatrix(final float[] matrix) {
		final int dimension = (int) Math.sqrt(matrix.length);

		for (int i = 0; i < dimension; i++) {
			for (int j = 0; j < dimension; j++) {
				if (j == i) {
					matrix[i * dimension + j] = 1.0f;
				} else {
					matrix[i * dimension + j] = 0.0f;
				}
			}
		}
	}

	/**
	 * calculates the current orientation of the phone with only the accelerometer and the compass, if no gyroscope is available this method also sets
	 * the three angles accordingly
	 */
	private void calculateAccelerometerCompassOrientation() {
		if (SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerVector, mCompassVector)) {
			SensorManager.getOrientation(mRotationMatrix, mAccelerometerCompassOrientation);

			if (mGyroscope == null) {
				setEulerAngles();
			}

			if (!mGyroscopeReady) {
				mGyroscopeReady = true;
			}
		}
	}

	/**
	 * this method retrieves the needed data from the gyroscopes raw data vector and sets the corresponding GyroscopeMatrix and OrientationMatrix of
	 * the gyroscope
	 * 
	 * @param event
	 *            SensorEvent passed to retrieve the values from
	 */
	private void calculateGyroscopeData(final SensorEvent event) {
		if (mGyroscopeReady) {
			/* needs to be done only once */
			if (!mGyroscopeInitialised) {
				/* calculate the rotationMatrix from the current orientation */
				float[] gyroscopeRotationMatrix = calculateRotationMatrixFromOrientation(mAccelerometerCompassOrientation);
				/* and multiply it with the identity matrix */
				mGyroscopeMatrix = matrixMultiplication(mGyroscopeMatrix, gyroscopeRotationMatrix);
				mGyroscopeInitialised = true;
			}

			/* the gyroscope is time sensitive, which is why we have to use the timestamp from the SensorEvent to calculate new values */
			float[] deltaVector = new float[4];
			if (mTimestamp != 0) {
				/* get the time factor used for calculating the rotation vector */
				final float timeFactor = (event.timestamp - mTimestamp) * NS2S;
				/* get the raw value vector */
				System.arraycopy(event.values, 0, mGyroscopeVector, 0, 3);
				/* calculate the rotation from the value vector, will be stored in deltaVector */
				calculateRotationVectorFromGyroscope(mGyroscopeVector, deltaVector, timeFactor / 2.0f);
			}

			/* set the timestamp accordingly */
			mTimestamp = event.timestamp;

			/* calculate the deltaRotation using the calculated deltaVector */
			float[] deltaRotationMatrix = new float[9];
			SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaVector);

			/* and mutliply the old gyroscopeMatrix with the new rotationMatrix */
			mGyroscopeMatrix = matrixMultiplication(mGyroscopeMatrix, deltaRotationMatrix);
			/* and retrieve the current orientation storing it in mGyroscopeOrientation */
			SensorManager.getOrientation(mGyroscopeMatrix, mGyroscopeOrientation);
		}
	}

	private void calculateRotationVectorFromGyroscope(final float[] gyroscopeVector, final float[] deltaVector, final float timeFactor) {
		final float[] normVector = new float[gyroscopeVector.length];
		final float omegaMagnitude = (float) Math.sqrt(gyroscopeVector[0] * gyroscopeVector[0] + gyroscopeVector[1] * gyroscopeVector[1] + gyroscopeVector[2] * gyroscopeVector[2]);

		if (omegaMagnitude > EPSILON) {
			for (int i = 0; i < gyroscopeVector.length; i++) {
				normVector[i] = gyroscopeVector[i] / omegaMagnitude;
			}
		}

		final float sinThetaOverTwo = (float) Math.sin(omegaMagnitude * timeFactor);
		for (int i = 0; i < normVector.length; i++) {
			deltaVector[i] = normVector[i] * sinThetaOverTwo;
		}
		deltaVector[normVector.length] = (float) Math.cos(omegaMagnitude * timeFactor);
	}

	private float[] calculateRotationMatrixFromOrientation(final float[] orientation) {
		float[] xM = new float[9];
		float[] yM = new float[9];
		float[] zM = new float[9];

		float sinX = (float) Math.sin(orientation[1]);
		float cosX = (float) Math.cos(orientation[1]);
		float sinY = (float) Math.sin(orientation[2]);
		float cosY = (float) Math.cos(orientation[2]);
		float sinZ = (float) Math.sin(orientation[0]);
		float cosZ = (float) Math.cos(orientation[0]);

		/* rotation about x-axis (pitch) */
		xM[0] = 1.0f;
		xM[1] = 0.0f;
		xM[2] = 0.0f;
		xM[3] = 0.0f;
		xM[4] = cosX;
		xM[5] = sinX;
		xM[6] = 0.0f;
		xM[7] = -sinX;
		xM[8] = cosX;

		/* rotation about y-axis (roll) */
		yM[0] = cosY;
		yM[1] = 0.0f;
		yM[2] = sinY;
		yM[3] = 0.0f;
		yM[4] = 1.0f;
		yM[5] = 0.0f;
		yM[6] = -sinY;
		yM[7] = 0.0f;
		yM[8] = cosY;

		/* rotation about z-axis (azimuth) */
		zM[0] = cosZ;
		zM[1] = sinZ;
		zM[2] = 0.0f;
		zM[3] = -sinZ;
		zM[4] = cosZ;
		zM[5] = 0.0f;
		zM[6] = 0.0f;
		zM[7] = 0.0f;
		zM[8] = 1.0f;

		/* rotation order is y, x, z (roll, pitch, azimuth) */
		float[] resultMatrix = matrixMultiplication(xM, yM);
		resultMatrix = matrixMultiplication(zM, resultMatrix);
		return resultMatrix;
	}

	/**
	 * simple matrix multiplication, multiplies matrix a and b and returns the calculated new matrix
	 */
	private float[] matrixMultiplication(final float[] a, final float[] b) {
		if (a.length != b.length) {
			return null;
		}

		final int dimension = (int) Math.sqrt(a.length);
		final float[] c = new float[a.length];

		for (int i = 0; i < dimension; i++) {
			for (int j = 0; j < dimension; j++) {
				for (int l = 0; l < dimension; l++) {
					c[i * dimension + j] += a[i * dimension + l] * b[l * dimension + j];
				}
			}
		}

		return c;
	}

	/*
	 * ---------------------------------------------------------- FusedOrientationCalculator
	 */

	private boolean mAnglesStatus = false;
	private Object mAnglesStatusLock = new Object();

	private void setAnglesStatus(final boolean status) {
		synchronized (mAnglesStatusLock) {
			mAnglesStatus = status;
		}
	}

	private boolean getAnglesStatus() {
		synchronized (mAnglesStatusLock) {
			return mAnglesStatus;
		}
	}

	/**
	 * TimerTask which will run with the given Frequency and calculates the fused orientation matrix
	 * 
	 * @author David Riedl (Code Comb)
	 * 
	 */
	@SuppressWarnings("unused")
	private class FusedOrientationCalculator extends TimerTask {
		public static final int FREQUENCY_HIGH = 30;
		public static final int FREQUENCY_MID = 100;
		public static final int FREQUENCY_LOW = 250;

		@Override
		public void run() {
			final float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

			/* Roll */
			if (mGyroscopeOrientation[2] < -0.5 * Math.PI && mAccelerometerCompassOrientation[2] > 0.0) {
				mFusedOrientation[2] = (float) (FILTER_COEFFICIENT * (mGyroscopeOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * mAccelerometerCompassOrientation[2]);
				mFusedOrientation[2] -= (mFusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
			} else if (mAccelerometerCompassOrientation[2] < -0.5 * Math.PI && mGyroscopeOrientation[2] > 0.0) {
				mFusedOrientation[2] = (float) (FILTER_COEFFICIENT * mGyroscopeOrientation[2] + oneMinusCoeff * (mAccelerometerCompassOrientation[2] + 2.0 * Math.PI));
				mFusedOrientation[2] -= (mFusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
			} else {
				mFusedOrientation[2] = FILTER_COEFFICIENT * mGyroscopeOrientation[2] + oneMinusCoeff * mAccelerometerCompassOrientation[2];
			}

			/* Azimuth */
			if (mGyroscopeOrientation[0] < -0.5 * Math.PI && mAccelerometerCompassOrientation[0] > 0.0) {
				mFusedOrientation[0] = (float) (FILTER_COEFFICIENT * (mGyroscopeOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * mAccelerometerCompassOrientation[0]);
				mFusedOrientation[0] -= (mFusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
			} else if (mAccelerometerCompassOrientation[0] < -0.5 * Math.PI && mGyroscopeOrientation[0] > 0.0) {
				mFusedOrientation[0] = (float) (FILTER_COEFFICIENT * mGyroscopeOrientation[0] + oneMinusCoeff * (mAccelerometerCompassOrientation[0] + 2.0 * Math.PI));
				mFusedOrientation[0] -= (mFusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
			} else {
				mFusedOrientation[0] = FILTER_COEFFICIENT * mGyroscopeOrientation[0] + oneMinusCoeff * mAccelerometerCompassOrientation[0];
			}

			/* Pitch */
			if (mGyroscopeOrientation[1] < -0.5 * Math.PI && mAccelerometerCompassOrientation[1] > 0.0) {
				mFusedOrientation[1] = (float) (FILTER_COEFFICIENT * (mGyroscopeOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * mAccelerometerCompassOrientation[1]);
				mFusedOrientation[1] -= (mFusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
			} else if (mAccelerometerCompassOrientation[1] < -0.5 * Math.PI && mGyroscopeOrientation[1] > 0.0) {
				mFusedOrientation[1] = (float) (FILTER_COEFFICIENT * mGyroscopeOrientation[1] + oneMinusCoeff * (mAccelerometerCompassOrientation[1] + 2.0 * Math.PI));
				mFusedOrientation[1] -= (mFusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
			} else {
				mFusedOrientation[1] = FILTER_COEFFICIENT * mGyroscopeOrientation[1] + oneMinusCoeff * mAccelerometerCompassOrientation[1];
			}

			/* overwrite the gyroscopeMatrix and orientation with the fused ones to comensate the gyroscope's drift */
			mGyroscopeMatrix = calculateRotationMatrixFromOrientation(mFusedOrientation);
			System.arraycopy(mFusedOrientation, 0, mGyroscopeOrientation, 0, 3);
			setEulerAngles();
		}
	}
}
