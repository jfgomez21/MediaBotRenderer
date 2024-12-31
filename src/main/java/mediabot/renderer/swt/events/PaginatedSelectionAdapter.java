package mediabot.renderer.swt.events;

import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.ScrollBar;

import mediabot.renderer.ContentDirectoryClient;

public class PaginatedSelectionAdapter extends SelectionAdapter {
	private ContentDirectoryClient client;

	public PaginatedSelectionAdapter(ContentDirectoryClient client){
		this.client = client;
	}

	@Override
	public void widgetSelected(SelectionEvent e){
		ScrolledComposite composite = (ScrolledComposite) ((ScrollBar) e.getSource()).getParent();
		Rectangle bounds = composite.getClientArea();
		Point origin = composite.getOrigin();

		System.out.println(String.format("x=%d y=%d w=%d h=%d", origin.x, origin.y, bounds.width, bounds.height));
	}
}
