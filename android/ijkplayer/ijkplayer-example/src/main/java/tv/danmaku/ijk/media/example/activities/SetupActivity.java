/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tv.danmaku.ijk.media.example.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.company.NetSDK.CB_fSearchDevicesCB;
import com.company.NetSDK.DEVICE_NET_INFO_EX;
import com.company.NetSDK.INetSDK;
import com.dahuatech.netsdk.common.NetSDKLib;
import com.dahuatech.netsdk.common.WIFIConfigurationModule;

import java.util.HashSet;
import java.util.Set;

import tv.danmaku.ijk.media.example.R;

public class SetupActivity extends AppCompatActivity {
    private Context mContext;
    private static final int REQUESTCODE_PERMISSION_STORAGE = 1234;
    WIFIConfigurationModule mConfigModule;
    final Set<String> inforSet = new HashSet<String>();

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, SetupActivity.class);
        return intent;
    }

    public static void intentTo(Context context) {
        context.startActivity(newIntent(context));
    }
    private CB_fSearchDevicesCB callback = new  CB_fSearchDevicesCB(){

        @Override
        public void invoke(DEVICE_NET_INFO_EX device_net_info_ex) {
            String temp = "IP : "+ new String(device_net_info_ex.szIP).trim() + "\n" +
                    "IP : " + new String(device_net_info_ex.szSerialNo).trim();
            String ipaddress = new String(device_net_info_ex.szIP).trim();
            ///Filter repeated and only show IPV4
            ///过滤重复的以及只显示IPV4
            if((!inforSet.contains(temp)) && (device_net_info_ex.iIPVersion == 4)){
                inforSet.add(temp);
                //Message msg = mHandler.obtainMessage(UPDATE_SEARCH_DEV_INFOR);
                //msg.obj = temp;
                //mHandler.sendMessage(msg);
                String videoUrl = "rtsp://admin:abc12345@"+ipaddress+":554/cam/realmonitor?channel=1&subtype=0";
                VideoActivity.intentTo(mContext, videoUrl);
            }
            Log.d("Device","Got Devices"+temp);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        final Button button = (Button) findViewById(R.id.button);
        mContext = this;
        ensureStoragePermissionGranted();
        /// Initializing the NetSDKLib is important and necessary to ensure that
        /// all the APIs of INetSDK.jar are effective.
        /// 注意: 必须调用 init 接口初始化 INetSDK.jar 仅需要一次初始化
        NetSDKLib.getInstance().init();

        // Open sdk log
        final String file = new String(Environment.getExternalStorageDirectory().getPath() + "/sdk_log.log");
        NetSDKLib.getInstance().openLog(file);
        INetSDK.StartSearchDevices(callback);
        //mConfigModule.StartSearchDevices(callback);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                // currentContext.startActivity(activityChangeIntent);
                EditText edit = (EditText)findViewById(R.id.ipAddress);
                String ip = edit.getText().toString();

                Log.d("Setup","on click: "+ip);
                String videoUrl = "rtsp://admin:abc123@"+ip+":554/cam/realmonitor?channel=1&subtype=0";
                VideoActivity.intentTo(mContext, videoUrl);
                //PresentActivity.this.startActivity(activityChangeIntent);
            }
        });
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        //Fragment newFragment = SettingsFragment.newInstance();
        //FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //transaction.replace(R.id.body, newFragment);
        //transaction.commit();
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
