package com.thadaladi.firebase;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;
import java.util.Locale;


public class FilesRecyclerViewAdapter extends
        RecyclerView.Adapter<FilesRecyclerViewAdapter.ViewHolder>{

    private List<String> fileList;
    private Context context;
    private FirebaseStorage firebaseStorage;
    private FirebaseFirestore firestoreDB;
    public Context c;
    public String mystring;
    private String userId;
    private String userPath;
    View view;
    private BroadcastReceiver mBroadcastReceiver;
    public ProgressDialog mProgressDialog;
    private static final String TAG = "FilesAdapter";
    private String DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS).getPath();

    public FilesRecyclerViewAdapter(List<String> list, Context ctx, String uid) {
        fileList = list;
        context = ctx;
        userId = uid;
        userPath =  "";
    }
    @Override
    public int getItemCount() {
        return fileList.size();
    }

    @Override
    public FilesRecyclerViewAdapter.ViewHolder
    onCreateViewHolder(ViewGroup parent, int viewType) {

         view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_item_layout, parent, false);
c=view.getContext();
        firebaseStorage = FirebaseStorage.getInstance();
        firestoreDB = FirebaseFirestore.getInstance();

        // Local broadcast receiver
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive:" + intent);

                hideProgressDialog();

                switch (intent.getAction()) {
                   case MyDownloadService.DOWNLOAD_COMPLETED:
                        // Get number of bytes downloaded
                       hideProgressDialog();
                        long numBytes = intent.getLongExtra(MyDownloadService.EXTRA_BYTES_DOWNLOADED, 0);

                        // Alert success
                       showMessageDialog(context.getString(R.string.success), String.format(Locale.getDefault(),
                                                                      "%d bytes downloaded from %s",
                               numBytes,
                               intent.getStringExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH)));
                       hideProgressDialog();
                        //showMessageDialog("Success","Hello");
                        break;
                    case MyDownloadService.DOWNLOAD_ERROR:
                        // Alert failure
                        showMessageDialog("Error", String.format(Locale.getDefault(),
                                "Failed to download from %s",
                                intent.getStringExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH)));
                        break;

                }
            }
        };
        FilesRecyclerViewAdapter.ViewHolder viewHolder =
                new FilesRecyclerViewAdapter.ViewHolder(view);
        return viewHolder;
    }
    private void showMessageDialog(String title, String message) {
        hideProgressDialog();
        AlertDialog ad = new AlertDialog.Builder(view.getContext())
                .setTitle(title)
                .setMessage(message)
                .create();
        ad.show();

    }

    private void showProgressDialog(String caption) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(view.getContext());
      //      mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.setMessage(caption);
        mProgressDialog.show();
    }

    public void hideProgressDialog() {
      if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
       }
    }

    @Override
    public void onBindViewHolder(FilesRecyclerViewAdapter.ViewHolder holder, int position) {
        final int itemPos = position;
        final String fileName = fileList.get(position);
        holder.name.setText(fileName);

        holder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadFile(fileName);
            }
        });

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteFile(fileName, itemPos);
            }
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public Button download;
        public Button delete;

        public ViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.file_name_i);
            download = view.findViewById(R.id.download_file_b);
            delete = view.findViewById(R.id.delete_file_b);
        }
    }
    private void downloadFile(String fileName){
        StorageReference storageRef = firebaseStorage.getReference();
        StorageReference downloadRef = storageRef.child(""+fileName);
        File fileNameOnDevice = new File(DOWNLOAD_DIR+"/"+fileName);
        // Kick off MyDownloadService to download the file
        Intent intent = new Intent(context, MyDownloadService.class)
                .putExtra(MyDownloadService.EXTRA_DOWNLOAD_PATH, fileName)
                .setAction(MyDownloadService.ACTION_DOWNLOAD);
        context.startService(intent);
        Toast.makeText(view.getContext(),
                "Downloading",
                Toast.LENGTH_SHORT).show();

        LocalBroadcastManager.getInstance(view.getContext()).unregisterReceiver(mBroadcastReceiver);

    }
    private void deleteFile(final String fileName, final int iPos){
        StorageReference storageRef = firebaseStorage.getReference();
        StorageReference deleteRef = storageRef.child(""+fileName);
        deleteRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Log.d("File RecylerView", "delete the file");
                    Toast.makeText(context,"File has been deleted",
                            Toast.LENGTH_SHORT).show();
                    fileList.remove(iPos);
                    notifyItemRemoved(iPos);
                    notifyItemRangeChanged(iPos, fileList.size());

                    deleteFileNameFromDB(fileName);

                }else{
                    Log.e("File RecylerView", "Failed to delete the file");
                    Toast.makeText(context,
                            "File Couldn't be deleted",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
    private void deleteFileNameFromDB(String fileName){
        firestoreDB.collection("files").document(""+fileName).delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.e("File RecylerView", "File name deleted from db");
                    }
                });
    }
}