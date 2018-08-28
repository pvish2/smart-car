/*
 * Copyright 2016, The Android Open Source Project
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
package com.piyushdroid.androidthings.smartcar;

import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private RecyclerView mRecyclerView;
    private AccidentEntryAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Reference for accident events from embedded device
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("logs");

        mRecyclerView = (RecyclerView) findViewById(R.id.entryView);
        // Show most recent items at the top
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);
        mRecyclerView.setLayoutManager(layoutManager);

        // Initialize RecyclerView adapter
        mAdapter = new AccidentEntryAdapter(this, ref,isDriver());
        mRecyclerView.setAdapter(mAdapter);
        if(isDriver()){
            setTitle("Dashboard");
        }else{
            setTitle("Emergency Receiver");
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        // Initialize Firebase listeners in adapter
        mAdapter.startListening();

        // Make sure new events are visible
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();

        // Tear down Firebase listeners in adapter
        mAdapter.stopListening();
    }


    private boolean isDriver(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
     return    settings.getBoolean("isDriver", false);
    }


    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
    }
}
