package mediabot.renderer.mpv;

import java.io.Closeable;
import java.io.IOException;

public interface MpvClient extends Closeable {
	public void open(String filepath) throws IOException;

	public int read(byte[] bytes) throws IOException;

	public void write(byte[] bytes) throws IOException;
}
