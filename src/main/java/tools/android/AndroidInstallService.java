package tools.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import exception.NotFoundException;

public class AndroidInstallService {

  private final AndroidUtils androidUtils;
  private final String packageNameOrFilter;
  private final String adbPath;
  private final String apkPath;
  private final Object targetDevice;
  private Map<String, Boolean> installResult;

  public static enum InstallTargetDevice {
    all, single
  }

  public AndroidInstallService(
      String adbPath,
      String packageNameOrFilter,
      String apkPath,
      Object targetDevice) {
    this.adbPath = adbPath;
    this.androidUtils = new AndroidUtils(adbPath);
    this.packageNameOrFilter = packageNameOrFilter;
    this.apkPath = apkPath;
    this.targetDevice = targetDevice;
  }

  public Map<String, Boolean> getInstallResult() {
    return installResult;
  }

  public void run() throws Exception {
    List<String> deviecList = androidUtils.getUsableDevice();
    if (deviecList.size() == 0) {
      throw new NotFoundException("No android device found.");
    }
    InstatllThread instatllThread;
    installResult = new HashMap<String, Boolean>();
    if (!(targetDevice instanceof InstallTargetDevice)) {
      String specifiedDevice = targetDevice.toString();
      if (!deviecList.toString().contains(specifiedDevice)) {
        throw new NotFoundException("No such android device .");
      }
      instatllThread = new InstatllThread(new AndroidUtils(adbPath), specifiedDevice);
      instatllThread.start();
      instatllThread.join();
      installResult.put(instatllThread.getID(), instatllThread.getResult());
    } else {
      switch ((InstallTargetDevice) targetDevice) {
        case all:
          List<InstatllThread> threadList = new ArrayList<InstatllThread>();
          for (String device : deviecList) {
            instatllThread = new InstatllThread(new AndroidUtils(adbPath), device);
            threadList.add(instatllThread);
            threadList.get(threadList.size() - 1).start();
          }
          for (InstatllThread thread : threadList) {
            thread.join();
            installResult.put(thread.getID(), thread.getResult());
          }
          break;
        case single:
          instatllThread = new InstatllThread(new AndroidUtils(adbPath), deviecList.get(0));
          instatllThread.start();
          instatllThread.join();
          installResult.put(instatllThread.getID(), instatllThread.getResult());
          break;
      }
    }

  }

  class InstatllThread extends Thread {
    private AndroidUtils androidUtils;
    private String deviceID;
    private boolean result = false;

    public InstatllThread(AndroidUtils androidUtils, String deviceID) {
      this.androidUtils = androidUtils;
      this.deviceID = deviceID;
      androidUtils.setDeviceID(this.deviceID);
    }

    @Override
    public void run() {
      try {
        if (packageNameOrFilter == null || packageNameOrFilter.isEmpty()) {
          throw new IOException("Must have package Name Or Filter");
        }
        androidUtils.uninstallPackage(packageNameOrFilter);
        androidUtils.installApkToDevice(apkPath);
        result = true;
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    public boolean getResult() {
      return result;
    }

    public String getID() {
      return deviceID;
    }
  }
}
