package tools.network.wireless;

import exception.NotFoundException;

public class WirelessConnection implements WirelessInterface {
	private final WirelessAction WA;

	public WirelessConnection() {
		WA = WirelessAction.getInstance();
	}

	@Override
	public boolean checkConnection() {
		return WA.checkConnection();
	}

	@Override
	public boolean checkConnection(String checkTarget) {
		try {
			return WA.checkConnection();
		} catch (Exception e) {
			System.err.println(checkTarget);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String getWirelessMAC() {
		return WA.getWirelessMAC().toUpperCase().replaceAll("[:-]", "");
	}

	@Override
	public boolean connect(String ssid, String... option) {
		if (!hasWirelessInterface()) {
			throw new NotFoundException("Can't find wireless interface in this pc.");
		}
		try {
			return WA.connect(ssid, option);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public void disconnect(boolean... deleteTemp) {
		WA.disconnect(deleteTemp);

	}

	@Override
	public void cleanTempFile() {
		WA.cleanTempFile();

	}

	public boolean hasWirelessInterface() {
		try {
			return getWirelessMAC().matches("[\\d\\w]{12}");
		} catch (Exception e) {
			return false;
		}
	}

}
