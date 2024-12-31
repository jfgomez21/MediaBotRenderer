package mediabot.renderer.swt.concurrent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

public class LoadImageUrlTask implements Runnable {
	private Map<String, Image> images;
	private Display display;
	private Label label;
	private String url;

	public LoadImageUrlTask(Map<String, Image> images, Display display, Label label, String url){
		this.images = images;
		this.display = display;
		this.label = label;
		this.url = url;
	}

	@Override
	public void run(){
		HttpResponse<byte[]> response = Unirest.get(url).asBytes(); 

		//TODO - check response code
		//TODO - use default image on failure

		if(response.isSuccess()){
			display.asyncExec(new Runnable(){
				@Override
				public void run(){
					try(InputStream input = new ByteArrayInputStream(response.getBody())){
						Image image = new Image(display, input);

						label.setImage(image);
						label.setSize(label.computeSize(SWT.DEFAULT, SWT.DEFAULT));

						images.put(url, image);
					}
					catch(SWTException | IOException ex){
						ex.printStackTrace();
					}
				}
			});
		}
	}
}
