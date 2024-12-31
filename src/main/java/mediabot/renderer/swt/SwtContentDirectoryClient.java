package mediabot.renderer.swt;

import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URI;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.model.message.header.ServiceTypeHeader;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.contentdirectory.callback.Search;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;

import mediabot.renderer.ContentDirectoryClient;
import mediabot.renderer.swt.events.ApplicationMouseAdapter;
import mediabot.renderer.swt.events.PaginatedSelectionAdapter;
import mediabot.renderer.swt.layout.ApplicationLayout;
import mediabot.renderer.upnp.ContentBrowseActionCallback;
import mediabot.renderer.upnp.UpnpItemType;

public class SwtContentDirectoryClient extends DefaultRegistryListener implements ContentDirectoryClient {
	private static final DeviceType MEDIA_SERVER_DEVICE_TYPE = DeviceType.valueOf("urn:schemas-upnp-org:device:MediaServer:1");
	private static final ServiceType CONTENT_DIRECTORY_SERVICE_TYPE = ServiceType.valueOf("urn:schemas-upnp-org:service:ContentDirectory:1");
	private ImageService imageService = new ImageService();

	private UpnpService upnpService;
	private Service service;
	private Display display;
	private Shell shell;
	private Text search;
	private Label path;
	private ScrolledComposite scrolledComposite;
	private MouseAdapter mouseAdapter; 

	public SwtContentDirectoryClient(){
	}

	public void start(UpnpService upnpService){
		this.upnpService = upnpService;

		display = new Display();

		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		
		Composite main = new Composite(shell, SWT.NONE);
		main.setLayout(new ApplicationLayout());

		search = new Text(main, SWT.NONE);
		search.setLayoutData(SWT.CENTER);

		path = new Label(main, SWT.NONE);
		path.setLayoutData(SWT.LEFT);

		scrolledComposite = new ScrolledComposite(main, SWT.V_SCROLL | SWT.BORDER);
		
		RowLayout contentLayout = new RowLayout();
		contentLayout.wrap = true;
		contentLayout.pack = true;

		Composite content = new Composite(scrolledComposite, SWT.NONE);
		content.setSize(new Point(300, 300));
		content.setLayout(contentLayout);

		scrolledComposite.setContent(content);
		scrolledComposite.addControlListener(new ControlAdapter(){
			private int columnCount = 1;

			@Override
			public void controlResized(ControlEvent event){
				ScrolledComposite composite = (ScrolledComposite) event.getSource();
				Rectangle bounds = composite.getBounds();
				int count = bounds.width / 300;

				if(count != columnCount){
					Composite content = (Composite) composite.getContent();
					
					content.setSize(content.computeSize(bounds.width, SWT.DEFAULT));
					content.layout();
				}
			}
		});

		mouseAdapter = new ApplicationMouseAdapter(this);

		shell.pack();
		shell.open();

		findContentDirectoryServices();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()){
				display.sleep();
			}
		}

		display.dispose();
	}

	private void disposeChildren(Composite composite){
		for(Control child : composite.getChildren()){
			if(!child.isDisposed()){
				child.dispose();
			}
		}
	}

	@Override
	public void findContentDirectoryServices(){
		display.asyncExec(new Runnable(){
			@Override
			public void run(){
				path.setText("Devices");
				path.getParent().layout();

				Composite content = (Composite) scrolledComposite.getContent();

				disposeChildren(content);	

				//TODO - display loading icon

				content.setSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				scrolledComposite.layout();
			}
		});

		upnpService.getControlPoint().search(new ServiceTypeHeader(CONTENT_DIRECTORY_SERVICE_TYPE));
	}	

	private String getImageUrl(DIDLObject obj){
		String url = null;

		try{
			DIDLObject.Property[] properties = obj.getProperties(DIDLObject.Property.UPNP.ALBUM.class);

			if(properties.length == 0){
				properties = obj.getProperties(DIDLObject.Property.UPNP.ICON.class);
			}

			url = ((URI) properties[0].getValue()).toURL().toString();
		}
		catch(MalformedURLException ex){
			ex.printStackTrace();
		}

		return url;
	}

	private void createMovieWidget(Composite parent, Service service, DIDLObject obj){
		Res resource = obj.getResources().get(0);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new RowLayout(SWT.VERTICAL));
		composite.setLayoutData(new RowData(300, SWT.DEFAULT));
		composite.addMouseListener(mouseAdapter);
		composite.setData("service", service);
		composite.setData("type", UpnpItemType.MOVIE);
		composite.setData("objectId", obj.getId());
		composite.setData("url", resource.getValue());

		String url = getImageUrl(obj);
		Label image = new Label(composite, SWT.NONE);
		image.setLayoutData(new RowData(300, 445));
		image.addControlListener(new ControlAdapter(){
			@Override
			public void controlResized(ControlEvent event){
				if(image.getImage() != null){
					System.out.println(String.format("%s - %dx%d", obj.getTitle(), image.getImage().getBounds().width, image.getImage().getBounds().height));
				}
				parent.setSize(parent.computeSize(parent.getBounds().width, SWT.DEFAULT));
				parent.layout();
			}
		});
		//TODO - draw and copy images larger than 300x445

		//TODO - set dummy image 

		if(url != null){
			imageService.loadImageUrlAsync(display, image, getImageUrl(obj));
		}

		Label title = new Label(composite, SWT.NONE);
		title.setText(obj.getTitle());

		Label runtime = new Label(composite, SWT.NONE);
		runtime.setText(resource.getDuration());
	}

	@Override
	public void browseContent(Service service, String objectId, SortCriterion ... orderBy){
		upnpService.getControlPoint().execute(new ContentBrowseActionCallback(this, service, objectId, orderBy));
	}
	
	@Override
	public void searchContent(Search action){
		upnpService.getControlPoint().execute(action);
	}

	@Override
	public void browseContentDirectoryStatus(Browse.Status status, Service service, List<DIDLObject> results){
		display.asyncExec(new Runnable(){
			@Override
			public void run(){
				if(status == Browse.Status.LOADING){
					//TODO - update path label
					Composite content = (Composite) scrolledComposite.getContent();

					disposeChildren(content);	

					//TODO - display loading icon

					content.setSize(content.computeSize(scrolledComposite.getClientArea().width, SWT.DEFAULT));
					scrolledComposite.layout();
					
					//TODO - check if listener has already been added
					scrolledComposite.getVerticalBar().addSelectionListener(new PaginatedSelectionAdapter(SwtContentDirectoryClient.this));

					SwtContentDirectoryClient.this.service = service;
				}
				else if(status == Browse.Status.OK){
					Composite content = (Composite) scrolledComposite.getContent();

					for(DIDLObject obj : results){
						UpnpItemType type = obj instanceof Container ? UpnpItemType.CONTAINER : UpnpItemType.MOVIE;

						if(type == UpnpItemType.CONTAINER){
							Label label = new Label(content, SWT.CENTER);
							label.setText(obj.getTitle());
							label.addMouseListener(mouseAdapter);
							label.setData("service", service);
							label.setData("type", type);
							label.setData("objectId", obj.getId());
						}
						else if(type == UpnpItemType.MOVIE){
							createMovieWidget(content, service, obj);
						}

						System.out.println(String.format("id - %s parent - %s", obj.getId(), obj.getParentID()));
					}

					content.setSize(content.computeSize(scrolledComposite.getBounds().width, SWT.DEFAULT));
					scrolledComposite.layout();
				}
				else{
					//TODO - no content
				}
			}
		});
	}

	@Override
	public void browseContentDirectoryFailure(String errorMessage){
		System.out.println(errorMessage);
	}

	@Override
	public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
		//TODO - display error message
	}

	@Override
	public void deviceAdded(Registry registry, Device device) {
		if(MEDIA_SERVER_DEVICE_TYPE.equals(device.getType())){
			Service service = device.findService(CONTENT_DIRECTORY_SERVICE_TYPE);

			System.out.println(String.format("addContentDirectoryService - %s - %s - %s", service.getDevice().getIdentity().getUdn(), service.getDevice().getDetails().getFriendlyName(),  service.getServiceType()));
			System.out.println(String.format("serviceId - %s", service.getServiceId().getId()));
			
			display.asyncExec(new Runnable(){
				@Override
				public void run(){
					//TODO - custom widget
					//TODO - display icon
					Composite content = (Composite) scrolledComposite.getContent();

					Label label = new Label(content, SWT.CENTER);
					label.setText(device.getDetails().getFriendlyName());
					label.addMouseListener(mouseAdapter);
					label.setData("service", service);
					label.setData("deviceId", device.getIdentity().getUdn());
					label.setData("type", UpnpItemType.DEVICE);
					label.setData("objectId", "0");

					content.setSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					scrolledComposite.layout();
				}
			});
		}
	}

	@Override
	public void deviceRemoved(Registry registry, Device device) {
		if(MEDIA_SERVER_DEVICE_TYPE.equals(device.getType())){
			Service service = device.findService(CONTENT_DIRECTORY_SERVICE_TYPE);

			display.asyncExec(new Runnable(){
				@Override
				public void run(){
					//TODO - notify user?
					Composite content = (Composite) scrolledComposite.getContent();
					UDN deviceId = device.getIdentity().getUdn();

					for(Control child : content.getChildren()){
						if(child.getData("deviceId").equals(deviceId)){
							if(!child.isDisposed()){
								child.dispose();
							}
						}
					}

					content.setSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					scrolledComposite.layout();
				}
			});
		}
	}
}
