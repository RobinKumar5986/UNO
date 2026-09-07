package com.kgjr.uno.models.sensors;

import android.Manifest;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Every sensor the app knows how to work with. This is the single place new sensors get added —
 * the sensors screen, the selection state and the code generator all read from here.
 *
 * <p>Only sensors that report a value belong here. Android's trigger-style sensors — step
 * detector, significant motion, stationary detect — fire an event without a reading a program
 * could substitute into a command, so they are left out.
 *
 * <p>Nor are Android's alternative fusions of sensors already listed here: the game rotation
 * vector is the rotation vector without the magnetometer, the geomagnetic one is it without the
 * gyroscope, and neither is separate hardware. {@link PhoneSensor#derived} marks the fusions that
 * are worth carrying regardless, so the detail sheet can say which readings come off a chip.
 *
 * <p>Adding one means: a constant below, an entry in {@link #ALL}, a drawable named by
 * {@code image(...)}, and a case in {@code SensorLiveReadingHelper.read}.
 */
public final class SensorCatalog {

    // ------------------------------------------------------------- orientation

    /**
     * Compass heading and tilt, derived from the rotation matrix. Azimuth is reported as a single
     * 0–360° heading so a program can steer by it directly. Backed by the rotation vector where
     * the phone has one, otherwise derived from the accelerometer and magnetometer.
     */
    public static final PhoneSensor ORIENTATION = PhoneSensor.named("orientation")
            .displayName("Orientation")
            .description("How the phone is held. Azimuth is the compass heading — 0° points to "
                    + "magnetic north and rises clockwise. Pitch is the forward tilt and roll "
                    + "the sideways tilt.")
            .type(SensorType.INPUT)
            .image("orientation_sensor")
            .channel(SensorChannel.bounded(ChannelKey.AZIMUTH, "Azimuth", "°", 0f, 360f))
            .channel(SensorChannel.bounded(ChannelKey.PITCH, "Pitch", "°", -90f, 90f))
            .channel(SensorChannel.bounded(ChannelKey.ROLL, "Roll", "°", -180f, 180f))
            .requires(Sensor.TYPE_ROTATION_VECTOR)
            .requires(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD)
            .derived()
            .build();

    // --------------------------------------------------------------- motion

    /**
     * Raw acceleration on each axis, gravity included — a phone lying flat reads about 9.81 on
     * Z. Use {@link #LINEAR_ACCELERATION} when only movement matters.
     */
    public static final PhoneSensor ACCELEROMETER = PhoneSensor.named("accelerometer")
            .displayName("Accelerometer")
            .description("Acceleration along each axis, with gravity included. X runs across the "
                    + "screen, Y up it and Z out of it.")
            .type(SensorType.INPUT)
            .image("ic_sensor_accelerometer")
            .channel(SensorChannel.unbounded(ChannelKey.X, "X", "m/s²"))
            .channel(SensorChannel.unbounded(ChannelKey.Y, "Y", "m/s²"))
            .channel(SensorChannel.unbounded(ChannelKey.Z, "Z", "m/s²"))
            .requires(Sensor.TYPE_ACCELEROMETER)
            .build();

    /** How fast the phone is turning about each axis. Zero when it is held still. */
    public static final PhoneSensor GYROSCOPE = PhoneSensor.named("gyroscope")
            .displayName("Gyroscope")
            .description("Rate of rotation about each axis in radians per second. Reads zero "
                    + "while the phone is held still, whatever angle it is at.")
            .type(SensorType.INPUT)
            .image("ic_sensor_gyroscope")
            .channel(SensorChannel.unbounded(ChannelKey.X, "X", "rad/s"))
            .channel(SensorChannel.unbounded(ChannelKey.Y, "Y", "rad/s"))
            .channel(SensorChannel.unbounded(ChannelKey.Z, "Z", "rad/s"))
            .requires(Sensor.TYPE_GYROSCOPE)
            .build();

    /**
     * The gravity component of the accelerometer, isolated. Bounded because the magnitude across
     * the three axes is always about 9.81.
     */
    public static final PhoneSensor GRAVITY = PhoneSensor.named("gravity")
            .displayName("Gravity")
            .description("Which way is down, as an acceleration on each axis. Together the three "
                    + "always add up to about 9.81 m/s², so this shows tilt rather than movement.")
            .type(SensorType.INPUT)
            .image("ic_sensor_gravity")
            .channel(SensorChannel.bounded(ChannelKey.X, "X", "m/s²", -9.81f, 9.81f))
            .channel(SensorChannel.bounded(ChannelKey.Y, "Y", "m/s²", -9.81f, 9.81f))
            .channel(SensorChannel.bounded(ChannelKey.Z, "Z", "m/s²", -9.81f, 9.81f))
            .requires(Sensor.TYPE_GRAVITY)
            .derived()
            .build();

    /** The accelerometer with gravity taken out, so it only reacts to being moved. */
    public static final PhoneSensor LINEAR_ACCELERATION = PhoneSensor.named("linear_acceleration")
            .displayName("Linear Acceleration")
            .description("Acceleration with gravity removed, so it reads zero while the phone "
                    + "rests at any angle and only responds to being moved.")
            .type(SensorType.INPUT)
            .image("ic_sensor_linear_acceleration")
            .channel(SensorChannel.unbounded(ChannelKey.X, "X", "m/s²"))
            .channel(SensorChannel.unbounded(ChannelKey.Y, "Y", "m/s²"))
            .channel(SensorChannel.unbounded(ChannelKey.Z, "Z", "m/s²"))
            .requires(Sensor.TYPE_LINEAR_ACCELERATION)
            .derived()
            .build();

    /** The raw magnetometer, before it is folded into a heading. */
    public static final PhoneSensor MAGNETOMETER = PhoneSensor.named("magnetometer")
            .displayName("Magnetic Field")
            .description("Strength of the magnetic field along each axis in microtesla. Earth's "
                    + "field is roughly 25–65 µT; a magnet held nearby swamps it.")
            .type(SensorType.INPUT)
            .image("ic_sensor_magnetometer")
            .channel(SensorChannel.unbounded(ChannelKey.X, "X", "µT"))
            .channel(SensorChannel.unbounded(ChannelKey.Y, "Y", "µT"))
            .channel(SensorChannel.unbounded(ChannelKey.Z, "Z", "µT"))
            .requires(Sensor.TYPE_MAGNETIC_FIELD)
            .build();

    /**
     * Step count since the phone last booted. Rises, never falls.
     *
     * <p>Android 10 put the step counter behind {@code ACTIVITY_RECOGNITION} and hides it from
     * the sensor list until that is granted — so without the permission the card would read
     * "Unavailable" on a phone that does in fact have the hardware.
     */
    public static final PhoneSensor STEP_COUNTER = PhoneSensor.named("step_counter")
            .displayName("Step Counter")
            .description("Steps counted since the phone last booted. It only ever goes up, so "
                    + "compare two readings to count the steps in between.")
            .type(SensorType.INPUT)
            .image("ic_sensor_step_counter")
            .channel(SensorChannel.unbounded(ChannelKey.STEPS, "Steps", ""))
            .requires(Sensor.TYPE_STEP_COUNTER)
            .permission(stepCounterPermission())
            .derived()
            .build();

    // ----------------------------------------------------------- environment

    /** Ambient brightness at the front of the phone. */
    public static final PhoneSensor LIGHT = PhoneSensor.named("light")
            .displayName("Light")
            .description("How bright it is in front of the phone, in lux. Roughly 10 in a dim "
                    + "room, a few hundred indoors, tens of thousands in sunlight.")
            .type(SensorType.INPUT)
            .image("ic_sensor_light")
            .channel(SensorChannel.unbounded(ChannelKey.LUX, "Illuminance", "lx"))
            .requires(Sensor.TYPE_LIGHT)
            .build();

    /**
     * Distance to whatever is in front of the earpiece. Most phones report only two values —
     * near and far — rather than a smooth distance.
     */
    public static final PhoneSensor PROXIMITY = PhoneSensor.named("proximity")
            .displayName("Proximity")
            .description("How far away the nearest object in front of the earpiece is, in "
                    + "centimetres. Many phones only report near or far rather than a smooth "
                    + "distance.")
            .type(SensorType.INPUT)
            .image("ic_sensor_proximity")
            .channel(SensorChannel.unbounded(ChannelKey.DISTANCE, "Distance", "cm"))
            .requires(Sensor.TYPE_PROXIMITY)
            .build();

    /**
     * Barometric pressure, plus the altitude it works out to. Altitude assumes standard sea level
     * pressure, so it drifts with the weather — good for relative height, not for a map fix.
     */
    public static final PhoneSensor PRESSURE = PhoneSensor.named("pressure")
            .displayName("Pressure")
            .description("Air pressure in hectopascals, about 1013 at sea level. Altitude is "
                    + "worked out from it against standard sea level pressure, so it tracks "
                    + "changes in height well but drifts with the weather.")
            .type(SensorType.INPUT)
            .image("ic_sensor_pressure")
            .channel(SensorChannel.bounded(ChannelKey.PRESSURE, "Pressure", "hPa", 300f, 1100f))
            .channel(SensorChannel.unbounded(ChannelKey.ALTITUDE, "Altitude", "m"))
            .requires(Sensor.TYPE_PRESSURE)
            .build();

    /** Air temperature, on the few phones that have a thermometer for it. */
    public static final PhoneSensor TEMPERATURE = PhoneSensor.named("temperature")
            .displayName("Temperature")
            .description("Air temperature around the phone in degrees Celsius. Only a few "
                    + "phones carry the thermometer this needs.")
            .type(SensorType.INPUT)
            .image("ic_sensor_temperature")
            .channel(SensorChannel.bounded(ChannelKey.TEMPERATURE, "Temperature", "°C", -40f, 85f))
            .requires(Sensor.TYPE_AMBIENT_TEMPERATURE)
            .build();

    /** Relative humidity, on the few phones that have a hygrometer. */
    public static final PhoneSensor HUMIDITY = PhoneSensor.named("humidity")
            .displayName("Humidity")
            .description("Relative humidity of the air as a percentage. Only a few phones carry "
                    + "the hygrometer this needs.")
            .type(SensorType.INPUT)
            .image("ic_sensor_humidity")
            .channel(SensorChannel.bounded(ChannelKey.HUMIDITY, "Humidity", "%", 0f, 100f))
            .requires(Sensor.TYPE_RELATIVE_HUMIDITY)
            .build();

    // ---------------------------------------------------------------- device

    /**
     * Fold angle of a foldable, from a real hinge sensor rather than a fusion. Introduced in
     * Android 11 — {@code getDefaultSensor} simply returns null on anything older, so the card
     * greys itself out without needing a version check here.
     */
    public static final PhoneSensor HINGE_ANGLE = PhoneSensor.named("hinge_angle")
            .displayName("Hinge Angle")
            .description("How far a foldable is open, in degrees. 0° is shut and 180° is flat. "
                    + "Only phones with a hinge have this.")
            .type(SensorType.INPUT)
            .image("ic_sensor_hinge_angle")
            .channel(SensorChannel.bounded(ChannelKey.ANGLE, "Angle", "°", 0f, 360f))
            .requires(Sensor.TYPE_HINGE_ANGLE)
            .build();

    // ------------------------------------------------------------------ body

    /**
     * Pulse from an optical heart rate monitor. Needs the user's permission as well as the
     * hardware, which is why {@link PhoneSensor#permission} exists.
     */
    public static final PhoneSensor HEART_RATE = PhoneSensor.named("heart_rate")
            .displayName("Heart Rate")
            .description("Pulse in beats per minute, from the optical monitor next to the "
                    + "camera. Needs a fingertip held over it, and your permission to read it.")
            .type(SensorType.INPUT)
            .image("ic_sensor_heart_rate")
            .channel(SensorChannel.bounded(ChannelKey.BPM, "Heart Rate", "bpm", 0f, 250f))
            .requires(Sensor.TYPE_HEART_RATE)
            .permission(heartRatePermission())
            .build();

    // ---------------------------------------------------- permission versions

    /** API 36 = Android 16, where the health permissions replaced BODY_SENSORS. */
    private static final int ANDROID_16 = 36;

    /** API 29 = Android 10, where the step counter became a guarded sensor. */
    private static final int ANDROID_10 = 29;

    /**
     * Null below Android 10, where the step counter needs nothing — returning the permission
     * anyway would leave {@code checkSelfPermission} denying a name the platform doesn't know,
     * and the sensor blocked for good.
     */
    private static String stepCounterPermission() {
        return Build.VERSION.SDK_INT >= ANDROID_10
                ? Manifest.permission.ACTIVITY_RECOGNITION
                : null;
    }

    /**
     * Android 16 stopped honouring {@code BODY_SENSORS} for apps targeting it and moved the heart
     * rate monitor behind a health permission. Named as a literal because the constant only
     * exists from API 34, below the versions this still has to run on.
     */
    private static String heartRatePermission() {
        return Build.VERSION.SDK_INT >= ANDROID_16
                ? "android.permission.health.READ_HEART_RATE"
                : Manifest.permission.BODY_SENSORS;
    }

    // -------------------------------------------------------------- registry

    /** Screen order: orientation first, then motion, environment, device, body. */
    private static final List<PhoneSensor> ALL = Collections.unmodifiableList(Arrays.asList(
            ORIENTATION,
            ACCELEROMETER,
            GYROSCOPE,
            GRAVITY,
            LINEAR_ACCELERATION,
            MAGNETOMETER,
            STEP_COUNTER,
            LIGHT,
            PROXIMITY,
            PRESSURE,
            TEMPERATURE,
            HUMIDITY,
            HINGE_ANGLE,
            HEART_RATE));

    private SensorCatalog() {
    }

    /** Everything in the catalog, in the order it should appear on screen. */
    public static List<PhoneSensor> all() {
        return ALL;
    }

    /** Only the sensors this phone actually has the hardware for. */
    public static List<PhoneSensor> availableOn(Context context) {
        SensorManager manager = sensorManager(context);

        List<PhoneSensor> available = new ArrayList<>();
        for (PhoneSensor sensor : ALL) {
            if (sensor.isAvailable(manager)) available.add(sensor);
        }
        return available;
    }

    /** Looks a definition up by its stable name, or null when the name is unknown. */
    public static PhoneSensor byName(String name) {
        if (name == null) return null;

        String trimmed = name.trim();
        for (PhoneSensor sensor : ALL) {
            if (sensor.name.equalsIgnoreCase(trimmed)) return sensor;
        }
        return null;
    }

    public static SensorManager sensorManager(Context context) {
        if (context == null) return null;
        return (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }
}
