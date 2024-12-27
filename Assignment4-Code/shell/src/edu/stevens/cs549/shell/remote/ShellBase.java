package edu.stevens.cs549.shell.remote;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.DeploymentException;

import edu.stevens.cs549.shell.main.IContext;
import edu.stevens.cs549.shell.main.IShell;

public abstract class ShellBase implements IShell {

	protected final IContext context;

	protected final String name;

	protected SessionManager sessionManager = SessionManager.getSessionManager();

	protected ShellManager shellManager = ShellManager.getShellManager();

	protected ShellBase(String name, IContext context) {
		this.name = name;
		this.context = context;
	}


	@Override
	public String getName() {
		return name;
	}

	@Override
	public IContext getContext() {
		return context;
	}

	@Override
	public void addCommandLine(String[] inputs) {
		context.addCommandLine(inputs);
	}

	@Override
	public void msg(String m) throws IOException {
		context.msg(m);
	}

	@Override
	public void msgln(String m) throws IOException {
		context.msgln(m);
	}

	@Override
	public void err(Throwable t) throws IOException {
		context.err(t);
	}

	@Override
	public void err(String mesg, Throwable t) throws IOException {
		context.err(mesg, t);
	}

	@Override
	public boolean debug() {
		return context.debug();
	}
	
	public void debug(String tag, String mesg) {
		context.debug(tag, mesg);
	}
	
	public void fatalError(String tag, String mesg) {
		context.fatalError(tag, mesg);
	}

	public void fatalError(String tag, String mesg, Throwable t) {
		context.fatalError(tag, mesg, t);
	}

	public void fatalError(Throwable t) {
		context.fatalError(t);
	}

	@Override
	public void stop() {
		context.stop();
	}

	protected void help(String[] inputs) throws IOException {
		if (inputs.length == 1) {
			
			msgln("Commands are:");
			
			// Network-wide commands
			msgln("  get key: get values under a key");
			msgln("  add key value: add a value under a key");
			msgln("  del key value: delete a value under a key");
			
			// Logging commands.
			msgln("  debug: toggle on and off debug logging");
			
			// Remote control commands.
			msgln("  connect host port: connect to a site to control it remotely");
			msgln("  accept: accept the current pending remote control request");
			msgln("  reject: reject the current pending remote control request");
			msgln("  quit: exit the current shell");

		}
	}

	protected void connect(String[] inputs) throws IOException {
		if (inputs.length != 3) {
			msgln("Usage: connect host port");
		} else {
			try {
				URI uri = ContextBase.getControlClientUri(inputs[1], inputs[2], name);
				
				// TODO Make a connection request to the remote node.
				ControllerClient client = new ControllerClient(this);
				try {
					client.connect(uri);
				} catch (DeploymentException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (URISyntaxException e) {
				msgln("Badly formatted URI.");
			}
		}
	}

	/**
	 * Accept the pending session (see SessionManager) and 
	 * start running the CLI for the new shell that will have been
	 * pushed on the shell stack.
	 */
	protected void accept(String[] inputs) throws IOException {
		if (inputs.length != 1) {
			msgln("Usage: accept");
		} else {
			// Spot the race condition.
			sessionManager.acceptSession();
			shellManager.getCurrentShell().cli();
		}
	}

	/**
	 * Reject and remove the pending session (see SessionManager).
	 */
	protected void reject(String[] inputs) throws IOException {
		if (inputs.length != 1) {
			msgln("Usage: reject");
		} else {
			sessionManager.rejectSession();
		}
	}


}
