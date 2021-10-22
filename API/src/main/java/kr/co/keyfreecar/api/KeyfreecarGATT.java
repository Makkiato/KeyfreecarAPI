package kr.co.keyfreecar.api;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

class KeyfreecarGATT extends BluetoothGattCallback {

    private String Logtag = KeyfreecarGATT.class.getSimpleName();
    BluetoothDevice btDevice;
    String password;

    Callback callback;
    HandlerThread ht;
    KeyfreecarConnector connector;
    int wrongPasswordCounter = 0;
    int reconnectTrial = 0;
    HandlerThread pingThread;
    Handler pingRepeater;
    Runnable onGoing;

    private int setStatChecker = 0x0;
    private final int setStatMask = 0xFFE;

    private byte[][] setStatBuffer;


    final static String CUSTOM_SERVICES_UUID = "?";
    final static String CUSTOM_COMMAND_UUID = "?";
    final static String CUSTOM_PING_UUID = "?";
    final static String CUSTOM_NOTI_UUID = "?";
    final static String CUSTOM_NOTI_DSC_UUID = "?";
    final static String CUSTOM_STRING_UUID = "?";
    final static String CUSTOM_INFO_UUID = "?";
    final static String CUSTOM_REMOCON_OUT_UUID = "?";
    final static String CUSTOM_NAME_UUID = "?";
    final static String CUSTOM_PASS_UUID = "?";
    final static String CUSTOM_SMARTU1_UUID = "?";
    final static String CUSTOM_SMARTU2_UUID = "?";
    final static String CUSTOM_SMARTU3_UUID = "?";
    final static String CUSTOM_SMARTU4_UUID = "?";
    final static String CUSTOM_SMART_DATA_UUID = "?";
    final static String CUSTOM_ENGINE_DATA_UUID = "?";
    final static String CUSTOM_STATUS_DATA_UUID = "?";

    ArrayList<String> UUIDs = new ArrayList<>();
    ArrayList<String> names = new ArrayList<>();

    private boolean isReady = false;

    private void prepareList() {
        UUIDs.add(CUSTOM_SERVICES_UUID);
        UUIDs.add(CUSTOM_COMMAND_UUID);
        UUIDs.add(CUSTOM_PING_UUID);
        UUIDs.add(CUSTOM_NOTI_UUID);
        UUIDs.add(CUSTOM_NOTI_DSC_UUID);
        UUIDs.add(CUSTOM_STRING_UUID);
        UUIDs.add(CUSTOM_INFO_UUID);
        UUIDs.add(CUSTOM_REMOCON_OUT_UUID);
        UUIDs.add(CUSTOM_NAME_UUID);
        UUIDs.add(CUSTOM_PASS_UUID);
        UUIDs.add(CUSTOM_SMARTU1_UUID);
        UUIDs.add(CUSTOM_SMARTU2_UUID);
        UUIDs.add(CUSTOM_SMARTU3_UUID);
        UUIDs.add(CUSTOM_SMARTU4_UUID);
        UUIDs.add(CUSTOM_SMART_DATA_UUID);
        UUIDs.add(CUSTOM_ENGINE_DATA_UUID);
        UUIDs.add(CUSTOM_STATUS_DATA_UUID);

        names.add("custom service");
        names.add("custom command");
        names.add("custom ping");
        names.add("custom noti");
        names.add("custom noti dsc");
        names.add("custom string");
        names.add("custom remocon out");
        names.add("custom name");
        names.add("custom pass");
        names.add("custom smart1");
        names.add("custom smart2");
        names.add("custom smart3");
        names.add("custom smart4");
        names.add("custom smart data");
        names.add("custom engine data");
        names.add("custom status data");


    }


    protected KeyfreecarGATT(BluetoothDevice btDevice, String password, Callback callback) {
        this.btDevice = btDevice;

        this.password = TrimPassword(password);

        this.callback = callback;

        prepareList();

        ht = new HandlerThread("bleTest");
        ht.start();
        pingThread = new HandlerThread("pingThread");



    }
    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        // GATT Server connected
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            reconnectTrial = 0;
            ////Utils.print_Log("AlexBluetoothLeService","BluetoothProfile.STATE_CONNECTED");

            Log.d(Logtag + ".ConnectionState", "Connected - " + gatt.getDevice().getName());
            //mServiceDiscovered = false;
            //이게 필요할까?

            //broadcastConnectionUpdate(ACTION_GATT_CONNECTED);
            // 연결되면 바로 서비스를 검색 한다.

            gatt.discoverServices();
        }
        // GATT Server disconnected
        else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            ////Utils.print_Log("AlexBluetoothLeService","BluetoothProfile.STATE_DISCONNECTED");
            Log.d(Logtag + ".ConnectionState", "Disconnected");
            isReady = false;

            if (wrongPasswordCounter > 10) {
                gatt.close();
                callback.onUnavailable(connector, KeyfreecarBluetoothManager.FAIL_REASON_WRONG_PASSWORD);
            }

            else {
                reconnectTrial += 1;

                if(reconnectTrial > 3) {
                    callback.onUnavailable(connector, KeyfreecarBluetoothManager.FAIL_REASON_UNKNOWN);
                }
                else{
                    gatt.connect();
                }
            }



            //broadcastConnectionUpdate(ACTION_GATT_DISCONNECTED);
        }
        // GATT Server Connecting
        else if (newState == BluetoothProfile.STATE_CONNECTING) {
            Log.d(Logtag + ".ConnectionState", "Connecting");
            //Utils.print_Log("AlexBluetoothLeService","BluetoothProfile.STATE_CONNECTING");

            // broadcastConnectionUpdate(ACTION_GATT_CONNECTING);
        }
        // GATT Server disconnected
        else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
            Log.d(Logtag + ".ConnectionState", "Disconnecting");
            //Utils.print_Log("AlexBluetoothLeService","BluetoothProfile.STATE_DISCONNECTING");

            //broadcastConnectionUpdate(ACTION_GATT_DISCONNECTING);
        }
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(Logtag, "ServiceDiscovered = true");
            for (BluetoothGattService service : gatt.getServices()) {
                Log.d(Logtag, "UUID - " + service.getUuid().toString());
                DiscoverServiceAndCharacteristic(service, 1);

                if (service.getUuid().compareTo(UUID.fromString(CUSTOM_SERVICES_UUID)) == 0) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(service.getUuid());
                    Log.d(Logtag, "this is CustomService");
                    Log.d(Logtag, "UUID - " + service.getUuid().toString());
//                    Log.d(Logtag,"characteristic - " + characteristic.toString());


                    //READ_INFO
                    BluetoothGattCharacteristic info = service.getCharacteristic(UUID.fromString(CUSTOM_INFO_UUID));
                    gatt.readCharacteristic(info);


                }


            }


        } else {
            Log.d(Logtag, "SERVICE DISCOVERY UNSUCCESSFUL ...");

        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        Log.d(Logtag, "onDescriptorWrite");
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        String characteristicUUID = characteristic.getUuid().toString();

        if (status == BluetoothGatt.GATT_SUCCESS) {



            String append = "";

            for (int i = 0; i < characteristic.getValue().length; i++) {
                append = append.concat(characteristic.getValue()[i] < 10 ? String.valueOf(characteristic.getValue()[i])+" " :(char) characteristic.getValue()[i] + " ");
            }

            Log.d(Logtag, "Write SUCCESS - " + append.trim());
            if (connector != null) {
                connector.DispatchConfirmed(characteristic.getValue());
            }


            /*
            if (characteristicUUID.contentEquals(CUSTOM_COMMAND_UUID.toLowerCase())) {

                Handler h = new Handler(ht.getLooper());
                h.postDelayed(() -> {
                    byte[] msg = {'T', 'l', 5};
                    characteristic.setValue(msg);
                    Log.d(Logtag, "postDelayed");
                    gatt.writeCharacteristic(characteristic);


                }, 3000);
            }*/
        } else {
//                Utils.toast("ERROR MESSAGE WRITE", Toast.LENGTH_SHORT);
            Log.d(Logtag, "Write ERROR");

        }

    }

    boolean notiOneShot = false;

    @Override
    @SuppressLint("MissingPermission")
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(Logtag, ".onCharacteristicRead");
        if (status == BluetoothGatt.GATT_SUCCESS) {

            if (characteristic.getUuid().compareTo(UUID.fromString(CUSTOM_INFO_UUID)) == 0) {
                byte[] rawData = characteristic.getValue();

                Log.d(Logtag + ".onCharacteristicRead", UUIDtoString(characteristic.getUuid()) + " - " + TranslateRawdata(rawData));
                Log.d(Logtag + ".onCharacteristicRead", visibleRawData(rawData));

                //SEND_NOTI
                notiOneShot = true;
                BluetoothGattCharacteristic noti = characteristic.getService().getCharacteristic(UUID.fromString(CUSTOM_NOTI_UUID));
                byte[] val = {1};
                BluetoothGattDescriptor descNoti = noti.getDescriptor(UUID.fromString(CUSTOM_NOTI_DSC_UUID));
                descNoti.setValue(val);
                gatt.writeDescriptor(descNoti);
                gatt.setCharacteristicNotification(noti, true);

                //next send pass
            }
        }
    }


    @Override
    @SuppressLint("MissingPermission")
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] rawData = characteristic.getValue();
        Log.d(Logtag, ".onCharacteristicChanged");
        //Log.d(Logtag, "rawdata[0] - " + rawData[0] + ", UUID - " + UUIDtoString(characteristic.getUuid()));


        if (notiOneShot && characteristic.getUuid().compareTo(UUID.fromString(CUSTOM_NOTI_UUID)) == 0 ) {
            notiOneShot = false;

            //SEND PASS
            BluetoothGattCharacteristic command = characteristic.getService().getCharacteristic(UUID.fromString(CUSTOM_COMMAND_UUID));
            command.setValue(password.getBytes());
            wrongPasswordCounter = 0;
            setStatChecker = 0x0;
            setStatBuffer = new byte[12][20];
            gatt.writeCharacteristic(command);

/*
            Log.d(Logtag + ".onCharacteristicChanged", UUIDtoString(characteristic.getUuid()) + " - " + TranslateRawdata(rawData));
            Log.d(Logtag + ".onCharacteristicChanged", visibleRawData(rawData));
*/
        } else if (!isReady) {
            if (rawData[0] >= 0 && rawData[0] <= 11) {

                setStatChecker = setStatChecker | (1 << rawData[0]);
                Log.d(Logtag,String.format("statCheck progress - %X",setStatChecker));
                setStatBuffer[rawData[0]] = rawData;
                if((setStatChecker & setStatMask) == setStatMask){
                    isReady = true;

                    connector = new KeyfreecarConnector(btDevice, gatt,setStatBuffer,this::GracefullyDisconnect);
                    Log.d(Logtag,"now it will be enable");

                    pingThread.start();
                    pingRepeater = new Handler(pingThread.getLooper());
                    onGoing = new Runnable() {
                        @Override
                        public void run() {
                            SendPing(gatt);
                            pingRepeater.postDelayed(this,10*1000);
                        }
                    };

                    pingRepeater.postDelayed(onGoing,10*1000);
                    callback.onAvailable(connector);
                }


            } else {
                wrongPasswordCounter += 1;
                if (wrongPasswordCounter > 10) {
                    isReady = true;
                    gatt.disconnect();

                }

            }

        }
        Log.d(Logtag, "rawData[0] - " + rawData[0]);
        if (connector != null) {
            connector.ParseMessage(rawData);
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        Log.d(Logtag + ".onMTUChanged", "" + mtu);
        if (status == BluetoothGatt.GATT_SUCCESS) {

        }
    }

    private void DiscoverServiceAndCharacteristic(BluetoothGattService service, int depth) {

        String indent = "";
        for (int i = 0; i < depth; i++) {
            indent = indent.concat("\t");
        }

        Log.d(Logtag + "Discover", indent + "included Service");
        for (BluetoothGattService subService : service.getIncludedServices()) {
            Log.d(Logtag + "Discover", indent + " - " + UUIDtoString(subService.getUuid()) + " - " + subService.getUuid());

            DiscoverServiceAndCharacteristic(subService, depth + 1);
        }
        Log.d(Logtag + "Discover", indent + "included Characteristic");
        for (BluetoothGattCharacteristic subChar : service.getCharacteristics()) {
            Log.d(Logtag + "Discover", indent + " - " + UUIDtoString(subChar.getUuid()) + " - " + subChar.getUuid());


            DiscoverCharacteristicAndDescriptor(subChar, depth + 1);
        }
    }

    private void DiscoverCharacteristicAndDescriptor(BluetoothGattCharacteristic characteristic, int depth) {
        String indent = "";
        for (int i = 0; i < depth; i++) {
            indent = indent.concat("\t");
        }

        Log.d(Logtag + "Discover", indent + "included Descriptor");
        for (BluetoothGattDescriptor subDescriptor : characteristic.getDescriptors()) {
            Log.d(Logtag + "Discover", indent + " - " + UUIDtoString(subDescriptor.getUuid()) + " - " + subDescriptor.getUuid());


        }
    }
    @SuppressLint("MissingPermission")
    void GracefullyDisconnect(BluetoothGatt gatt){
        pingRepeater.removeCallbacks(onGoing);
        pingThread.quit();
        gatt.close();
        gatt.disconnect();


        callback.onUnavailable(connector,KeyfreecarBluetoothManager.FAIL_REASON_GRACEFULLY_DISCONNECTED);
    }

    private void SendPing(BluetoothGatt gatt) {
        byte[] data = new byte[20];
        //Logger.dataBLElog("SendPing " );
        Calendar c = Calendar.getInstance();

        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Log.d(Logtag,"ping time - " + sdfNow.format(c.getTimeInMillis()));
        Log.d(Logtag,"ping time byte - " + c.get(Calendar.DAY_OF_WEEK));
        Log.d(Logtag,"ping time byte - " + c.get(Calendar.HOUR_OF_DAY));
        Log.d(Logtag,"ping time byte - " + c.get(Calendar.MINUTE));
        Log.d(Logtag,"ping time byte - " + c.get(Calendar.SECOND));

        long currentMill = System.currentTimeMillis();
        data[0] = 'P';
        data[1] = 'I';
        data[2] = 1;
        /*
        if (MyApplication.strAppType.contentEquals(Constants.APP_TYPE_MANAGER)) {
            data[1] = 'I';
            data[2] = 1;                //  2021.04.10
        } else {
            data[1] = 'i';
            data[2] = 0;
        }
        */

        data[3] = (byte) c.get(Calendar.DAY_OF_WEEK);
        data[4] = (byte) c.get(Calendar.HOUR_OF_DAY);
        data[5] = (byte) c.get(Calendar.MINUTE);
        data[6] = (byte) c.get(Calendar.SECOND);


        data[7] = 0;


        data[8] = 0;


        BluetoothGattCharacteristic command = gatt
                .getService(UUID.fromString(KeyfreecarGATT.CUSTOM_SERVICES_UUID))
                .getCharacteristic(UUID.fromString(KeyfreecarGATT.CUSTOM_COMMAND_UUID));

        command.setValue(data);

        gatt.writeCharacteristic(command);

    }

    private String UUIDtoString(UUID uuid) {

        String uuidString = uuid.toString().toUpperCase();


        int targetIdx = UUIDs.indexOf(uuidString);
        if (targetIdx != -1) {
            return names.get(targetIdx);
        } else {
            return "unknown";
        }

    }

    public static int[] byteToUnsignedInt(byte[] bytes) {
        int[] data = new int[20];
        for (int i = 0; i < 20 && i < bytes.length; i++) {
            data[i] = 0x00 << 24 | bytes[i] & 0xff;
        }
        return data;
    }

    private static String visibleRawData(byte[] rawData) {
        String rawString = "";

        for (int i = 0; i < rawData.length; i++) {
            rawString = rawString.concat(String.format("%d", rawData[i])).concat(" ");
        }
        return rawString.trim();
    }

    private static String TranslateRawdata(byte[] rawData) {
        int[] data = byteToUnsignedInt(rawData);
        // data[0] BleProcessStage 0 ~ 11


        StringBuffer sb1 = new StringBuffer();
        for (int j = 0; j < data.length; j++) {
            sb1.append(data[j] + " ");

        }


        return sb1.toString();
    }

    static String TrimPassword(String password){
        if (password.length() > 16) {
            return password.substring(0, 15);
        } else if (password.length() < 16) {
            String realPassword = password.concat("");
            while (realPassword.length() < 16) {
                realPassword = realPassword.concat("0");
            }
            return realPassword;
        } else {
            return password;
        }
    }

    interface Callback {
        void onAvailable(KeyfreecarConnector connector);

        void onUnavailable(KeyfreecarConnector connector, int reason);


    }


}
