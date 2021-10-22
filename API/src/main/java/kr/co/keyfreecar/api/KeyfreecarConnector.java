// 보안을 위해 GATT Payload는 다 '?' 로 대체됨

package kr.co.keyfreecar.api;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;

import kr.co.keyfreecar.util.Consumer;

public class KeyfreecarConnector {

    //public BluetoothAdapter adapter;
    /**
     * 이 키프리카 기기의 BluetoothDevice 객체입니다.
     */

    public BluetoothDevice device;

    private final BluetoothGatt gatt;
    private final String Logtag = KeyfreecarConnector.class.getSimpleName();

    private Consumer<BluetoothGatt> gracefulDisconnect;

    private final ArrayMap<Integer, CommandCallback> callbackHashMap;

    private ArrayMap<Integer, onUpdate> onUpdate;


    private KeyfreecarDetail keyfreecarDetail;

    private static final int COMMAND_LOCK_DOOR = 0;
    private static final int COMMAND_UNLOCK_DOOR = 1;
    private static final int COMMAND_CHANGE_NAME = 2;
    private static final int COMMAND_CHANGE_PASSWORD = 3;
    private static final int COMMAND_ADD_SMART_USER = 4;
    private static final int COMMAND_DEL_SMART_USER = 5;
    private static final int COMMAND_SMART_LOCK_SETTING = 6;
    private static final int COMMAND_SMART_UNLOCK_SETTING = 7;
    private static final int COMMAND_SMART_ACTIVATION_SETTING = 8;
    private static final int COMMAND_ENGINE_SETTING = 9;
    private static final int COMMAND_VALET_SETTING = 10;
    private static final int COMMAND_SLEEP_SETTING = 11;
    private static final int COMMAND_DOOR_HANDLE_SETTING = 12;
    private static final int COMMAND_DOOR_HANDLE_ENABLE = 13;
    private static final int COMMAND_SMART_RANGE = 14;
    private static final int COMMAND_ENGINE_CONTROL = 15;



    public static final int COMMAND_FAIL_UNKNOWN = 0;
    public static final int COMMAND_FAIL_ALREADY_DISPATCHED = 1;
    public static final int COMMAND_FAIL_TIMEOUT = 2;
    public static final int COMMAND_FAIL_WRONG_FORMAT = 3;
    public static final int COMMAND_FAIL_NO_CHANGE = 4;


    int timeout = 3 * 1000;

    private boolean complete = false;

    KeyfreecarConnector(BluetoothDevice device, BluetoothGatt gatt, byte[][] setStatBuffer, Consumer<BluetoothGatt> gracefulDisconnect) {

        this.device = device;
        this.gatt = gatt;
        this.callbackHashMap = new ArrayMap<>();
        this.gracefulDisconnect = gracefulDisconnect;
        this.keyfreecarDetail = new KeyfreecarDetail(setStatBuffer, this::onValueSet);
        this.complete = true;
        onUpdate = new ArrayMap<>();


    }

    public KeyfreecarDetail getKeyfreecarDetail() {
        return keyfreecarDetail;
    }

    private CommandCallback toCommandCallback(ResultCallback callback, int CommandType, int timelimit) {
        CommandCallback cc = new CommandCallback() {

            @Override
            public void onSuccess() {
                progress = FINISHED;
                if (callbackHashMap.containsKey(CommandType) && callbackHashMap.get(CommandType).equals(this)) {
                    callbackHashMap.remove(CommandType);
                }


                callback.onSuccess(this.myKeyfreecar);
            }

            @Override
            public void onDispatch() {
                progress = DISPATCHED;
                this.timeout.cancel();
                callback.onDispatch(this.myKeyfreecar);
            }

            @Override
            public void onFailure(int reason) {
                progress = FAILED;
                if (callbackHashMap.containsKey(CommandType) && callbackHashMap.get(CommandType).equals(this)) {
                    callbackHashMap.remove(CommandType);
                }
                callback.onFailure(this.myKeyfreecar, reason);
            }
        };
        cc.myKeyfreecar = this;

        cc.timeout.schedule(new TimerTask() {
            @Override
            public void run() {

                cc.onFailure(COMMAND_FAIL_TIMEOUT);
            }
        }, timelimit);

        return cc;
    }

    private CommandCallback toCommandCallback(ResultCallback callback, int CommandType) {
        return toCommandCallback(callback, CommandType, timeout);
    }

    private CommandCallback toCommandCallback(String commandExtra, ResultCallback callback, int CommandType) {
        CommandCallback basic = toCommandCallback(callback, CommandType);
        basic.commandExtra = commandExtra;
        return basic;
    }


    /**
     * 기기에 대한 GATT Write/Read 제한시간을 설정합니다.
     *
     * @param timeout 설정 이후부터 호출되는 모든 명령에서 해당 값(milliseconds)이 제한 시간으로 적용되며,
     *                제한시간 초과시, {@link ResultCallback#onFailure(KeyfreecarConnector, int)}이 호출됩니다.
     *                기본값은 3000 (3초) 입니다.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * 연결을 종료합니다.
     * <p>
     * 연결 종료가 완료되면 {@link KeyfreecarBluetoothManager.ConnectionCallback#onDeviceDisable}이 호출됩니다.
     */


    public void Disconnect() {

        gracefulDisconnect.accept(gatt);
    }

    /**
     * 문 잠금 명령을 전송합니다.
     * 진행중인 잠금 명령이 있다면 새로운 명령을 무시합니다.
     * 무시한 경우 callback 또한 호출되지 않습니다.
     *
     * @param callback 잠금 명령의 전송 결과에 대한 callback
     * @return 명령의 전송 여부. 전송했다면 true, 무시했다면 false.
     */
    @SuppressLint("MissingPermission")
    public void LockDoor(ResultCallback callback) {
        if (callbackHashMap.containsKey(COMMAND_LOCK_DOOR)) {
            callback.onFailure(this, COMMAND_FAIL_ALREADY_DISPATCHED);
        } else {
            CommandCallback cc = toCommandCallback(callback, COMMAND_LOCK_DOOR);

            BluetoothGattCharacteristic command = gatt
                    .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                    .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));
            byte[] msg = {'?', '?'};
            command.setValue(msg);

            callbackHashMap.put(COMMAND_LOCK_DOOR, cc);
            gatt.writeCharacteristic(command);

        }

    }

    /**
     * 문 잠금해제 명령을 전송합니다.
     * 진행중인 잠금해제 명령이 있다면 새로운 명령을 무시합니다.
     * 무시한 경우 callback 또한 호출되지 않습니다.
     *
     * @param callback 잠금 명령의 전송 결과에 대한 callback
     * @return 명령의 전송 여부. 전송했다면 true, 무시했다면 false.
     */

    @SuppressLint("MissingPermission")
    public void UnlockDoor(ResultCallback callback) {
        if (callbackHashMap.containsKey(COMMAND_UNLOCK_DOOR)) {
            callback.onFailure(this, COMMAND_FAIL_ALREADY_DISPATCHED);
        } else {
            CommandCallback cc = toCommandCallback(callback, COMMAND_UNLOCK_DOOR);

            BluetoothGattCharacteristic command = gatt
                    .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                    .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));
            byte[] msg = {'?', '?'};
            command.setValue(msg);

            callbackHashMap.put(COMMAND_UNLOCK_DOOR, cc);
            gatt.writeCharacteristic(command);

        }
    }

    @SuppressLint("MissingPermission")
    public void ChangeName(String newName, ResultCallback callback) {
        if (callbackHashMap.containsKey(COMMAND_CHANGE_NAME)) {
            callback.onFailure(this, COMMAND_FAIL_ALREADY_DISPATCHED);
        } else if (newName.length() != 4) {
            callback.onFailure(this, COMMAND_FAIL_WRONG_FORMAT);
        } else {
            CommandCallback cc = toCommandCallback(newName, callback, COMMAND_CHANGE_NAME);

            Pattern regex = Pattern.compile("([a-zA-Z0-9]{4})");
            if (!regex.matcher(newName).matches()) {
                callback.onFailure(this, COMMAND_FAIL_WRONG_FORMAT);
            } else {
                byte[] byteCommand = {'?', '?'};
                byte[] byteDetail = newName.getBytes(StandardCharsets.US_ASCII);
                byte[] msg = new byte[byteCommand.length + byteDetail.length];

                System.arraycopy(byteCommand, 0, msg, 0, byteCommand.length);
                System.arraycopy(byteDetail, 0, msg, byteCommand.length, byteDetail.length);

                BluetoothGattCharacteristic command = gatt
                        .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                        .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));


                command.setValue(msg);


                callbackHashMap.put(COMMAND_CHANGE_NAME, cc);
                gatt.writeCharacteristic(command);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void ChangePassword(String newPassword, ResultCallback callback) {
        String trimmed = KeyfreecarGATT.TrimPassword(newPassword);
        if (callbackHashMap.containsKey(COMMAND_CHANGE_PASSWORD)) {
            callback.onFailure(this, COMMAND_FAIL_ALREADY_DISPATCHED);
        } else if (newPassword.contentEquals(keyfreecarDetail.getDevicePassword())) {
            callback.onFailure(this, COMMAND_FAIL_NO_CHANGE);
        } else {
            CommandCallback cc = toCommandCallback(trimmed, callback, COMMAND_CHANGE_PASSWORD);


            Pattern regex = Pattern.compile("([a-zA-Z0-9]{16})");
            Log.d(Logtag + ".ChangePassword", "trimmed = " + trimmed);
            if (!regex.matcher(trimmed).matches()) {
                callback.onFailure(this, COMMAND_FAIL_WRONG_FORMAT);
            }

            byte[] byteCommand = {'?', '?'};
            byte[] byteDetail = trimmed.getBytes(StandardCharsets.US_ASCII);
            byte[] msg = new byte[byteCommand.length + byteDetail.length];

            System.arraycopy(byteCommand, 0, msg, 0, byteCommand.length);
            System.arraycopy(byteDetail, 0, msg, byteCommand.length, byteDetail.length);

            BluetoothGattCharacteristic command = gatt
                    .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                    .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));

            command.setValue(msg);

            callbackHashMap.put(COMMAND_CHANGE_PASSWORD, cc);

            gatt.writeCharacteristic(command);
        }
    }

    @SuppressLint("MissingPermission")
    public void PrepareSmartRegistration(ResultCallback callback) {
        if (callbackHashMap.containsKey(COMMAND_ADD_SMART_USER)) {
            callback.onFailure(this, COMMAND_FAIL_ALREADY_DISPATCHED);
        } else if (keyfreecarDetail.isSmartRegisterReady()) {
            callback.onFailure(this, COMMAND_FAIL_NO_CHANGE);
        } else {
            CommandCallback cc = toCommandCallback(callback, COMMAND_ADD_SMART_USER, 15 * 1000);


            BluetoothGattCharacteristic command = gatt
                    .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                    .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));
            byte[] msg = {'?', '?'};
            command.setValue(msg);

            callbackHashMap.put(COMMAND_ADD_SMART_USER, cc);
            gatt.writeCharacteristic(command);

        }
    }

    @SuppressLint("MissingPermission")
    public void RemoveSmartRegistration(Integer targetIndex, ResultCallback callback) {

        if (callbackHashMap.containsKey(COMMAND_DEL_SMART_USER)) {
            callback.onFailure(this, COMMAND_FAIL_ALREADY_DISPATCHED);
        } else if (targetIndex > 4 || targetIndex < 0) {
            callback.onFailure(this, COMMAND_FAIL_WRONG_FORMAT);
        } else if(keyfreecarDetail.getSmartUsers()[targetIndex].contentEquals("00:00:00:00:00:00")){
            callback.onFailure(this, COMMAND_FAIL_NO_CHANGE);
        }else {
            CommandCallback cc = toCommandCallback(callback, COMMAND_DEL_SMART_USER, 15 * 1000);

            byte prefix = '?';
            byte offset = targetIndex.byteValue();


            BluetoothGattCharacteristic command = gatt
                    .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                    .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));
            byte[] msg = {'?', '?', ((Integer) (prefix + offset)).byteValue()};
            command.setValue(msg);

            callbackHashMap.put(COMMAND_DEL_SMART_USER, cc);
            gatt.writeCharacteristic(command);

        }
    }

    @SuppressLint("MissingPermission")
    public void SetSmartRange(Integer range, ResultCallback callback) {

        if (callbackHashMap.containsKey(COMMAND_SMART_RANGE)) {
            callback.onFailure(this, COMMAND_FAIL_ALREADY_DISPATCHED);
        } else if (range > KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_LONG || range < KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_OFF) {
            callback.onFailure(this, COMMAND_FAIL_WRONG_FORMAT);
        } else if (range == keyfreecarDetail.getSmartRange()) {
            callback.onFailure(this, COMMAND_FAIL_NO_CHANGE);
        } else {
            CommandCallback cc = toCommandCallback(callback, COMMAND_SMART_RANGE, 15 * 1000);

            cc.commandExtra = range.toString();

            byte prefix = '?';
            if (range == KeyfreecarDetail.Update.Value.STATE_SMART_RANGE_OFF) {
                prefix = '?';
            } else {
                byte offset = range.byteValue();
                prefix = ((Integer) (prefix + offset - 10)).byteValue();
            }

            BluetoothGattCharacteristic command = gatt
                    .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                    .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));
            byte[] msg = {'?', prefix};
            command.setValue(msg);

            callbackHashMap.put(COMMAND_SMART_RANGE, cc);
            gatt.writeCharacteristic(command);

        }
    }

    @SuppressLint("MissingPermission")
    public void SetDoorHandle(Integer password, boolean enable, ResultCallback callback) {

        if (callbackHashMap.containsKey(COMMAND_DOOR_HANDLE_SETTING)) {
            callback.onFailure(this, COMMAND_FAIL_ALREADY_DISPATCHED);
        } else if (password > 9999 || password < 1000 || String.valueOf(password).contains("0")) {
            callback.onFailure(this, COMMAND_FAIL_WRONG_FORMAT);
        } else if (password.equals(keyfreecarDetail.getDoorHandlePassword()) && enable == keyfreecarDetail.isDoorHandleEnable()){
            callback.onFailure(this,COMMAND_FAIL_NO_CHANGE);
        } else {
            CommandCallback cc = toCommandCallback(callback, COMMAND_DOOR_HANDLE_SETTING, 5 * 1000);

            cc.commandExtra = password.toString();


            BluetoothGattCharacteristic command = gatt
                    .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                    .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));
            byte[] msg = {'?', '?',
                    ((Integer) (password / 1000)).byteValue(),
                    ((Integer) ((password / 100) % 10)).byteValue(),
                    ((Integer) ((password / 10) % 10)).byteValue(),
                    ((Integer) (password % 10)).byteValue(),
                    enable ? (byte) 1 : (byte) 0};
            command.setValue(msg);

            callbackHashMap.put(COMMAND_DOOR_HANDLE_SETTING, cc);
            gatt.writeCharacteristic(command);

        }
    }

    @SuppressLint("MissingPermission")
    public void EngineStart(int timeInMinute, ResultCallback callback){
        if (callbackHashMap.containsKey(COMMAND_ENGINE_CONTROL)) {
            callback.onFailure(this, COMMAND_FAIL_ALREADY_DISPATCHED);
        } else if (timeInMinute > 30 || timeInMinute<5){
            callback.onFailure(this, COMMAND_FAIL_WRONG_FORMAT);
        } else if (keyfreecarDetail.getAccState() != KeyfreecarDetail.Update.Value.STATE_ACC_OFF){
            callback.onFailure(this,COMMAND_FAIL_NO_CHANGE);
        } else {
            CommandCallback cc = toCommandCallback(callback, COMMAND_ENGINE_CONTROL, 15 * 1000);

            cc.commandExtra = "Start";


            BluetoothGattCharacteristic command = gatt
                    .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                    .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));
            byte[] msg = {'?', '?',
                    (byte)timeInMinute};
            command.setValue(msg);

            callbackHashMap.put(COMMAND_ENGINE_CONTROL, cc);
            gatt.writeCharacteristic(command);

        }
    }

    @SuppressLint("MissingPermission")
    public void EngineStop(ResultCallback callback){
        if (callbackHashMap.containsKey(COMMAND_ENGINE_CONTROL)) {
            callback.onFailure(this, COMMAND_FAIL_ALREADY_DISPATCHED);
        } else if (keyfreecarDetail.getAccState() == KeyfreecarDetail.Update.Value.STATE_ACC_OFF){
            callback.onFailure(this,COMMAND_FAIL_NO_CHANGE);
        } else {
            CommandCallback cc = toCommandCallback(callback, COMMAND_ENGINE_CONTROL, 15 * 1000);


            cc.commandExtra = "Stop";

            BluetoothGattCharacteristic command = gatt
                    .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                    .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));
            byte[] msg = {'?', '?'};
            command.setValue(msg);

            callbackHashMap.put(COMMAND_ENGINE_CONTROL, cc);
            gatt.writeCharacteristic(command);

        }
    }


    void DispatchConfirmed(byte[] rawData) {

        if (rawData[0] == '?' && rawData[1] == '?') {


            CommandCallback callback = callbackHashMap.get(COMMAND_LOCK_DOOR);
            if (callback != null) {
                callback.onDispatch();
                callback.onSuccess();
            }


        } else if (rawData[0] == '?' && rawData[1] == '?') {
            CommandCallback callback = callbackHashMap.get(COMMAND_UNLOCK_DOOR);
            if (callback != null) {
                callback.onDispatch();
                callback.onSuccess();
            }

        }else if(rawData[0] == '?' && (rawData[1] == '?' || rawData[1] == '?')){
            CommandCallback callback = callbackHashMap.get(COMMAND_ENGINE_CONTROL);
            if (callback != null)
                callback.onDispatch();
            return;
        }

        else if (rawData[0] == '?') {
            if (rawData[1] == '?' && rawData[2] == '?') {

                CommandCallback callback = callbackHashMap.get(COMMAND_CHANGE_NAME);
                if (callback != null) {
                    String commandName = callback.commandExtra;
                    String responseName = new String(rawData, 3, 4, StandardCharsets.US_ASCII);

                    if (commandName.contentEquals(responseName)) {
                        callback.onDispatch();

                    }
                }


            } else if (rawData[1] == '?' && rawData[2] == '?') {
                CommandCallback callback = callbackHashMap.get(COMMAND_CHANGE_PASSWORD);
                if (callback != null) {
                    String commandPassword = callback.commandExtra;
                    String responsePassword = new String(rawData, 3, 16, StandardCharsets.US_ASCII);

                    Log.d(Logtag + ".ChangePassword", "callback commandPassword - " + commandPassword);
                    Log.d(Logtag + ".ChangePassword", "callback responsePassword - " + responsePassword);

                    if (commandPassword.contentEquals(responsePassword)) {
                        callback.onDispatch();
                    }
                }


            } else if (rawData[1] == '?' && rawData[2] == '?') {

                CommandCallback callback = callbackHashMap.get(COMMAND_ADD_SMART_USER);

                callback.onDispatch();

            } else if (rawData[1] == '?' && rawData[2] == '?') {

                CommandCallback callback = callbackHashMap.get(COMMAND_DEL_SMART_USER);

                callback.onDispatch();

            } else if (rawData[1] == '?' && rawData[2] == '?' && rawData[3] == '?') {
            /*
            if (strReadUUID.contentEquals(BluetoothLeService.CUSTOM_SMART_DATA_UUID) == true) {
                ExitWriteReadThread = true;
                strReadUUID = "";
            }*/
                return;
            } else if (rawData[1] == '?' && rawData[2] == '?') {
                //풀림 횟수
            /*
            if (strReadUUID.contentEquals(BluetoothLeService.CUSTOM_SMART_DATA_UUID) == true) {
                ExitWriteReadThread = true;
                strReadUUID = "";
            }*/
                return;
            } else if (rawData[1] == '?' && rawData[2] == '?') {
                // 스마트 짧음
                CommandCallback callback = callbackHashMap.get(COMMAND_SMART_RANGE);
                if (callback != null)
                    callback.onDispatch();
                return;
            } else if (rawData[1] == '?' && rawData[2] == '?') {
                // 스마트 보통
                CommandCallback callback = callbackHashMap.get(COMMAND_SMART_RANGE);
                if (callback != null)
                    callback.onDispatch();
                return;
            } else if (rawData[1] == '?' && rawData[2] == '?') {
                // 스마트 넓음
                CommandCallback callback = callbackHashMap.get(COMMAND_SMART_RANGE);
                if (callback != null)
                    callback.onDispatch();
                return;
            } else if (rawData[1] == '?' && rawData[2] == '?') {
                // 스마트 정지
                CommandCallback callback = callbackHashMap.get(COMMAND_SMART_RANGE);
                if (callback != null)
                    callback.onDispatch();
                return;
            } else if (rawData[1] == '?' && rawData[2] == '?') {
                // 도어핸들 버튼
                CommandCallback callback = callbackHashMap.get(COMMAND_DOOR_HANDLE_SETTING);
                if (callback != null)
                    callback.onDispatch();
                return;
            }

        } else if (rawData[1] == '?') {/*
            if (strReadUUID.contentEquals(BluetoothLeService.CUSTOM_ENGINE_DATA_UUID) == true) {
                ExitWriteReadThread = true;
                strReadUUID = "";
            }*/
            return;
        } else if (rawData[1] == '?' && rawData[2] == '?') {/*
            if (strReadUUID.contentEquals(BluetoothLeService.CUSTOM_SMART_DATA_UUID) == true) {
                ExitWriteReadThread = true;
                strReadUUID = "";
            }*/
            return;
        } else if (rawData[1] == '?' && rawData[2] == '?') {/*
            if (strReadUUID.contentEquals(BluetoothLeService.CUSTOM_SMART_DATA_UUID) == true) {
                ExitWriteReadThread = true;
                strReadUUID = "";
            }*/
            return;
        } else if (rawData[1] == '?' && rawData[2] == '?') {/*
            Utils.print_Log("AlexBLEStage8", "R, sb1 = " + sb1);
            if (strReadUUID.contentEquals(BluetoothLeService.CUSTOM_SMART_DATA_UUID) == true) {
                ExitWriteReadThread = true;
                strReadUUID = "";
            }*/
            return;
        } else if (rawData[1] == '?' && rawData[2] == '?') {/*
            Utils.print_Log("AlexBLEStage8", "R, sb1 = " + sb1);
            if (strReadUUID.contentEquals(BluetoothLeService.CUSTOM_SMART_DATA_UUID) == true) {
                ExitWriteReadThread = true;
                strReadUUID = "";
            }*/
            return;
        }
    }

    void UpdateStatus(byte[] rawData) {

        Log.d(Logtag + ".UpdateStatus", "Whole message - " + new String(rawData, StandardCharsets.US_ASCII));
        Log.d(Logtag + ".UpdateStatus", "Response tag - " + new String(rawData, 1, 2, StandardCharsets.UTF_8));
        keyfreecarDetail.parseData(rawData);

    }

    void ParseMessage(byte[] rawData) {
        if (rawData[0] == '?') {
            DispatchConfirmed(rawData);
        } else {
            if (complete)
                UpdateStatus(rawData);
        }
    }

    public static @Nullable
    String FailReasonToPlainString(int reason) {
        switch (reason) {
            case COMMAND_FAIL_UNKNOWN:
                return "UNKNOWN";

            case COMMAND_FAIL_ALREADY_DISPATCHED:
                return "ALREADY DISPATCHED";

            case COMMAND_FAIL_TIMEOUT:
                return "TIMEOUT";
            case COMMAND_FAIL_WRONG_FORMAT:
                return "WRONG FORMAT";
            case COMMAND_FAIL_NO_CHANGE:
                return "NO CHANGE";

            default:
                return null;

        }
    }

    /**
     * @param item     갱신을 기다릴 상태를 지정합니다 {@link KeyfreecarDetail.Update.Item}
     * @param onUpdate 기기의 상태값이 갱신되었을때 사용할 callback을 지정합니다. {@link KeyfreecarConnector.onUpdate}
     */

    public void setOnUpdate(int item, onUpdate onUpdate) {

        this.onUpdate.put(item, onUpdate);
    }


    void onValueSet(Integer item, Object value) {
        if (complete) {
            if (onUpdate.get(item) != null) {
                if (item == KeyfreecarDetail.Update.Item.UPDATE_INTEGER_ITEM_DOOR_HANDLE_PASSWORD && ((Integer) value) > 9999) {
                    Integer divide = ((Integer) value) / 10;
                    onUpdate.get(item).onUpdate(divide);
                } else {
                    onUpdate.get(item).onUpdate(value);
                }

            }

            switch (item) {
                case KeyfreecarDetail.Update
                        .Item.UPDATE_BOOLEAN_ITEM_IS_SMART_DEVICE_CONNECTED:
                    break;
                case KeyfreecarDetail.Update
                        .Item.UPDATE_BOOLEAN_ITEM_IS_SMART_REGISTER_READY:
                    CommandCallback isAddSmartReadyCallback = callbackHashMap.get(COMMAND_ADD_SMART_USER);
                    if (isAddSmartReadyCallback != null) {
                        boolean isAddSmartReady = (boolean) value;
                        if (isAddSmartReady)
                            isAddSmartReadyCallback.onSuccess();

                    }
                    break;
                case KeyfreecarDetail.Update
                        .Item.UPDATE_STRING_ARRAY_ITEM_SMART_USERS:
                    CommandCallback removeSmartUserCallback = callbackHashMap.get(COMMAND_DEL_SMART_USER);
                    if (removeSmartUserCallback != null) {
                        removeSmartUserCallback.onSuccess();
                    }
                    break;

                case KeyfreecarDetail.Update.Item.UPDATE_INTEGER_ITEM_SMART_RANGE:
                    CommandCallback smartRangeCallback = callbackHashMap.get(COMMAND_SMART_RANGE);
                    if (smartRangeCallback != null) {
                        int smartRange = (int) value;

                        if (smartRange == Integer.parseInt(smartRangeCallback.commandExtra))
                            smartRangeCallback.onSuccess();
                    }
                    break;
                case KeyfreecarDetail.Update.Item.UPDATE_STRING_ITEM_DEVICE_NAME:
                    CommandCallback deviceNameCallback = callbackHashMap.get(COMMAND_CHANGE_NAME);
                    if (deviceNameCallback != null) {
                        String name = (String) value;

                        if (name.contentEquals(deviceNameCallback.commandExtra))
                            deviceNameCallback.onSuccess();
                    }

                    break;
                case KeyfreecarDetail.Update.Item.UPDATE_STRING_ITEM_DEVICE_PASSWORD:
                    CommandCallback devicePasswordCallback = callbackHashMap.get(COMMAND_CHANGE_PASSWORD);
                    if (devicePasswordCallback != null) {
                        String password = (String) value;

                        if (password.contentEquals(devicePasswordCallback.commandExtra))
                            devicePasswordCallback.onSuccess();
                    }
                    break;

                case KeyfreecarDetail.Update.Item.UPDATE_INTEGER_ITEM_DOOR_HANDLE_PASSWORD:
                    CommandCallback doorHandlePasswordCallback = callbackHashMap.get(COMMAND_DOOR_HANDLE_SETTING);
                    if (doorHandlePasswordCallback != null) {
                        Integer password = (Integer) value;

                        if (password < 10000 && password.equals(Integer.valueOf(doorHandlePasswordCallback.commandExtra))) {
                            doorHandlePasswordCallback.onSuccess();
                        }
                    }
                    break;
                case KeyfreecarDetail.Update.Item.UPDATE_BOOLEAN_ITEM_DOOR_HANDLE_ENABLE:
                    CommandCallback doorHandleEnableCallback = callbackHashMap.get(COMMAND_DOOR_HANDLE_SETTING);

                    if (doorHandleEnableCallback != null) {
                        doorHandleEnableCallback.onSuccess();
                    }
                    break;
                case KeyfreecarDetail.Update.Item.UPDATE_INTEGER_ITEM_ACC_STATE:
                    CommandCallback engineControlCallback = callbackHashMap.get(COMMAND_ENGINE_CONTROL);

                    if (engineControlCallback != null) {
                        if(engineControlCallback.commandExtra.contentEquals("Start") && keyfreecarDetail.getAccState() != KeyfreecarDetail.Update.Value.STATE_ACC_OFF){
                            engineControlCallback.onSuccess();
                        } else if (engineControlCallback.commandExtra.contentEquals("Stop") && keyfreecarDetail.getAccState() == KeyfreecarDetail.Update.Value.STATE_ACC_OFF){
                            engineControlCallback.onSuccess();
                        }
                    }
                    break;
            }
        }
    }


    /**
     * 작동 명령에 대한 callback 을 지정하는 interface 입니다.
     */

    public interface ResultCallback {

        /**
         * 작동 명령이 성공적으로 수행되었을 때,
         *
         * @param myKeyfreecar 작동 명령을 수행한 KeyfreecarConnector
         */

        void onSuccess(KeyfreecarConnector myKeyfreecar);


        /**
         * 작동 명령의 전송이 성공적으로 수행되었을 때,
         *
         * @param myKeyfreecar 작동 명령을 전송한 KeyfreecarConnector
         */

        void onDispatch(KeyfreecarConnector myKeyfreecar);

        /**
         * 작동 명령을 수행하지 못했을 때,
         *
         * @param myKeyfreecar 작동 명령을 받은 KeyfreecarConnector
         */

        void onFailure(KeyfreecarConnector myKeyfreecar, int reason);

    }

    private abstract static class CommandCallback {
        Timer timeout = new Timer();
        KeyfreecarConnector myKeyfreecar;
        String commandExtra;
        final int FAILED = -1;
        final int PENDING = 0;
        final int DISPATCHED = 1;
        final int FINISHED = 2;
        int progress = PENDING;

        abstract void onSuccess();

        abstract void onDispatch();

        abstract void onFailure(int reason);

    }

    /**
     * 기기의 상태값이 갱신되었을 떄, 실행할 callback interface입니다. {@link KeyfreecarConnector#setOnUpdate(int, KeyfreecarConnector.onUpdate)}를 통해 현재 연결된 기기에 지정할 수 있습니다.
     */

    public interface onUpdate {
        /**
         * 업데이트 된 기기의 상태를 처리합니다.
         *
         * @param value 업데이트된 값입니다. {@link KeyfreecarDetail.Update.Value}을 참조하세요
         */
        void onUpdate(Object value);
    }
}
