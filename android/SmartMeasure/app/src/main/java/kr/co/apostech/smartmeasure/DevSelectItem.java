/*
 * Copyright (C) 2019 The Android Open Source Project
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

package kr.co.apostech.smartmeasure;

//import com.google.android.material.R;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import kr.co.apostech.smartmeasure.R;

/** A simple two line list item. */
public class DevSelectItem extends ViewHolder {

  public final ImageView icon;
  public final TextView text;
  public final TextView secondary;

  public DevSelectItem(@NonNull View view) {
    super(view);
    this.icon = itemView.findViewById(R.id.dev_list_icon);
    this.text = itemView.findViewById(R.id.dev_list_title);
    this.secondary = itemView.findViewById(R.id.dev_list_addr);
  }

  @NonNull
  public static DevSelectItem create(@NonNull ViewGroup parent) {
    return new DevSelectItem(
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.dev_select_item, parent, false));
  }
}
