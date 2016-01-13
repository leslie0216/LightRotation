package usask.chl848.lightrotation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Locale;

/**
 * Experiment 1 (lock type) Login page (User id, User name, Color, Mode)
 */
public class LoginActivity_Experiment_1 extends Activity {
    private static final String[] LockModes = {"None", "Static", "Dynamic"};
    private String m_lockMode;

    private static final String[] PassModes = {"Multiple", "Single"};
    private String m_passMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_experiment_1);

        Spinner spinnerLock = (Spinner)findViewById(R.id.spinner_lock_mode);
        ArrayAdapter<String> adapterLock = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, LockModes);
        adapterLock.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLock.setAdapter(adapterLock);
        spinnerLock.setOnItemSelectedListener(new SpinnerLockSelectedListener());
        spinnerLock.setVisibility(View.VISIBLE);

        Spinner spinnerPass = (Spinner) findViewById(R.id.spinner_pass_mode);
        ArrayAdapter<String> adapterPass = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PassModes);
        adapterPass.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPass.setAdapter(adapterPass);
        spinnerPass.setOnItemSelectedListener(new SpinnerPassSelectedListener());
        spinnerPass.setVisibility(View.VISIBLE);

        Button btnPrev=(Button)this.findViewById(R.id.btn_go_ex1);
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = getUserName();
                String id = getUserId();
                if (!user.isEmpty()) {
                    Intent intent = new Intent();
                    intent.setClass(LoginActivity_Experiment_1.this, MainActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("user", user);
                    bundle.putString("id", id);
                    bundle.putInt("color", Color.BLACK);
                    bundle.putString("mode", "Compass");
                    bundle.putString("lockMode", m_lockMode);
                    bundle.putString("passMode", m_passMode);
                    bundle.putBoolean("isLogEnabled", isLogEnabled());
                    intent.putExtras(bundle);
                    LoginActivity_Experiment_1.this.startActivity(intent);
                    LoginActivity_Experiment_1.this.finish();
                }
            }
        });
    }

    class SpinnerLockSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
            m_lockMode = LockModes[arg2];
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    class SpinnerPassSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
            m_passMode = PassModes[arg2];
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    private String getUserName() {
        TextView user = (TextView)this.findViewById(R.id.txt_userName_ex1);
        return  user.getText().toString();
    }

    private String getUserId() {
        TextView user = (TextView)this.findViewById(R.id.txt_userId_ex1);
        return  user.getText().toString();
    }

    private boolean isLogEnabled() {
        return ((CheckBox)findViewById(R.id.checkbox_log)).isChecked();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void exit() {
        new AlertDialog.Builder(LoginActivity_Experiment_1.this,  AlertDialog.THEME_HOLO_DARK).setTitle("Warning").setMessage("Do you want to exit?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                System.exit(0);
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
            }
        }).show();
    }
}
