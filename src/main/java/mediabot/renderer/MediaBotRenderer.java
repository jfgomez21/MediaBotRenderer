package mediabot.renderer;

import java.io.IOException;
import java.net.URI;

import org.fourthline.cling.support.avtransport.AVTransportException;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.TransportAction;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.renderingcontrol.RenderingControlException;

public interface MediaBotRenderer {
	public void setURI(URI uri) throws AVTransportException;
	public MediaInfo getCurrentMediaInfo();
	public TransportInfo getCurrentTransportInfo();
	public PositionInfo getCurrentPositionInfo();
	public TransportAction[] getCurrentTransportActions();
	public void setVolume(int volume) throws RenderingControlException ;
	public void setMute(boolean mute) throws RenderingControlException ;
	public int getVolume();
	public void play() throws AVTransportException;
	public void stop() throws AVTransportException;
	public void pause() throws AVTransportException;
	public void seek(SeekMode mode, long timestamp) throws AVTransportException;
	public void start() throws IOException;
}
