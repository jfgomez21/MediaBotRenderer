package mediabot.renderer.upnp;

import java.net.URI;
import java.util.logging.Logger;
import java.util.Map;

import mediabot.renderer.MediaBotRenderer;

import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.types.ErrorCode;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.AbstractAVTransportService;
import org.fourthline.cling.support.avtransport.AVTransportErrorCode;
import org.fourthline.cling.support.avtransport.AVTransportException;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.DeviceCapabilities;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PlayMode;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.StorageMedium;
import org.fourthline.cling.support.model.TransportAction;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportSettings;
import org.seamless.http.HttpFetch;
import org.seamless.util.URIUtil;

public class MediaBotAVTransportService extends AbstractAVTransportService {
	private final static Logger log = Logger.getLogger(MediaBotAVTransportService.class.getName());

	private MediaBotRenderer mediaRenderer;

	public MediaBotAVTransportService(LastChange lastChange, MediaBotRenderer mediaRenderer) {
		super(lastChange);

		this.mediaRenderer = mediaRenderer;
	}

	@Override
	public void setAVTransportURI(UnsignedIntegerFourBytes instanceId, String currentURI, String currentURIMetaData) throws AVTransportException {
		URI uri;

		try {
			uri = new URI(currentURI);
		} catch (Exception ex) {
			throw new AVTransportException(ErrorCode.INVALID_ARGS, "CurrentURI can not be null or malformed");
		}

		if (currentURI.startsWith("http:")) {
			try {
				HttpFetch.validate(URIUtil.toURL(uri));
			} catch (Exception ex) {
				throw new AVTransportException(AVTransportErrorCode.RESOURCE_NOT_FOUND, ex.getMessage());
			}
		} 
		else if (!currentURI.startsWith("file:")) {
			throw new AVTransportException(ErrorCode.INVALID_ARGS, "Only HTTP and file: resource identifiers are supported");
		}

		// TODO: Check mime type of resource against supported types
		// TODO: DIDL fragment parsing and handling of currentURIMetaData

		mediaRenderer.setURI(uri);
	}

	@Override
	public MediaInfo getMediaInfo(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		return mediaRenderer.getCurrentMediaInfo();
	}

	@Override
	public TransportInfo getTransportInfo(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		return mediaRenderer.getCurrentTransportInfo();
	}

	@Override
	public PositionInfo getPositionInfo(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		return mediaRenderer.getCurrentPositionInfo();
	}

	@Override
	public DeviceCapabilities getDeviceCapabilities(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		return new DeviceCapabilities(new StorageMedium[]{StorageMedium.NETWORK});
	}

	@Override
	public TransportSettings getTransportSettings(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		return new TransportSettings(PlayMode.NORMAL);
	}

	@Override
	public void stop(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		mediaRenderer.stop();
	}

	@Override
	public void play(UnsignedIntegerFourBytes instanceId, String speed) throws AVTransportException {
		mediaRenderer.play();
	}

	@Override
	public void pause(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		mediaRenderer.pause();
	}

	@Override
	public void record(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		log.info("Record not implemented");
	}

	@Override
	public void seek(UnsignedIntegerFourBytes instanceId, String unit, String target) throws AVTransportException {
		log.fine(String.format("unit - %s target - %s value - %d", unit, target, ModelUtil.fromTimeString(target)));

		SeekMode seekMode = SeekMode.valueOrExceptionOf(unit);

		if (!seekMode.equals(SeekMode.REL_TIME)) {
			throw new AVTransportException(AVTransportErrorCode.SEEKMODE_NOT_SUPPORTED, "Unsupported seek mode: " + unit);
		}

		mediaRenderer.seek(seekMode, ModelUtil.fromTimeString(target));

	}

	@Override
	public void next(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		//TODO - next
	}

	@Override
	public void previous(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		//TODO - previous
	}

	@Override
	public void setNextAVTransportURI(UnsignedIntegerFourBytes instanceId, String nextURI, String nextURIMetaData) throws AVTransportException {
		//TODO - next uri
	}

	@Override
	public void setPlayMode(UnsignedIntegerFourBytes instanceId, String newPlayMode) throws AVTransportException {
		//TODO - play mode
	}

	@Override
	public void setRecordQualityMode(UnsignedIntegerFourBytes instanceId, String newRecordQualityMode) throws AVTransportException {
		log.info("SetRecordQualityMode not implemented");
	}

	@Override
	protected TransportAction[] getCurrentTransportActions(UnsignedIntegerFourBytes instanceId) throws Exception {
		return mediaRenderer.getCurrentTransportActions();
	}

	@Override
	public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
		return new UnsignedIntegerFourBytes[] {
			new UnsignedIntegerFourBytes(1L)
		};
	}
}
