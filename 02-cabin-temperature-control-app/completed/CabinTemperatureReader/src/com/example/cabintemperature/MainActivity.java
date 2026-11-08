package com.example.cabintemperature;

import android.app.Activity;
import android.car.Car;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final String TAG = "CabinTemperatureReader";

    // 0x20000000 VENDOR | 0x01000000 GLOBAL | 0x00600000 FLOAT | 0x0000F001.
    private static final int CUSTOM_CABIN_TEMPERATURE = 0x2160F001;
    private static final int GLOBAL_AREA = 0;
    private static final float STEP_CELSIUS = 0.5f;

    private Car car;
    private volatile CarPropertyManager propertyManager;
    private volatile boolean started;
    private final ExecutorService propertyExecutor = Executors.newSingleThreadExecutor();
    private TextView valueText;
    private TextView statusText;
    private float currentTemperature = 22.5f;


    private final CarPropertyManager.CarPropertyEventCallback propertyCallback =
            new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue value) {
                    if (!started) { return; }
                    if (value.getPropertyId() != CUSTOM_CABIN_TEMPERATURE
                            || value.getStatus() != CarPropertyValue.STATUS_AVAILABLE) {
                        return;
                    }
                    Object rawValue = value.getValue();
                    if (rawValue instanceof Float) {
                        Log.i(TAG, "Temperature callback: timestamp=" + value.getTimestamp()
                                + ", value=" + rawValue);
                        showValue((Float) rawValue, "VHAL event received");
                    }
                }

                @Override
                public void onErrorEvent(int propertyId, int areaId) {
                    Log.e(TAG, "VHAL error property=" + propertyId + ", area=" + areaId);
                    showStatus("VHAL reported a property error");
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
        connectToCar();
    }

    @Override
    protected void onStop() {
        started = false;
        if (propertyManager != null) {
            propertyManager.unregisterCallback(
                    propertyCallback, CUSTOM_CABIN_TEMPERATURE);
            Log.i(TAG, "Unregistered custom-property callback");
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
        propertyExecutor.shutdownNow();
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

            CarPropertyConfig<?> config = propertyManager.getCarPropertyConfig(
                    CUSTOM_CABIN_TEMPERATURE);
            if (config == null) {
                showStatus("Custom property 0x2160F001 is not configured");
                return;
            }

            boolean registered = propertyManager.registerCallback(
                    propertyCallback,
                    CUSTOM_CABIN_TEMPERATURE,
                    CarPropertyManager.SENSOR_RATE_ONCHANGE);
            if (!registered) {
                showStatus("Unable to subscribe; check permission and Logcat");
                return;
            }

            final CarPropertyManager manager = propertyManager;
            propertyExecutor.execute(() -> {
                try {
                    final float initialValue = manager.getFloatProperty(
                            CUSTOM_CABIN_TEMPERATURE, GLOBAL_AREA);
                    runOnUiThread(() -> {
                        if (started && propertyManager == manager) {
                            showValue(initialValue, "Connected to custom VHAL property");
                        }
                    });
                } catch (RuntimeException e) {
                    Log.w(TAG, "Initial custom-property read failed", e);
                    showStatus("Initial custom-property value is unavailable");
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Missing CAR_VENDOR_EXTENSION", e);
            showStatus("Permission denied: CAR_VENDOR_EXTENSION");
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to access custom property", e);
            showStatus("Unable to access custom vehicle property");
        }
    }


    private void changeTemperature(float delta) {
        final CarPropertyManager manager = propertyManager;
        if (manager == null) {
            showStatus("Car Service is not connected");
            return;
        }
        final float requestedValue = currentTemperature + delta;
        showStatus(String.format(Locale.getDefault(), "Requested %.1f °C; awaiting confirmation",
                requestedValue));
        propertyExecutor.execute(() -> {
            try {
                Log.i(TAG, "Requested temperature: " + requestedValue);
                manager.setFloatProperty(CUSTOM_CABIN_TEMPERATURE, GLOBAL_AREA, requestedValue);
                final float confirmedValue = manager.getFloatProperty(
                        CUSTOM_CABIN_TEMPERATURE, GLOBAL_AREA);
                runOnUiThread(() -> {
                    if (started && propertyManager == manager) {
                        Log.i(TAG, "Confirmed temperature: " + confirmedValue);
                        showValue(confirmedValue, "Readback confirms backend state");
                    }
                });
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to write or confirm custom property", e);
                showStatus("Unable to write or confirm custom property");
            }
        });
    }


    private void showValue(float value, String status) {
        runOnUiThread(() -> {
            if (!started) { return; }
            currentTemperature = value;
            valueText.setText(String.format(Locale.getDefault(), "%.1f °C", value));
            statusText.setText(status);
        });
    }

    private void showStatus(String status) {
        runOnUiThread(() -> { if (started) { statusText.setText(status); } });
    }


    private void createUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);
        root.setBackgroundColor(Color.rgb(16, 20, 24));

        TextView title = new TextView(this);
        title.setText("Custom cabin temperature");
        title.setTextColor(Color.LTGRAY);
        title.setTextSize(24);

        valueText = new TextView(this);
        valueText.setText("--.- °C");
        valueText.setTextColor(Color.WHITE);
        valueText.setTextSize(56);
        valueText.setGravity(Gravity.CENTER);
        valueText.setPadding(0, 28, 0, 28);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        Button decrease = new Button(this);
        decrease.setText("− 0.5 °C");
        decrease.setOnClickListener(view -> changeTemperature(-STEP_CELSIUS));

        Button increase = new Button(this);
        increase.setText("+ 0.5 °C");
        increase.setOnClickListener(view -> changeTemperature(STEP_CELSIUS));

        statusText = new TextView(this);
        statusText.setText("Waiting for Car Service…");
        statusText.setTextColor(Color.rgb(255, 183, 77));
        statusText.setTextSize(16);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 24, 0, 0);

        buttons.addView(decrease);
        buttons.addView(increase);
        root.addView(title);
        root.addView(valueText);
        root.addView(buttons);
        root.addView(statusText);
        setContentView(root);
    }
}
