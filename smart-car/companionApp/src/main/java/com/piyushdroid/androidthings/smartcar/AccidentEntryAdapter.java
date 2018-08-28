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

import android.content.Context;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * RecyclerView adapter to populate accident entries from Firebase.
 */
public class AccidentEntryAdapter extends FirebaseRecyclerAdapter<AccidentEntry, AccidentEntryAdapter.AccidentEntryViewHolder> {

    /**
     * ViewHolder for each accident entry
     */
    public static class AccidentEntryViewHolder extends RecyclerView.ViewHolder {

        public final ImageView image;
        public final TextView time;
        public final TextView metadata;
        public final Button playSong;

        public AccidentEntryViewHolder(View itemView) {
            super(itemView);

            this.image = (ImageView) itemView.findViewById(R.id.imageView1);
            this.time = (TextView) itemView.findViewById(R.id.textView1);
            this.metadata = (TextView) itemView.findViewById(R.id.textView2);
            this.playSong  =(Button) itemView.findViewById(R.id.playSong);
        }
    }

    private Context mApplicationContext;
    private FirebaseStorage mFirebaseStorage;
    private boolean isDriver;

    public AccidentEntryAdapter(Context context, DatabaseReference ref,boolean isDriver) {
        super(new FirebaseRecyclerOptions.Builder<AccidentEntry>()
                .setQuery(ref, AccidentEntry.class)
                .build());

        mApplicationContext = context.getApplicationContext();
        mFirebaseStorage = FirebaseStorage.getInstance();
        this.isDriver = isDriver;
    }

    @Override
    public AccidentEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View entryView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.accident_entry, parent, false);

        return new AccidentEntryViewHolder(entryView);
    }

    @Override
    protected void onBindViewHolder(AccidentEntryViewHolder holder, int position, AccidentEntry model) {
        // Display the timestamp
        CharSequence prettyTime = DateUtils.getRelativeDateTimeString(mApplicationContext,
                model.getTimestamp(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        holder.image.setVisibility(View.GONE);
        holder.metadata.setVisibility(View.GONE);
        holder.time.setVisibility(View.GONE);
        holder.playSong.setVisibility(View.GONE);
        // Display the image
        if (model.getImage() != null && !isDriver && !model.isDriver()) {
            holder.image.setVisibility(View.VISIBLE);

            StorageReference imageRef = mFirebaseStorage.getReferenceFromUrl(model.getImage());

            GlideApp.with(mApplicationContext)
                    .load(imageRef)
                    .placeholder(R.drawable.ic_image)
                    .into(holder.image);
            holder.metadata.setVisibility(View.VISIBLE);
            holder.time.setVisibility(View.VISIBLE);
            holder.metadata.setText("Car Location:\n"+model.getLatitude()+"\n"+model.getLongitude()+"\n"+model.getTemprature()+"\n\n\n\n");
            holder.time.setText(prettyTime);
            holder.metadata.setTextColor(Color.BLACK);
        }else if(isDriver && model.isDriver() && model.getUserState()=="InActive"){
            holder.metadata.setVisibility(View.VISIBLE);
            holder.metadata.setText(model.getMessage());
            holder.metadata.setTextColor(Color.RED);
            holder.playSong.setVisibility(View.VISIBLE);
        }else if(isDriver && model.isDriver()){
            holder.metadata.setVisibility(View.VISIBLE);
            holder.metadata.setText(model.getMessage());
            holder.metadata.setTextColor(Color.RED);
        }


    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
    }



}
