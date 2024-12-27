/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stevens.cs549.shell.main;

import java.io.EOFException;
import java.io.IOException;

import edu.stevens.cs549.shell.remote.ProxyContext;
import edu.stevens.cs549.shell.remote.ShellBase;
import edu.stevens.cs549.shell.state.IState;

/*
 * CLI for a node.
 * 
 * This is the local shell that executes all commands locally.
 */

public class LocalShell extends ShellBase {

	protected final IState state;

	private LocalShell(String name, IState state, IContext main) {
		super(name, main);
		this.state = state;
	}

	public static LocalShell createTopLevel(String name, IState s, LocalContext m) {
		return new LocalShell(name, s, m);
	}

	public static LocalShell createRemotelyControlled(LocalShell shell, ProxyContext context) {
		return new LocalShell(shell.name, shell.state, context);
	}

	@Override
	public LocalShell getLocal() {
		return this;
	}
	
	@Override
	public boolean isTerminated() {
		return false;
	}

	@Override
	public void notifyPendingSession(String source) throws IOException {
		context.msgln("");
		context.msgln("** Pending connection request from " + source);
		context.msgln("");
	}

	@Override
	public void cli() {

		// Main command-line interface loop

		Dispatch d = new Dispatch(state);

		try {
			while (true) {
				msg("shell<" + state.getName() + "> ");
				String[] inputs = context.readline();
				if (inputs.length > 0) {
					String cmd = inputs[0];
					if (cmd.length() == 0)
						;
					else if ("get".equals(cmd)) {
						d.get(inputs);
					} else if ("add".equals(cmd)) {
						d.add(inputs);
					} else if ("del".equals(cmd)) {
						d.delete(inputs);
					} else if ("debug".equals(cmd)) {
						d.debug(inputs);
					
					} else if ("connect".equals(cmd)) {
						connect(inputs);
					} else if ("accept".equals(cmd)) {
						accept(inputs);
					} else if ("reject".equals(cmd)) {
						reject(inputs);
					} else if ("help".equals(cmd)) {
						help(inputs);
					} else if ("quit".equals(cmd)) {
						return;
					} else {
						msgln("Bad input.  Type \"help\" for more information.");
					}
				}
			}
		} catch (EOFException e) {
			
		} catch (IOException e) {
			try {
				err(e);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			stop();
		}

	}

	protected class Dispatch {
		
		private IState state;

		protected Dispatch(IState state) {
			this.state = state;
		}

		public void get(String[] inputs) throws IOException {
			if (inputs.length == 2)
				try {
					String[] vs = state.get(inputs[1]);
					if (vs != null)
						msgln(displayVals(vs));
				} catch (Exception e) {
					err(e);
				}
			else
				msgln("Usage: get <key>");
		}

		public void add(String[] inputs) throws IOException {
			if (inputs.length == 3)
				try {
					state.add(inputs[1], inputs[2]);
				} catch (Exception e) {
					err(e);
				}
			else
				msgln("Usage: add <key> <value>");
		}

		public void delete(String[] inputs) throws IOException {
			if (inputs.length == 3)
				try {
					state.delete(inputs[1], inputs[2]);
				} catch (Exception e) {
					err(e);
				}
			else
				msgln("Usage: del <key> <value>");
		}

		public void debug(String[] inputs) throws IOException {
			if (inputs.length == 1)
				try {
					context.toggleDebug();
				} catch (Exception e) {
					err(e);
				}
			else
				msgln("Usage: debug");
		}
		
		private String displayVals(String[] vs) {
			String vals = "{";
			if (vs.length > 0) {
				for (int i = 0; i < vs.length - 1; i++) {
					vals += vs[i];
					vals += ",";
				}
				vals += vs[vs.length - 1];
			}
			return vals+"}";
		}


	}


}
