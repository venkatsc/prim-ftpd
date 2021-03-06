package org.primftpd.util;

import android.app.ActivityManager;
import android.app.Notification;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.primftpd.PrefsBean;
import org.primftpd.PrimitiveFtpdActivity;
import org.primftpd.R;
import org.primftpd.StartStopWidgetProvider;
import org.primftpd.prefs.LoadPrefsUtil;
import org.primftpd.remotecontrol.PftpdPowerTogglesPlugin;
import org.primftpd.remotecontrol.TaskerReceiver;
import org.primftpd.services.FtpServerService;
import org.primftpd.services.SshServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Utility methods to start and stop server services.
 */
public class ServicesStartStopUtil {

    public static final String EXTRA_PREFS_BEAN = "prefs.bean";
    public static final String EXTRA_FINGERPRINT_PROVIDER = "fingerprint.provider";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesStartStopUtil.class);

    public static void startServers(Context context) {
        SharedPreferences prefs = LoadPrefsUtil.getPrefs(context);
        PrefsBean prefsBean = LoadPrefsUtil.loadPrefs(LOGGER, prefs);
        startServers(context, prefsBean, new KeyFingerprintProvider(), null);
    }

    public static void startServers(
            Context context,
            PrefsBean prefsBean,
            KeyFingerprintProvider keyFingerprintProvider,
            PrimitiveFtpdActivity activity) {
        if (!isPasswordOk(prefsBean)) {
            Toast.makeText(
                context,
                R.string.haveToSetAuthMechanism,
                Toast.LENGTH_LONG).show();

            if (activity == null) {
                // Launch the main activity so that the user may set their password.
                Intent activityIntent = new Intent(context, PrimitiveFtpdActivity.class);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(activityIntent);
            }
        } else {
            boolean continueServerStart = true;
            if (prefsBean.getServerToStart().startSftp()) {
                boolean keyPresent = true;
                if (activity != null) {
                    keyPresent = activity.isKeyPresent();
                    if (!keyPresent) {
                        // cannot start sftp server when key is not present
                        // ask user to generate it
                        activity.showGenKeyDialog();
                        continueServerStart = false;
                    }
                }
                if (keyPresent) {
                    LOGGER.debug("going to start sshd");
                    try {
                        Intent intent = createSshServiceIntent(context, prefsBean, keyFingerprintProvider);
                        context.startService(intent);
                    } catch (Exception e) {
                        LOGGER.error("could not start sftp server", e);
                        Toast.makeText(
                                context,
                                "could not start sftp server, " + e.getMessage(),
                                Toast.LENGTH_SHORT);
                    }
                }
            }
            if (continueServerStart) {
                if (prefsBean.getServerToStart().startFtp()) {
                    LOGGER.debug("going to start ftpd");
                    try {
                        Intent intent = createFtpServiceIntent(context, prefsBean, keyFingerprintProvider);
                        context.startService(intent);
                    } catch (Exception e) {
                        LOGGER.error("could not start ftp server", e);
                        Toast.makeText(
                                context,
                                "could not start ftp server, " + e.getMessage(),
                                Toast.LENGTH_SHORT);
                    }
                }
            }
        }
    }

    public static void stopServers(Context context) {
        context.stopService(createFtpServiceIntent(context, null, null));
        context.stopService(createSshServiceIntent(context, null, null));
    }

    protected static Intent createFtpServiceIntent(
            Context context,
            PrefsBean prefsBean,
            KeyFingerprintProvider keyFingerprintProvider) {
        Intent intent = new Intent(context, FtpServerService.class);
        putPrefsInIntent(intent, prefsBean);
        putKeyFingerprintProviderInIntent(intent, keyFingerprintProvider);
        return intent;
    }

    protected static Intent createSshServiceIntent(
            Context context,
            PrefsBean prefsBean,
            KeyFingerprintProvider keyFingerprintProvider) {
        Intent intent = new Intent(context, SshServerService.class);
        putPrefsInIntent(intent, prefsBean);
        putKeyFingerprintProviderInIntent(intent, keyFingerprintProvider);
        return intent;
    }

    protected static void putPrefsInIntent(Intent intent, PrefsBean prefsBean) {
        if (prefsBean != null) {
            intent.putExtra(EXTRA_PREFS_BEAN, prefsBean);
        }
    }

    protected static void putKeyFingerprintProviderInIntent(Intent intent, KeyFingerprintProvider keyFingerprintProvider) {
        if (keyFingerprintProvider != null) {
            intent.putExtra(EXTRA_FINGERPRINT_PROVIDER, keyFingerprintProvider);
        }
    }

    protected static boolean isPasswordOk(PrefsBean prefsBean) {
        if (!prefsBean.getServerToStart().isPasswordMandatory(prefsBean)) {
            return true;
        }
        return !StringUtils.isBlank(prefsBean.getPassword());
    }

    public static ServersRunningBean checkServicesRunning(Context context) {
        ServersRunningBean serversRunning = new ServersRunningBean();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        String ftpServiceClassName = FtpServerService.class.getName();
        String sshServiceClassName = SshServerService.class.getName();
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            String currentClassName = service.service.getClassName();
            if (ftpServiceClassName.equals(currentClassName)) {
                serversRunning.ftp = true;
            }
            if (sshServiceClassName.equals(currentClassName)) {
                serversRunning.ssh = true;
            }
            if (serversRunning.ftp && serversRunning.ssh) {
                break;
            }
        }
        return serversRunning;
    }

    private static void updateWidget(Context context, boolean running)
    {
        LOGGER.debug("updateWidget()");
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);

        if (running) {
            remoteViews.setInt(R.id.widgetLayout,
                    "setBackgroundResource",
                    R.drawable.widget_background_enabled);
            remoteViews.setImageViewResource(
                    R.id.widgetIcon,
                    R.drawable.ic_stop_white_48dp);
            remoteViews.setTextViewText(
                    R.id.widgetText,
                    context.getText(R.string.widgetTextStop));
        } else {
            remoteViews.setInt(R.id.widgetLayout,
                    "setBackgroundResource",
                    R.drawable.widget_background_disabled);
            remoteViews.setImageViewResource(
                    R.id.widgetIcon,
                    R.drawable.ic_play_white_48dp);
            remoteViews.setTextViewText(
                    R.id.widgetText,
                    context.getText(R.string.widgetTextStart));
        }

        ComponentName thisWidget = new ComponentName(context, StartStopWidgetProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(thisWidget, remoteViews);
    }

    public static Notification updateNonActivityUI(
            Context ctxt,
            boolean serverRunning,
            PrefsBean prefsBean,
            KeyFingerprintProvider keyFingerprintProvider) {
        Notification notification = null;
        updateWidget(ctxt, serverRunning);
        if (serverRunning) {
            notification = NotificationUtil.createStatusbarNotification(
                    ctxt,
                    prefsBean,
                    keyFingerprintProvider);
        } else {
            LOGGER.debug("removeStatusbarNotification()");
            NotificationUtil.removeStatusbarNotification(ctxt);
        }
        new PftpdPowerTogglesPlugin().sendStateUpdate(ctxt, serverRunning);
        TaskerReceiver.sendRequestQueryCondition(ctxt);
        return notification;
    }
}
