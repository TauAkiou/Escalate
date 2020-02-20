package uk.co.cameronhunter.escalate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;

import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.RingtonePreference;
import android.preference.TwoStatePreference;
import android.widget.FrameLayout;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ContentFrameLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static android.content.Intent.ACTION_DELETE;
import static android.content.Intent.ACTION_INSERT_OR_EDIT;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.co.cameronhunter.escalate.R.xml.preferences;


public class MainActivity extends AppCompatActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState);
        setContentView(R.layout.main_view);

    }

}
