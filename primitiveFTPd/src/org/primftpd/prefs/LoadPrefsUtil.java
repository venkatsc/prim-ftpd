package org.primftpd.prefs;

import org.primftpd.R;
import org.primftpd.util.Defaults;
import org.slf4j.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.io.File;

public class LoadPrefsUtil
{
	public static final String PREF_KEY_USER = "userNamePref";
	public static final String PREF_KEY_PASSWORD = "passwordPref";
	public static final String PREF_KEY_PORT = "portPref";
	public static final String PREF_KEY_SECURE_PORT = "securePortPref";
	public static final String PREF_KEY_START_DIR = "startDirPref";
	public static final String PREF_KEY_ANNOUNCE = "announcePref";
	public static final String PREF_KEY_WAKELOCK = "wakelockPref";
	public static final String PREF_KEY_WHICH_SERVER = "whichServerToStartPref";
	public static final String PREF_KEY_THEME = "themePref";
	public static final String PREF_KEY_LOGGING = "loggingPref";

	public static final int PORT_DEFAULT_VAL = 12345;
	private static final String PORT_DEFAULT_VAL_STR = String.valueOf(PORT_DEFAULT_VAL);
	public static final int SECURE_PORT_DEFAULT_VAL = 1234;
	private static final String SECURE_PORT_DEFAULT_VAL_STR =
		String.valueOf(SECURE_PORT_DEFAULT_VAL);

	/**
	 * @return Android {@link SharedPreferences} object.
	 */
	public static SharedPreferences getPrefs(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static String userName(SharedPreferences prefs) {
		return prefs.getString(
			LoadPrefsUtil.PREF_KEY_USER,
			"user");
	}

	public static String password(SharedPreferences prefs) {
		return prefs.getString(
			LoadPrefsUtil.PREF_KEY_PASSWORD,
			null);
	}

	public static File startDir(SharedPreferences prefs) {
		String prefStr = prefs.getString(
			LoadPrefsUtil.PREF_KEY_START_DIR,
			null);
		return prefStr != null ? new File(prefStr) : Defaults.HOME_DIR;
	}

	public static Boolean announce(SharedPreferences prefs) {
		// default to false as it may cause crashes
		return prefs.getBoolean(
			LoadPrefsUtil.PREF_KEY_ANNOUNCE,
			Boolean.FALSE);
	}

	public static Boolean wakelock(SharedPreferences prefs) {
		return prefs.getBoolean(
			LoadPrefsUtil.PREF_KEY_WAKELOCK,
			Boolean.TRUE);
	}

	public static ServerToStart serverToStart(SharedPreferences prefs) {
		String whichServerStr = prefs.getString(
			LoadPrefsUtil.PREF_KEY_WHICH_SERVER,
			ServerToStart.ALL.xmlValue());
		return ServerToStart.byXmlVal(whichServerStr);
	}

	public static Theme theme(SharedPreferences prefs) {
		String themeStr = prefs.getString(
			PREF_KEY_THEME,
			Theme.DARK.xmlValue());
		return Theme.byXmlVal(themeStr);
	}

	public static int loadAndValidatePortInsecure(
		Context context,
		Logger logger,
		SharedPreferences prefs)
	{
		return loadAndValidatePort(
			context,
			logger,
			prefs,
			PREF_KEY_PORT,
			PORT_DEFAULT_VAL,
			PORT_DEFAULT_VAL_STR);
	}

	public static int loadAndValidatePortSecure(
		Context context,
		Logger logger,
		SharedPreferences prefs)
	{
		return loadAndValidatePort(
			context,
			logger,
			prefs,
			PREF_KEY_SECURE_PORT,
			SECURE_PORT_DEFAULT_VAL,
			SECURE_PORT_DEFAULT_VAL_STR);
	}

	private static int loadAndValidatePort(
		Context context,
		Logger logger,
		SharedPreferences prefs,
		String prefsKey,
		int defaultVal,
		String defaultValStr)
	{
		// load port
		int port = defaultVal;
		String portStr = prefs.getString(
			prefsKey,
			defaultValStr);
		try {
			port = Integer.valueOf(portStr);
		} catch (NumberFormatException e) {
			logger.info("NumberFormatException while parsing port key '{}'", prefsKey);
		}

		// validate port
		// I would prefer to do this in a prefsChangeListener, but that seems not to work
		if (!validatePort(port)) {
			Toast.makeText(
				context,
				R.string.portInvalid,
				Toast.LENGTH_LONG).show();
			port = defaultVal;
			Editor prefsEditor = prefs.edit();
			prefsEditor.putString(
				prefsKey,
				defaultValStr);
			prefsEditor.commit();
		}

		return port;
	}

	/**
	 * @param port
	 * @return True if port is valid, false if invalid.
	 */
	private static boolean validatePort(int port) {
		if (port > 1024 && port < 64000) {
			return true;
		}
		return false;
	}

	public static void resetPortsToDefault(SharedPreferences prefs) {
		Editor prefsEditor = prefs.edit();
		prefsEditor.putString(
			LoadPrefsUtil.PREF_KEY_PORT,
			LoadPrefsUtil.PORT_DEFAULT_VAL_STR);
		prefsEditor.putString(
			LoadPrefsUtil.PREF_KEY_SECURE_PORT,
			LoadPrefsUtil.SECURE_PORT_DEFAULT_VAL_STR);
		prefsEditor.commit();
	}
}