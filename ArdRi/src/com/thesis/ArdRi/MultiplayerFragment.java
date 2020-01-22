package com.thesis.ArdRi;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.thesis.ArdRi.bluetooth.DeviceListActivity;

/**
 * Created by jerwinlipayon on 2/17/15.
 */
public class MultiplayerFragment extends Fragment {

    public static final String TAG = "MultiplayerFragment";

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.multiplayer_menu, container, false);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, ArdRiBluetooth.REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TextView join = (TextView)view.findViewById(R.id.joinGame);
        TextView create = (TextView)view.findViewById(R.id.createGame);
        join.setOnClickListener(joinGame);
        create.setOnClickListener(createGame);
        super.onViewCreated(view, savedInstanceState);
    }


    View.OnClickListener createGame = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Log.d(TAG," Create Game ");
            Intent ardriBluetoothIntent = new Intent(getActivity(), ArdRiBluetooth.class);
            ardriBluetoothIntent.putExtra(ArdRiBluetooth.PLAY_MODE, true);
            startActivity(ardriBluetoothIntent);
        }
    };

    View.OnClickListener joinGame = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            Log.d(TAG, " Join Game ");
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent( getActivity(), DeviceListActivity.class);
            startActivityForResult(serverIntent, ArdRiBluetooth.REQUEST_CONNECT_DEVICE_SECURE);

        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ArdRiBluetooth.REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("Connecting","resultCode == Activity.RESULT_OK");
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    //get the device name
                    String name =  data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_NAME);

                    Intent ardriBluetoothIntent = new Intent(getActivity(), ArdRiBluetooth.class);

                    ardriBluetoothIntent.putExtra(ArdRiBluetooth.PLAY_MODE, false);
                    ardriBluetoothIntent.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, address);
                    ardriBluetoothIntent.putExtra(DeviceListActivity.EXTRA_DEVICE_NAME, name);
                    startActivity(ardriBluetoothIntent);
                }
                break;
            case ArdRiBluetooth.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

}
