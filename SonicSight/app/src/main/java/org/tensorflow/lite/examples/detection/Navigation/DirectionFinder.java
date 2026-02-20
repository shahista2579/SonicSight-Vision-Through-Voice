package org.tensorflow.lite.examples.detection.Navigation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DirectionFinder {
    private final DirectionFinderListener listener;
    private final String origin;
    private final String destination;

    public DirectionFinder(DirectionFinderListener listener, String origin, String destination) {
        this.listener = listener;
        this.origin = origin;
        this.destination = destination;
    }


    public String createUrll() throws UnsupportedEncodingException {
        return URLEncoder.encode(destination, "utf-8");
    }

}
