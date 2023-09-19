package org.jboss.nexus.content;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;

/** Interface representing one of the assets.
 */
public interface Asset {
    /** Usually relative path of the file within repository, such as "something/file.jar"
     *
     * @return name of the asset
     */
    public String name();

    /** Size of the asset (length of the file)
     *
     * @return size
     */
    public long size();

    public InputStream openContentInputStream() throws IOException;
}
