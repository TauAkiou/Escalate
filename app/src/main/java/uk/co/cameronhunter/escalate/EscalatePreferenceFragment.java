package uk.co.cameronhunter.escalate;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
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

        RingtonePreference ringtone =
                findPreference(getString(R.string.ringtone_key));

        ringtone.setDefaultValue(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
        );

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

        final Preference escalateIsActive = findPreference(getString(R.string.on_call_key));
        final CheckBoxPreference showReminder =
                findPreference(getString(R.string.show_notification_key));
        final EditTextPreference reminderMessage =
                findPreference(getString(R.string.notification_message_key));

        Preference.OnPreferenceChangeListener prefChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (Boolean.TRUE.equals((Boolean) newValue && (isChecked(escalateIsActive) || isChecked(showReminder)))) {
                    // Create a new notification channel - required for operation on Android O and later.
                    createNotificationChannelReminder();
                    createNotificationChannelAnnoy();
                    Intent updateReminder = new Intent(ACTION_INSERT_OR_EDIT);
                    updateReminder.putExtra(getString(R.string.notification_message_key), reminderMessage.getText());


                    preference.getContext().sendBroadcast(updateReminder);
                } else {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationManager nm =
                                (NotificationManager) getContext().getSystemService(NotificationManager.class);
                        nm.deleteNotificationChannel(getString(R.string.reminder_channel));
                    }
                    preference.getContext().sendBroadcast(new Intent(ACTION_DELETE));
                }
                return true;
            }
        };

        escalateIsActive.setOnPreferenceChangeListener(prefChangeListener);
        showReminder.setOnPreferenceChangeListener(prefChangeListener);

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
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(
                    name.toString(),
                    description,
                    importance
            );
            NotificationManager nm = this.getContext().getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
            }
    }
}
