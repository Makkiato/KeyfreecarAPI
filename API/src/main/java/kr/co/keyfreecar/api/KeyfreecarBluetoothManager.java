package kr.co.keyfreecar.api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static android.Manifest.permission.BLUETOOTH_CONNECT;

/**
 * KeyfreecarBluetoothManager class는 키프리카 기기와의 연결을 준비해줍니다.
 * {@link KeyfreecarBluetoothManager.Builder}를 이용해 객체화하여 사용합니다.
 */

public class KeyfreecarBluetoothManager extends BluetoothGattCallback {

    private static final String Logtag = KeyfreecarBluetoothManager.class.getSimpleName();

    Context ctx;

    ConnectionCallback connectionCallback;


    BluetoothDevice connected;
    BluetoothGatt gatt;

    boolean scanning = false;

    /**
     * 확인 되지 않은 이유
     */
    public static final int FAIL_REASON_UNKNOWN = 0;
    /**
     * 비밀번호가 맞지 않아, 연결이 안됩니다.
     */
    public static final int FAIL_REASON_WRONG_PASSWORD = 1;
    /**
     * 직접 연결 종료를 시켰습니다
     */
    public static final int FAIL_REASON_GRACEFULLY_DISCONNECTED = 2;
    /**
     * 이미 검색중입니다.
     */
    public static final int FAIL_REASON_ALREADY_SCANNING = 3;
    /**
     * 앱이 블루투스 및 위치 사용권한이 없습니다.
     */
    public static final int FAIL_REASON_APP_NOT_REGISTERED = 4;
    /**
     * BLE를 지원하지 않는 단말기 입니다.
     */
    public static final int FAIL_REASON_UNSUPPORTED = 5;
    /**
     * KFC 기기가 아닌 기기를 {@link KeyfreecarBluetoothManager#Connect(BluetoothDevice, String)}의 parameter로 사용했습니다.
     */
    public static final int FAIL_REASON_NOT_KFC = 6;

    /**
     * 지정되지 않은 필터를 {@link KeyfreecarBluetoothManager#StartSearch(BluetoothAdapter, String, int)}의 parameter로 사용했습니다.
     */
    public static final int FAIL_REASON_WRONG_FILTER = 7;

    /**
     * {@link KeyfreecarBluetoothManager#StartSearch(BluetoothAdapter, String, int)}에서 filter를 기기의 이름에 적용시키고자 할때 사용합니다.
     */
    public static final int SCAN_FILTER_FOR_NAME = 10;
    /**
     * {@link KeyfreecarBluetoothManager#StartSearch(BluetoothAdapter, String, int)}에서 filter를 기기의 Bluetooth Address에 적용시키고자 할때 사용합니다.
     */
    public static final int SCAN_FILTER_FOR_ADDRESS = 11;

    private KeyfreecarBluetoothManager(Builder builder) {
        this.ctx = builder.ctx;
        this.connectionCallback = builder.ConnCallback;


    }

    public boolean isScanning(){
        return scanning;
    }

    /**
     * 지정한 {@link BluetoothAdapter}를 이용해 기기 탐색을 시작합니다.
     * {@link KeyfreecarBluetoothManager.Builder}를 통해 선언된 {@link ConnectionCallback}을 통해 중간 진행상황을 알리며,
     * 선언되지 않은 경우 로그만 출력합니다.
     * 지정한 기기 이름 혹은 Bluetooth Address를 이용하여 필터를 적용시킵니다.
     *
     * @param btAdapter     사용할 BluetoothAdapter 객체
     * @param NameOrAddress 검색시 적용시킬 필터. 검색된 디바이스의 Name혹은 Address가 완전히 일치해야함.
     * @param target        필터를 적용시킬 항목을 선택. {@link KeyfreecarBluetoothManager#SCAN_FILTER_FOR_NAME} 또는 {@link KeyfreecarBluetoothManager#SCAN_FILTER_FOR_ADDRESS}
     */
    @SuppressLint("MissingPermission")
    public void StartSearch(@NonNull BluetoothAdapter btAdapter, @NonNull String NameOrAddress, int target) {
        ArrayList<ScanFilter> filters = new ArrayList<>();
        switch (target) {
            case SCAN_FILTER_FOR_ADDRESS:
                ScanFilter scanFilterAddr = new ScanFilter.Builder()
                        .setDeviceAddress(NameOrAddress)
                        .build();
                filters.add(scanFilterAddr);
                StartSearch(btAdapter, filters);
                break;
            case SCAN_FILTER_FOR_NAME:
                ScanFilter scanFilterName = new ScanFilter.Builder()
                        .setDeviceName(NameOrAddress)
                        .build();
                filters.add(scanFilterName);
                StartSearch(btAdapter, filters);
                break;
            default:
                connectionCallback.onScanFinish(FAIL_REASON_WRONG_FILTER);
                break;

        }
    }

    /**
     * 지정한 {@link BluetoothAdapter}를 이용해 기기 탐색을 시작합니다.
     * {@link KeyfreecarBluetoothManager.Builder}를 통해 선언된 {@link ConnectionCallback}을 통해 중간 진행상황을 알리며,
     * 선언되지 않은 경우 로그만 출력합니다.
     * 지정한 기기 이름 혹은 Bluetooth Address를 이용하여 필터를 적용시킵니다.
     *
     * @param btAdapter    사용할 BluetoothAdapter 객체
     * @param deviceAdress 검색시 적용시킬 필터. 검색된 디바이스의 Address가 완전히 일치해야함.
     * @param deviceName   검색시 적용시킬 필터. 검색된 디바이스의 이름이 완전히 일치해야함.
     */
    @SuppressLint("MissingPermission")
    public void StartSearch(@NonNull BluetoothAdapter btAdapter, @NonNull String deviceAdress, @NonNull String deviceName) {
        ArrayList<ScanFilter> filters = new ArrayList<>();


        ScanFilter scanFilterAddr = new ScanFilter.Builder()
                .setDeviceAddress(deviceAdress)
                .setDeviceName(deviceName)
                .build();
        filters.add(scanFilterAddr);
        StartSearch(btAdapter, filters);

    }

    /**
     * 지정한 {@link BluetoothAdapter}를 이용해 기기 탐색을 시작합니다.
     * {@link KeyfreecarBluetoothManager.Builder}를 통해 선언된 {@link ConnectionCallback}을 통해 중간 진행상황을 알리며,
     * 선언되지 않은 경우 로그만 출력합니다.
     * 지정한 기기 이름 혹은 Bluetooth Address를 이용하여 필터를 적용시킵니다.
     * <p>
     * 필터를 지정하지 않았기 때문에 검색 속도가 제한되고 백그라운드 상태에서는 검색이 중지 되며, 다시 포그라운드로 돌아온 뒤에 재개합니다.
     * 또한 {@link ConnectionCallback#onDeviceLost(BluetoothDevice, KeyfreecarBluetoothManager)}를 수신할 수 없습니다.
     *
     * @param btAdapter 사용할 BluetoothAdapter 객체
     */

    @SuppressLint("MissingPermission")
    public void StartSearch(@NonNull BluetoothAdapter btAdapter) {
        StartSearch(btAdapter, null);
    }

    @SuppressLint("MissingPermission")
    private void StartSearch(@NonNull BluetoothAdapter btAdapter, List<ScanFilter> filters) {
        final KeyfreecarBluetoothManager self = this;
        BluetoothLeScanner scanner = btAdapter.getBluetoothLeScanner();


        ScanCallback scanCallback = new ScanCallback() {
            ArrayMap<String, BluetoothDevice> allFound;

            @Override
            public void onScanResult(int callbackType, ScanResult result) {


                switch (callbackType) {
                    case ScanSettings.CALLBACK_TYPE_MATCH_LOST:
                        // Log.d(Logtag, "CALLBACK_TYPE_MATCH_LOST");
                        connectionCallback.onDeviceLost(allFound.remove(result.getDevice().getAddress()), self);
                        break;

                    default:
                        //Log.d(Logtag, "CALLBACK_TYPE_FIRST_MATCH | CALLBACK_TYPE_ALL_MATCHES");
                        if (allFound == null) {
                            allFound = new ArrayMap<>();
                        }


                        if (!allFound.containsKey(result.getDevice().getAddress())) {
                            allFound.put(result.getDevice().getAddress(), result.getDevice());
                            connectionCallback.onDeviceFound(result.getDevice(), self);
                        }

                        break;

                }

            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);

                switch (errorCode) {
                    case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                        connectionCallback.onScanFinish(FAIL_REASON_ALREADY_SCANNING);
                        break;
                    case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        scanning = false;
                        connectionCallback.onScanFinish(FAIL_REASON_APP_NOT_REGISTERED);
                        break;
                    case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                        scanning = false;
                        connectionCallback.onScanFinish(FAIL_REASON_UNSUPPORTED);
                        break;
                    case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                        scanning = false;
                        connectionCallback.onScanFinish(FAIL_REASON_UNKNOWN);
                        break;

                }
            }


            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.d(Logtag, "BastchResult size - " + results.size());
                for (ScanResult result : results) {
                    onScanResult(ScanSettings.CALLBACK_TYPE_FIRST_MATCH, result);
                }
            }
        };

        scanning = true;

        if (filters == null) {
            scanner.startScan(scanCallback);
        } else {

            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH | ScanSettings.CALLBACK_TYPE_MATCH_LOST)
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build();


            scanner.startScan(filters, scanSettings, scanCallback);
        }


        connectionCallback.onScanStart(scanCallback);

    }

    /**
     * 기본 {@link BluetoothAdapter}({@link BluetoothManager#getAdapter()})를 이용해 기기 탐색을 시작합니다.
     * {@link KeyfreecarBluetoothManager.Builder}를 통해 선언된 각각의 Callback을 통해 중간 진행상황을 알리며,
     * 선언되지 않은 경우 로그만 출력합니다.
     * <p>
     * 필터를 지정하지 않았기 때문에 백그라운드 상태에서는 검색이 중지 되며, 다시 포그라운드로 돌아온 뒤에 재개합니다.
     **/
    @SuppressLint("MissingPermission")
    public void StartSearch() {
        BluetoothManager bm = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter defaultAdapter = bm.getAdapter();

        StartSearch(defaultAdapter);

    }

    /**
     * 해당 BluetoothAdapter로 진행된 기기 탐색을 중단하고 {@link ConnectionCallback#onScanFinish(int)}를 호출합니다.
     * {@link KeyfreecarBluetoothManager.Builder}를 통해 선언된 Callback을 통해 취즉한 ScanCallback을 함께 명시해야합니다.
     *
     * @param adapter      기기 탐색을 진행하는 BluetoothAdapter
     * @param scanCallback 탐색중인 ScanCallback
     **/

    @SuppressLint("MissingPermission")
    public void StopSearch(@NonNull BluetoothAdapter adapter, @NonNull ScanCallback scanCallback) {
        adapter.getBluetoothLeScanner().stopScan(scanCallback);
        scanning = false;
        connectionCallback.onScanFinish(FAIL_REASON_GRACEFULLY_DISCONNECTED);
    }

    /**
     * 해당 BluetoothDevice가 키프리카의 기기인지 확인합니다.
     * 스마트도어를 입출력을 하는 BluetoothDevice인지 확인하려면 {@link KeyfreecarBluetoothManager#isSmartDevice(BluetoothDevice)}를 사용하세요.
     * <p>
     * Check the BluetoothDevice is Keyfreecar device.
     * If you want to check Smartdoor device, use {@link KeyfreecarBluetoothManager#isSmartDevice(BluetoothDevice)} instead.
     *
     * @param device 일반 KFC 기기인지 확인할 BluetoothDevice
     * @return 확인한 결과
     */
    @SuppressLint("MissingPermission")
    public static boolean isKFCDevice(@NonNull BluetoothDevice device) {
        String name = device.getName();
        return name != null && name.startsWith("KFC");
    }

    /**
     * 해당 BluetoothDevice가 스마트도어 기기인지 확인합니다.
     * 일반 연결을 하는 BluetoothDevice인지 확인하려면 {@link KeyfreecarBluetoothManager#isKFCDevice(BluetoothDevice)}를 사용하세요.
     * <p>
     * Check the BluetoothDevice is Smartdoor device.
     * If you want to check keyfreecar device, use {@link KeyfreecarBluetoothManager#isKFCDevice(BluetoothDevice)} instead.
     *
     * @param device 스마트도어 기기인지 확인할 BluetoothDevice
     * @return 확인한 결과
     */
    @SuppressLint("MissingPermission")
    public static boolean isSmartDevice(@NonNull BluetoothDevice device) {
        String name = device.getName();
        return name != null && (name.startsWith("Smart") || name.startsWith("Brown"));
    }


    /**
     * 선택한 기기로 선택한 비밀번호를 이용하여 GATT 연결을 시작합니다.
     * 연결이 완료될 경우 {@link ConnectionCallback#onDeviceAvailable(KeyfreecarConnector)} 을 호출합니다.
     * 기기는 반드시 일반 연결을 하는 KFC 기기여야 하며, 아닌 경우 예외를 발생시킵니다.
     *
     * @param device   GATT 연결할 BluetoothDevice
     * @param password 이용할 비밀번호이며, 16자 까지 사용합니다. 길이를 초과하는 부분은 잘라냅니다.
     * @throws IllegalArgumentException KFC 기기가 아닌경우 발생합니다.
     */

    @SuppressLint("MissingPermission")
    public void Connect(@NonNull BluetoothDevice device, @NonNull String password) {
        if (isKFCDevice(device)) {

            KeyfreecarGATT kfcGATT = new KeyfreecarGATT(device, password, new KeyfreecarGATT.Callback() {

                @Override
                public void onAvailable(KeyfreecarConnector connector) {
                    connected = device;

                    connectionCallback.onDeviceAvailable(connector);
                }

                @Override
                public void onUnavailable(KeyfreecarConnector connector, int reason) {
                    if (connected == device) {
                        connected = null;
                    }
                    connectionCallback.onDeviceDisable(connector, reason);

                }
            });


            device.connectGatt(ctx, false, kfcGATT, BluetoothDevice.TRANSPORT_LE);


        } else {
            connectionCallback.onDeviceDisable(null, FAIL_REASON_NOT_KFC);
        }
    }

    /**
     * @param reason ConncetionCallback.onDeviceDisable() 을 통해 전달받은 연결 실패사유 reason을 읽을 수 있는 문자열로 변환합니다.
     * @return 변환된 문자열 (FAIL_REASON_UNKNOWN -> "UNKNOWN", FAIL_REASON_WRONG_PASSWORD -> "WRONG PASSWORD")
     */

    public static @Nullable
    String FailReasonToPlainString(int reason) {
        switch (reason) {
            case FAIL_REASON_UNKNOWN:
                return "UNKNOWN";

            case FAIL_REASON_WRONG_PASSWORD:
                return "WRONG PASSWORD";

            case FAIL_REASON_GRACEFULLY_DISCONNECTED:
                return "GRACEFULLY DISCONNECTED";

            default:
                return null;

        }
    }

    /**
     * KeyfreecarBluetoothManager의 객체를 생성하기 위해 사용합니다.
     */

    public static class Builder {

        Context ctx;

        ConnectionCallback ConnCallback = new ConnectionCallback() {
            @Override
            public void onScanStart(ScanCallback scanCallback) {
                Log.i(Logtag, "onDiscoveryStart");
            }

            @Override
            public void onScanFinish(int reason) {
                Log.i(Logtag, "onDiscoveryFinish");
            }

            @Override
            public void onDeviceFound(BluetoothDevice device, KeyfreecarBluetoothManager manager) {
                Log.i(Logtag, "onDeviceFound");
            }

            @Override
            public void onDeviceLost(BluetoothDevice device, KeyfreecarBluetoothManager manager) {
                Log.i(Logtag, "onDeviceLost");
            }

            @Override
            public void onDeviceAvailable(KeyfreecarConnector myKeyfreecar) {
                Log.i(Logtag, "onDeviceAvailable");
            }

            @Override
            public void onDeviceDisable(KeyfreecarConnector myKeyfreecar, int reason) {
                Log.i(Logtag, "onDeviceDisable - " + reason);
            }

        };

        /**
         * KeyfreecarBluetoothManager의 객체를 생성하기 위해 사용합니다.
         *
         * @param ctx Application 혹은 Activity Context 입니다. 연결 과정에서 Android 시스템과 BroadcastReceiver를 통해 Bluetooth 연결 상황을 수신하는데 사용합니다.
         *            해당 BroadcastReceiver는 Bluetooth discovery가 종료되었을때, 스스로 해제됩니다.
         *            KeyfreecarBluetoothManager.Search()가 수행되는 동안(호출 이후 ~ ConnectionCallback.onDiscoveryFinish() callback 호출 이전) 이 Context는 유지되어야 합니다.
         *            Context 소멸로 인해 영향을 받지 않도록 Activity context의 경우 Activity life cycle을 관리하여야 합니다.
         */


        public Builder(@NonNull Context ctx) {
            this.ctx = ctx;
        }

        /**
         * 지정한 Builder 설정에 따라 KeyfreecarBluetoothManager를 생성합니다.
         * 만약 필요한 블루투스 통신 권한을 획득하지 못했다면, null을 반환합니다.
         * {@link androidx.core.app.ActivityCompat#requestPermissions(Activity, String[], int)}을 확인하세요
         *
         * @return KeyfreecarBluetoothManager 객체. 이후 Search(), Connect()등을 이용해 실제 연결을 완료합니다.
         */

        public @Nullable
        KeyfreecarBluetoothManager build() {
            if (
                    ctx.checkSelfPermission(BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                            ctx.checkSelfPermission(BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                            ctx.checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                            (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ctx.checkSelfPermission(BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
            ) {
                return new KeyfreecarBluetoothManager(this);

            } else {
                this.ConnCallback.onDeviceDisable(null, FAIL_REASON_APP_NOT_REGISTERED);
                return null;
            }

        }

        /**
         * 연결중 발생할 주요 이벤트들에 대한 Callback을 설정합니다
         *
         * @param callback callback interface입니다.
         * @return 계속해서 Builder를 설정하거나 build()하기 위한 현재 Builder 객체입니다.
         */

        public Builder setConnectionCallback(@NonNull ConnectionCallback callback) {
            ConnCallback = callback;
            return this;
        }
    }

    /**
     * KeyfreecarBluetoothManager.Builder.setConnectionCallback 에서 입력해야하는 callback interface입니다.
     */

    @Keep
    public interface ConnectionCallback {
        /**
         * Bluetooth 장치 검색이 시작되었을때, 호출됩니다.
         *
         * @param scanCallback 장치 검색 진행상황을 처리하는 {@link ScanCallback} 입니다.
         *                     해당 scanCallback을 {@link KeyfreecarBluetoothManager#StopSearch(BluetoothAdapter, ScanCallback)} 를 이용해 장치 검색을 중단할 수 있습니다.
         */
        void onScanStart(ScanCallback scanCallback);

        /**
         * Bluetooth 장치 검색이 종료되었을때(직접 {@link KeyfreecarBluetoothManager#StopSearch(BluetoothAdapter, ScanCallback)}를 호출하여 중단된 경우도 포함합니다.), 호출됩니다.
         *
         * @param reason 장치 검색이 종료된 이유를 설명합니다.
         *               {@link KeyfreecarBluetoothManager#FAIL_REASON_UNKNOWN}등 KeyfreecarBluetoothManager.FAIL_REASON_XXX 를 찹조하세요.
         */
        void onScanFinish(int reason);

        /**
         * Bluetooth 장치 검색 도중 특정 기기가 새로 검색되었을때 호출됩니다.
         *
         * @param device  검색된 기기
         * @param manager 검색을 수행하던 {@link KeyfreecarBluetoothManager} 객체
         */

        void onDeviceFound(BluetoothDevice device, KeyfreecarBluetoothManager manager);

        /**
         * Bluetooth 장치 검색 도중 검색 되었던 기기가, 범위를 벗어나서 더 이상 접근 불가하다고 판단되면 호출됩니다.
         *
         * @param device  범위를 이탈한 기기
         * @param manager 검색을 수행하던 {@link KeyfreecarBluetoothManager} 객체
         */

        void onDeviceLost(BluetoothDevice device, KeyfreecarBluetoothManager manager);

        /**
         * {@link KeyfreecarBluetoothManager#Connect}로 GATT 연결이 완료되었을때 호출됩니다.
         *
         * @param myKeyfreecar 연결이 완료 되어, GATT 제어를 할 수 있는 {@link KeyfreecarConnector} 객체.
         */


        void onDeviceAvailable(KeyfreecarConnector myKeyfreecar);

        /**
         * {@link KeyfreecarBluetoothManager#Connect}로 GATT 연결이 해제되었을때 호출됩니다.
         *
         * @param myKeyfreecar 기존에 연결이 완료된 상태에서 끊겼다면 연결이 해제된 {@link KeyfreecarConnector} 객체, 연결이 완료되지 못하고 끊겼다면 null
         * @param reason       연결이 해제/실패한 이유를 설명합니다.
         *                     {@link KeyfreecarBluetoothManager#FAIL_REASON_UNKNOWN}등 KeyfreecarBluetoothManager.FAIL_REASON_XXX 를 찹조하세요.
         */

        void onDeviceDisable(@Nullable KeyfreecarConnector myKeyfreecar, int reason);

    }
}
