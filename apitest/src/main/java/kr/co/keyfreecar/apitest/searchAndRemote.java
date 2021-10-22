package kr.co.keyfreecar.apitest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.graphics.Color;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

import kr.co.keyfreecar.api.KeyfreecarBluetoothManager;
import kr.co.keyfreecar.api.KeyfreecarConnector;

import static kr.co.keyfreecar.apitest.MainActivity.connected;
import static kr.co.keyfreecar.apitest.MainActivity.kbm;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link searchAndRemote#newInstance} factory method to
 * create an instance of this fragment.
 */
public class searchAndRemote extends Fragment {

    private final String Logtag = "apitest.searchAndRemote";

    private LinearLayout scroll;
    private Button search;

    private Button connect;
    private Button disconnect;
    private Button lock;
    private Button unlock;
    private Button changeName;
    private Button changePassword;
    private Button engineStart;
    private Button engineStop;

    private BottomNavigationView bottomNavigationView;


    private LinearLayout selected = null;
    ArrayMap<LinearLayout, BluetoothDevice> selector;
    ArrayMap<String, LinearLayout> tracker;

    ArrayList<Button> activatedWhenConnected;
    ArrayList<Button> activatedWhenDisconnected;

    ArrayList<Button> deactivatedWhenConnected;
    ArrayList<Button> deactivatedWhenDisconnected;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public searchAndRemote() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment searchAndRemote.
     */
    // TODO: Rename and change types and number of parameters
    public static searchAndRemote newInstance(String param1, String param2) {
        searchAndRemote fragment = new searchAndRemote();
        Bundle args = new Bundle();

        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);


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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_search_and_remote, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SearchAndRemote();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (kbm != null && kbm.isScanning()) {
            search.callOnClick();
        }
    }

    void SearchAndRemote() {

        activatedWhenConnected = new ArrayList<>();
        activatedWhenDisconnected = new ArrayList<>();

        deactivatedWhenConnected = new ArrayList<>();
        deactivatedWhenDisconnected = new ArrayList<>();


        scroll = getView().findViewById(R.id.SelectScroll);
        search = getView().findViewById(R.id.Search);

        connect = getView().findViewById(R.id.connect);
        connect.setEnabled(false);

        deactivatedWhenConnected.add(connect);


        disconnect = getView().findViewById(R.id.disconnect);
        activatedWhenConnected.add(disconnect);
        deactivatedWhenDisconnected.add(disconnect);

        lock = getView().findViewById(R.id.lock);
        activatedWhenConnected.add(lock);
        deactivatedWhenDisconnected.add(lock);

        unlock = getView().findViewById(R.id.unlock);
        activatedWhenConnected.add(unlock);
        deactivatedWhenDisconnected.add(unlock);

        changeName = getView().findViewById(R.id.changeName);
        deactivatedWhenDisconnected.add(changeName);
        activatedWhenConnected.add(changeName);

        changePassword = getView().findViewById(R.id.changePassword);
        deactivatedWhenDisconnected.add(changePassword);
        activatedWhenConnected.add(changePassword);

        engineStart = getView().findViewById(R.id.engineStart);
        deactivatedWhenDisconnected.add(engineStart);
        activatedWhenConnected.add(engineStart);

        engineStop = getView().findViewById(R.id.engineStop);
        deactivatedWhenDisconnected.add(engineStop);
        activatedWhenConnected.add(engineStop);


        if (connected != null) {
            for (Button b : activatedWhenConnected) {
                b.setEnabled(true);
            }
            for (Button b : deactivatedWhenConnected) {
                b.setEnabled(false);
            }

        } else {
            for (Button b : activatedWhenDisconnected) {
                b.setEnabled(true);
            }
            for (Button b : deactivatedWhenDisconnected) {
                b.setEnabled(false);
            }
        }


        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View.OnClickListener onClick = this;
                selector = new ArrayMap<>();
                tracker = new ArrayMap<>();

                kbm = new KeyfreecarBluetoothManager.Builder(getContext())
                        .setConnectionCallback(new KeyfreecarBluetoothManager.ConnectionCallback() {
                            @Override
                            public void onScanStart(ScanCallback scanCallback) {
                                Button b = (Button) v;
                                b.setText("Searching click to stop");
                                b.setOnClickListener(v2 -> {
                                    kbm.StopSearch(BluetoothAdapter.getDefaultAdapter(), scanCallback);
                                    b.setOnClickListener(onClick);
                                });
                                scroll.removeAllViews();

                                selected = null;
                                connect.setEnabled(false);

                            }

                            @Override
                            public void onScanFinish(int reason) {
                                Button b = (Button) v;
                                b.setText("Finished Searching. try again?");


                            }


                            @Override
                            public void onDeviceFound(BluetoothDevice bluetoothDevice, KeyfreecarBluetoothManager keyfreecarBluetoothManager) {


                                getActivity().runOnUiThread(() -> {
                                    LinearLayout wrapper = new LinearLayout(scroll.getContext());
                                    wrapper.setBackgroundColor(Color.argb(0xAA, 0x00, 0xAA, 0x00));
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                    wrapper.setPadding(15, 30, 15, 30);
                                    params.setMargins(5, 5, 5, 5);

                                    wrapper.setLayoutParams(params);
                                    wrapper.setOrientation(LinearLayout.HORIZONTAL);

                                    String deviceName = bluetoothDevice.getName() == null ? "Unknown Name" : bluetoothDevice.getName();
                                    String deviceMac = bluetoothDevice.getAddress().toUpperCase();

                                    LinearLayout.LayoutParams textParam = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

                                    TextView name = new TextView(wrapper.getContext());
                                    name.setText(deviceName);
                                    name.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                                    name.setTextColor(Color.WHITE);
                                    name.setLayoutParams(textParam);

                                    TextView mac = new TextView(wrapper.getContext());
                                    mac.setText(deviceMac);
                                    mac.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                                    mac.setTextColor(Color.WHITE);
                                    mac.setLayoutParams(textParam);

                                    wrapper.addView(name);
                                    wrapper.addView(mac);

                                    wrapper.setOnClickListener((v) -> {
                                        if (selected != null) {
                                            selected.setBackgroundColor(Color.argb(0xAA, 0x00, 0xAA, 0x00));

                                        }
                                        connect.setEnabled(true);
                                        selected = wrapper;
                                        selected.setBackgroundColor(Color.argb(0x55, 0x00, 0xAA, 0x00));
                                    });

                                    wrapper.setClickable(true);

                                    selector.put(wrapper, bluetoothDevice);
                                    tracker.put(bluetoothDevice.getAddress(), wrapper);

                                    scroll.addView(wrapper);

                                    //Toast.makeText(self,"new device",Toast.LENGTH_SHORT).show();

                                });


                            }

                            @Override
                            public void onDeviceLost(BluetoothDevice bluetoothDevice, KeyfreecarBluetoothManager keyfreecarBluetoothManager) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "device lost", Toast.LENGTH_SHORT).show();
                                    LinearLayout wrapper = tracker.remove(bluetoothDevice.getAddress());

                                    if (selected.equals(wrapper)) {
                                        selected = null;
                                    }

                                    selector.remove(wrapper);
                                    scroll.removeView(wrapper);

                                });
                            }

                            @Override
                            public void onDeviceAvailable(KeyfreecarConnector keyfreecarConnector) {
                                Log.d(Logtag, "onDeviceAvailable");
                                connected = keyfreecarConnector;

                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "연결되었습니다.", Toast.LENGTH_SHORT).show();

                                    for (Button b : activatedWhenConnected) {
                                        b.setEnabled(true);
                                    }
                                    for (Button b : deactivatedWhenConnected) {
                                        b.setEnabled(false);
                                    }

                                });

                            }

                            @Override
                            public void onDeviceDisable(KeyfreecarConnector keyfreecarConnector, int reason) {
                                Log.d(Logtag, "onDeviceDisable");
                                if (connected == keyfreecarConnector) {
                                    connected = null;
                                }
                                getActivity().runOnUiThread(() -> {


                                    for (Button b : activatedWhenDisconnected) {
                                        b.setEnabled(true);
                                    }
                                    for (Button b : deactivatedWhenDisconnected) {
                                        b.setEnabled(false);
                                    }
                                    Toast.makeText(getContext(), "연결 해제했습니다. - " + KeyfreecarBluetoothManager.FailReasonToPlainString(reason), Toast.LENGTH_SHORT).show();
                                });
                            }
                        })
                        .build();

                //kbm.StartSearch(BluetoothAdapter.getDefaultAdapter(),"KFC-8404",KeyfreecarBluetoothManager.SCAN_FILTER_FOR_NAME);
                kbm.StartSearch();

            }
        });

        connect.setOnClickListener(v -> {
            if (selected == null || kbm == null) {
                Toast.makeText(getContext(), "연결할 기기를 검색, 선택후 시도하세요", Toast.LENGTH_SHORT).show();
            } else {
                EditText pwIn = new EditText(getContext());
                pwIn.setHint("초기 비밀번호는 0입니다.");

                new AlertDialog.Builder(getContext())
                        .setView(pwIn)
                        .setTitle("기기의 비밀번호를 입력하세요")
                        .setPositiveButton("연결", (i, z) -> {
                            kbm.Connect(selector.get(selected), pwIn.getText().toString());
                            i.dismiss();
                        })
                        .setNegativeButton("취소", (i, z) -> {
                            i.dismiss();
                        }).create().show();


            }
        });

        disconnect.setOnClickListener(v -> {
            if (connected == null) {
                Toast.makeText(getContext(), "연결된 기기가 없습니다.", Toast.LENGTH_SHORT).show();
            } else {


                connected.Disconnect();
            }
        });

        lock.setOnClickListener(v -> {
            if (connected == null) {
                Toast.makeText(getContext(), "연결을 완료하고 다시 시도 하세요.", Toast.LENGTH_SHORT).show();
            } else {
                connected.LockDoor(new KeyfreecarConnector.ResultCallback() {
                    @Override
                    public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "잠금 성공", Toast.LENGTH_SHORT).show();
                        });

                    }

                    @Override
                    public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "잠금 전송", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(KeyfreecarConnector keyfreecarConnector, int reason) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "잠금 실패 - " + KeyfreecarConnector.FailReasonToPlainString(reason), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        unlock.setOnClickListener(v -> {
            if (connected == null) {
                Toast.makeText(getContext(), "연결을 완료하고 다시 시도 하세요.", Toast.LENGTH_SHORT).show();
            } else {
                connected.UnlockDoor(new KeyfreecarConnector.ResultCallback() {
                    @Override
                    public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "풀림 성공", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "풀림 전송", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(KeyfreecarConnector keyfreecarConnector, int reason) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "풀림 실패 - " + KeyfreecarConnector.FailReasonToPlainString(reason), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        changeName.setOnClickListener(v -> {
            if (connected == null) {
                Toast.makeText(getContext(), "연결을 완료하고 다시 시도 하세요.", Toast.LENGTH_SHORT).show();
            } else {
                EditText nameIn = new EditText(getContext());
                nameIn.setHint("영문 대/소문자 및 숫자를 이용하여 4자리까지 가능합니다.");

                new AlertDialog.Builder(getContext())
                        .setView(nameIn)
                        .setTitle("새로운 이름을 입력하세요")
                        .setPositiveButton("확인", (i, z) -> {
                            connected.ChangeName(nameIn.getText().toString(), new KeyfreecarConnector.ResultCallback() {
                                @Override
                                public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "이름 변경 성공", Toast.LENGTH_SHORT).show();
                                    });

                                }

                                @Override
                                public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "이름 변경 전송", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onFailure(KeyfreecarConnector keyfreecarConnector, int reason) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "이름 변경 실패 - " + KeyfreecarConnector.FailReasonToPlainString(reason), Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                            i.dismiss();
                        })
                        .setNegativeButton("취소", (i, z) -> {
                            i.dismiss();
                        }).create().show();


            }
        });

        changePassword.setOnClickListener(v -> {
            if (connected == null) {
                Toast.makeText(getContext(), "연결을 완료하고 다시 시도 하세요.", Toast.LENGTH_SHORT).show();
            } else {
                EditText pwIn = new EditText(getContext());
                pwIn.setHint("영문 대/소문자 및 숫자를 이용하여 16자리까지 가능합니다.");

                new AlertDialog.Builder(getContext())
                        .setView(pwIn)
                        .setTitle("새로운 비밀번호를 입력하세요")
                        .setPositiveButton("확인", (i, z) -> {
                            connected.ChangePassword(pwIn.getText().toString(), new KeyfreecarConnector.ResultCallback() {
                                @Override
                                public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "비밀번호 변경 성공", Toast.LENGTH_SHORT).show();
                                    });

                                }

                                @Override
                                public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "비밀번호 변경 전송", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onFailure(KeyfreecarConnector keyfreecarConnector, int reason) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "비밀번호 변경 실패 - " + KeyfreecarConnector.FailReasonToPlainString(reason), Toast.LENGTH_SHORT).show();
                                    });

                                }
                            });
                            i.dismiss();
                        })
                        .setNegativeButton("취소", (i, z) -> {
                            i.dismiss();
                        }).create().show();


            }
        });

        engineStart.setOnClickListener(view -> {
            if (connected == null) {
                Toast.makeText(getContext(), "연결을 완료하고 다시 시도 하세요.", Toast.LENGTH_SHORT).show();
            } else {
                EditText engineTime = new EditText(getContext());
                engineTime.setHint("분 단위로 5분 이상 30분 이하입니다.");

                new AlertDialog.Builder(getContext())
                        .setView(engineTime)
                        .setTitle("시동 유지시간을 입력하세요")
                        .setPositiveButton("확인", (i, z) -> {
                            int minute;
                            try {
                                minute = Integer.parseInt(engineTime.getText().toString());
                            } catch (NumberFormatException e) {
                                Toast.makeText(getContext(), "잘못된 시간입니다.", Toast.LENGTH_SHORT).show();
                                i.dismiss();
                            }
                            connected.EngineStart(Integer.parseInt(engineTime.getText().toString()), new KeyfreecarConnector.ResultCallback() {
                                @Override
                                public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "시동 시작 성공", Toast.LENGTH_SHORT).show();
                                    });

                                }

                                @Override
                                public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "시동 시작 전송", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onFailure(KeyfreecarConnector keyfreecarConnector, int reason) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(getContext(), "시동 시작 실패 - " + KeyfreecarConnector.FailReasonToPlainString(reason), Toast.LENGTH_SHORT).show();
                                    });

                                }
                            });
                            i.dismiss();
                        })
                        .setNegativeButton("취소", (i, z) -> {
                            i.dismiss();
                        }).create().show();


            }
        });

        engineStop.setOnClickListener(view -> {
            if (connected == null) {
                Toast.makeText(getContext(), "연결을 완료하고 다시 시도 하세요.", Toast.LENGTH_SHORT).show();
            } else {

                connected.EngineStop( new KeyfreecarConnector.ResultCallback() {
                    @Override
                    public void onSuccess(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "시동 정지 성공", Toast.LENGTH_SHORT).show();
                        });

                    }

                    @Override
                    public void onDispatch(KeyfreecarConnector keyfreecarConnector) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "시동 정지 전송", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(KeyfreecarConnector keyfreecarConnector, int reason) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "시동 정지 실패 - " + KeyfreecarConnector.FailReasonToPlainString(reason), Toast.LENGTH_SHORT).show();
                        });

                    }
                });


            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setNegativeButton("무조건 2번", (d, b) -> {
                    Log.d(Logtag, "무조건 2번");
                    ((AlertDialog) d).setOnDismissListener((d2 -> {
                        Log.d(Logtag, "무조건 2번째 실행");
                        d2.dismiss();
                    }));

                })
                .setPositiveButton("1번 이후 확인 1번 더", (d, b) -> {
                    Log.d(Logtag, "확인후 추가");
                    ((AlertDialog) d).setOnDismissListener((d2 -> {

                        if (((AlertDialog) d2).getWindow().isActive()) {
                            Log.d(Logtag, "확인후 추가 실행");
                            d2.dismiss();
                        }
                    }));
                })
                .setTitle("dismiss 테스트")
                .create();

        dialog.dismiss();
        dialog.dismiss();

    }
}