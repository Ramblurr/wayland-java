package org.freedesktop.wayland.server;

public class ClientCredentials {
    private final int pid;
    private final int uid;
    private final int gid;

    public ClientCredentials(final int pid,
                             final int uid,
                             final int gid) {
        this.pid = pid;
        this.uid = uid;
        this.gid = gid;
    }

    public int getPid() {
        return pid;
    }

    public int getUid() {
        return uid;
    }

    public int getGid() {
        return gid;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        final ClientCredentials that = (ClientCredentials) o;

        return pid == that.pid && uid == that.uid && gid == that.gid;
    }

    @Override
    public int hashCode() {
        int result = pid;
        result = 31 * result + uid;
        result = 31 * result + gid;
        return result;
    }
}
