package tools.network.wireless;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;

import exception.NotFoundException;
import tools.file.FileUtils;
import tools.network.HttpUtils;
import tools.system.SystemUtils;

class WirelessConnectWin extends WirelessAction {

	private final static String File_SEP = FileUtils.File_SEP;
	private final static String wirelessProfileDefaultName = "autoWirelessProfile";
	private String WirelessProfileFolder = new File(wirelessProfileDefaultName + File_SEP).getAbsolutePath();
	private File WirelessProfile;
	// win
	private final static String WIN_DELETE_PROFILE_CMD = "netsh wlan delete profile name=*";
	private final static String WIN_DISCONNECT_CMD = "netsh wlan disconnect";
	private final static String WIN_SHOW_INTERFACE = "netsh wlan show interface";

	public WirelessConnectWin() {
		File fileFolder = new File(WirelessProfileFolder);
		if (fileFolder.exists()) {
			FileUtils.fileDelete(new File(WirelessProfileFolder), wirelessProfileDefaultName);
		}
		fileFolder.mkdirs();
	}

	@Override
	public boolean checkConnection(String checkTarget) {
		boolean state = false;
		String returnStr = executeCMD(WIN_SHOW_INTERFACE);
		Pattern connectPattern = Pattern.compile("\\s*:[\\s&&[^\r\n\n]]*([\\p{L}]{2}\\s+)|connected");
		state = connectPattern.matcher(returnStr).find();
		return HttpUtils.ping(checkTarget) && state;
	}

	@Override
	public String getWirelessMAC() {
		String returnStr = executeCMD(WIN_SHOW_INTERFACE);
		Pattern macPattern = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
		Matcher macMatcher = macPattern.matcher(returnStr);
		return macMatcher.find() ? macMatcher.group() : null;
	}

	@Override
	public boolean connect(String ssid, String... option) throws IOException {
		String str = "";
		int Timer = 0;
		disconnect();
		String[] args = null;
		if (option != null && option.length > 1) {
			List<String> optionList = Arrays.asList(option);
			String ssidKey = "-" + wirelessOption.ssid.toString();
			if (!optionList.contains(ssidKey)) {
				List<String> newOptionList = new ArrayList<String>();
				newOptionList.add(ssidKey);
				newOptionList.add(ssid);
				newOptionList.addAll(optionList);
				args = newOptionList.toArray(new String[newOptionList.size()]);
			} else {
				args = option;
			}
		} else if (option == null || option.length == 0 || option[0].trim().isEmpty()) {
			args = new String[] {
					"-" + wirelessOption.ssid.toString(), ssid,
					"-" + wirelessOption.auth.toString(), auth.open.toString(),
					"-" + wirelessOption.encrypt.toString(), encrypt.none.toString() };
		} else {
			args = new String[] {
					"-" + wirelessOption.ssid.toString(), ssid,
					"-" + wirelessOption.auth.toString(), auth.WPA2PSK.toString(),
					"-" + wirelessOption.encrypt.toString(), encrypt.AES.toString(),
					"-" + wirelessOption.key.toString(), option[0] };
		}
		configWirelessOption(args);
		do {
			str = executeCMD(winConnectCmdBuilder(ssid));
			sleep();
			if (str.contains("成功") || str.toLowerCase().contains("success")) {
				break;
			}
			disconnect(false);
		} while (10 > Timer++);
		Timer = 0;
		while (20 > Timer++) {
			if (checkConnection())
				return true;
			sleep();
		}
		return false;
	}

	@Override
	public void disconnect(boolean... deleteTemp) {
		boolean delete = deleteTemp.length > 0 ? deleteTemp[0] : true;
		executeCMD(WIN_DISCONNECT_CMD);
		if (delete) {
			executeCMD(WIN_DELETE_PROFILE_CMD);
			cleanWirelessProfile();
		}
	}

	public void cleanTempFile() {
		FileUtils.fileDelete(new File(WirelessProfileFolder), wirelessProfileDefaultName);
	}

	private boolean configWirelessOption(String[] args) throws IOException {
		CommandLine cmd = getConfigParser(args);
		String winCMD = "";
		List<String> warnUpCMDList = new ArrayList<String>();
		String SSID = "";
		if (cmd.hasOption(wirelessOption.ssid.toString())) {
			SSID = cmd.getOptionValue(wirelessOption.ssid.toString());
			winCMD += "SSIDname=" + SSID + " ";
		} else {
			throw new NotFoundException("No ssid configured.");
		}
		if (cmd.hasOption(wirelessOption.auth.toString())) {
			winCMD += "authentication=" + cmd.getOptionValue(wirelessOption.auth.toString()) + " ";
		}
		if (cmd.hasOption(wirelessOption.encrypt.toString())) {
			String encryptStr = cmd.getOptionValue(wirelessOption.encrypt.toString());
			winCMD += "encryption=" + encryptStr + " ";
			if (encryptStr.contains(encrypt.WEP.toString())) {
				warnUpCMDList.add("keyType=networkKey");
			}
		}
		if (cmd.hasOption(wirelessOption.key.toString())) {
			winCMD += "keyMaterial=" + cmd.getOptionValue(wirelessOption.key.toString()) + " ";
		}
		if (SystemUtils.isWindows()) {
			String setParameterBasicCMD = "netsh wlan set profileparameter name=" + SSID + " ";
			if (!winCMD.trim().isEmpty()) {
				createWirelessProfile(SSID);
				for (String warnUpCMD : warnUpCMDList) {
					executeCMD(setParameterBasicCMD + warnUpCMD);
					sleep();
				}
				executeCMD(setParameterBasicCMD + winCMD);
			}
		}
		return true;
	}

	private void cleanWirelessProfile() {
		if (WirelessProfile != null && WirelessProfile.exists()) {
			FileUtils.fileDelete(WirelessProfile, wirelessProfileDefaultName);
			WirelessProfile = null;
		}
	}

	private File createWirelessProfile(String ssid) throws IOException {
		String hexvalue = getStringToHex(ssid);
		cleanWirelessProfile();
		WirelessProfile = new File(WirelessProfileFolder + File_SEP + wirelessProfileDefaultName + "_" + (int) (Math.random() * 10000) + ".xml");
		List<String> contentList = new ArrayList<String>();
		contentList.add("<?xml version=\"1.0\"?>\r\n");
		contentList.add("<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\r\n");
		contentList.add("\t<name>" + ssid + "</name>\r\n");
		contentList.add("\t<SSIDConfig>\r\n");
		contentList.add("\t\t<SSID>\r\n");
		contentList.add("\t\t\t<hex>" + hexvalue + "</hex>\r\n");
		contentList.add("\t\t\t<name>" + ssid + "</name>\r\n");
		contentList.add("\t\t</SSID>\r\n");
		contentList.add("\t</SSIDConfig>\r\n");
		contentList.add("\t<connectionType>ESS</connectionType>\r\n");
		contentList.add("\t<connectionMode>auto</connectionMode>\r\n");
		contentList.add("\t<MSM>\r\n");
		contentList.add("\t\t<security>\r\n");
		contentList.add("\t\t\t<authEncryption>\r\n");
		contentList.add("\t\t\t\t<authentication>WPA2PSK</authentication>\r\n");
		contentList.add("\t\t\t\t<encryption>AES</encryption>\r\n");
		contentList.add("\t\t\t\t<useOneX>false</useOneX>\r\n");
		contentList.add("\t\t\t</authEncryption>\r\n");
		contentList.add("\t\t\t<sharedKey>\r\n");
		contentList.add("\t\t\t\t<keyType>passPhrase</keyType>\r\n");
		contentList.add("\t\t\t\t<protected>true</protected>\r\n");
		contentList
					.add("\t\t\t\t<keyMaterial>01000000D08C9DDF0115D1118C7A00C04FC297EB01000000ECDB404630656F41BE9565C1D0C7359900000000020000000000106600000001000020000000650BE8B26DCFBF4ACF5B49375934C42A0EFB6FA42C9FE59D33CF65B030C7F7F4000000000E8000000002000020000000E19A43B41EE1D2E5DED5A78F9F4B02C69F0C1D2955C8C577FF3FD35705193F1E10000000126C176E9FC0E59F5C5D55A79B8CC6C540000000DC6427F0A2C1C6F278DDAB78FE74D67372896152F2AB8D6604DC73E95F9CF2076B7768B5B268BCABD910D92121A9427F80BAFB298B606C1A2721C704EEC3AAC8</keyMaterial>\r\n");
		contentList.add("\t\t\t</sharedKey>\r\n");
		contentList.add("\t\t</security>\r\n");
		contentList.add("\t</MSM>\r\n");
		contentList.add("</WLANProfile>\r\n");
		FileUtils.writeFileData(WirelessProfile, false, contentList, true);
		executeCMD(winAddProfileCmdBuilder(WirelessProfile.getAbsolutePath()));
		sleep();
		return WirelessProfile;
	}

	private String winConnectCmdBuilder(String ssid) {
		return "netsh wlan connect name=" + ssid;
	}

	private String winAddProfileCmdBuilder(String path) {
		return "netsh wlan add profile filename=" + path;
	}

}
