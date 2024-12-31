package mediabot.renderer.swt.events;

import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import org.fourthline.cling.model.meta.Service;

import mediabot.renderer.ContentDirectoryClient;

public class PaginatedSelectionAdapter extends SelectionAdapter {
	private ContentDirectoryClient client;
	private Service service;
	private String objectId;
	private long pageCount;
	private long totalCount;
	private boolean enabled = true;

	public PaginatedSelectionAdapter(ContentDirectoryClient client, Service service, String objectId, long pageCount, long totalCount){
		this.client = client;
		this.service = service;
		this.objectId = objectId;
		this.pageCount = pageCount;
		this.totalCount = totalCount;
	}

	@Override
	public void widgetSelected(SelectionEvent e){
		ScrolledComposite composite = (ScrolledComposite) ((ScrollBar) e.getSource()).getParent();
		Rectangle bounds = composite.getClientArea();
		Point origin = composite.getOrigin();

		int childCount = ((Composite) composite.getContent()).getChildren().length;

		if(enabled && childCount < totalCount){
			int columnCount = bounds.width / 300;
			int rowCount = (int) Math.ceil(totalCount / (double) columnCount);

			int startRow = origin.y / 500;
			int endRow = (origin.y + bounds.height) / 500;

			int lastRow = (int) Math.ceil(childCount / (double) columnCount) - 1;

			if(endRow >= lastRow){
				System.out.println(String.format("%s NEXT PAGE - %d", this, childCount + 1));

				enabled = false;

				client.browseContent(service, objectId, childCount + 1, pageCount);
			}

			//System.out.println(String.format("childCount - %d columnCount - %d totalCount - %d startRow - %d endRow - %d lastRow - %d", childCount, columnCount, totalCount, startRow, endRow, lastRow));
		}

	}

	public void setEnabled(boolean enabled){
		this.enabled = enabled;
	}

	public boolean isEnabled(){
		return enabled;
	}
}
