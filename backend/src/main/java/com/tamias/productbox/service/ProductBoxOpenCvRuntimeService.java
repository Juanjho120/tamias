package com.tamias.productbox.service;

import com.tamias.common.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductBoxOpenCvRuntimeService {

    public static final String OPENCV_DISABLED_MESSAGE =
            "Product Box OpenCV texture processing is disabled in this environment.";

    public static final String OPENCV_UNAVAILABLE_MESSAGE =
            "Product Box OpenCV texture processing is not available in this runtime.";

    private static final Logger log = LoggerFactory.getLogger(ProductBoxOpenCvRuntimeService.class);

    private final boolean opencvConfigured;

    private volatile boolean openCvLoaded;
    private volatile Throwable openCvLoadFailure;

    public ProductBoxOpenCvRuntimeService(
            @Value("${tamias.product-box.opencv.enabled:true}") boolean opencvConfigured
    ) {
        this.opencvConfigured = opencvConfigured;
    }

    public boolean isConfigured() {
        return opencvConfigured;
    }

    public boolean isAvailable() {
        if (!opencvConfigured) {
            return false;
        }

        try {
            requireAvailable();
            return true;
        } catch (BadRequestException ex) {
            return false;
        }
    }

    public String getDisabledMessage() {
        if (!opencvConfigured) {
            return OPENCV_DISABLED_MESSAGE;
        }

        if (!isAvailable()) {
            return OPENCV_UNAVAILABLE_MESSAGE;
        }

        return null;
    }

    public synchronized void requireAvailable() {
        if (!opencvConfigured) {
            throw new BadRequestException(OPENCV_DISABLED_MESSAGE);
        }

        if (openCvLoaded) {
            return;
        }

        if (openCvLoadFailure != null) {
            throw new BadRequestException(OPENCV_UNAVAILABLE_MESSAGE);
        }

        try {
            nu.pattern.OpenCV.loadLocally();
            openCvLoaded = true;
        } catch (LinkageError | RuntimeException ex) {
            openCvLoadFailure = ex;
            log.warn("OpenCV native runtime could not be initialized for Product Box textures.", ex);
            throw new BadRequestException(OPENCV_UNAVAILABLE_MESSAGE);
        }
    }
}
