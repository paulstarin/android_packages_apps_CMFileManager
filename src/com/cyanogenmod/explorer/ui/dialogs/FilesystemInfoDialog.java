/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.cyanogenmod.explorer.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Switch;
import android.widget.TextView;

import com.cyanogenmod.explorer.R;
import com.cyanogenmod.explorer.console.ConsoleBuilder;
import com.cyanogenmod.explorer.model.DiskUsage;
import com.cyanogenmod.explorer.model.MountPoint;
import com.cyanogenmod.explorer.preferences.ExplorerSettings;
import com.cyanogenmod.explorer.preferences.Preferences;
import com.cyanogenmod.explorer.ui.widgets.DiskUsageGraph;
import com.cyanogenmod.explorer.util.CommandHelper;
import com.cyanogenmod.explorer.util.DialogHelper;
import com.cyanogenmod.explorer.util.FileHelper;
import com.cyanogenmod.explorer.util.MountPointHelper;

/**
 * A class that wraps a dialog for showing information about a mount point.<br />
 * This class display information like mount point name, device name, size, type, ...
 */
public class FilesystemInfoDialog implements OnClickListener {

    /**
     * An interface to communicate when the user change the mount state
     * of a filesystem.
     */
    public interface OnMountListener {
        /**
         * Method invoked when the mount state of a mount point has changed.
         *
         * @param mountPoint The mount point that has changed
         */
        void onRemount(MountPoint mountPoint);
    }



    private static final String TAG = "FilesystemInfoDialog"; //$NON-NLS-1$

    private final MountPoint mMountPoint;
    private final DiskUsage mDiskUsage;

    private final Context mContext;
    private final AlertDialog mDialog;
    private View mInfoViewTab;
    private View mDiskUsageViewTab;
    private View mInfoView;
    private View mDiskUsageView;
    private Switch mSwStatus;
    private DiskUsageGraph mDiskUsageGraph;
    private TextView mInfoMsgView;

    private OnMountListener mOnMountListener;

    private boolean mIsMountAllowed;

    /**
     * Constructor of <code>FilesystemInfoDialog</code>.
     *
     * @param context The current context
     * @param mountPoint The mount point information
     * @param diskUsage The disk usage of the mount point
     */
    public FilesystemInfoDialog(Context context, MountPoint mountPoint, DiskUsage diskUsage) {
        super();

        //Save the context
        this.mContext = context;

        //Save data
        this.mMountPoint = mountPoint;
        this.mDiskUsage = diskUsage;
        this.mIsMountAllowed = false;

        //Inflate the content
        LayoutInflater li =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = li.inflate(R.layout.filesystem_info_dialog, null);

        //Create the dialog
        this.mDialog = DialogHelper.createDialog(
                                        context,
                                        R.drawable.ic_holo_light_sdcard,
                                        R.string.filesystem_info_dialog_title,
                                        contentView);
        //Fill the dialog
        fillData(contentView);
    }

    /**
     * Method that shows the dialog.
     */
    public void show() {
        this.mDialog.show();
    }

    /**
     * Method that sets the listener for listen mount events.
     *
     * @param onMountListener The mount listener
     */
    public void setOnMountListener(OnMountListener onMountListener) {
        this.mOnMountListener = onMountListener;
    }

    /**
     * Method that fill the dialog with the data of the mount point.
     *
     * @param contentView The content view
     */
    @SuppressWarnings({ "boxing" })
    private void fillData(View contentView) {
        //Get the tab views
        this.mInfoViewTab = contentView.findViewById(R.id.filesystem_info_dialog_tab_info);
        this.mDiskUsageViewTab =
                contentView.findViewById(R.id.filesystem_info_dialog_tab_disk_usage);
        this.mInfoView = contentView.findViewById(R.id.filesystem_tab_info);
        this.mDiskUsageView = contentView.findViewById(R.id.filesystem_tab_diskusage);
        this.mDiskUsageGraph =
                (DiskUsageGraph)contentView.findViewById(R.id.filesystem_disk_usage_graph);

        // Set the user preference about free disk space warning level
        String fds = Preferences.getSharedPreferences().getString(
                ExplorerSettings.SETTINGS_FREE_DISK_SPACE_WARNING_LEVEL.getId(),
                (String)ExplorerSettings.
                    SETTINGS_FREE_DISK_SPACE_WARNING_LEVEL.getDefaultValue());
        this.mDiskUsageGraph.setFreeDiskSpaceWarningLevel(Integer.parseInt(fds));

        //Register the listeners
        this.mInfoViewTab.setOnClickListener(this);
        this.mDiskUsageViewTab.setOnClickListener(this);

        //Gets text views
        this.mSwStatus = (Switch)contentView.findViewById(R.id.filesystem_info_status);
        this.mSwStatus.setOnClickListener(this);
        TextView tvMountPoint =
                (TextView)contentView.findViewById(R.id.filesystem_info_mount_point);
        TextView tvDevice = (TextView)contentView.findViewById(R.id.filesystem_info_device);
        TextView tvType = (TextView)contentView.findViewById(R.id.filesystem_info_type);
        TextView tvOptions = (TextView)contentView.findViewById(R.id.filesystem_info_options);
        TextView tvDumpPass = (TextView)contentView.findViewById(R.id.filesystem_info_dump_pass);
        TextView tvTotal =
                (TextView)contentView.findViewById(R.id.filesystem_info_total_disk_usage);
        TextView tvUsed = (TextView)contentView.findViewById(R.id.filesystem_info_used_disk_usage);
        TextView tvFree = (TextView)contentView.findViewById(R.id.filesystem_info_free_disk_usage);
        this.mInfoMsgView = (TextView)contentView.findViewById(R.id.filesystem_info_msg);

        //Fill the text views
        tvMountPoint.setText(this.mMountPoint.getMountPoint());
        tvDevice.setText(this.mMountPoint.getDevice());
        tvType.setText(this.mMountPoint.getType());
        tvOptions.setText(this.mMountPoint.getOptions());
        tvDumpPass.setText(
                String.format("%d / %d",  //$NON-NLS-1$
                        this.mMountPoint.getDump(),
                        this.mMountPoint.getPass()));
        if (this.mDiskUsage != null) {
            tvTotal.setText(FileHelper.getHumanReadableSize(this.mDiskUsage.getTotal()));
            tvUsed.setText(FileHelper.getHumanReadableSize(this.mDiskUsage.getUsed()));
            tvFree.setText(FileHelper.getHumanReadableSize(this.mDiskUsage.getFree()));
        } else {
            tvTotal.setText("-"); //$NON-NLS-1$
            tvUsed.setText("-"); //$NON-NLS-1$
            tvFree.setText("-"); //$NON-NLS-1$
        }

        //Configure status switch
        boolean hasPrivileged = false;
        try {
            hasPrivileged = ConsoleBuilder.getConsole(this.mContext).isPrivileged();
        } catch (Throwable ex) {/**NON BLOCK**/}
        boolean mountAllowed = MountPointHelper.isMountAllowed(this.mMountPoint);
        if (hasPrivileged) {
            this.mSwStatus.setEnabled(mountAllowed);
            if (!mountAllowed) {
                this.mInfoMsgView.setText(
                        this.mContext.getString(R.string.filesystem_info_couldnt_be_mounted_msg));
                this.mInfoMsgView.setVisibility(View.VISIBLE);
            }
        } else {
            this.mInfoMsgView.setVisibility(View.VISIBLE);
            this.mInfoMsgView.setOnClickListener(this);
        }
        this.mIsMountAllowed = hasPrivileged && mountAllowed;
        this.mSwStatus.setEnabled(this.mIsMountAllowed);
        this.mSwStatus.setChecked(MountPointHelper.isReadWrite(this.mMountPoint));

        //Change the tab
        onClick(this.mInfoViewTab);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.filesystem_info_dialog_tab_info:
                if (!this.mInfoViewTab.isSelected()) {
                    this.mInfoViewTab.setSelected(true);
                    ((TextView)this.mInfoViewTab).setTextAppearance(
                            this.mContext, R.style.primary_text_appearance);
                    this.mDiskUsageViewTab.setSelected(false);
                    ((TextView)this.mDiskUsageViewTab).setTextAppearance(
                            this.mContext, R.style.secondary_text_appearance);
                    this.mInfoView.setVisibility(View.VISIBLE);
                    this.mDiskUsageView.setVisibility(View.GONE);
                }
                this.mInfoMsgView.setVisibility (this.mIsMountAllowed ? View.GONE : View.VISIBLE);
                break;

            case R.id.filesystem_info_dialog_tab_disk_usage:
                if (!this.mDiskUsageViewTab.isSelected()) {
                    this.mInfoViewTab.setSelected(false);
                    ((TextView)this.mInfoViewTab).setTextAppearance(
                            this.mContext, R.style.secondary_text_appearance);
                    this.mDiskUsageViewTab.setSelected(true);
                    ((TextView)this.mDiskUsageViewTab).setTextAppearance(
                            this.mContext, R.style.primary_text_appearance);
                    this.mInfoView.setVisibility(View.GONE);
                    this.mDiskUsageView.setVisibility(View.VISIBLE);
                }
                this.mDiskUsageGraph.post(new Runnable() {
                    @Override
                    @SuppressWarnings("synthetic-access")
                    public void run() {
                        //Animate disk usage graph
                        FilesystemInfoDialog.this.mDiskUsageGraph.drawDiskUsage(
                                FilesystemInfoDialog.this.mDiskUsage);
                    }
                });
                break;

            case R.id.filesystem_info_status:
                //Mount the filesystem
                Switch sw = (Switch)v;
                boolean ret = false;
                try {
                    ret = CommandHelper.remount(
                            this.mContext,
                            this.mMountPoint, sw.isChecked(), null);
                    //Hide warning message
                    this.mInfoMsgView.setVisibility(View.GONE);
                    //Communicate the mount change
                    if (this.mOnMountListener != null) {
                        this.mOnMountListener.onRemount(this.mMountPoint);
                    }

                } catch (Throwable e) {
                    Log.e(TAG,
                            String.format(
                                    "Fail to remount %s", //$NON-NLS-1$
                                    this.mMountPoint.getMountPoint()), e);
                }
                if (!ret) {
                    //Show warning message
                    this.mInfoMsgView.setText(R.string.filesystem_info_mount_failed_msg);
                    this.mInfoMsgView.setVisibility(View.VISIBLE);
                    sw.setChecked(!sw.isChecked());
                }
                break;

            case R.id.filesystem_info_msg:
                //Change the console
                boolean superuser = ConsoleBuilder.changeToPrivilegedConsole(this.mContext);
                if (superuser) {
                    this.mInfoMsgView.setOnClickListener(null);

                    // Is filesystem able to be mounted?
                    boolean mountAllowed = MountPointHelper.isMountAllowed(this.mMountPoint);
                    if (mountAllowed) {
                        this.mInfoMsgView.setVisibility(View.GONE);
                        this.mInfoMsgView.setBackground(null);
                        this.mSwStatus.setEnabled(true);
                        this.mIsMountAllowed = true;
                        break;
                    }

                    // Show the message
                    this.mInfoMsgView.setText(
                            this.mContext.getString(
                                    R.string.filesystem_info_couldnt_be_mounted_msg));
                    this.mInfoMsgView.setVisibility(View.VISIBLE);
                    this.mIsMountAllowed = false;
                }
                break;

            default:
                break;
        }
    }

}
