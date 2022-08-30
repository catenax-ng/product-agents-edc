package io.catenax.knowledge.dataspace.edc;

import java.io.IOException;
import java.lang.reflect.*;

import javax.servlet.ReadListener;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

/**
 * An invocation handler which maps all jakarta objects
 * to a javax.servlet level
 */
public class JakartaServletInputStreamWrapper extends javax.servlet.ServletInputStream implements IJakartaWrapper<jakarta.servlet.ServletInputStream> {
    
    jakarta.servlet.ServletInputStream jakartaDelegate;
    Monitor monitor;

    public JakartaServletInputStreamWrapper(jakarta.servlet.ServletInputStream jakartaDelegate, Monitor monitor) {
        this.jakartaDelegate=jakartaDelegate;
        this.monitor=monitor;
    }

    @Override
    public jakarta.servlet.ServletInputStream getDelegate() {
        return jakartaDelegate;
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public boolean isFinished() {
        return jakartaDelegate.isFinished();
    }

    @Override
    public boolean isReady() {
        return jakartaDelegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        //jakartaDelegate.setReadListener(readListener);
    }

    @Override
    public int read() throws IOException {
        return jakartaDelegate.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return jakartaDelegate.read(buf);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return jakartaDelegate.read(b, off, len);
    }

}
