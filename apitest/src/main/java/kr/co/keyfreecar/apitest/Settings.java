package kr.co.keyfreecar.apitest;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import kr.co.keyfreecar.api.KeyfreecarConnector;
import kr.co.keyfreecar.api.KeyfreecarDetail;

import static kr.co.keyfreecar.apitest.MainActivity.connected;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Settings#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Settings extends Fragment {

    private Button register;
    private Button unregister1;
    private Button unregister2;
    private Button unregister3;
    private Button unregister4;

    private TextView user1;
    private TextView user2;
    private TextView user3;
    private TextView user4;
    private TextView smartPrepared;

    private TextView smartRange;
    private TextView accState;
    private Button changeSmartRange;

    private TextView doorHandleState;
    private Button doorHandleChange;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public Settings() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Settings.
     */
    // TODO: Rename and change types and number of parameters
    public static Settings newInstance(String param1, String param2) {
        Settings fragment = new Settings();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        settings();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    void settings() {
        register = getView().findViewById(R.id.registerSmart);

        unregister1 = getView().findViewById(R.id.unregisterSmart1);
        unregister2 = getView().findViewById(R.id.unregisterSmart2);
        unregister3 = getView().findViewById(R.id.unregisterSmart3);
        unregister4 = getView().findViewById(R.id.unregisterSmart4);

        user1 = getView().findViewById(R.id.smartRegistered1);
        user2 = getView().findViewById(R.id.smartRegistered2);
        user3 = getView().findViewById(R.id.smartRegistered3);
        user4 = getView().findViewById(R.id.smartRegistered4);
        smartPrepared = getView().findViewById(R.id.smartPrepared);

        smartRange = getView().findViewById(R.id.smartRange);
        changeSmartRange = getView().findViewById(R.id.changeSmartRange);
        accState = getView().findViewById(R.id.accState);

        doorHandleChange = getView().findViewById(R.id.doorhandleChange);
        doorHandleState = getView().findViewById(R.id.doorhandleUnlock);



        smartPrepared.setText("준비 안됨");

        if (connected.getKeyfreecarDetail().isSmartRegisterReady()) {
            smartPrepared.setText("준비 됨");
        }
        connected.setOnUpdate(KeyfreecarDetail.Update.Item.UPDATE_BOOLEAN_ITEM_IS_SMART_REGISTER_READY, o -> {
            Boolean ready = (Boolean) o;
            if (ready) {
                getActivity().runOnUiThread(() -> {
                    smartPrepared.setText("준비 됨");
                });
            } else {
                getActivity().runOnUiThread(() -> {
                    smartPrepared.setText("준비 안됨");
                });
            }
        });

        String[] initSmartUsers = connected.getKeyfreecarDetail().getSmartUsers();
        user1.setText(initSmartUsers[0]);
        user2.setText(initSmartUsers[1]);
        user3.setText(initSmartUsers[2]);
        user4.setText(initSmartUsers[3]);

        connected.setOnUpdate(KeyfreecarDetail.Update.Item.UPDATE_STRING_ARRAY_ITEM_SMART_USERS, o -> {
            String[] smartUsers = (String[]) o;
            getActivity().runOnUiThread(() -> {
                user1.setText(smartUsers[0]);
                user2.setText(smartUsers[1]);
                user3.setText(smartUsers[2]);
                user4.setText(smartUsers[3]);
            });
        });

        String initAcc;
        switch (connected.getKeyfreecarDetail().getAccState()) {
            case KeyfreecarDetail.Update.Value.STATE_ACC_OFF:
                initAcc = "OFF";
                break;
            case KeyfreecarDetail.Update.Value.STATE_ACC_ON:
                initAcc = "ON";
                break;
            case KeyfreecarDetail.Update.Value.STATE_ACC_START:
                initAcc = "START";
                break;
            default:
                initAcc="UNKNOWN";
                break;

        }

        accState.setText(initAcc);

        String initRange;
        switch (connected.getKeyfreecarDetail().getSmartRange()) {
            case KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_OFF:
                initRange = "비활성화";
                break;
            case KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_SHORT:
                initRange = "짧음";
                break;
            case KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_MEDIUM:
                initRange = "보통";
                break;
            case KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_LONG:
                initRange = "넓음";
                break;
            default:
                initRange="알 수 없음";
                break;

        }

        smartRange.setText(initRange);

        connected.setOnUpdate(KeyfreecarDetail.Update.Item.UPDATE_INTEGER_ITEM_ACC_STATE, o -> {
            Integer state = (Integer) o;
            getActivity().runOnUiThread(() -> {
                String stringify;
                if (state == KeyfreecarDetail.Update.Value.STATE_ACC_OFF) {
                    stringify = "OFF";
                } else if (state == KeyfreecarDetail.Update.Value.STATE_ACC_ON) {
                    stringify = "ON";
                } else if (state == KeyfreecarDetail.Update.Value.STATE_ACC_START) {
                    stringify = "START";
                } else {
                    stringify = "UNKNOWN";
                }

                accState.setText(stringify);
            });
        });

        connected.setOnUpdate(KeyfreecarDetail.Update.Item.UPDATE_INTEGER_ITEM_SMART_RANGE, o -> {
            Integer range = (Integer) o;
            getActivity().runOnUiThread(() -> {
                String stringify;
                if (range == KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_OFF) {
                    stringify = "비활성화";
                } else if (range == KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_SHORT) {
                    stringify = "짧음";
                } else if (range == KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_MEDIUM) {
                    stringify = "보통";
                } else if (range == KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_LONG) {
                    stringify = "넓음";
                } else {
                    stringify = "UNKNOWN";
                }

                smartRange.setText(stringify);
            });
        });

        doorHandleState.setText(String.format(" %s(%s) ",connected.getKeyfreecarDetail().isDoorHandleEnable() ? "활성화" : "비활성화",connected.getKeyfreecarDetail().getDoorHandlePassword()));

        connected.setOnUpdate(KeyfreecarDetail.Update.Item.UPDATE_BOOLEAN_ITEM_DOOR_HANDLE_ENABLE, o -> {
            Boolean enabled = (Boolean) o;
            getActivity().runOnUiThread(() -> {

                doorHandleState.setText(String.format(" %s(%s) ",enabled ? "활성화" : "비활성화", connected.getKeyfreecarDetail().getDoorHandlePassword()));
            });
        });

        connected.setOnUpdate(KeyfreecarDetail.Update.Item.UPDATE_INTEGER_ITEM_DOOR_HANDLE_PASSWORD, o -> {
            Integer password = (Integer) o;
            getActivity().runOnUiThread(() -> {
                String stringify = password.toString();


                doorHandleState.setText(String.format(" %s(%s) ",connected.getKeyfreecarDetail().isDoorHandleEnable() ? "활성화" : "비활성화",stringify));
            });
        });

        changeSmartRange.setOnClickListener(v -> {



            new AlertDialog.Builder(getContext())
                    .setItems(new CharSequence[]{"비활성화", "짧음", "보통", "넓음"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (connected != null) {
                                connected.SetSmartRange(i + 10, new KeyfreecarConnector.ResultCallback() {
                                    @Override
                                    public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "smart range changed", Toast.LENGTH_SHORT).show();
                                        });
                                    }

                                    @Override
                                    public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "smart range change preparing", Toast.LENGTH_SHORT).show();
                                        });
                                    }

                                    @Override
                                    public void onFailure(KeyfreecarConnector keyfreecarConnector, int i) {
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "smart range change failed - " + KeyfreecarConnector.FailReasonToPlainString(i), Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                            }

                            dialogInterface.dismiss();
                        }
                    }).create().show();
        });


        unregister1.setOnClickListener(v -> {
            if (connected != null) {
                connected.RemoveSmartRegistration(0, new KeyfreecarConnector.ResultCallback() {
                    @Override
                    public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user0 unregistered", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user0 unregister preparing", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(KeyfreecarConnector keyfreecarConnector, int i) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user0 unregister failed - "+ KeyfreecarConnector.FailReasonToPlainString(i), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        unregister2.setOnClickListener(v -> {
            if (connected != null) {
                connected.RemoveSmartRegistration(1, new KeyfreecarConnector.ResultCallback() {
                    @Override
                    public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user1 unregistered", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user1 unregister preparing", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(KeyfreecarConnector keyfreecarConnector, int i) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user1 unregister failed - "+ KeyfreecarConnector.FailReasonToPlainString(i), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        unregister3.setOnClickListener(v -> {
            if (connected != null) {
                connected.RemoveSmartRegistration(2, new KeyfreecarConnector.ResultCallback() {
                    @Override
                    public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user2 unregistered", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user2 unregister preparing", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(KeyfreecarConnector keyfreecarConnector, int i) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user2 unregister failed - "+ KeyfreecarConnector.FailReasonToPlainString(i), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });
        unregister4.setOnClickListener(v -> {
            if (connected != null) {
                connected.RemoveSmartRegistration(3, new KeyfreecarConnector.ResultCallback() {
                    @Override
                    public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user3 unregistered", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user3 unregister preparing", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(KeyfreecarConnector keyfreecarConnector, int i) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart user3 unregister failed - "+ KeyfreecarConnector.FailReasonToPlainString(i), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });


        register.setOnClickListener(v -> {
            if (connected != null) {
                connected.PrepareSmartRegistration(new KeyfreecarConnector.ResultCallback() {
                    @Override
                    public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart register prepared", Toast.LENGTH_SHORT).show();
                        });

                        BroadcastReceiver bcr = new BroadcastReceiver() {

                            @Override
                            public void onReceive(Context context, Intent intent) {
                                String action = intent.getAction();
                                switch (action) {
                                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:

                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "starting smart door register", Toast.LENGTH_SHORT).show();
                                        });

                                        break;
                                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                                        Log.d("keyfreecar.Smart", "Discovery Finish");

                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(getContext(), "finishing smart door register", Toast.LENGTH_SHORT).show();
                                        });
                                        getActivity().unregisterReceiver(this);
                                        break;
                                    case BluetoothDevice.ACTION_FOUND:
                                        BluetoothDevice targetDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                        String name = "Smart " + connected.getKeyfreecarDetail().getDeviceName();
                                        Log.d("keyfreecar.Smart ", "target name = " + name + " compare to " + targetDevice.getName());
                                        if (targetDevice.getName() != null && (targetDevice.getName().contentEquals(name))) {
                                            Log.d("keyfreecar.Smart ", "scanName = " + targetDevice.getName());
                                            Log.d("keyfreecar.Smart ", "scanClass = " + String.format("%x", targetDevice.getBluetoothClass().getDeviceClass()));
                                            if (targetDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                                                Log.d("keyfreecar.Smart", "already bonded");
                                            }

                                            targetDevice.createBond();
                                            getActivity().unregisterReceiver(this);
                                        }
                                        break;
                                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                                        BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                        int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                                        switch (state) {
                                            case BluetoothDevice.BOND_BONDED:


                                                if (BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
                                                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                                                }
                                                getActivity().runOnUiThread(() -> {
                                                    Toast.makeText(getContext(), "smart registartion complete", Toast.LENGTH_SHORT).show();
                                                });

                                                break;
                                            case BluetoothDevice.BOND_BONDING:
                                                break;
                                            case BluetoothDevice.BOND_NONE:
                                                break;
                                        }


                                }
                            }
                        };
                        IntentFilter filter = new IntentFilter();
                        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                        filter.addAction(BluetoothDevice.ACTION_FOUND);

                        getContext().registerReceiver(bcr, filter);

                        BluetoothAdapter.getDefaultAdapter().startDiscovery();


                    }

                    @Override
                    public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart register preparing", Toast.LENGTH_SHORT).show();
                        });

                    }

                    @Override
                    public void onFailure(KeyfreecarConnector keyfreecarConnector, int i) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "smart register failed - " + KeyfreecarConnector.FailReasonToPlainString(i), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        doorHandleChange.setOnClickListener((v) -> {
            new AlertDialog.Builder(getContext())
                    .setItems(new CharSequence[]{"활성화", "비활성화"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            final KeyfreecarConnector.ResultCallback commonCallback = new KeyfreecarConnector.ResultCallback() {
                                @Override
                                public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "doorHandle Setting changed", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "doorHandle Setting preparing", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onFailure(KeyfreecarConnector keyfreecarConnector, int i) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "doorHandle Setting failed - " + KeyfreecarConnector.FailReasonToPlainString(i), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            };
                            final boolean activation;
                            activation = i == 0;

                            if(activation){


                                final EditText dhPasswordInput = new EditText(getContext());
                                dhPasswordInput.setHint("비밀번호는 1~9로 네 자리");
                                dhPasswordInput.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);



                                new AlertDialog.Builder(getContext())
                                        .setPositiveButton("비밀번호 변경", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                if (connected != null) {
                                                    if(dhPasswordInput.getText().length() > 0)
                                                        connected.SetDoorHandle(Integer.valueOf(dhPasswordInput.getText().toString()),activation ,commonCallback);
                                                    else
                                                        connected.SetDoorHandle(connected.getKeyfreecarDetail().getDoorHandlePassword(),activation,commonCallback);
                                                }

                                                dialogInterface.dismiss();
                                            }
                                        })
                                        .setNegativeButton("비밀번호 유지", (d,i2) ->{
                                            if(connected != null){
                                                connected.SetDoorHandle(connected.getKeyfreecarDetail().getDoorHandlePassword(),activation,commonCallback);
                                            }
                                            dialogInterface.dismiss();
                                        }).setView(dhPasswordInput).create().show();
                            } else {
                                connected.SetDoorHandle(connected.getKeyfreecarDetail().getDoorHandlePassword(),activation,commonCallback);
                            }


                            dialogInterface.dismiss();
                        }
                    }).create().show();
        });

    }
}