package kr.co.apostech.smartmeasure;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kr.co.apostech.smartmeasure.R;

public class MainActivity extends AppCompatActivity implements NavigationHost {

    private final static String TAG = "MainActivity";
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mRemoteDevice;
    private BluetoothLeService mMainBleService = null;
    private String mDeviceAddress;
    private MeasureData mMeasureData;

    private SharedPreferences mPref;

    // BackPress Event Handling
    private long pressedTime = 0;
    private OnBackPressedListener mBackListener;

    private ArrayList<String> REQUIRED_SDK_PERMISSIONS = new ArrayList<String>();



    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mMainBleService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mMainBleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                //Toast.makeText(getContext(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.i(TAG, "onServiceConnected");
            mMainBleService.connect(mDeviceAddress);
            //Toast.makeText(getContext(), "Service Connected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            mMainBleService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mMeasureData = new MeasureData();

        checkPermissions();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        /* for android10 is it works? */
        /*
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!LocationManagerCompat.isLocationEnabled(lm)) {
            // Start Location Settings Activity, you should explain to the user why he need to enable location before.
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
        */

        mPref = getSharedPreferences(MeasureData.SHARED_PREF_KEY_NAME , MODE_PRIVATE);
        mDeviceAddress = mPref.getString(MeasureData.PREF_KEY_DEV_ADDR, "");
        if(mDeviceAddress != "" && savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new SmartMeasureFragment()).commit();
            //Fragment fragment = new RooftopControlFragment();
            //((NavigationHost) getActivity()).navigateTo(fragment, false); // Navigate to the next Fragment
        }
        else if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new DevSelectFragment())
                    .commit();
        }
    }

    public interface OnBackPressedListener {
        public void onBack();
    }
    public void setOnBackPressedListener(OnBackPressedListener listener) {
        mBackListener = listener;
    }
    @Override
    public void onBackPressed() {
        // 다른 Fragment 에서 리스너를 설정했을 때 처리됩니다.
        if(mBackListener != null) {
            mBackListener.onBack();
        } else {
            super.onBackPressed();
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }


    /**
     * Navigate to the given fragment.
     *
     * @param fragment       Fragment to navigate to.
     * @param addToBackstack Whether or not the current fragment should be added to the backstack.
     */
    @Override
    public void navigateTo(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, fragment);

        if (addToBackstack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    public void setDeviceAddress(String str)
    {
        mDeviceAddress = str;
    }
    public String getDeviceAddress() {
        return mDeviceAddress;
    }
    public BluetoothDevice getBleDevice()
    {
        Log.i(TAG, "getBleDevice : "+mDeviceAddress);
        mRemoteDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        if(mRemoteDevice == null) {
            Log.e(TAG, "getBleDevice Fail : "+mDeviceAddress);
        }
        return mRemoteDevice;
    }


    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public void bleBindService()
    {
        bindService(new Intent(this, BluetoothLeService.class), mServiceConnection,  BIND_AUTO_CREATE);
        Log.i(TAG, "Bindig called");
    }

    public void bleUnbindService()
    {
        unbindService(mServiceConnection);
        mMainBleService = null;
        Log.i(TAG, "UnBindig called");
    }

    public BluetoothLeService getBleService()
    {
        return mMainBleService;
    }

    public MeasureData getMeasureData()
    {
        return mMeasureData;
    }

    public SharedPreferences getPref() {
        return mPref;
    }

    /**
     * Checks the dynamically-controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions

        REQUIRED_SDK_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        //REQUIRED_SDK_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //REQUIRED_SDK_PERMISSIONS.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        /*
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);

            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        }
        */
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions.toArray(new String[0]);
            for (final String permission : permissions) {
                Toast.makeText(this, permission, Toast.LENGTH_SHORT).show();
                boolean shouldProviceRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(this,
                                permission);
                if (shouldProviceRationale) {
                    final MainActivity act = this;
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("알림");
                    builder.setMessage("블루투스 사용을 위해 위치 권한이 필요합니다.");
                    builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(act,
                                    new String[]{permission}, REQUEST_CODE_ASK_PERMISSIONS);
                        }
                    });
                    builder.create();
                    builder.show();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{permission}, REQUEST_CODE_ASK_PERMISSIONS);
                }
            }
        }
        else {

            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.size()];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            String[] permissions = new String[REQUIRED_SDK_PERMISSIONS.size()];
            REQUIRED_SDK_PERMISSIONS.toArray(permissions);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, permissions, grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                //initialize();
                break;
        }
    }


}
