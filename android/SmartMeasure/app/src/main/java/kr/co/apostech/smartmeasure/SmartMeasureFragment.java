package kr.co.apostech.smartmeasure;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.Objects;
import java.util.UUID;

import kr.co.apostech.smartmeasure.R;

public class SmartMeasureFragment extends Fragment implements MainActivity.OnBackPressedListener {
    /*
            Const Variables
     */
    private final static String TAG = "SMART_MEASURE";

    /*
        Bluetooth Interface
     */
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattService mBleSvcCtrl;
    private BluetoothGattCharacteristic mBleCharResult;
    private BluetoothGattCharacteristic mBleCharCmd;

    /*
            Top Line Layout
     */
    private MaterialButton mBtnConnect;

    /*
            Mid Line Layout
     */
    private ImageView mImgIcon;
    private TextView mTvMode;
    private ProgressBar mProgValue;
    private TextView mTvValue;

    /*
            Bottom Line Layout
     */
    private MaterialButton mBtnMode;
    private MaterialButton mBtnMeasure;

    private MaterialButton mBtnFind;

    /*
            State Variables
     */
    private boolean mConnection = false;
    private MeasureData mMeasureData;
    private int mBtnState;

    private MainActivity mMain;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mMain = (MainActivity)getActivity();
        View view = inflater.inflate(R.layout.smart_measure_fragment, container, false);
        mBtnConnect = (MaterialButton)view.findViewById(R.id.btn_connect);
        mBtnConnect.setEnabled(true);
        mBtnConnect.setBackgroundColor(Color.LTGRAY);




        mImgIcon = (ImageView)view.findViewById(R.id.mode_img);
        mTvMode = (TextView)view.findViewById(R.id.tv_mode);

        mProgValue = (ProgressBar) view.findViewById(R.id.pb_value);
        mTvValue = (TextView)view.findViewById(R.id.tv_value);

        mBtnMode = (MaterialButton)view.findViewById(R.id.btn_mode);
        mBtnMeasure = (MaterialButton)view.findViewById(R.id.btn_measure);

        mBtnFind = (MaterialButton)view.findViewById(R.id.btn_find);
        mMeasureData = mMain.getMeasureData();


        mMain.bleBindService();
        mBluetoothLeService = mMain.getBleService();
        if(mBluetoothLeService != null && mBluetoothLeService.getConnectionState() == BluetoothLeService.BLE_STATE_CONNECTED) {
            mBleSvcCtrl = mBluetoothLeService.getService(MeasureData.UUID_MEASURE_SVC);
            mBleCharResult = mBleSvcCtrl.getCharacteristic(UUID.fromString(MeasureData.UUID_MEASURE_RESULT));
            mBleCharCmd = mBleSvcCtrl.getCharacteristic(UUID.fromString(MeasureData.UUID_MEASURE_CMD));
            mConnection = true;
            updateConnectionStateUI();
        }

        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mConnection && mBluetoothLeService != null) {
                    mBluetoothLeService.connect(mMain.getDeviceAddress());
                }
            }
        });

        mBtnMeasure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] val = new byte[1];
                val[0] = 'r';
                mBleCharCmd.setValue(val);
                mBluetoothLeService.writeCharacteristic(mBleCharCmd);
            }
        });

        mBtnMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] val = new byte[1];
                val[0] = 'm';
                mBleCharCmd.setValue(val);
                mBluetoothLeService.writeCharacteristic(mBleCharCmd);
            }
        });

        mBtnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mGattUpdateReceiver != null) {
                    Objects.requireNonNull(getActivity()).unregisterReceiver(mGattUpdateReceiver);
                    mGattUpdateReceiver = null;
                }

                // Release
                if(mBluetoothLeService != null) {
                    mBluetoothLeService.disconnect();
                }
                mMain.bleUnbindService();
                Editor editor = mMain.getPref().edit();
                editor.remove(MeasureData.PREF_KEY_DEV_ADDR);
                editor.commit();


                Fragment fragment = new DevSelectFragment();
                ((NavigationHost) getActivity()).navigateTo(fragment, false); // Navigate to the next Fragment
            }
        });

        return view;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_NOTI_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DESCRIPTOR_WRITTEN);
        intentFilter.addAction(BluetoothLeService.ACTION_STATE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        return intentFilter;
    }


    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                Log.i(TAG, "ACTION_BOND_STATE_CHANGED received");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName() != null) {
                    if(device.getName().equals(mMain.getDeviceAddress())) {
                        final int bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                        final int bond_prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                        Log.i(TAG, "ACTION_BOND_STATE_CHANGED(bond_state) : " + bond_state);
                        Log.i(TAG, "ACTION_BOND_STATE_CHANGED(bond_prevState) : " + bond_prevState);
                        if (bond_state == BluetoothDevice.BOND_BONDED) {
                            Log.d(TAG, "Paired");
                        }
                    }
                }
            }
            else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnection = true;
                updateConnectionStateUI();
                mBluetoothLeService = mMain.getBleService();
                Log.i(TAG, "Connected braoad received");

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnection = false;
                updateConnectionStateUI();
                Log.i(TAG, "DisConnected braoad received");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                mBleSvcCtrl = mBluetoothLeService.getService(MeasureData.UUID_MEASURE_SVC);
                if(mBleSvcCtrl == null) {
                    Log.e(TAG, "getService Fail");
                }
                mBleCharResult = mBleSvcCtrl.getCharacteristic(UUID.fromString(MeasureData.UUID_MEASURE_RESULT));
                mBluetoothLeService.setCharacteristicNotification(mBleCharResult, true);
                mBleCharCmd = mBleSvcCtrl.getCharacteristic(UUID.fromString(MeasureData.UUID_MEASURE_CMD));
                //byte[] val = new byte[1];
                //val[0] = 'i';
                //mBleCharCmd.setValue(val);
                //mBluetoothLeService.writeCharacteristic(mBleCharCmd);
                Log.e(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
            }
            else if( BluetoothLeService.ACTION_DESCRIPTOR_WRITTEN.equals(action)) {
                //startReadTimer(true, READ_TIMER_PERIOD);
                byte[] val = new byte[1];
                val[0] = 'i';
                mBleCharCmd.setValue(val);
                mBluetoothLeService.writeCharacteristic(mBleCharCmd);
                Log.e(TAG, "ACTION_DESCRIPTOR_WRITTEN");
            }
            else if (BluetoothLeService.ACTION_NOTI_DATA_AVAILABLE.equals(action)) {
                byte data[] = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if(data != null) {
                    //BatteryData.set_data(data);
                    //displayUniRooftopState(mRooftopState);
                    mMeasureData.setData(data);
                    displayMeasure(mMeasureData);
                }
                else {
                    Log.e(TAG, "Noti data null");
                }
            }
            else if (BluetoothLeService.ACTION_STATE_DATA_AVAILABLE.equals(action)) {
                // Read
                byte data[] = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if(data != null) {
                    //BatteryData.set_data(data);
                    //displayUniRooftopState(mRooftopState);
                }

            }
        }
    };

    public void displayMeasure(MeasureData data)
    {
        int mode;
        int val;
        switch(data.getMode()) {
            case MeasureData.MODE_WAIT:
                mImgIcon.setImageResource(R.drawable.wait);
                mTvMode.setText(MeasureData.STR_MODE_WAIT);
                break;
            case MeasureData.MODE_HEIGHT:
                mImgIcon.setImageResource(R.drawable.height);
                mTvMode.setText(MeasureData.STR_MODE_HEIGHT);
                break;
            case MeasureData.MODE_DISTANCE :
                mImgIcon.setImageResource(R.drawable.distance);
                mTvMode.setText(MeasureData.STR_MODE_DISTANCE);
                break;
            case MeasureData.MODE_CONFIG :
                mImgIcon.setImageResource(R.drawable.setting);
                mTvMode.setText(MeasureData.STR_MODE_CONFIG);
                break;
        }
        mProgValue.setProgress(data.getValue());
        mTvValue.setText(Integer.toString(data.getValue()));
    }

    public void updateConnectionStateUI()
    {
        if(mConnection) {
            mBtnConnect.setEnabled(false);
            mBtnConnect.setIconResource(R.drawable.bt_conn);
            mBtnConnect.setText(getString(R.string.ble_state_connected));
            mBtnConnect.setBackgroundColor(Color.CYAN);
        }
        else {
            mBtnConnect.setEnabled(true);
            mBtnConnect.setIconResource(R.drawable.bt_disconn);
            mBtnConnect.setText(getString(R.string.ble_state_disconnected));
            mBtnConnect.setBackgroundColor(Color.LTGRAY);
        }
    }



    public void clear_resource()
    {

    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        Log.i(TAG, "onAttach()");
        ((MainActivity)context).setOnBackPressedListener(this);
    }

    @Override
    public void onBack()
    {
        Log.i(TAG, "onBack()");
        clear_resource();
        //서비스 언바인드
        mMain.bleUnbindService();
        mMain.finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onResume()
    {
        Log.i(TAG, "OnResume()");
        getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        mBluetoothLeService = mMain.getBleService();
        if(mBluetoothLeService != null) {
            mBleSvcCtrl = mBluetoothLeService.getService(MeasureData.UUID_MEASURE_SVC);
            mBleCharResult = mBleSvcCtrl.getCharacteristic(UUID.fromString(MeasureData.UUID_MEASURE_RESULT));
            mBleCharCmd = mBleSvcCtrl.getCharacteristic(UUID.fromString(MeasureData.UUID_MEASURE_CMD));
            if(mBluetoothLeService.getConnectionState() == BluetoothLeService.BLE_STATE_CONNECTED) {
                mConnection = true;
            }
            else {
                mConnection = false;
            }

            Handler delayHandler = new Handler();
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateConnectionStateUI();
                }
            }, 500);
        }
        super.onResume();
    }

    @Override
    public void onPause()
    {
        Log.i(TAG, "OnPause()");
        super.onPause();
    }

    /*
        onDestroy called when navigate to other Fragment
     */
    @Override
    public void onDestroy()
    {
        Log.i(TAG, "OnDestroy()");
        clear_resource();
        if(mGattUpdateReceiver.isInitialStickyBroadcast()) {
            getActivity().unregisterReceiver(mGattUpdateReceiver);
        }
        mBluetoothLeService = null;
        super.onDestroy();
    }
}