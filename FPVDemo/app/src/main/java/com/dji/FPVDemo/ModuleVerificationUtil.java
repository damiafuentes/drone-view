package com.dji.FPVDemo;

import android.support.annotation.Nullable;

import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.Simulator;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;

/**
 * Created by dji on 16/1/6.
 */
public class ModuleVerificationUtil {
    public static boolean isProductModuleAvailable() {
        return (null != FPVDemoApplication.getProductInstance());
    }

    public static boolean isAircraft() {
        return FPVDemoApplication.getProductInstance() instanceof Aircraft;
    }

    public static boolean isHandHeld() {
        return FPVDemoApplication.getProductInstance() instanceof HandHeld;
    }

    public static boolean isCameraModuleAvailable() {
        return isProductModuleAvailable() && (null != FPVDemoApplication.getProductInstance().getCamera());
    }

    public static boolean isPlaybackAvailable() {
        return isCameraModuleAvailable() && (null != FPVDemoApplication.getProductInstance()
                .getCamera()
                .getPlaybackManager());
    }

    public static boolean isMediaManagerAvailable() {
        return isCameraModuleAvailable() && (null != FPVDemoApplication.getProductInstance()
                .getCamera()
                .getMediaManager());
    }

    public static boolean isRemoteControllerAvailable() {
        return isProductModuleAvailable() && isAircraft() && (null != FPVDemoApplication.getAircraftInstance()
                .getRemoteController());
    }

    public static boolean isFlightControllerAvailable() {
        return isProductModuleAvailable() && isAircraft() && (null != FPVDemoApplication.getAircraftInstance()
                .getFlightController());
    }

    public static boolean isCompassAvailable() {
        return isFlightControllerAvailable() && isAircraft() && (null != FPVDemoApplication.getAircraftInstance()
                .getFlightController()
                .getCompass());
    }

    public static boolean isFlightLimitationAvailable() {
        return isFlightControllerAvailable() && isAircraft();
    }

    public static boolean isGimbalModuleAvailable() {
        return isProductModuleAvailable() && (null != FPVDemoApplication.getProductInstance().getGimbal());
    }

    public static boolean isAirlinkAvailable() {
        return isProductModuleAvailable() && (null != FPVDemoApplication.getProductInstance().getAirLink());
    }

    public static boolean isWiFiLinkAvailable() {
        return isAirlinkAvailable() && (null != FPVDemoApplication.getProductInstance().getAirLink().getWiFiLink());
    }

    public static boolean isLightbridgeLinkAvailable() {
        return isAirlinkAvailable() && (null != FPVDemoApplication.getProductInstance()
                .getAirLink()
                .getLightbridgeLink());
    }

    @Nullable
    public static Simulator getSimulator() {
        Aircraft aircraft = FPVDemoApplication.getAircraftInstance();
        if (aircraft != null) {
            FlightController flightController = aircraft.getFlightController();
            if (flightController != null) {
                return flightController.getSimulator();
            }
        }
        return null;
    }

    @Nullable
    public static FlightController getFlightController() {
        Aircraft aircraft = FPVDemoApplication.getAircraftInstance();
        if (aircraft != null) {
            return aircraft.getFlightController();
        }
        return null;
    }

}