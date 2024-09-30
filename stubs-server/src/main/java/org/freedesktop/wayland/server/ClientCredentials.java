/*
 * Copyright © 2015 Erik De Rijcke
 * Copyright © 2024 Casey Link
 *
 * Licensed under the Apache License,Version2.0(the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

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
        return this.pid;
    }

    public int getUid() {
        return this.uid;
    }

    public int getGid() {
        return this.gid;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ClientCredentials that = (ClientCredentials) o;

        return this.pid == that.pid && this.uid == that.uid && this.gid == that.gid;
    }

    @Override
    public int hashCode() {
        int result = this.pid;
        result = 31 * result + this.uid;
        result = 31 * result + this.gid;
        return result;
    }
}
