package szumszum;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {

	private Hasher() {
	}

	public static String getSHA1(Path path) throws IOException {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
			try (InputStream input = Files.newInputStream(path)) {
				byte[] buffer = new byte[8192];
				int len = input.read(buffer);

				while (len != -1) {
					sha1.update(buffer, 0, len);
					len = input.read(buffer);
				}

				byte[] digest = sha1.digest();
				StringBuilder result = new StringBuilder();
				for (byte b : digest) {
					result.append(String.format("%02x", Byte.toUnsignedInt(b)));
				}

				return result.toString();
			}
		} catch (NoSuchAlgorithmException ex) {
			throw new Error(ex);
		}
	}

}
