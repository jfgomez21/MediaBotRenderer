package mediabot.renderer.mpv;

import java.io.IOException;

import java.net.URI;

import java.nio.charset.StandardCharsets;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.types.ErrorCode;
import org.fourthline.cling.support.avtransport.AVTransportException;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.TransportAction;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.RenderingControlException;
import org.fourthline.cling.support.renderingcontrol.lastchange.ChannelMute;
import org.fourthline.cling.support.renderingcontrol.lastchange.ChannelVolume;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlVariable;

import org.seamless.util.OS;

import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;

import mediabot.renderer.MediaBotRenderer;
import mediabot.renderer.mpv.models.MpvRequestModel;
import mediabot.renderer.mpv.models.MpvResponseModel;

public class MpvMediaBotRenderer implements MediaBotRenderer {
	private static final Logger log = Logger.getLogger(MpvMediaBotRenderer.class.getName());
	private static final long IDLE_SLEEP_TIME = 250;

	private TransportInfo currentTransportInfo = new TransportInfo();
	private PositionInfo currentPositionInfo = new PositionInfo();
	private MediaInfo currentMediaInfo = new MediaInfo();
	private Lock lock = new ReentrantLock();
	private Condition condition  = lock.newCondition(); 
	private AtomicInteger count = new AtomicInteger();
	private byte[] buffer = new byte[2048];

	private MpvClient client;
	private LastChange avTransportLastChange;
        private LastChange renderingControlLastChange;
	private JSON json;

	private long readTimestamp = 0;
	private long time = 0;
	private int volume = 100;
	private int previousVolume = 100;
	private boolean isEOF = false;

	public MpvMediaBotRenderer(LastChange avTransportLastChange, LastChange renderingControlLastChange){
		this.avTransportLastChange = avTransportLastChange;
		this.renderingControlLastChange = renderingControlLastChange;

		json = JSON.builder().register(JacksonAnnotationExtension.std).build();
	}

	private void write(MpvRequestModel request) throws IOException {
		lock.lock();

		try{
			String str = json.asString(request);

			log.fine(String.format("sent - %s", str));

			client.write(String.format("%s\n", str).getBytes(StandardCharsets.UTF_8));

			count.incrementAndGet();
			condition.signalAll();
		}
		finally{
			lock.unlock();
		}
	}

	private TransportState getTransportState(){
		return currentTransportInfo.getCurrentTransportState();
	}

	private void transportStateChanged(TransportState newState) {
		lock.lock();

		try{
			TransportState currentTransportState = getTransportState();

			log.info("Current state is: " + currentTransportState + ", changing to new state: " + newState);

			currentTransportInfo = new TransportInfo(newState);

			avTransportLastChange.setEventedValue(
				1,
				new AVTransportVariable.TransportState(newState),
				new AVTransportVariable.CurrentTransportActions(getCurrentTransportActions())
			);
		}
		finally{
			lock.unlock();
		}
	}

	@Override
	public void setURI(URI uri) throws AVTransportException {
		lock.lock();

		try{
			TransportState state = getTransportState();
			boolean isPaused = false;

			if(state == TransportState.PLAYING || state == TransportState.PAUSED_PLAYBACK){
				isPaused = state == TransportState.PAUSED_PLAYBACK;

				transportStateChanged(TransportState.STOPPED);
			}

			String str = uri.toString();

			MpvRequestModel request = new MpvRequestModel();
			request.addCommands("loadfile", str, "replace");
			
			write(request);

			currentMediaInfo = new MediaInfo(str, "");
			currentPositionInfo = new PositionInfo(1, "", str);

			avTransportLastChange.setEventedValue(
				1,
				new AVTransportVariable.AVTransportURI(uri),
				new AVTransportVariable.CurrentTrackURI(uri)
			);

			if(isPaused || isEOF){
				request = new MpvRequestModel();
				request.addCommands("set_property", "pause", false);

				write(request);
			}

			transportStateChanged(TransportState.PLAYING);	
			isEOF = false;
		}
		catch(IOException ex){
			throw new AVTransportException(ErrorCode.ACTION_FAILED.getCode(), "failed to set URI", ex);
		}
		finally{
			lock.unlock();
		}
	}

	@Override
	public TransportInfo getCurrentTransportInfo(){ //returns the current transport state
		return currentTransportInfo;
	}

	@Override
	public MediaInfo getCurrentMediaInfo(){
		return currentMediaInfo;
	}

	@Override
	public PositionInfo getCurrentPositionInfo(){
		return currentPositionInfo;
	}

	private <T> T[] toArray(T ... values){
		return values;
	}

	@Override
	public TransportAction[] getCurrentTransportActions(){ //returns the available actions based on the transport state
		TransportState state = getTransportState();
		TransportAction[] actions = null;

		if(state == TransportState.STOPPED){
			actions = toArray(TransportAction.Play);
		}
		else if(state == TransportState.PLAYING){
			actions = toArray(TransportAction.Stop, TransportAction.Pause, TransportAction.Seek);
		}
		else if(state == TransportState.PAUSED_PLAYBACK){
			actions = toArray(TransportAction.Stop, TransportAction.Pause, TransportAction.Seek, TransportAction.Play);
		}

		return actions;
	}

	private void setVolume(int volume, boolean updateMpv) throws RenderingControlException {
		lock.lock();

		try{
			if(volume != this.volume){
				if(updateMpv){
					MpvRequestModel request = new MpvRequestModel();
					request.addCommands("set_property", "volume", volume);

					write(request);
				}

				previousVolume = this.volume;
				this.volume = volume;

				RenderingControlVariable.Mute mute = null;

				if((previousVolume == 0 && volume > 0) || (previousVolume > 0 && volume == 0)){
					mute = new RenderingControlVariable.Mute(new ChannelMute(Channel.Master, previousVolume > 0 && volume == 0));
				}

				renderingControlLastChange.setEventedValue(
					1,
					new RenderingControlVariable.Volume(new ChannelVolume(Channel.Master, volume)),
					mute	
				);
			}
		}
		catch(IOException ex){
			throw new RenderingControlException(ErrorCode.ACTION_FAILED.getCode(), "failed to set volume", ex);
		}
		finally{
			lock.unlock();
		}
	}

	@Override
	public void setVolume(int volume) throws RenderingControlException  {
		setVolume(volume, true);	
	}	

	@Override
	public int getVolume(){
		return volume;
	}

	@Override
	public void setMute(boolean mute) throws RenderingControlException  {
		if(mute){
			if(getVolume() > 0) {
				setVolume(0);
			}
		}
		else{
			if(getVolume() == 0){
				setVolume(previousVolume);
			}

		}
	}

	private void play(boolean updateMpv) throws AVTransportException {
		lock.lock();

		try{
			TransportState state = getTransportState();

			if(state == TransportState.PAUSED_PLAYBACK){
				if(updateMpv){
					MpvRequestModel request = new MpvRequestModel();
					request.addCommands("set_property", "pause", false);

					write(request);
				}

				transportStateChanged(TransportState.PLAYING);
			}
			else if(state == TransportState.STOPPED){
				if(updateMpv){
					MpvRequestModel request = new MpvRequestModel();
					request.addCommands("playlist-play-index", "current");

					write(request);
				}

				transportStateChanged(TransportState.PLAYING);
				isEOF = false;
			}
		}
		catch(IOException ex){
			throw new AVTransportException(ErrorCode.ACTION_FAILED.getCode(), "failed to change state to play", ex);
		}
		finally{
			lock.unlock();
		}
	}

	@Override
	public void play() throws AVTransportException {
		play(true);	
	}

	private void stop(boolean updateMpv) throws AVTransportException {
		lock.lock();

		try{
			if(getTransportState() != TransportState.STOPPED){
				if(updateMpv){
					//TODO - stop playback
					//pause
					//seek back to the start
				}

				transportStateChanged(TransportState.STOPPED);
			}
		}
		finally{
			lock.unlock();
		}
	}

	@Override
	public void stop() throws AVTransportException {
		/*
		try{

		}
		catch(IOException ex){
			throw new AVTransportException(ErrorCode.ACTION_FAILED.getCode(), "failed to set volume", ex);
		}
		*/
	}

	private void pause(boolean updateMpv) throws AVTransportException {
		lock.lock();

		try{
			if(updateMpv){
				MpvRequestModel request = new MpvRequestModel();
				request.addCommands("set_property", "pause", true);

				write(request);
			}

			transportStateChanged(TransportState.PAUSED_PLAYBACK);
		}
		catch(IOException ex){
			throw new AVTransportException(ErrorCode.ACTION_FAILED.getCode(), "failed to pause", ex);
		}
		finally{
			lock.unlock();
		}
	}

	@Override
	public void pause() throws AVTransportException {
		pause(true);	
	}

	@Override
	public void seek(SeekMode mode, long timestamp) throws AVTransportException {
		lock.lock();

		try{
			MpvRequestModel request = new MpvRequestModel();
			request.addCommands("seek", timestamp, "absolute+keyframes");

			write(request);
		}
		catch(IOException ex){
			throw new AVTransportException(ErrorCode.ACTION_FAILED.getCode(), "failed to set volume", ex);
		}
		finally{
			lock.unlock();
		}
	}	

	private void addListeners(){
		List<String> properties = Arrays.asList("volume", "time-pos", "eof-reached", "pause");

		for(String property : properties){
			try{
				MpvRequestModel request = new MpvRequestModel();
				request.addCommands("observe_property", 1, property);

				write(request);
			}
			catch(IOException ex){
				log.log(Level.SEVERE, String.format("failed to observe property - %s", property), ex);
			}
		}
	}

	private boolean isReadNeeded(){
		return count.get() > 0 || (getTransportState() == TransportState.PLAYING && (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - readTimestamp) > IDLE_SLEEP_TIME));
	}

	private int read(Queue<MpvResponseModel> queue) throws IOException {
		lock.lock();

		try{
			if(isReadNeeded()){
				//TODO - handle the case when partial lines are returned

				int byteCount = client.read(buffer);

				readTimestamp = System.nanoTime();

				if(byteCount > 0){
					String str = new String(buffer, 0, byteCount, StandardCharsets.UTF_8);

					log.fine(String.format("received - %s", str));

					String[] lines = str.split("\n");

					for(String line : lines){
						try{
							queue.offer(json.beanFrom(MpvResponseModel.class, line));
						}
						catch(IOException ex){
							log.log(Level.FINE, "failed to parse json", ex);
						}
					}

					count.set(Math.max(0, count.get() - lines.length));

					return lines.length;
				}

				count.set(0);

				return -1;
			}
			else{
				try{
					condition.await(IDLE_SLEEP_TIME, TimeUnit.MILLISECONDS);

					//we need to ping the server when paused so that we don't 
					//block when waiting for a response
					//this enables us to pause/unpause from both the MPV and the UPNP client
					if(getTransportState() == TransportState.PAUSED_PLAYBACK){
						MpvRequestModel request = new MpvRequestModel();
						request.addCommands("get_time_us");

						write(request);
					}
				}
				catch(InterruptedException ex){
					log.fine("interrupted while waiting");
				}
				catch(IOException ex){
					log.log(Level.SEVERE, "failed to write command - get_time_us", ex);

					return -1;
				}
			}

			return 0;
		}
		finally{
			lock.unlock();
		}
	}

	private boolean isEvent(MpvResponseModel response){
		String event = response.getEvent();

		return event != null && !event.isEmpty();
	}

	private void setTimePosition(int value){
		lock.lock();

		try{
			if(value != time){
				currentPositionInfo = new PositionInfo(
					1,
					currentMediaInfo.getMediaDuration(),
					currentMediaInfo.getCurrentURI(),
					ModelUtil.toTimeString(value),
					ModelUtil.toTimeString(value)
				);

				time = value;
			}
		}
		finally{
			lock.unlock();
		}
	}

	private void togglePause(boolean value) throws AVTransportException {
		lock.lock();

		try{
			TransportState state = getTransportState();

			if(value){
				if(state == TransportState.PLAYING){
					pause(false);
				}
			}
			else{
				if(state == TransportState.PAUSED_PLAYBACK){
					play(false);
				}
			}
		}
		finally{
			lock.unlock();
		}
	}

	private void eofReached(boolean value){
		lock.lock();

		try{
			if(!value){
				if(getTransportState() != TransportState.PLAYING){
					transportStateChanged(TransportState.PLAYING);
				}
			}
			else{
				transportStateChanged(TransportState.STOPPED);
			}

			isEOF = value;
		}
		finally{
			lock.unlock();
		}
	}

	private void processMpvResponse(MpvResponseModel response){
		try{
			if(isEvent(response)){
				String event = response.getEvent();

				if("property-change".equals(event)){
					String name = response.getName();
					Object data = response.getData();

					if(data != null){
						if("volume".equals(name)){
							setVolume(((Number) data).intValue(), false);
						}
						else if("time-pos".equals(name)){
							setTimePosition(((Number) data).intValue());		
						}	
						else if("pause".equals(name)){
							togglePause(((Boolean) data).booleanValue());				
						}
						else if("eof-reached".equals(name)){
							//set the EOF flag and fire the state change
							//when selecting a new file to play
							//the eof-reached event is thrown with
							//the data set to false
							eofReached(((Boolean) data).booleanValue());
						}
					}
					else if("eof-reached".equals(name)){
						//set the EOF flag and fire the state change
						//when the eof is reached, there is no data 
						//associated with the event
						eofReached(true);
					}
				}
			}
		}
		catch(/*RenderingControlException | AVTransportException*/ Exception ex){
			log.log(Level.SEVERE, "failed processing input", ex);
		}
	}

	private void sleep(long timestamp){
		try{
			Thread.sleep(timestamp);
		}
		catch(InterruptedException ex){
			log.log(Level.SEVERE, "interrupted while waiting for MPV to start up", ex);
		}
	}

	public void start() throws IOException {
		String filepath = System.getProperty("MEDIABOT_RENDERER_MPV_FILEPATH");

		if(filepath == null){
			filepath = "mpv";

			if(OS.checkForWindows()){
				filepath = String.format("%s.exe", filepath);
			}
		}
		
		log.fine(String.format("mpv filepath - %s", filepath));

		String pipe = System.getProperty("MEDIABOT_RENDERER_IPC_FILEPATH"); 

		if(pipe == null){
			pipe = FilenameUtils.concat(System.getProperty("java.io.tmpdir"), "mediabot");

			if(OS.checkForWindows()){
				pipe = "\\\\.\\mediabot";
			}
		}	

		log.fine(String.format("input-ipc-servce path - %s", pipe));

		String[] args = toArray(filepath, "--no-terminal", "--fs", "--idle", "--force-window", String.format("--input-ipc-server=%s", pipe));

		if(OS.checkForWindows()){
			args = toArray("cmd", "/c", "start", "/wait", filepath, "--no-terminal", "--fs", "--keep-open=yes", String.format("--input-ipc-server=%s", pipe));
		}  

		Process process = Runtime.getRuntime().exec(args);

		sleep(1500);

		if(OS.checkForWindows()){
			client = new NamedPipeMpvClient();
		}
		else{
			client = new NetcatMpvClient();
		}
		
		try{
			client.open(pipe);

			addListeners();

			Queue<MpvResponseModel> queue = new ArrayDeque<>();
			int count = 0;

			while(process.isAlive() && (count = read(queue)) > -1){
				while(!queue.isEmpty()){
					processMpvResponse(queue.poll());
				}	
			}
		}
		finally{
			IOUtils.closeQuietly(client);
		}
	}
}
