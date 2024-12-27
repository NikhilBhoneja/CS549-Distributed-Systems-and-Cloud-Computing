package edu.stevens.cs549.shell.remote;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

import edu.stevens.cs549.shell.main.IShell;


public class ControllerClient extends Endpoint implements MessageHandler.Whole<String> {

	public static final String TAG = ControllerClient.class.getCanonicalName();

	private final CountDownLatch messageLatch = new CountDownLatch(1);

	private final ClientManager client = ClientManager.createClient();

	// TODO configure the client to use proper encoder for messages sent to server
	private final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().
			encoders(Arrays.asList(CommandLineEncoder.class)).
			decoders(Arrays.asList(CommandLineDecoder.class))
			.build();
	
	private final ShellManager shellManager = ShellManager.getShellManager();

	
	// Report WS communication failures locally
	private void err(String mesg, Throwable t) {
		shell.fatalError(TAG, mesg, t);
	}
	
	private void err(Throwable t) {
		shell.fatalError(t);
	}
	
	private IShell shell;

	private boolean initializing = true;
		
	private Session session;

	public ControllerClient(IShell shell) {
		this.shell = shell;
	}
	
	public void connect(URI uri) throws DeploymentException, IOException {
		try {
			shell.msg("Requesting control of node at " + uri.toString() + "...");
			// TODO make the connection request (use client)
			client.asyncConnectToServer(this, cec, uri);
			
			while (true) {
				try {
					// Synchronize with receipt of an ack from the remote node.
					boolean connected = messageLatch.await(100, TimeUnit.SECONDS);
					
					if (connected) {
						/*
						 * TODO If we are connected, a new toplevel shell has been pushed
						 * ("shell" variable updated in onMessage).  Execute its CLI.
						 */
						shellManager.getCurrentShell().cli();
						return;
					}

					// Be sure to return when done, to exit the loop.
					return;
					
				} catch (InterruptedException e) {
					// Keep on waiting for the specified time interval
					shell.err(e);
				}
			}
		} catch (IOException e) {
			shell.err(e);
		}
	}
	
	protected void endInitialization() {
		initializing = false;
		messageLatch.countDown();
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		// TODO session created, add a message handler for receiving communication from server.
		
		// We should also cache the session for use by some of the other operations.
		this.session = session;
		this.session.addMessageHandler(this);
	}

	@Override
	public void onMessage(String message) {
		try {
			if (initializing) {
				if (SessionManager.ACK.equals(message)) {
					/*
					 * Server has accepted our remote control request, push a proxy shell on the shell stack
					 * and flag that initialization has finished (allowing the UI thread to continue).
					 * Make sure to replace the cached shell in this callback with the new proxy shell!
					 * 
					 * If the server rejects our request, they will just close the channel.
					 */
					shell.msgln("request accepted.");
					
					// TODO create a proxy shell to control the remote node, and update "shell".

					ProxyShell proxyShell = (ProxyShell) ProxyShell.createRemoteController(shellManager.getCurrentShell(), session.getBasicRemote());	
					shellManager.addShell(proxyShell);
					endInitialization();
				} else {
					throw new IllegalStateException("Unexpected response to remote control request: " + message);
				}
			} else {
				// TODO provide the message to the shell
				shellManager.getCurrentShell().msg(message);
			}
		} catch (IOException e) {
			/*
			 * Failure to communicate with the shell (which may be remote if we are controlled).
			 */
			err("Failure to communicate with the client shell.", e);
		}
	}
	
	@Override
	public void onClose(Session session, CloseReason reason) {
		shell.debug(TAG, "Server closed Websocket connection: "+reason.getReasonPhrase());
		try {
			shutdown();
		} catch (IOException e) {
			err(e);
		}
	}
	
	@Override
	public void onError(Session session, Throwable t) {
		try {
			shell.err(t);
			shutdown();
		} catch (IOException e) {
			err(e);
		}
	}
	
	protected void shutdown() throws IOException {
		/*
		 * Shutdown initiated by error or closure of the connection.  Three cases: 
		 * 1. We are still initializing when this happens (need to unblock the client thread).
		 * 2. We are running an on-going remote control session (need to remove the proxy shell).
		 * 3. The remote control session has terminated (which caused the channel to be closed).
		 */
		if (initializing) {
			shell.debug(TAG, "...shutdown of remote control request.");
			shell.msgln("request rejected.");
			endInitialization();
		} else if (!shell.isTerminated()) {
			shell.debug(TAG, "...removing shell proxy from shell stack.");
			shellManager.removeShell();
		}
	}
}
