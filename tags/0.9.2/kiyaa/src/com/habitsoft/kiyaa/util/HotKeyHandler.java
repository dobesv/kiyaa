package com.habitsoft.kiyaa.util;

public interface HotKeyHandler {

	// Two possible enter keys
	public static final int HOTKEY_ENTER1 = 10;
	public static final int HOTKEY_ENTER2 = 13;
	public static final int HOTKEY_CTRL_S = 19;
	public static final int HOTKEY_S = 's';
	
	/**
	 * Returns true if the hotkey was handled.
	 * @param callback TODO
	 */
	public boolean hotkeyPressed(int charCode);
}
