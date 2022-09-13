package kr.co.apostech.smartmeasure;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kr.co.apostech.smartmeasure.R;


/**
 * Fragment representing the login screen for Shrine.
 */
public class DevSelectFragment extends Fragment implements MainActivity.OnBackPressedListener {

    private final static String TAG = "SELECT";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBLEScanner;
    private Handler mHandler;
    private boolean mScanning;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;
    private static final long SCAN_PERIOD = 20000;
    private MainActivity mMain;

    static final int ITEM_SINGLE_LINE = 0;
    static final int ITEM_TWO_LINE = 1;
    static final int ITEM_THREE_LINE = 2;

    DevSelectAdapter mAdapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstanceState) {

        mMain = (MainActivity)getActivity();
        mBluetoothAdapter = mMain.getBluetoothAdapter();

        mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (mBLEScanner == null) {
            Toast.makeText(getContext(), "Can not find BLE Scanner", Toast.LENGTH_SHORT).show();
        }


        mHandler = new Handler();
        scanLeDevice(true);


        //RecyclerView view =
          //      (RecyclerView) layoutInflater.inflate(R.layout.dev_select_fragment, viewGroup, false);

        View view =
                layoutInflater.inflate(R.layout.dev_select_fragment, viewGroup, false);

        RecyclerView rview = (RecyclerView)view.findViewById(R.id.recycler_view);
        if(rview == null) {
            Log.e("VIEW", "FAIL");
        }

        rview.setLayoutManager(new LinearLayoutManager(getContext()));
        if(rview == null) {
            Log.e("VIEW2", "FAIL");
        }
        mAdapter = new DevSelectFragment.DevSelectAdapter();
        rview.setAdapter(mAdapter);


        return view;


       // Snippet from "Navigate to the next Fragment" section goes here.
        // Set an error if the password is less than 8 characters.

        // Clear the error once more than 8 characters are typed.
      //  passwordEditText.setOnKeyListener(new View.OnKeyListener() {
      //      @Override
      //      public boolean onKey(View view, int i, KeyEvent keyEvent) {
      //          if (isPasswordValid(passwordEditText.getText())) {
      //              passwordTextInput.setError(null); //Clear the error
      //          }
      //          return false;
      //      }
      //  });



    }

    // "isPasswordValid" from "Navigate to the next Fragment" section method goes here
    /*
    private boolean isPasswordValid(@Nullable Editable text) {
        return text != null && text.length() >= 8;
    }
    */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("AP", "coarse location permission granted");
                } else {

                }
            }
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBLEScanner.stopScan((ScanCallback) mScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBLEScanner.startScan((ScanCallback) mScanCallback);

        } else {
            mScanning = false;
            mBLEScanner.stopScan((ScanCallback) mScanCallback);
        }
    }

    // Device scan callback.
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
        }

        private void processResult(final ScanResult result) {

            BluetoothDevice device = result.getDevice();
            //Log.i("Dev", device.getName()+"/"+getString(R.string.ble_server_name));
            if( device.getName() != null && device.getName().equalsIgnoreCase(getString(R.string.ble_server_name))) {
                mAdapter.addDevice(result.getDevice());
                mAdapter.notifyItemInserted(mAdapter.getItemCount());
            }
        }
    };
/*
    @Override
    public void onPause() {
        super.onPause();
        scanLeDevice(false);
        mAdapter.clear();
    }

 */

    /** An Adapter that shows Single, Two, and Three line list items */
    public class DevSelectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private ArrayList<BluetoothDevice> mLeDevices;

        public DevSelectAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
            return DevSelectItem.create(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int position) {
            bind((DevSelectItem) viewHolder, position);

            viewHolder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Toast.makeText(v.getContext(), R.string.mtrl_list_item_clicked, Toast.LENGTH_SHORT).show();
                            //Bundle bundle = new Bundle(1);
                            //bundle.putString(getString(R.string.ble_address_key), mAdapter.getDevice(position).getAddress());
                            BluetoothDevice device = mAdapter.getDevice(position);
                            mMain.setDeviceAddress(device.getAddress());
                            SharedPreferences.Editor editor = mMain.getPref().edit();
                            editor.putString(MeasureData.PREF_KEY_DEV_ADDR, mMain.getDeviceAddress());
                            editor.commit();
                            scanLeDevice(false);

                            Fragment fragment = new SmartMeasureFragment();
                            //fragment.setArguments(bundle);
                            ((NavigationHost) getActivity()).navigateTo(fragment, false); // Navigate to the next Fragment
                        }
                    });
        }

        private void bind(DevSelectItem vh, int position) {
            BluetoothDevice device = mLeDevices.get(position);
            final String deviceName = device.getName();
            if(deviceName != null && deviceName.length()>0) {
                vh.text.setText(deviceName);
            }
            else {
                vh.text.setText("Unknown Device");
            }
            vh.secondary.setText(device.getAddress());
            vh.icon.setImageResource(R.drawable.ci_40);
        }

        @Override
        public int getItemViewType(int position) {
            return ITEM_TWO_LINE;
        }

        @Override
        public int getItemCount() {
            return mLeDevices.size();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {

            mLeDevices.clear();
        }

    }

    @Override
    public void onBack() {
        Log.e("DevSelect", "onBack()");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.e("Other", "onAttach()");
        ((MainActivity)context).setOnBackPressedListener(null);
    }
}