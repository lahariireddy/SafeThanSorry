package com.example.safethansorry.RoutingHelpers;

import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public interface TaskLoadedCallback {
    void onTaskDone(int token, List<PolylineOptions> poly);
}
