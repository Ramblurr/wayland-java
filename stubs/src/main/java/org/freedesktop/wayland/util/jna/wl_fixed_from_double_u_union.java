package org.freedesktop.wayland.util.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;

/**
 * <i>native declaration : /usr/include/wayland-util.h:224</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.com/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> , <a href="http://rococoa.dev.java.net/">Rococoa</a>, or <a href="http://jna.dev.java.net/">JNA</a>.
 */
public class wl_fixed_from_double_u_union extends Union {
    public double d;
    public long   i;

    public wl_fixed_from_double_u_union() {
        super();
    }

    public wl_fixed_from_double_u_union(double d) {
        super();
        this.d = d;
        setType(Double.TYPE);
    }

    public wl_fixed_from_double_u_union(long i) {
        super();
        this.i = i;
        setType(Long.TYPE);
    }

    public wl_fixed_from_double_u_union(Pointer peer) {
        super(peer);
    }

    protected ByReference newByReference() { return new ByReference(); }

    protected ByValue newByValue() { return new ByValue(); }

    protected wl_fixed_from_double_u_union newInstance() { return new wl_fixed_from_double_u_union(); }

    public static class ByReference extends wl_fixed_from_double_u_union implements Structure.ByReference {

    }

    public static class ByValue extends wl_fixed_from_double_u_union implements Structure.ByValue {

    }
}
