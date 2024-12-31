package mediabot.renderer.upnp;

import java.util.ArrayList;
import java.util.List;

import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.BrowseResult;
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
	private String objectId;
	private long totalCount;

	public ContentBrowseActionCallback(ContentDirectoryClient client, Service service, String objectId, long index, long maxResults, SortCriterion ... orderBy){
		super(service, objectId, BrowseFlag.DIRECT_CHILDREN, "*", index, maxResults, orderBy);

		this.client = client;
		this.service = service;
		this.objectId = objectId;
	}

	@Override
	public boolean receivedRaw(ActionInvocation actionInvocation, BrowseResult browseResult) {
		boolean result = super.receivedRaw(actionInvocation, browseResult);

		totalCount = browseResult.getTotalMatchesLong();

		return result;
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
	public void updateStatus(Browse.Status status) {
		client.browseContentDirectoryStatus(status, service, objectId, results, totalCount);	
	}

	@Override
	public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
		client.browseContentDirectoryFailure(defaultMsg);
	}
}
