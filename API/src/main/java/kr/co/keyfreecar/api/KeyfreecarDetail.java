package kr.co.keyfreecar.api;

import android.util.Log;

import java.nio.charset.StandardCharsets;

public class KeyfreecarDetail {

    private static final String Logtag = "KeyfreecarDetail";

    private byte[][] originalData;

    private boolean isSmartRegisterReady = false;


    private boolean isSmartDeviceConnected = false;

    private int accState = Update.Value.STATE_COMMON_UNKNOWN;


    private int smartRange = Update.Value.STATE_COMMON_UNKNOWN;


    private boolean isValetEnable = false;

    private int doorHandlePassword = 0;
    private boolean isDoorHandleEnable = false;

    private String deviceName = "";

    private String devicePassword = "";


    private String[] smartUsers = {"UNREGISTERED", "UNREGISTERED", "UNREGISTERED", "UNREGISTERED"};


    private int smartRepeat = Update.Value.STATE_COMMON_UNKNOWN;
    private int smartLockWaitTime = 0;
    private int smartLockSignalLength = 0;
    private int smartLockSignalInterval = 0;

    private UpdateCallback updateCallback ;


    //TODO AUX 무언가

    //TODO 시동 설정값


    KeyfreecarDetail(byte[][] originalData, UpdateCallback updateCallback) {
        this.originalData = originalData;
        this.updateCallback = updateCallback;
        parseData(originalData);
    }


    private void parseData(byte[][] allStat) {

        for (byte[] singleStat : allStat) {
            parseData(singleStat);
            String toLog = "";
            for(byte b : singleStat){
                toLog = toLog.concat(String.format("%02X ",b));
            }
            Log.d(Logtag,toLog.trim());
        }
    }

    void parseData(byte[] singleStat) {
        allCommon(singleStat);
        switch (singleStat[0]) {
            case 0:
                exceptThree(singleStat);
                parseZero(singleStat);
                break;
            case 1:
                exceptThree(singleStat);
                parseFirst(singleStat);
                break;
            case 2:
                exceptThree(singleStat);
                parseSecond(singleStat);
                break;
            case 3:
                parseThird(singleStat);
                break;
            case 4:
                exceptThree(singleStat);
                parseFourth(singleStat);
                break;
            case 5:
                exceptThree(singleStat);
                parseFifth(singleStat);
                break;
            case 6:
                exceptThree(singleStat);
                parseSixth(singleStat);
                break;
            case 7:
                exceptThree(singleStat);
                parseSeventh(singleStat);
                break;
            case 8:
                exceptThree(singleStat);
                parseEighth(singleStat);
                break;
            case 9:
                exceptThree(singleStat);
                parseNinth(singleStat);
                break;
            case 10:
                exceptThree(singleStat);
                parseTenth(singleStat);
                break;
            case 11:
                exceptThree(singleStat);
                parseEleventh(singleStat);
                break;


        }
    }

    private void allCommon(byte[] anyStat) {
        boolean before1 = isSmartRegisterReady;
        boolean before2 = isSmartDeviceConnected;
        Log.d(Logtag,"isSmartRR, isSmartDC, anyStat[1] - " + before1+ " , " + before2 + " , "+anyStat[1]);
        if (anyStat[1] == 0) {

            isSmartRegisterReady = false;

        } else {
            isSmartRegisterReady = true;
        }

        if(before1 ^ isSmartRegisterReady){
            Log.d(Logtag,"isSmartRegisterReady update callback");
            updateCallback.onUpdate(Update.Item.UPDATE_BOOLEAN_ITEM_IS_SMART_REGISTER_READY,isSmartRegisterReady);
        }

        if (anyStat[2] == '0') {
            isSmartDeviceConnected = false;
        } else {
            isSmartDeviceConnected = true;
        }
        if(before2 ^ isSmartDeviceConnected){
            Log.d(Logtag,"isSmartDeviceConnected update callback");
            updateCallback.onUpdate(Update.Item.UPDATE_BOOLEAN_ITEM_IS_SMART_DEVICE_CONNECTED,isSmartDeviceConnected);
        }

    }

    private void exceptThree(byte[] notThree) {
        int beforeRange = smartRange;
        int beforeAcc = accState;

        switch (notThree[4]) {
            case 0:
                accState = Update.Value.STATE_ACC_OFF;
                break;
            case 1:
                accState =Update.Value.STATE_ACC_ON;
                break;
            case 2:
                accState = Update.Value.STATE_ACC_START;
                break;
        }

        switch (notThree[5]) {
            case 0:
                smartRange = Update.Value.STATE_SMART_RANGE_OFF;

                break;
            case 1:
                smartRange = Update.Value.STATE_SMART_RANGE_SHORT;
                break;
            case 2:
                smartRange = Update.Value.STATE_SMART_RANGE_MEDIUM;
                break;
            case 3:
                smartRange = Update.Value.STATE_SMART_RANGE_LONG;
                break;
        }
        if(beforeAcc != accState){
            updateCallback.onUpdate(Update.Item.UPDATE_INTEGER_ITEM_ACC_STATE,accState);
        }

        if(beforeRange != smartRange){
            updateCallback.onUpdate(Update.Item.UPDATE_INTEGER_ITEM_SMART_RANGE,smartRange);
        }
    }

    private void parseZero(byte[] singleStat) {
        //NOTHING
    }

    private void parseFirst(byte[] singleStat) {
        if (singleStat[6] == 0) {
            isValetEnable = false;
        } else {
            isValetEnable = true;
        }

        int incomePassword = 0;

        //TODO 도어핸들 이거 그대로 읽어오면 되는건가? -> ㅇㅇ
        for (int i = 0; i < 4; i++) {
            incomePassword *= 10;
            incomePassword += singleStat[15 + i];
        }

        boolean beforeHandle = isDoorHandleEnable;

        if (singleStat[19] == 0) {
            isDoorHandleEnable = false;
        } else {
            isDoorHandleEnable = true;
        }

        boolean enableChanged = beforeHandle^isDoorHandleEnable;

        if(enableChanged){

            updateCallback.onUpdate(Update.Item.UPDATE_BOOLEAN_ITEM_DOOR_HANDLE_ENABLE,isDoorHandleEnable);

        }

        if(incomePassword != doorHandlePassword){
            doorHandlePassword = incomePassword;
            if(enableChanged){
                 // enable을 같이 바꿔을 때는 onSuccess를 enable에서 보내고 여기 값을 수정해서 password에서는 보내지 않도로 조정
                updateCallback.onUpdate(Update.Item.UPDATE_INTEGER_ITEM_DOOR_HANDLE_PASSWORD,doorHandlePassword*10);
            } else {
                updateCallback.onUpdate(Update.Item.UPDATE_INTEGER_ITEM_DOOR_HANDLE_PASSWORD,doorHandlePassword);
            }

        }




    }

    private void parseSecond(byte[] singleStat) {
        String before = deviceName;
        deviceName = new String(singleStat, 6, 4, StandardCharsets.US_ASCII);
        Log.d(Logtag,String.format("%02X - %02X - %02X - %02X", singleStat[6],singleStat[7],singleStat[8],singleStat[9]));
        Log.d(Logtag,"second device name - "+deviceName);
        if(!deviceName.contentEquals(before)){
            updateCallback.onUpdate(Update.Item.UPDATE_STRING_ITEM_DEVICE_NAME,deviceName.concat(""));
        }

        if (singleStat[10] == '-') {
            smartUsers[0] = "UNREGISTERED";
        } else {
            String Concatenation = "";
            for (int i = 0; i < 6; i++) {
                Concatenation = Concatenation.concat(String.format("%02X:", singleStat[11 + i]));
            }
            String beforeUser = smartUsers[0];
            smartUsers[0] = Concatenation.substring(0, Concatenation.lastIndexOf(":"));
            if(!beforeUser.contentEquals(smartUsers[0])){
                updateCallback.onUpdate(Update.Item.UPDATE_STRING_ARRAY_ITEM_SMART_USERS,smartUsers.clone());
            }

        }
    }

    private void parseThird(byte[] singleStat) {
        String before = devicePassword;
        devicePassword = new String(singleStat, 4, 16, StandardCharsets.US_ASCII);

        if(!devicePassword.contentEquals(before)){
            updateCallback.onUpdate(Update.Item.UPDATE_STRING_ITEM_DEVICE_PASSWORD,devicePassword.concat(""));
        }
    }

    private void parseFourth(byte[] singleStat) {

        if (singleStat[10] == '-') {
            smartUsers[1] = "UNREGISTERED";
        } else {
            String Concatenation = "";
            for (int i = 0; i < 6; i++) {
                Concatenation = Concatenation.concat(String.format("%02X:", singleStat[11 + i]));
            }

            String beforeUser = smartUsers[1];
            smartUsers[1] = Concatenation.substring(0, Concatenation.lastIndexOf(":"));
            if(!beforeUser.contentEquals(smartUsers[1])){
                updateCallback.onUpdate(Update.Item.UPDATE_STRING_ARRAY_ITEM_SMART_USERS,smartUsers.clone());
            }

        }
    }

    private void parseFifth(byte[] singleStat) {


        if (singleStat[10] == '-') {
            smartUsers[2] = "UNREGISTERED";
        } else {

            String Concatenation = "";
            for (int i = 0; i < 6; i++) {
                Concatenation = Concatenation.concat(String.format("%02X:", singleStat[11 + i]));
            }


            String beforeUser = smartUsers[2];
            smartUsers[2] = Concatenation.substring(0, Concatenation.lastIndexOf(":"));
            if(!beforeUser.contentEquals(smartUsers[2])){
                updateCallback.onUpdate(Update.Item.UPDATE_STRING_ARRAY_ITEM_SMART_USERS,smartUsers.clone());
            }

        }
    }

    private void parseSixth(byte[] singleStat) {


        if (singleStat[10] == '-') {
            smartUsers[3] = "UNREGISTERED";
        } else {
            String Concatenation = "";
            for (int i = 0; i < 6; i++) {
                Concatenation = Concatenation.concat(String.format("%02X:", singleStat[11 + i]));
            }


            String beforeUser = smartUsers[3];
            smartUsers[3] = Concatenation.substring(0, Concatenation.lastIndexOf(":"));
            if(!beforeUser.contentEquals(smartUsers[3])){
                updateCallback.onUpdate(Update.Item.UPDATE_STRING_ARRAY_ITEM_SMART_USERS,smartUsers.clone());
            }

        }
    }

    private void parseSeventh(byte[] singleStat) {
    }

    private void parseEighth(byte[] singleStat) {
    }

    private void parseNinth(byte[] singleStat) {
    }

    private void parseTenth(byte[] singleStat) {
    }

    private void parseEleventh(byte[] singleStat) {
    }

    public boolean isDoorHandleEnable() {
        return isDoorHandleEnable;
    }

    public boolean isSmartDeviceConnected() {
        return isSmartDeviceConnected;
    }

    public boolean isSmartRegisterReady() {
        return isSmartRegisterReady;
    }

    public boolean isValetEnable() {
        return isValetEnable;
    }

    public int getAccState() {
        return accState;
    }

    public int getSmartLockSignalInterval() {
        return smartLockSignalInterval;
    }

    public int getSmartLockSignalLength() {
        return smartLockSignalLength;
    }

    public int getSmartLockWaitTime() {
        return smartLockWaitTime;
    }

    public int getSmartRange() {
        return smartRange;
    }

    public int getSmartRepeat() {
        return smartRepeat;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getDoorHandlePassword() {
        return doorHandlePassword;
    }

    public String getDevicePassword() {
        return devicePassword;
    }

    public String[] getSmartUsers() {
        return smartUsers;
    }

    interface UpdateCallback{
        void onUpdate(Integer item, Object value);
    }

    public static class Update  {
        public static class Item {

            public static final int UPDATE_BOOLEAN_ITEM_IS_SMART_REGISTER_READY = 100;
            public static final int UPDATE_BOOLEAN_ITEM_IS_SMART_DEVICE_CONNECTED = 200;
            public static final int UPDATE_INTEGER_ITEM_ACC_STATE = 300;
            public static final int UPDATE_INTEGER_ITEM_SMART_RANGE = 400;
            public static final int UPDATE_BOOLEAN_ITEM_VALET_ENABLE = 500;
            public static final int UPDATE_INTEGER_ITEM_DOOR_HANDLE_PASSWORD = 600;
            public static final int UPDATE_BOOLEAN_ITEM_DOOR_HANDLE_ENABLE = 700;
            public static final int UPDATE_STRING_ITEM_DEVICE_NAME = 800;
            public static final int UPDATE_STRING_ITEM_DEVICE_PASSWORD = 900;
            public static final int UPDATE_STRING_ARRAY_ITEM_SMART_USERS = 1000;
            public static final int UPDATE_INTEGER_ITEM_SMART_REPEAT = 1100;
            public static final int UPDATE_INTEGER_ITEM_SMART_LOCK_WAIT_TIME = 1200;
            public static final int UPDATE_INTEGER_ITEM_SIGNAL_LENGTH = 1300;
            public static final int UPDATE_INTEGER_ITEM_SIGNAL_INTERVAL = 1400;
        }

        public static class Value {

            public static final int STATE_COMMON_UNKNOWN = -1;

            public static final int STATE_ACC_OFF = 0;
            public static final int STATE_ACC_ON = 1;
            public static final int STATE_ACC_START = 2;

            public static final int STATE_SMART_RANGE_OFF = 10;
            public static final int STATE_SMART_RANGE_SHORT = 11;
            public static final int STATE_SMART_RANGE_MEDIUM = 12;
            public static final int STATE_SMART_RANGE_LONG = 13;

            public static final int STATE_SMART_REPEAT_ONCE = 20;
            public static final int STATE_SMART_REPEAT_TWICE = 21;
            public static final int STATE_SMART_REPEAT_THREE = 22;
        }


    }
}
