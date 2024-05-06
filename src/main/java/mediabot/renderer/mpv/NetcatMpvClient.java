package mediabot.renderer.mpv;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NetcatMpvClient implements MpvClient {
	private static final Logger log = Logger.getLogger(NetcatMpvClient.class.getName());
	private Process process;

	@Override
	public void open(String filepath) throws IOException {
		String[] args = {"nc", "-U", filepath};

		process = Runtime.getRuntime().exec(args);
	}

	@Override
	public int read(byte[] bytes) throws IOException {
		return process.getInputStream().read(bytes);
	}

	@Override
	public void write(byte[] bytes) throws IOException {
		process.getOutputStream().write(bytes);
		process.getOutputStream().flush();
	}

	@Override
	public void close() throws IOException {
		try{
			if(process != null){
				process.destroy();
				process.waitFor();
			}
		}
		catch(InterruptedException ex){
			log.log(Level.FINE, "interrupted while waiting process to finish", ex);
		}
	}	
}
