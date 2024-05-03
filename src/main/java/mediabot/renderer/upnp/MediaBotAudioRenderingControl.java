package mediabot.renderer.upnp;

import java.util.logging.Logger;
import java.util.Map;

import mediabot.renderer.MediaBotRenderer;

import org.fourthline.cling.model.types.ErrorCode;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.model.types.UnsignedIntegerTwoBytes;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.renderingcontrol.AbstractAudioRenderingControl;
import org.fourthline.cling.support.renderingcontrol.RenderingControlErrorCode;
import org.fourthline.cling.support.renderingcontrol.RenderingControlException;

public class MediaBotAudioRenderingControl extends AbstractAudioRenderingControl {
	private final static Logger log = Logger.getLogger(MediaBotAudioRenderingControl.class.getName());

	private MediaBotRenderer mediaRenderer;

	public MediaBotAudioRenderingControl(LastChange lastChange, MediaBotRenderer mediaRenderer) {
		super(lastChange);

		this.mediaRenderer = mediaRenderer;
	}

	protected void checkChannel(String channelName) throws RenderingControlException {
		if (!getChannel(channelName).equals(Channel.Master)) {
			throw new RenderingControlException(ErrorCode.ARGUMENT_VALUE_INVALID, "Unsupported audio channel: " + channelName);
		}
	}

	@Override
	public boolean getMute(UnsignedIntegerFourBytes instanceId, String channelName) throws RenderingControlException {
		checkChannel(channelName);

		return mediaRenderer.getVolume() == 0;
	}

	@Override
	public void setMute(UnsignedIntegerFourBytes instanceId, String channelName, boolean desiredMute) throws RenderingControlException {
		checkChannel(channelName);

		log.fine("Setting backend mute to: " + desiredMute);

		mediaRenderer.setMute(desiredMute);
	}

	@Override
	public UnsignedIntegerTwoBytes getVolume(UnsignedIntegerFourBytes instanceId, String channelName) throws RenderingControlException {
		checkChannel(channelName);

		int volume = mediaRenderer.getVolume();
		
		log.fine("Getting backend volume: " + volume);

		return new UnsignedIntegerTwoBytes(volume);
	}

	@Override
	public void setVolume(UnsignedIntegerFourBytes instanceId, String channelName, UnsignedIntegerTwoBytes desiredVolume) throws RenderingControlException {
		checkChannel(channelName);

		int volume = desiredVolume.getValue().intValue();

		log.fine("Setting backend volume to: " + volume);

		mediaRenderer.setVolume(volume);
	}

	@Override
	protected Channel[] getCurrentChannels() {
		return new Channel[] {
			Channel.Master
		};
	}

	@Override
	public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
		return new UnsignedIntegerFourBytes[] {
			new UnsignedIntegerFourBytes(1L)
		};	
	}
}
