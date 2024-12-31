package mediabot.renderer.swt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import mediabot.renderer.swt.concurrent.DaemonThreadFactory;
import mediabot.renderer.swt.concurrent.LoadImageUrlTask;
import java.util.Map;

public class ImageService {
	private ExecutorService service = Executors.newCachedThreadPool(new DaemonThreadFactory());
	private Map<String, Image> images = new ConcurrentHashMap<String, Image>();

	public void loadImageUrlAsync(Display display, Label label, String url){
		service.execute(new LoadImageUrlTask(images, display, label, url));
	}

	public boolean containsImage(String id){
		return images.containsKey(id);
	}

	public Image getImage(String id){
		return images.get(id);
	}
}
