package tools.network.wireless;

import java.io.IOException;

public interface WirelessInterface {
	static enum wirelessOption {
		ssid, auth, encrypt, key
	}

	static enum auth {
		open, shared, WPA, WPA2, WPAPSK, WPA2PSK;
	}

	static enum encrypt {
		none, WEP, TKIP, AES
	}

	boolean checkConnection();

	boolean checkConnection(String checkTarget);

	String getWirelessMAC();

	boolean connect(String ssid, final String... option) throws IOException;

	void disconnect(boolean... deleteTemp);

	void cleanTempFile();

}
