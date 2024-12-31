package mediabot.renderer;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import mediabot.renderer.mpv.MpvMediaBotRenderer;
import mediabot.renderer.swt.SwtContentDirectoryClient;
import mediabot.renderer.upnp.MediaBotAVTransportService;
import mediabot.renderer.upnp.MediaBotAudioRenderingControl;
import org.apache.commons.io.IOUtils;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.binding.LocalServiceBinder;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.ServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.connectionmanager.ConnectionManagerService;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.lastchange.LastChangeAwareServiceManager;
import org.fourthline.cling.support.model.ProtocolInfo;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;
import org.seamless.util.MimeType;

public class Main {
	public static final long LAST_CHANGE_FIRING_INTERVAL_MILLISECONDS = 500;
	
	private static byte[] getResourceAsBytes(String filename) throws IOException {
		InputStream input = null;

		try{
			if(filename.startsWith("classpath:/")){
				input = Main.class.getClassLoader().getResourceAsStream(filename.replace("classpath:/", ""));
			}
			else{
				input = new FileInputStream(filename);
			}

			return IOUtils.toByteArray(input);
		}
		finally{
			IOUtils.closeQuietly(input);
		}
	}

	private static Icon createDeviceIcon(byte[] bytes, int width, int height, int depth) throws IOException {
		return new Icon("image/png", width, height, depth, MediaBotRenderer.class.getName(), bytes);
	}

	private static Icon createDeviceIcon(String filename) throws IOException {
		byte[] bytes = getResourceAsBytes(filename);

		try(InputStream input = new ByteArrayInputStream(bytes)){
			BufferedImage image = ImageIO.read(input);

			return createDeviceIcon(bytes, image.getWidth(), image.getHeight(), image.getColorModel().getPixelSize());
		}	
	}
	
	public static void main(String[] args){
		LastChange avTransportLastChange = new LastChange(new AVTransportLastChangeParser());
		LastChange renderingControlLastChange = new LastChange(new RenderingControlLastChangeParser());
		MediaBotRenderer mediaRenderer = new MpvMediaBotRenderer(avTransportLastChange, renderingControlLastChange);

		LocalServiceBinder binder = new AnnotationLocalServiceBinder();

		// The connection manager doesn't have to do much, HTTP is stateless
		LocalService connectionManagerService = binder.read(ConnectionManagerService.class);
		ServiceManager<ConnectionManagerService> connectionManager =
			new DefaultServiceManager(connectionManagerService) {
				@Override
				protected Object createServiceInstance() throws Exception {
					ConnectionManagerService service = new ConnectionManagerService();
					service.getSinkProtocolInfo().add(new ProtocolInfo(MimeType.valueOf("video/mp4")));

					return service;
				}
			};
		connectionManagerService.setManager(connectionManager);

		// The AVTransport just passes the calls on to the backend players
		LocalService<MediaBotAVTransportService> avTransportService = binder.read(MediaBotAVTransportService.class);
		LastChangeAwareServiceManager<MediaBotAVTransportService> avTransport =
			new LastChangeAwareServiceManager<MediaBotAVTransportService>(
				avTransportService,
				new AVTransportLastChangeParser()
			) {
				@Override
				protected MediaBotAVTransportService createServiceInstance() throws Exception {
					return new MediaBotAVTransportService(avTransportLastChange, mediaRenderer);
				}
			};
		avTransportService.setManager(avTransport);

		// The Rendering Control just passes the calls on to the backend players
		LocalService<MediaBotAudioRenderingControl> renderingControlService = binder.read(MediaBotAudioRenderingControl.class);
		LastChangeAwareServiceManager<MediaBotAudioRenderingControl> renderingControl =
			new LastChangeAwareServiceManager<MediaBotAudioRenderingControl>(
				renderingControlService,
				new RenderingControlLastChangeParser()
			) {
				@Override
				protected MediaBotAudioRenderingControl createServiceInstance() throws Exception {
					return new MediaBotAudioRenderingControl(renderingControlLastChange, mediaRenderer);
				}
			};
		renderingControlService.setManager(renderingControl);

		try{
			LocalDevice device = new LocalDevice(
				new DeviceIdentity(UDN.uniqueSystemIdentifier("MediaBotRenderer")),
				new UDADeviceType("MediaBotRenderer", 1),
				new DeviceDetails(
					"MediaBotRenderer on " + ModelUtil.getLocalHostName(false),
					new ManufacturerDetails("MediaBotRenderer", "https://github.com/jfgomez21/MediaBotRenderer"),
					new ModelDetails("MediaBotRenderer", "MediabotRenderer", "1", "https://github.com/jfgomez21/MediaBotRenderer")
				),
				new Icon[] {createDeviceIcon("classpath:/smart-tv.png")},
				new LocalService[]{
					avTransportService,
					renderingControlService,
					connectionManagerService
				}
			);
			
			SwtContentDirectoryClient swt = new SwtContentDirectoryClient();

			UpnpService upnpService = new UpnpServiceImpl();
			upnpService.getRegistry().addListener(swt);
			//upnpService.getRegistry().addDevice(device);

			swt.start(upnpService);

			upnpService.shutdown();

			//Thread.sleep(10000);

			/*public void browseContent(Service service, String filter, long index, long maxCount, SortCriterion ... orderBy){
			for(org.fourthline.cling.model.meta.Service service : client.getContentDirectoryServices()){
				if("MediaBot".equals(service.getDevice().getDetails().getFriendlyName())){
					client.browseContent(service, "*", 0, 100);
				}
			}

			//mediaRenderer.start();
			try{
				Thread.currentThread().join();
			}
			finally{
				upnpService.shutdown();
			}
			*/
		}
		catch(IOException | ValidationException/* | InterruptedException*/  ex){
			ex.printStackTrace();
		}
	}

	//TODO
	// The backend player instances will fill the LastChange whenever something happens with
	// whatever event messages are appropriate. This loop will periodically flush these changes
	// to subscribers of the LastChange state variable of each service.
	protected void runLastChangePushThread(LastChangeAwareServiceManager<MediaBotAVTransportService> avTransport, LastChangeAwareServiceManager<MediaBotAudioRenderingControl> renderingControl) {
		// TODO: We should only run this if we actually have event subscribers
		new Thread() {
			@Override
			public void run() {
				try {
					while (true) {
						// These operations will NOT block and wait for network responses
						avTransport.fireLastChange();
						renderingControl.fireLastChange();

						Thread.sleep(LAST_CHANGE_FIRING_INTERVAL_MILLISECONDS);
					}
				} 
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		}.start();
	}
}
