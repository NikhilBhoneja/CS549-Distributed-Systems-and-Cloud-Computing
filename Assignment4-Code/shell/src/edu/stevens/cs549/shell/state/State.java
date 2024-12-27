package edu.stevens.cs549.shell.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 * @author dduggan
 */
public class State implements IState {

	static final long serialVersionUID = 0L;

	public static Logger log = Logger.getLogger(State.class.getCanonicalName());
	
	/*
	 * Using singleton pattern for state (one state per node).
	 */
	private static State state;
	
	public static void setState(String name) {
		if (state != null) {
			throw new IllegalStateException("Setting state after it is already set!");
		}
		state = new State(name);
	}
	
	public static State getState() {
		if (state == null) {
			throw new IllegalStateException("Getting state before it is set!");
		}
		return state;
	}
	
	protected String name;

	public State(String name) {
		super();
		this.name = name;
	}

	/*
	 * Get the info for this DHT node.
	 */
	public String getName() {
		return name;
	}

	/*
	 * Local table operations.
	 */
	private Map<String,List<String>> dict = new HashMap<>();


	public synchronized String[] get(String k) {
		List<String> vl = dict.get(k);
		if (vl == null) {
			return null;
		} else {
			String[] va = new String[vl.size()];
			return vl.toArray(va);
		}
	}

	public synchronized void add(String k, String v) {
		List<String> vl = dict.get(k);
		if (vl == null) {
			vl = new ArrayList<String>();
			dict.put(k, vl);
		}
		vl.add(v);
	}

	public synchronized void delete(String k, String v) {
		List<String> vs = dict.get(k);
		if (vs != null)
			vs.remove(v);
	}

	public synchronized void clear() {
		dict.clear();
	}
		
}
