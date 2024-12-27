package edu.stevens.cs549.shell.remote;

import java.net.URI;
import java.net.URISyntaxException;

import edu.stevens.cs549.shell.main.IContext;

public abstract class ContextBase implements IContext {
	
	public static URI getControlServerUri(String host, String port) throws URISyntaxException {
		return new URI(String.format("ws://%s:%s/shell/control", host, port));
	}

	public static URI getControlClientUri(String host, String port, String name) throws URISyntaxException {
		return new URI(String.format("ws://%s:%s/shell/control/%s", host, port, name));
	}

	/*
	 * Flag for turning on and off debug output.
	 */
	private boolean debug = false;
	
	public boolean debug() {
		return debug;
	}
	
	@Override
	public void toggleDebug() {
		debug = !debug;
	}
	
	protected static String debugLine(String tag, String mesg) {
		return tag + ": " + mesg;
	}
	
	/*
	 * Only to be used when there is a communication failure, that must be reported locally.
	 */
	@Override
	public void fatalError(String tag, String mesg, Throwable t) {
		fatalError(tag, mesg);
		fatalError(t);
	}
	
	@Override
	public void fatalError(String tag, String mesg) {
		System.err.println(debugLine(tag, mesg));
	}

	@Override
	public void fatalError(Throwable t) {
		t.printStackTrace(System.err);
	}


}
