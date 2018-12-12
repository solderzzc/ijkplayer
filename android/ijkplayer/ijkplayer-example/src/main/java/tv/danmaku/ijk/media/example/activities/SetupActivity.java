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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import tv.danmaku.ijk.media.example.R;
import tv.danmaku.ijk.media.example.fragments.SettingsFragment;

public class SetupActivity extends AppCompatActivity {
    private Context mContext;
    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, SetupActivity.class);
        return intent;
    }

    public static void intentTo(Context context) {
        context.startActivity(newIntent(context));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        final Button button = (Button) findViewById(R.id.button);
        mContext = this;
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
}
