package mediabot.renderer;

import java.util.Collection;
import java.util.List;

import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.contentdirectory.callback.Search;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.SortCriterion;

public interface ContentDirectoryClient extends RegistryListener {
	public void findContentDirectoryServices();
	public void browseContent(Service service, String objectId, SortCriterion ... orderBy);
	public void searchContent(Search action);
	public void browseContentDirectoryStatus(Browse.Status status, Service service, List<DIDLObject> results);
	public void browseContentDirectoryFailure(String errorMessage);

}
