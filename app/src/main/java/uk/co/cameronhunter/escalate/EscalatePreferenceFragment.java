package uk.co.cameronhunter.escalate;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;

import androidx.annotation.Nullable;

import androidx.media.AudioAttributesCompat;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.takisoft.preferencex.PreferenceFragmentCompat;
import com.takisoft.preferencex.RingtonePreference;
import com.takisoft.preferencex.EditTextPreference;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static android.content.Intent.ACTION_DELETE;
import static android.content.Intent.ACTION_INSERT_OR_EDIT;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class EscalatePreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        PreferenceCategory pcat = findPreference(getString(R.string.config_group));

        final Preference escalateIsActive = findPreference(getString(R.string.on_call_key));
        final CheckBoxPreference showReminder =
                findPreference(getString(R.string.show_notification_key));
        final EditTextPreference reminderMessage =
                findPreference(getString(R.string.notification_message_key));

        // Start by detecting OS version and hiding/unhiding relevant preferences.

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Preference notif_chan_settings = findPreference("goto_android_notificationchannel_settings");
            pcat.removePreference(findPreference("ringtone_key"));
            pcat.removePreference(findPreference("volume_key"));
            pcat.removePreference(findPreference("vibrate_key"));
            pcat.removePreference(findPreference("notification_light_key"));
            createNotificationChannelAnnoy();

            notif_chan_settings.setOnPreferenceClickListener( onclick -> {
                NotificationManager nm = this.getContext().getSystemService(NotificationManager.class);
                NotificationChannel nc = nm.getNotificationChannel("reminder_annoy");

                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, this.getContext().getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, nc.getId());
                startActivity(intent);

                return true;
            });
        }
        else {
            pcat.removePreference(findPreference("goto_android_notificationchannel_settings"));
            findPreference("ringtone_key").setVisible(true);
            findPreference("volume_key").setVisible(true);
            findPreference("vibrate_key").setVisible(true);
            findPreference("notification_light_key").setVisible(true);

            RingtonePreference ringtone =
                    findPreference(getString(R.string.ringtone_key));

            ringtone.setOnPreferenceChangeListener((new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return true;
                }
            }));

            ringtone.setDefaultValue(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
            );
        }
        EditTextPreference regex = findPreference(getString(R.string.regex_key));


        regex.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String pattern = (String) newValue;
                if (isNotBlank(pattern) && isValidPattern(pattern)) {
                    preference.setSummary(pattern);
                    return true;
                }
                return false;
            }
        });

        if (isNotBlank(regex.getText())) {
            regex.setSummary(regex.getText());
        }






        // Escalate Active
        Preference.OnPreferenceChangeListener escActiveListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (Boolean.TRUE.equals((Boolean) newValue)) {
                    // Create a new notification channel - required for operation on Android O and later.
                    createNotificationChannelAnnoy();
                    if(isChecked(showReminder)) {
                        createNotificationChannelReminder();
                        Intent updateReminder = new Intent(ACTION_INSERT_OR_EDIT);
                        updateReminder.putExtra(getString(R.string.notification_message_key), reminderMessage.getText());
                        preference.getContext().sendBroadcast(updateReminder);
                    }
                } else {
                            preference.getContext().sendBroadcast(new Intent(ACTION_DELETE));
                    }
                return true;
                }

        };

        Preference.OnPreferenceChangeListener showReminderListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (Boolean.TRUE.equals((Boolean) newValue)) {
                    // Create a new notification channel - required for operation on Android O and later.
                    createNotificationChannelReminder();
                    Intent updateReminder = new Intent(ACTION_INSERT_OR_EDIT);
                    updateReminder.putExtra(getString(R.string.notification_message_key), reminderMessage.getText());


                    preference.getContext().sendBroadcast(updateReminder);
                } else {
                    preference.getContext().sendBroadcast(new Intent(ACTION_DELETE));
                }
                return true;
            }
        };

        escalateIsActive.setOnPreferenceChangeListener(escActiveListener);
        showReminder.setOnPreferenceChangeListener(showReminderListener);

        reminderMessage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = (String) newValue;
                String message = isBlank(value) ? getString(R.string.notification_message_default) : value;

                preference.setSummary(message);

                if (isChecked(escalateIsActive) && isChecked(showReminder)) {
                    createNotificationChannelReminder();
                    Intent updateReminder = new Intent(ACTION_INSERT_OR_EDIT);
                    updateReminder.putExtra(getString(R.string.notification_message_key), message);
                    preference.getContext().sendBroadcast(updateReminder);
                }

                return true;
            }
        });

        reminderMessage.setSummary(isBlank(reminderMessage.getText()) ? getString(R.string.notification_message_default) : reminderMessage.getText());

        // Android API level 26 requires that the receivers be registered with contexts.
        if(Build.VERSION.SDK_INT >= 26) {
            BroadcastReceiver br = new ReminderReceiver();
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            filter.addAction(Intent.ACTION_INSERT_OR_EDIT);
            filter.addAction(Intent.ACTION_DELETE);
            getContext().registerReceiver(br, filter);
        }

        if(isChecked(escalateIsActive)) {
            createNotificationChannelAnnoy();
        }

        if (isChecked(escalateIsActive) && isChecked(showReminder)) {

            // Ensure that the notification channel exists if the reminder feature is active.
            createNotificationChannelReminder();
            getContext().sendBroadcast(new Intent(ACTION_INSERT_OR_EDIT));
        }


    }

    private static boolean isValidPattern( String pattern ) {
        try {
            Pattern.compile( pattern );
            return true;
        } catch ( PatternSyntaxException ex ) {
            return false;
        }
    }

    @SuppressLint( "NewApi" )
    private static boolean isChecked( Preference preference ) {
        if ( Build.VERSION.SDK_INT >= 16 ) {
            if ( preference instanceof TwoStatePreference ) {
                return ((TwoStatePreference) preference).isChecked();
            }
        }

        if ( preference instanceof CheckBoxPreference ) {
            return ((CheckBoxPreference) preference).isChecked();
        }
        else

        throw new RuntimeException( "Not supported preference" );
    }

    private void createNotificationChannelReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.reminder_channel);
            String description = getString(R.string.reminder_channel_desc);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(
                    name.toString(),
                    description,
                    importance
            );
            NotificationManager nm = this.getContext().getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private void createNotificationChannelAnnoy() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.reminder_annoy);
            String description = getString(R.string.reminder_annoy_desc);
            String ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(
                    name.toString(),
                    description,
                    importance
            );

            AudioAttributes aac = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            channel.enableVibration(true);
            channel.setVibrationPattern(new long[] {1000, 500, 0, 1000, 500, 0 });

            channel.enableLights(true);
            channel.setLightColor (Integer.parseInt("FF0000", 16));

            channel.setBypassDnd(true);
            channel.setSound(Uri.parse(ringtone), aac);
            NotificationManager nm = this.getContext().getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
            }
    }
}
