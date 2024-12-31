package mediabot.renderer.upnp;

import java.util.ArrayList;
import java.util.List;

import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

import mediabot.renderer.ContentDirectoryClient;

public class ContentBrowseActionCallback extends Browse {
	private List<DIDLObject> results = new ArrayList<>();
	private ContentDirectoryClient client;
	private Service service;

	public ContentBrowseActionCallback(ContentDirectoryClient client, Service service, String objectId, SortCriterion ... orderBy){
		super(service, objectId, BrowseFlag.DIRECT_CHILDREN, "*", 0L, null, /*15L,*/ orderBy); //TODO - change numResults

		this.service = service;
		this.client = client;
	}

	@Override
	public void received(ActionInvocation actionInvocation, DIDLContent didl) {
		for (Container childContainer : didl.getContainers()) {
			results.add(childContainer);
		}

		//TODO - only add movie items

		for (Item childItem : didl.getItems()) {
			results.add(childItem);
		}
	}

	@Override
	public void updateStatus(Status status) {
		client.browseContentDirectoryStatus(status, service, results);	
	}

	@Override
	public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
		client.browseContentDirectoryFailure(defaultMsg);
	}
}
