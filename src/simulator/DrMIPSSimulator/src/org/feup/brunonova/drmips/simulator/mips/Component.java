/*
    DrMIPS - Educational MIPS simulator
    Copyright (C) 2013-2015 Bruno Nova <ei08109@fe.up.pt>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.feup.brunonova.drmips.simulator.mips;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.feup.brunonova.drmips.simulator.exceptions.InvalidCPUException;
import org.feup.brunonova.drmips.simulator.util.Dimension;
import org.feup.brunonova.drmips.simulator.util.Point;

/**
 * Abstract base class to represent the MIPS CPU internal components.
 * 
 * <p>Each component has at least an ID, inputs and outputs.<br />
 * Each input and output has an identifier and an integer (32 bit) value.</p>
 * 
 * <p>Derived classes should call <tt>super(...)</tt> on their constructors and
 * must implement <tt>execute()</tt>, specifying what code the component
 * "executes" in each clock cycle.</p>
 * 
 * <p>Synchronous components extend from SynchronousComponent.</p>
 * 
 * <p>Components with an internal state should implement the <tt>HasInternalState</tt>
 * interface.</p>
 * 
 * @author Bruno Nova
 */
public abstract class Component {
	/** The available types of components. */
	public enum Type {PC, ADD, AND, OR, XOR, NOT, REGBANK, IMEM, FORK, CONTROL, DIST, MUX, CONST, SEXT,
		ZEXT, SLL, CONCAT, ALU_CONTROL, ALU, DMEM, PIPEREG, FWD_UNIT, HZD_UNIT, EXT_ALU}
	
	/** The unique identifier of the component. */
	private String id;
	/** The inputs of the component. */
	protected Map<String, Input> in;
	/** The outputs of the component. */
	protected Map<String, Output> out;
	/** The name displayed on the GUI. */
	private String displayName;
	/** The key of the component's description on the language file. */
	private String descriptionKey = null;
	/** The key of the component's name on the language file. */
	private String nameKey = null;
	/** The position of the component on the GUI. */
	private Point position;
	/** The size of the component on the GUI. */
	private Dimension size;
	/** The latency of the component. */
	private int latency = 0;
	/** The original latency of the component. */
	private int originalLatency = 0;
	/** The accumulated latency from the first component up to this one. */
	private int accumulatedLatency = 0;
	/** The component's custom description, if any, for each language. */
	private Map<String, String> customDescriptions = null;
	/** Whether this component is in the control path. */
	private boolean inControlPath = false;
	
	/**
	 * Component constructor that must be called by derived classes.
	 * @param id The component's identifier.
	 * @param latency The latency of the component.
	 * @param displayName The name displayed on the GUI.
	 * @param nameKey The key of the component's name on the language file, shown on the component's tooltip.
	 * @param descriptionKey The key of the component's description on the language file.
	 * @param position The position of the component on the GUI.
	 * @param size The size of the component on the GUI.
	 * @throws InvalidCPUException If <tt>id</tt> is empty.
	 */
	public Component(String id, int latency, String displayName, String nameKey, String descriptionKey, Point position, Dimension size) throws InvalidCPUException {
		setId(id);
		setLatency(latency);
		originalLatency = getLatency();
		setDisplayName(displayName);
		setNameKey(nameKey);
		setDescriptionKey(descriptionKey);
		setPosition(position);
		setSize(size);
		in = new TreeMap<String, Input>();
		out = new TreeMap<String, Output>();
	}
	
	/**
	 * "Executes" the normal action of the component in a clock cycle.
	 * <p>Derived classes must implement this method.<br />
	 * This method is executed automatically when an input is changed.</p>
	 */
	public abstract void execute();
	
	/**
	 * Adds a custom description to the component for the specified language.
	 * <p>The language is the language code (like en, pt, pt_PT) or "default" for
	 * the default language, in case the wanted language doesn't exist.</p>
	 * @param language The language.
	 * @param description The custom description.
	 */
	public void addCustomDescriptions(String language, String description) {
		if(customDescriptions == null) customDescriptions = new TreeMap<String, String>();
		customDescriptions.put(language.trim().toLowerCase(), description);
	}
	
	/**
	 * Returns whether the component has custom descriptions.
	 * @return <tt>True</tt> if the component has custom descriptions.
	 */
	public boolean hasCustomDescription() {
		return customDescriptions != null && customDescriptions.containsKey("default");
	}
	
	/**
	 * Returns the custom description for the given language, if it exists.
	 * @param language The language.
	 * @return The custom description, or <tt>null</tt> if the component doesn't have custom descriptions or doesn't have neither the given language or the default one.
	 */
	public String getCustomDescription(String language) {
		if(customDescriptions == null)
			return null;
		else {
			String lang = language.toLowerCase();
			String str;
			if((str = customDescriptions.get(lang)) != null) // get the description in the given language
				return str;
			else {
				// get description in a more general language designation (ex: pt for pt_PT)
				while(lang.contains("_")) {
					lang = lang.substring(0, lang.lastIndexOf('_'));
					if((str = customDescriptions.get(lang)) != null)
						return str;
				}
				
				// get the default description
				return customDescriptions.get("default");
			}
		}
	}
	
	/**
	 * Indicates that this component is in the control path.
	 */
	public void setInControlPath() {
		inControlPath = true;
		for(Input i: getInputs())
			i.setInControlPath();
		for(Output o: getOutputs())
			o.setInControlPath();
	}
	
	/**
	 * Returns whether this component is in the control path.
	 * @return <tt>True</tt> if in control path.
	 */
	public boolean isInControlPath() {
		return inControlPath;
	}
	
	/**
	 * Returns the unique identified of the component.
	 * @return Component's identifier.
	 */
	public final String getId() {
		return id;
	}
	
	/**
	 * Updates the component's unique identifier.
	 * @param id New identifier.
	 * @throws InvalidCPUException If <tt>id</tt> is empty.
	 */
	protected final void setId(String id) throws InvalidCPUException {
		if(id.isEmpty()) throw new InvalidCPUException("Invalid ID " + id +"!");
		this.id = id;
	}
	
	/**
	 * Returns the name displayed on the GUI.
	 * @return Component's display name.
	 */
	public final String getDisplayName() {
		return displayName;
	}
	
	/**
	 * Updates the component's display name.
	 * @param displayName New display name.
	 */
	protected final void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	/**
	 * Updates The key of the component's description on the language file.
	 * @param key New key.
	 */
	protected final void setDescriptionKey(String key) {
		descriptionKey = key;
	}
	
	/**
	 * Returns the key of the component's description on the language file.
	 * @return The key of the component's description on the language file.
	 */
	public String getDescriptionKey() {
		return descriptionKey;
	}
	
	/**
	 * Returns the key of the component's name on the language file.
	 * @return The key of the component's name on the language file.
	 */
	public String getNameKey() {
		return nameKey;
	}
	
	/**
	 * Updates The key of the component's name on the language file.
	 * @param key New key.
	 */
	protected final void setNameKey(String key) {
		nameKey = key;
	}
	
	/**
	 * Returns the position of the component on the GUI.
	 * @return Component's position on the GUI.
	 */
	public final Point getPosition() {
		return position;
	}
	
	/**
	 * Updates the component's pretended position on the GUI (the GUI isn't updated).
	 * @param position New position.
	 */
	protected final void setPosition(Point position) {
		this.position = position;
	}
	
	/**
	 * Returns the size of the component on the GUI.
	 * @return Component's graphical size.
	 */
	public final Dimension getSize() {
		return size;
	}
	
	/**
	 * Updates the component's pretended graphical size (the GUI isn't updated).
	 * @param size New sizze.
	 */
	protected final void setSize(Dimension size) {
		this.size = size;
	}
	
	/**
	 * Returns the latency of the component.
	 * @return Component's latency
	 */
	public final int getLatency() {
		return latency;
	}
	
	/**
	 * Updates the latency of the component (always positive).
	 * <p>This will make the calculated accumulated latencies and critical path invalid!</p>
	 * @param latency New latency.
	 */
	public final void setLatency(int latency) {
		this.latency = latency >= 0 ? latency : 0;
	}

	/**
	 * Returns the original latency of the component.
	 * @return The original latency of the component.
	 */
	public int getOriginalLatency() {
		return originalLatency;
	}
	
	/**
	 * Resets the component's latency to its original value.
	 * <p>The calculated accumulated latencies become invalid after this call.</p>
	 */
	public void resetLatency() {
		setLatency(originalLatency);
	}
	
	/**
	 * Returns the accumulated latency from the first component up to this one.
	 * @return Component's accumulated latency.
	 */
	public final int getAccumulatedLatency() {
		return accumulatedLatency;
	}
	
	/**
	 * Updates the component's accumulated latency, based on its inputs' accumulated latencies.
	 * @param instructionDependent Whether the performance should depend on the current instruction or not.
	 */
	protected void updateAccumulatedLatency(boolean instructionDependent) {
		accumulatedLatency = 0;
		List<Input> inputs = instructionDependent ? getLatencyInputs() : getInputs();
		for(Input i: inputs) // get highest accumulated latency from inputs
			if(i.canChangeComponentAccumulatedLatency() && i.getAccumulatedLatency() > accumulatedLatency)
				accumulatedLatency = i.getAccumulatedLatency();
		accumulatedLatency += latency; // add the component's own latency
		for(Output o: getOutputs()) // propagate accumulated latency
			if(o.isConnected())
				o.getConnectedInput().setAccumulatedLatency(accumulatedLatency, instructionDependent);
	}
	
	/**
	 * Updates the component's accumulated latency, based on its inputs' accumulated latencies.
	 */
	protected void updateAccumulatedLatency() {
		updateAccumulatedLatency(true);
	}
	
	/**
	 * Resets all performance information (component's and inputs' accumulated latencies and critical path).
	 */
	public void resetPerformance() {
		accumulatedLatency = 0;
		for(Input i: getInputs())
			i.resetAccumulatedLatency();
		for(Output o: getOutputs())
			o.unsetInCriticalPath();
	}
	
	/**
	 * Adds an input with an initial value.
	 * @param id Input identifier.
	 * @param data Data of the input (size and initial value).
	 * @return The added input.
	 * @throws InvalidCPUException If <tt>id</tt> is empty or duplicated.
	 */
	protected final Input addInput(String id, Data data) throws InvalidCPUException {
		return addInput(id, data, IOPort.Direction.WEST, true, false);
	}
	
	/**
	 * Adds an input with an initial value.
	 * @param id Input identifier.
	 * @param data Data of the input (size and initial value).
	 * @param direction The direction/side of the input on the component.
	 * @return The added input.
	 * @throws InvalidCPUException If <tt>id</tt> is empty or duplicated.
	 */
	protected final Input addInput(String id, Data data, Input.Direction direction) throws InvalidCPUException {
		return addInput(id, data, direction, true, false);
	}
	
	/**
	 * Adds an input with an initial value.
	 * @param id Input identifier.
	 * @param data Data of the input (size and initial value).
	 * @param direction The direction/side of the input on the component.
	 * @param changesComponentAccumulatedLatency Whether this input changes the respective component's accumulated latency (should be <b>false</b> if the value of the input is only used at the end of the clock cycle).
	 * @return The added input.
	 * @throws InvalidCPUException If <tt>id</tt> is empty or duplicated.
	 */
	protected final Input addInput(String id, Data data, Input.Direction direction, boolean changesComponentAccumulatedLatency) throws InvalidCPUException {
		return addInput(id, data, direction, changesComponentAccumulatedLatency, false);
	}
	
	/**
	 * Adds an input with an initial value.
	 * @param id Input identifier.
	 * @param data Data of the input (size and initial value).
	 * @param direction The direction/side of the input on the component.
	 * @param changesComponentAccumulatedLatency Whether this input changes the respective component's accumulated latency (should be <b>false</b> if the value of the input is only used at the end of the clock cycle).
	 * @param showTip Whether a balloon tip with the value of the input/output should be displayed.
	 * @return The added input.
	 * @throws InvalidCPUException If <tt>id</tt> is empty or duplicated.
	 */
	protected final Input addInput(String id, Data data, Input.Direction direction, boolean changesComponentAccumulatedLatency, boolean showTip) throws InvalidCPUException {
		if(hasInput(id)) throw new InvalidCPUException("Duplicated ID " + id + "!");
		Input input = new Input(this, id, data, direction, changesComponentAccumulatedLatency, showTip);
		in.put(id, input);
		return input;
	}
	
	/**
	 * Returns whether the component has an input with the specified identifier.
	 * @param id Input identifier.
	 * @return <tt>true</tt> if the component has the specified input.
	 */
	public final boolean hasInput(String id) {
		return in.containsKey(id);
	}
	
	/**
	 * Returns the value of the input with the specified identifier.
	 * @param id Input identifier.
	 * @return The data of the input, or <tt>null</tt> if the input doesn't exist.
	 */
	public final Input getInput(String id) {
		return in.get(id);
	}
	
	/**
	 * Returns the list of inputs.
	 * @return List of inputs.
	 */
	public final List<Input> getInputs() {
		return new ArrayList<Input>(in.values());
	}
	
	/**
	 * Returns the list of inputs for latency calculations. By default, does the
	 * same as getInputs()
	 *
	 * @return List of inputs.
	 */
	protected List<Input> getLatencyInputs() {
		return new ArrayList<Input>(in.values());
	}
	/**
	 * Adds an output with an initial value.
	 * @param id Output identifier.
	 * @param data Data of the output (size and initial value).
	 * @return The added output.
	 * @throws InvalidCPUException If <tt>id</tt> is empty or duplicated.
	 */
	protected final Output addOutput(String id, Data data) throws InvalidCPUException {
		return addOutput(id, data, IOPort.Direction.EAST, false);
	}
	
	/**
	 * Adds an output with an initial value.
	 * @param id Output identifier.
	 * @param data Data of the output (size and initial value).
	 * @param direction The direction/side of the output on the component.
	 * @return The added output.
	 * @throws InvalidCPUException If <tt>id</tt> is empty or duplicated.
	 */
	protected final Output addOutput(String id, Data data, Output.Direction direction) throws InvalidCPUException {
		return addOutput(id, data, direction, false);
	}
	
	/**
	 * Adds an output with an initial value.
	 * @param id Output identifier.
	 * @param data Data of the output (size and initial value).
	 * @param direction The direction/side of the output on the component.
	 * @param showTip Whether a balloon tip with the value of the input/output should be displayed.
	 * @return The added output.
	 * @throws InvalidCPUException If <tt>id</tt> is empty or duplicated.
	 */
	protected final Output addOutput(String id, Data data, Output.Direction direction, boolean showTip) throws InvalidCPUException {
		if(hasOutput(id)) throw new InvalidCPUException("Duplicated ID " + id + "!");
		Output output = new Output(this, id, data, direction, showTip);
		out.put(id, output);
		return output;
	}
	
	/**
	 * Returns whether the component has an output with the specified identifier.
	 * @param id Output identifier.
	 * @return <tt>true</tt> if the component has the specified output.
	 */
	public final boolean hasOutput(String id) {
		return out.containsKey(id);
	}
	
	/**
	 * Returns the value of the output with the specified identifier.
	 * @param id Output identifier.
	 * @return The data of the output, or <tt>null</tt> if the output doesn't exist.
	 */
	public final Output getOutput(String id) {
		return out.get(id);
	}
	
	/**
	 * Returns the list of outputs.
	 * @return List of outputs.
	 */
	public final List<Output> getOutputs() {
		return new ArrayList<Output>(out.values());
	}
	
	/**
	 * Returns the inputs (first) and the outputs that are in the specified direction/side.
	 * @param direction The desired direction/side of the component.
	 * @return Inputs and outputs on that direction/side.
	 */
	public final List<IOPort> getIOPortsInDirection(IOPort.Direction direction) {
		List<IOPort> ports = new LinkedList<IOPort>();
		
		for(Input i: getInputs())
			if(i.getDirection() == direction)
				ports.add(i);
		for(Output o: getOutputs())
			if(o.getDirection() == direction)
				ports.add(o);
		
		return ports;
	}
	
	/**
	 * Returns the graphical x,y position of the given input/output in the CPU.
	 * @param port The input/output.
	 * @return Position of the input/ouput in the graphical CPU.
	 */
	private Point getIOPortPosition(IOPort port) {
		if(port.hasPositionDefined()) return port.getPosition();
		
		Point pos;
		List<IOPort> ports = getIOPortsInDirection(port.getDirection()); // inputs/outputs in that direction
		int index = ports.indexOf(port); // index of the input/output in that direction<<<
		int numPorts = ports.size(); // number of inputs/outputs in that direction
		int length = (port.getDirection() == IOPort.Direction.WEST || port.getDirection() == IOPort.Direction.EAST) ? getSize().height : getSize().width; // width/height of the component, depending on the direction
		int dPos = length / (numPorts + 1) * (index + 1); // the additional x/y of the input/output
		
		switch(port.getDirection()) {
			case NORTH:       pos = new Point(getPosition().x + dPos,   getPosition().y); break;
			case SOUTH:       pos = new Point(getPosition().x + dPos,   getPosition().y + getSize().height); break;
			case WEST:        pos = new Point(getPosition().x,          getPosition().y + dPos); break;
			default /*EAST*/: pos = new Point(getPosition().x + getSize().width, getPosition().y + dPos); break;
		}
		
		port.setPosition(pos);
		return pos;
	}
	
	/**
	 * Returns the graphical x,y position of the given input in the CPU.
	 * @param input The input.
	 * @return Position of the input in the graphical CPU.
	 */
	public final Point getInputPosition(Input input) {
		return getIOPortPosition(input);
	}
	
	/**
	 * Returns the graphical x,y position of the given input in the CPU.
	 * @param id The identifier of the input.
	 * @return Position of the input in the graphical CPU.
	 */
	public final Point getInputPosition(String id) {
		Input input = getInput(id);
		return (input != null) ? getInputPosition(input) : null;
	}
	
	/**
	 * Returns the graphical x,y position of the given output in the CPU.
	 * @param output The output.
	 * @return Position of the output in the graphical CPU.
	 */
	public final Point getOutputPosition(Output output) {
		return getIOPortPosition(output);
	}
	
	/**
	 * Returns the graphical x,y position of the given output in the CPU.
	 * @param id The identifier of the output.
	 * @return Position of the output in the graphical CPU.
	 */
	public final Point getOutputPosition(String id) {
		Output output = getOutput(id);
		return (output != null) ? getOutputPosition(output) : null;
	}
}
