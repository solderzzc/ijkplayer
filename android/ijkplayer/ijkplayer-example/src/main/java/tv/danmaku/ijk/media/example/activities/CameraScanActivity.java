package tv.danmaku.ijk.media.example.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.company.NetSDK.CB_fSearchDevicesCB;
import com.company.NetSDK.DEVICE_NET_INFO_EX;
import com.company.NetSDK.INetSDK;
import com.dahuatech.netsdk.common.NetSDKLib;

import org.w3c.dom.Text;

import tv.danmaku.ijk.media.example.R;

public class CameraScanActivity extends ListActivity {
    private static final int REQUESTCODE_PERMISSION_STORAGE = 1234;
    public static final String CAMERAIPKEY = "tv.danmaku.ijk.media.example.activities.CameraScanActivity.VideoSettings";

    public static class CameraAdapter extends ArrayAdapter<CameraInfo> {
        private final Context context;

        private String currentSelectedIP = "";

        public String getCurrentSelectedIP() {
            return currentSelectedIP;
        }

        public void setCurrentSelectedIP(String currentSelectedIP) {
            this.currentSelectedIP = currentSelectedIP;
            this.notifyDataSetChanged();
        }

        public CameraAdapter(Context context) {
            super(context, R.layout.camera_list_item);
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            CameraInfo ci = getItem(position);

            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.camera_list_item, parent, false);
            TextView tv_ip = (TextView)rowView.findViewById(R.id.tv_ipaddress);
            TextView tv_sn = (TextView)rowView.findViewById(R.id.tv_sn);
            tv_ip.setText(ci.getIp());
            tv_sn.setText(ci.getSn());

            ImageView iv_selected = (ImageView)rowView.findViewById(R.id.iv_current);
            if (!TextUtils.isEmpty(currentSelectedIP) && ci.getIp().equals(currentSelectedIP)) {
                iv_selected.setImageResource(R.drawable.ic_true);
            }
            else {
                iv_selected.setImageResource(android.R.color.transparent);
            }
            return rowView;
        }
    }

    public static class CameraInfo {
        private String sn = null;
        private String ip = null;

        public CameraInfo(String serialNo, String ipAddr) {
            sn = serialNo;
            ip = ipAddr;
        }

        public String getSn() {
            return sn;
        }

        public void setSn(String sn) {
            this.sn = sn;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        @Override
        public String toString() {
            return "ip: " + ip;
        }
    }

    private CameraAdapter cameraInfoAdapter = null;

    private CB_fSearchDevicesCB callback = new  CB_fSearchDevicesCB(){

        @Override
        public void invoke(DEVICE_NET_INFO_EX device_net_info_ex) {
            String temp = "IP : "+ new String(device_net_info_ex.szIP).trim() +
                    " SN : " + new String(device_net_info_ex.szSerialNo).trim();
            final String ipaddress = new String(device_net_info_ex.szIP).trim();
            final String SN = new String(device_net_info_ex.szSerialNo).trim();
            ///Filter repeated and only show IPV4
            ///过滤重复的以及只显示IPV4
            /*if((!inforSet.contains(temp)) && (device_net_info_ex.iIPVersion == 4)){
                inforSet.add(temp);
                //Message msg = mHandler.obtainMessage(UPDATE_SEARCH_DEV_INFOR);
                //msg.obj = temp;
                //mHandler.sendMessage(msg);
                if(SN.equals("ND021711020155")){
                    String videoUrl = "rtsp://admin:abc123@"+ipaddress+":554/cam/realmonitor?channel=1&subtype=0";
                    VideoActivity.intentTo(mContext, videoUrl);
                }
            }*/
            if (device_net_info_ex.iIPVersion == 4 && !TextUtils.isEmpty(SN) && !TextUtils.isEmpty(ipaddress)) {
                CameraScanActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < cameraInfoAdapter.getCount(); i++) {
                            CameraInfo ci = cameraInfoAdapter.getItem(i);
                            if (ci.getSn().equals(SN)) {
                                return;
                            }
                        }
                        cameraInfoAdapter.add(new CameraInfo(SN, ipaddress));
                    }
                });

            }
            Log.d("Device","Got Devices"+temp);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NetSDKLib.getInstance().init();

        // Open sdk log
        final String file = new String(Environment.getExternalStorageDirectory().getPath() + "/sdk_log.log");
        NetSDKLib.getInstance().openLog(file);
        INetSDK.StartSearchDevices(callback);

        cameraInfoAdapter = new CameraAdapter(this);
        setListAdapter(cameraInfoAdapter);

        SharedPreferences sp = getSharedPreferences(CameraScanActivity.CAMERAIPKEY, Context.MODE_PRIVATE);
        String curIP = sp.getString("videoIP", "");
        if (!TextUtils.isEmpty(curIP)) {
            cameraInfoAdapter.setCurrentSelectedIP(curIP);
        }
    }
    private void popUpUsernameInputDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input Camera username");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //m_Text = input.getText().toString();
                saveUsername(input.getText().toString());
                popUpPasswordInputDialog();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void popUpPasswordInputDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Input Camera password");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                savePassword(input.getText().toString());
                SetupActivity.intentTo(getApplicationContext());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
    private String getUsername(){
        SharedPreferences sp = getSharedPreferences(CAMERAIPKEY, Context.MODE_PRIVATE);
        return sp.getString("cameraUsername","admin");
    }
    private String getPassword(){
        SharedPreferences sp = getSharedPreferences(CAMERAIPKEY, Context.MODE_PRIVATE);
        return sp.getString("cameraPassword","abc12345");
    }
    private void saveUsername(String username){

        SharedPreferences sp = getSharedPreferences(CAMERAIPKEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("cameraUsername", username);
        editor.commit();
    }
    private void savePassword(String password){

        SharedPreferences sp = getSharedPreferences(CAMERAIPKEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("cameraPassword", password);
        editor.commit();
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        CameraInfo ci = cameraInfoAdapter.getItem(position);
        SharedPreferences sp = getSharedPreferences(CAMERAIPKEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        String videoUrl = "rtsp://admin:abc12345@"+ci.getIp()+":554/cam/realmonitor?channel=1&subtype=0";
        editor.putString("videoURL", videoUrl);
        editor.putString("videoIP", ci.getIp());
        editor.putString("cameraSN", ci.getSn());
        editor.commit();
        cameraInfoAdapter.setCurrentSelectedIP(ci.getIp());
        popUpUsernameInputDialog();
    }

    /** For processes to access shared internal storage (/sdcard) we need this permission. */
    @TargetApi(Build.VERSION_CODES.M)
    public boolean ensureStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUESTCODE_PERMISSION_STORAGE);
                return false;
            }
        } else {
            // Always granted before Android 6.0.
            return true;
        }
    }
}
