package com.android.wm.shell.common;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.window.TaskSnapshot;

public interface TaskStackListenerCallback {
    default void onActivityDismissingDockedStack() {
    }

    default void onActivityForcedResizable(String str, int i, int i2) {
    }

    default void onActivityLaunchOnSecondaryDisplayFailed() {
    }

    default void onActivityLaunchOnSecondaryDisplayRerouted() {
    }

    default void onActivityPinned(String str, int i, int i2, int i3) {
    }

    default void onActivityRequestedOrientationChanged(int i, int i2) {
    }

    default void onActivityRestartAttempt(ActivityManager.RunningTaskInfo runningTaskInfo, boolean z, boolean z2, boolean z3) {
    }

    default void onActivityRotation(int i) {
    }

    default void onActivityUnpinned() {
    }

    default void onBackPressedOnTaskRoot(ActivityManager.RunningTaskInfo runningTaskInfo) {
    }

    default void onLockTaskModeChanged(int i) {
    }

    default void onRecentTaskListFrozenChanged(boolean z) {
    }

    default void onRecentTaskListUpdated() {
    }

    default void onTaskCreated(int i, ComponentName componentName) {
    }

    default void onTaskDescriptionChanged(ActivityManager.RunningTaskInfo runningTaskInfo) {
    }

    default void onTaskDisplayChanged(int i, int i2) {
    }

    default void onTaskMovedToBack(int i) {
    }

    default void onTaskMovedToFront(int i) {
    }

    default void onTaskProfileLocked(ActivityManager.RunningTaskInfo runningTaskInfo, int i) {
    }

    default void onTaskRemoved(int i) {
    }

    default boolean onTaskSnapshotChanged(int i, TaskSnapshot taskSnapshot) {
        return false;
    }

    default void onTaskStackChanged() {
    }

    default void onTaskStackChangedBackground() {
    }

    default void onActivityLaunchOnSecondaryDisplayFailed(ActivityManager.RunningTaskInfo runningTaskInfo) {
    }

    default void onActivityLaunchOnSecondaryDisplayRerouted(ActivityManager.RunningTaskInfo runningTaskInfo) {
    }

    default void onTaskMovedToBack(ActivityManager.RunningTaskInfo runningTaskInfo) {
    }

    default void onTaskMovedToFront(ActivityManager.RunningTaskInfo runningTaskInfo) {
    }
}
