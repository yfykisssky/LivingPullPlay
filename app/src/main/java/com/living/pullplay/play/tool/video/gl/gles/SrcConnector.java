package com.living.pullplay.play.tool.video.gl.gles;

import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;

public class SrcConnector<T> {
    private LinkedList<SinkConnector<T>> plugList = new LinkedList<SinkConnector<T>>();
    private Object mFormat;

    public SrcConnector() {
    }

    public synchronized boolean isConnected() {
        return !this.plugList.isEmpty();
    }

    public synchronized void connect(SinkConnector<T> sink) {
        if (!this.plugList.contains(sink)) {
            this.plugList.add(sink);
            sink.onConnected();
            if (mFormat != null) {
                sink.onFormatChanged(mFormat);
            }
        }
    }

    public synchronized void onFormatChanged(Object format) {
        mFormat = format;
        Iterator<SinkConnector<T>> it = this.plugList.iterator();
        while (it.hasNext()) {
            SinkConnector<T> pin = it.next();
            pin.onFormatChanged(format);
        }
    }

    public synchronized void onFrameAvailable(T frame, ByteBuffer byteBuffer) {
        Iterator<SinkConnector<T>> it = this.plugList.iterator();
        while (it.hasNext()) {
            SinkConnector<T> sink = it.next();
            sink.onFrameAvailable(frame, byteBuffer);
        }
    }

    public synchronized void setSurface(Surface surface) {

        Iterator<SinkConnector<T>> it = this.plugList.iterator();
        while (it.hasNext()) {
            SinkConnector<T> sink = it.next();
            sink.setSurface(surface);
        }
    }

    public synchronized void disconnect() {
        this.disconnect(null);
    }

    public synchronized void disconnect(SinkConnector<T> sink) {
        if (sink != null) {
            sink.onDisconnect();
            this.plugList.remove(sink);
        } else {
            Iterator it = this.plugList.iterator();
            while (it.hasNext()) {
                SinkConnector pin = (SinkConnector) it.next();
                pin.onDisconnect();
            }
            this.plugList.clear();
        }

    }
}
