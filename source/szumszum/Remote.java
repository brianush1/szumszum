package szumszum;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Remote {

	private Remote() {}

	private static final String BASE_URL = "https://raw.githubusercontent.com/brianush1/szumszum/master/public/";

	public static String downloadString(String path) throws IOException {
		URL url = new URL(BASE_URL + path);
		InputStream stream = url.openStream();
		return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
	}

	public static void downloadFile(String path, Path targetPath) throws IOException {
		URL url = new URL(BASE_URL + path);
		ReadableByteChannel channel = Channels.newChannel(url.openStream());
		FileOutputStream stream = new FileOutputStream(targetPath.toFile());
		stream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
		stream.close();
	}

}
