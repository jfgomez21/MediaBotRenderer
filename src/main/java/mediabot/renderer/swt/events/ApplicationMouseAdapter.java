package mediabot.renderer.swt.events;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Control;

import org.fourthline.cling.model.meta.Service;

import mediabot.renderer.ContentDirectoryClient;
import mediabot.renderer.upnp.UpnpItemType;

public class ApplicationMouseAdapter extends MouseAdapter {
	private ContentDirectoryClient client;

	public ApplicationMouseAdapter(ContentDirectoryClient client){
		this.client = client;
	}

	@Override
	public void mouseDown(MouseEvent event){
		System.out.println(String.format("mouseDown - button %d", event.button));

		if(event.button == 1){
			Control control = (Control) event.getSource();
			UpnpItemType type = (UpnpItemType) control.getData("type");

			if(type == UpnpItemType.DEVICE || type == UpnpItemType.CONTAINER){
				//TODO - calculate page size based on whats currently visible
				client.browseContent((Service) control.getData("service"), (String) control.getData("objectId"));
			}
		}
	}
}
