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

package org.feup.brunonova.drmips.simulator.exceptions;

/**
 * Exception to be thrown when a created CPU is invalid or incomplete.
 * 
 * @author Bruno Nova
 */
public class InvalidCPUException extends Exception {
	/**
	 * Exception constructor.
	 * @param msg The error message.
	 */
	public InvalidCPUException(String msg) {
		super(msg);
	}
}
