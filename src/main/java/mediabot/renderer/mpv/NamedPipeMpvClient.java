package mediabot.renderer.mpv;

import java.io.IOException;
import java.io.RandomAccessFile;

public class NamedPipeMpvClient implements MpvClient {
	private RandomAccessFile file;
	
	@Override
	public void open(String filepath) throws IOException {
		file = new RandomAccessFile(filepath, "rw");
	}

	@Override
	public int read(byte[] bytes) throws IOException {
		return file.read(bytes);
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		file.write(bytes);
	}

	@Override
	public void close() throws IOException {
		if(file != null){
			file.close();
		}
	}
}
