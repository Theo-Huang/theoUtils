package tools.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

import com.google.common.collect.Lists;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class SSHUtils {

  private boolean isAuthenticated = false;
  private final String account;
  private final String password;
  private Connection connection;

  public SSHUtils(String address, String account, String password) {
    this.account = account;
    this.password = password;
    connection = new Connection(address);
  }

  public void connect() throws IOException {
    try {
      connection.connect();
      isAuthenticated = connection.authenticateWithPassword(account, password);// Authenticate
      if (!isAuthenticated) {
        throw new IOException("isAuthenticated=" + isAuthenticated);
      }

    } catch (IOException e) {
      throw e;
    }
  }

  public String executeCommand(String... command) throws IOException, InterruptedException {
    return executeCommand(Lists.<String>newArrayList(command));
  }

  public String executeCommand(List<String> commands) throws IOException, InterruptedException {
    InputStream stdout;
    InputStream stderr;
    Session sess = connection.openSession();
    sess.requestPTY("bash");
    sess.startShell();
    PrintWriter out = new PrintWriter(sess.getStdin());
    for (String command : commands) {
      out.println(command);
    }
    out.println("exit");
    out.flush();
    out.close();
    stdout = new StreamGobbler(sess.getStdout());
    stderr = new StreamGobbler(sess.getStderr());
    final BufferedReader stdoutReader =
        new BufferedReader(new InputStreamReader(stdout, tools.office.StringUtils.getUTF8String()));
    final BufferedReader stderrReader =
        new BufferedReader(new InputStreamReader(stderr, tools.office.StringUtils.getUTF8String()));

    final StringBuilder returnBuilder = new StringBuilder();
    final Thread readThread = new Thread() {
      public void run() {
        String line;
        while (true) {
          try {
            line = stdoutReader.readLine();
            if (line == null) {
              break;
            }
            returnBuilder.append(line + tools.file.FileUtils.Line_SEP);
          } catch (IOException e) {
            returnBuilder.append(e.getCause() + e.getMessage());
            break;
          }
        }
      }
    };
    Thread readErrThread = new Thread() {
      public void run() {
        String errline;
        while (true) {
          try {
            errline = stderrReader.readLine();
            if (errline == null) {
              break;
            }
            returnBuilder.append(errline + tools.file.FileUtils.Line_SEP);
          } catch (IOException e) {
            returnBuilder.append(e.getCause() + e.getMessage());
            break;
          }
        }
      }
    };
    stdoutReader.close();
    stderrReader.close();
    readThread.start();
    readErrThread.start();
    readThread.join();
    readErrThread.join();
    sess.close();

    return returnBuilder.toString();
  }

  public void disconnect() {
    connection.close();
  }
}
