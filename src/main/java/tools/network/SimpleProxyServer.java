package tools.network;

import java.io.*;
import java.net.*;
import java.util.Vector;

import wait.ExpectedCondition;
import wait.Wait;
import wait.WaitUntil;

public class SimpleProxyServer {
  private volatile int waitCloseTimeoutInSec = 30;
  private String host;
  private int remotePort;
  private int localPort;
  private int requestByteBuffer;
  private int replyByteBuffer;
  private ServerSocket serverSocket;
  private Thread runningServerThread;
  private volatile boolean toClose = false;
  private volatile boolean isClosed = false;
  private final Vector<IOException> exceptionList = new Vector<IOException>();

  public SimpleProxyServer(String host, int remotePort, int localPort, int requestByteBuffer, int replyByteBuffer) {
    this.host = host;
    this.remotePort = remotePort;
    this.localPort = localPort;
    this.replyByteBuffer = replyByteBuffer;
    this.requestByteBuffer = requestByteBuffer;
    if (!tools.network.HttpUtils.isPortAvailable(localPort)) {
      throw new IllegalArgumentException("Port:" + localPort + " is unavailable.");
    }
  }

  public SimpleProxyServer(String host, int remotePort, int localPort) {
    this(host, remotePort, localPort, 409600, 409600);
  }

  public int getRemortPort() {
    return remotePort;
  }

  public int getLocalPort() {
    return localPort;
  }

  public boolean isClosed() {
    if (runningServerThread == null || serverSocket == null) {
      return true;
    }
    return serverSocket.isClosed() && !runningServerThread.isAlive();
  }

  /**
   * stop Server
   */
  public boolean closeServer() {
    toClose = true;
    if (runningServerThread != null && runningServerThread.isAlive()) {
      runningServerThread.interrupt();
    }
    ExpectedCondition<Boolean> expect = new ExpectedCondition<Boolean>() {
      @Override
      public Boolean apply(Object isClosed) {
        return (Boolean) isClosed;
      }
    };
    try {
      Wait<Object> wait = new WaitUntil(isClosed, waitCloseTimeoutInSec);
      wait.until(expect);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void runServer() throws IOException {
    runningServerThread = new Thread() {
      @Override
      public void run() {
        try {
          // Create a ServerSocket to listen for connections with
          try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(localPort));
          } catch (IOException e) {
            exceptionList.add(e);
            return;
          }
          final byte[] request = new byte[requestByteBuffer];
          final byte[] reply = new byte[replyByteBuffer];

          while (true) {
            Socket client = null, server = null;
            if (toClose) {
              break;
            }
            try {
              // Wait for a connection on the local port
              client = serverSocket.accept();
              final InputStream streamFromClient = client.getInputStream();
              final OutputStream streamToClient = client.getOutputStream();

              // Make a connection to the real server.
              // If we cannot connect to the server, send an error to the
              // client, disconnect, and continue waiting for connections.
              try {
                server = new Socket(host, remotePort);
              } catch (IOException e) {
                PrintWriter out = new PrintWriter(streamToClient);
                out.print("Proxy server cannot connect to " + host + ":"
                    + remotePort + ":\n" + e + "\n");
                out.flush();
                client.close();
                continue;
              }

              // Get server streams.
              final InputStream streamFromServer = server.getInputStream();
              final OutputStream streamToServer = server.getOutputStream();

              // a thread to read the client's requests and pass them
              // to the server. A separate thread for asynchronous.
              Thread clientStreamThread = new Thread() {
                public void run() {
                  int bytesRead;
                  try {
                    while ((bytesRead = streamFromClient.read(request)) != -1 && !toClose) {
                      streamToServer.write(request, 0, bytesRead);
                      streamToServer.flush();
                    }
                  } catch (IOException e) {}
                  finally {
                    try {
                      // the client closed the connection to us, so close our
                      // connection to the server.
                      streamToServer.close();
                    } catch (IOException e) {}
                  }
                }
              };

              // Start the client-to-server request thread running
              clientStreamThread.start();

              // Read the server's responses
              // and pass them back to the client.
              int bytesRead;
              try {
                while ((bytesRead = streamFromServer.read(reply)) != -1 && !toClose) {
                  streamToClient.write(reply, 0, bytesRead);
                  streamToClient.flush();
                }
              } catch (IOException e) {} finally {
                // The server closed its connection to us, so we close our
                // connection to our client.
                streamToClient.close();
              }
            } catch (IOException e) {
              exceptionList.add(e);
            } finally {
              try {
                if (server != null) {
                  server.close();
                }
              } catch (IOException e) {}
              try {
                if (client != null) {
                  client.close();
                }
              } catch (IOException e) {}
            }
          }
        } catch (Exception e) {} finally {
          try {
            if (serverSocket != null) {
              serverSocket.close();
              while (!serverSocket.isClosed()) {}
            }
          } catch (IOException e) {}
        }
        isClosed = true;
      }
    };
    runningServerThread.start();
    try {
      runningServerThread.join(500);
    } catch (InterruptedException e) {
      throw new IOException(e.getMessage());
    }
    if (!exceptionList.isEmpty()) {
      throw exceptionList.get(0);
    }
    if (!runningServerThread.isAlive()) {
      throw new IOException("Fail to start proxy.");
    }
  }
}