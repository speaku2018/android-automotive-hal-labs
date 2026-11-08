package com.example.vehiclespeedreader;

import android.app.Activity;
import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final String TAG = "VehicleSpeedReader";
    private static final int SPEED_PROPERTY = VehiclePropertyIds.PERF_VEHICLE_SPEED;
    private static final int GLOBAL_AREA = 0;
    private static final float MPS_TO_KPH = 3.6f;
    private static final int REQUEST_CAR_SPEED_PERMISSION = 100;

    private Car car;
    private volatile CarPropertyManager propertyManager;
    private volatile boolean started;
    private final ExecutorService readExecutor = Executors.newSingleThreadExecutor();
    private TextView speedText;
    private TextView statusText;

    private final CarPropertyManager.CarPropertyEventCallback speedCallback =
            new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue value) {
                    if (!started) { return; }
                    if (value.getPropertyId() != SPEED_PROPERTY) {
                        return;
                    }

                    if (value.getStatus() != CarPropertyValue.STATUS_AVAILABLE) {
                        showStatus("Speed is temporarily unavailable");
                        return;
                    }

                    Object rawValue = value.getValue();
                    if (rawValue instanceof Float) {
                        Log.i(TAG, "Speed callback: area=" + value.getAreaId()
                                + ", timestamp=" + value.getTimestamp() + ", value=" + rawValue);
                        showSpeed((Float) rawValue);
                    }
                }

                @Override
                public void onErrorEvent(int propertyId, int areaId) {
                    Log.e(TAG, "VHAL error for property=" + propertyId + ", area=" + areaId);
                    showStatus("VHAL reported a speed-property error");
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        started = true;
        if (checkSelfPermission(Car.PERMISSION_SPEED) == PackageManager.PERMISSION_GRANTED) {
            connectToCar();
        } else {
            showStatus("Vehicle speed permission is required");
            requestPermissions(
                    new String[]{Car.PERMISSION_SPEED}, REQUEST_CAR_SPEED_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CAR_SPEED_PERMISSION) {
            return;
        }

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connectToCar();
        } else {
            showStatus("Permission denied: CAR_SPEED");
        }
    }

    @Override
    protected void onStop() {
        started = false;
        if (propertyManager != null) {
            propertyManager.unregisterCallback(speedCallback, SPEED_PROPERTY);
            Log.i(TAG, "Unregistered speed callback");
            propertyManager = null;
        }
        if (car != null) {
            car.disconnect();
            car = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        readExecutor.shutdownNow();
        super.onDestroy();
    }

    private void connectToCar() {
        if (!started) { return; }
        try {
            car = Car.createCar(this);
            propertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);

            if (propertyManager == null) {
                showStatus("CarPropertyManager is unavailable");
                return;
            }

            CarPropertyConfig<?> speedConfig =
                    propertyManager.getCarPropertyConfig(SPEED_PROPERTY);
            if (speedConfig == null) {
                showStatus("PERF_VEHICLE_SPEED is not supported by this VHAL");
                return;
            }

            boolean registered = propertyManager.registerCallback(
                    speedCallback,
                    SPEED_PROPERTY,
                    CarPropertyManager.SENSOR_RATE_UI);

            if (!registered) {
                showStatus("Unable to subscribe to vehicle speed; check Logcat");
                return;
            }

            Log.i(TAG, "Subscribed to PERF_VEHICLE_SPEED at UI rate");
            showStatus("Listening for PERF_VEHICLE_SPEED");
            readInitialSpeed();
        } catch (SecurityException e) {
            Log.e(TAG, "Missing CAR_SPEED permission", e);
            showStatus("Permission denied: CAR_SPEED");
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to connect to car service", e);
            showStatus("Unable to connect to Car Service");
        }
    }

    private void readInitialSpeed() {
        final CarPropertyManager manager = propertyManager;
        if (manager == null) { return; }
        readExecutor.execute(() -> {
            try {
                final float metersPerSecond = manager.getFloatProperty(
                        SPEED_PROPERTY, GLOBAL_AREA);
                runOnUiThread(() -> {
                    if (started && propertyManager == manager) {
                        Log.i(TAG, "Initial speed read: " + metersPerSecond + " m/s");
                        showSpeed(metersPerSecond);
                    }
                });
            } catch (RuntimeException e) {
                Log.w(TAG, "No initial speed value is available yet", e);
            }
        });
    }

    private void showSpeed(float metersPerSecond) {
        runOnUiThread(() -> {
            if (!started) { return; }
            speedText.setText(String.format(
                    Locale.getDefault(), "%.1f km/h", metersPerSecond * MPS_TO_KPH));
            statusText.setText(String.format(
                    Locale.getDefault(), "Raw VHAL value: %.2f m/s", metersPerSecond));
        });
    }

    private void showStatus(String message) {
        runOnUiThread(() -> statusText.setText(message));
    }

    private void createUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);
        root.setBackgroundColor(Color.rgb(16, 20, 24));

        TextView title = new TextView(this);
        title.setText("Vehicle speed");
        title.setTextColor(Color.LTGRAY);
        title.setTextSize(24);

        speedText = new TextView(this);
        speedText.setText("--.- km/h");
        speedText.setTextColor(Color.WHITE);
        speedText.setTextSize(56);
        speedText.setGravity(Gravity.CENTER);

        statusText = new TextView(this);
        statusText.setText("Waiting for Car Service…");
        statusText.setTextColor(Color.rgb(128, 203, 196));
        statusText.setTextSize(16);
        statusText.setGravity(Gravity.CENTER);

        root.addView(title);
        root.addView(speedText);
        root.addView(statusText);
        setContentView(root);
    }
}
