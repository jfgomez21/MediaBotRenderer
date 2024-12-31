package mediabot.renderer.swt.layout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.graphics.Rectangle;

//TODO - Add fillLastChild flag

public class ApplicationLayout extends Layout {
	public ApplicationLayout(){
	}

	private Point layout(Composite composite, int wHint, int hHint, boolean flushCache, boolean move){
		Rectangle size = composite.getClientArea();
		int maxWidth = size.width;
		int height = 0;

		for(Control child : composite.getChildren()){
			Point p = child.computeSize(wHint, hHint, flushCache);

			height += p.y;

			if(p.x > maxWidth){
				maxWidth = p.x;
			}
		}

		if(move){
			Control[] children = composite.getChildren();
			int y = 0;

			for(int i = 0; i < children.length - 1; i++){
				Control child = children[i];

				Point p = child.computeSize(wHint, hHint, flushCache);
				Integer alignment = (Integer) child.getLayoutData();

				if(alignment == null){
					alignment = SWT.LEFT;
				}

				int x = 0;

				if(alignment == SWT.CENTER){
					x = Math.max(0, (size.width - p.x) / 2);
				}
				else if(alignment == SWT.RIGHT){
					x = Math.max(0, size.width - p.x);
				}

				child.setBounds(x, y, p.x, p.y);

				y += p.y;
			}

			Control child = children[children.length - 1];
			Point p = child.computeSize(wHint, hHint, flushCache);

			child.setBounds(0, y, size.width, Math.max(0, size.height - y));
		}

		return new Point(maxWidth, height);
	}

	@Override
	protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {	
		System.out.println(String.format("computeSize - %s defaultWidth - %s defaultHeight - %s wHint - %d hHint - %d", composite, wHint == SWT.DEFAULT, hHint == SWT.DEFAULT, wHint, hHint));
		System.out.println(String.format("computeSize - clientArea - %s", composite.getClientArea()));

		Point p = layout(composite, wHint, hHint, flushCache, false);

		if(wHint != SWT.DEFAULT){
			p.x = wHint;
		}

		if(hHint != SWT.DEFAULT){
			p.y = hHint;
		}

		return p;	
	}

	@Override
	protected void layout(Composite composite, boolean flushCache) {
		System.out.println(String.format("layout - %s flushCache - %s", composite, flushCache));
		System.out.println(String.format("layout - %s", composite.getBounds()));

		layout(composite, SWT.DEFAULT, SWT.DEFAULT, flushCache, true);
	}
}
