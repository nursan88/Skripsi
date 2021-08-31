package com.infobox.hasnat.ume.ume.Chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.infobox.hasnat.ume.ume.Model.Message;
import com.infobox.hasnat.ume.ume.R;
import com.infobox.hasnat.ume.ume.Adapter.MessageAdapter;
import com.infobox.hasnat.ume.ume.Utils.RealPathUtil;
import com.infobox.hasnat.ume.ume.Utils.UserLastSeenTime;
import com.infobox.hasnat.ume.ume.Elgamal.*;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import es.dmoral.toasty.Toasty;
import id.zelory.compressor.Compressor;

import static android.provider.MediaStore.Images.Media.getBitmap;
import static java.lang.Math.round;


public class ChatActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 100;
    private static final int THUMBNAIL_SIZE = 50;
    private static final int PICK_PHOTO_CODE =1046;
    private String messageReceiverID;
    private String messageReceiverName;
    private Uri filePath;
    private Toolbar chatToolbar;
    private TextView chatUserName;
    private TextView chatUserActiveStatus, ChatConnectionTV;
    private CircleImageView chatUserImageView;

    private DatabaseReference rootReference;

    // sending message
    private ImageView send_message, send_image;
    private EditText input_user_message;
    private FirebaseAuth mAuth;
    private String messageSenderId, download_url;

    private RecyclerView messageList_ReCyVw;
    private final List<Message> messageList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;

    private final static int GALLERY_PICK_CODE = 2;
    private StorageReference imageMessageStorageRef;
    private static final int PICK_IMAGE_REQUEST = 100;
    private ConnectivityReceiver connectivityReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootReference = FirebaseDatabase.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();

        messageReceiverID = getIntent().getExtras().get("visitUserId").toString();
        messageReceiverName = getIntent().getExtras().get("userName").toString();

        imageMessageStorageRef = FirebaseStorage.getInstance().getReference().child("messages_image");

        // appbar / toolbar
        chatToolbar = findViewById(R.id.chats_appbar);
        setSupportActionBar(chatToolbar);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater)
                this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.appbar_chat, null);
        actionBar.setCustomView(view);

        ChatConnectionTV = findViewById(R.id.ChatConnectionTV);
        chatUserName = findViewById(R.id.chat_user_name);
        chatUserActiveStatus = findViewById(R.id.chat_active_status);
        chatUserImageView = findViewById(R.id.chat_profile_image);

        // sending message declaration
        send_message = findViewById(R.id.c_send_message_BTN);
        send_image = findViewById(R.id.c_send_image_BTN);
        input_user_message = findViewById(R.id.c_input_message);

        // setup for showing messages
        messageAdapter = new MessageAdapter(messageList);
        messageList_ReCyVw = findViewById(R.id.message_list);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        messageList_ReCyVw.setLayoutManager(linearLayoutManager);
        messageList_ReCyVw.setHasFixedSize(true);
        //linearLayoutManager.setReverseLayout(true);
        messageList_ReCyVw.setAdapter(messageAdapter);

        fetchMessages();

        chatUserName.setText(messageReceiverName);
        rootReference.child("users").child(messageReceiverID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        final String active_status = dataSnapshot.child("active_now").getValue().toString();
                        final String thumb_image = dataSnapshot.child("user_thumb_image").getValue().toString();

//                        // FOR TESTING
//                        if (currentUser != null){
//                            rootReference.child("active_now").setValue(ServerValue.TIMESTAMP);
//                        }

                        // show image on appbar
                        Picasso.get()
                                .load(thumb_image)
                               // .networkPolicy(NetworkPolicy.OFFLINE) // for Offline
                                .placeholder(R.drawable.default_profile_image)
                                .into(chatUserImageView, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                    }
                                    @Override
                                    public void onError(Exception e) {
                                        Picasso.get()
                                                .load(thumb_image)
                                                .placeholder(R.drawable.default_profile_image)
                                                .into(chatUserImageView);
                                    }
                                });

                        //active status
                        if (active_status.contains("true")){
                            chatUserActiveStatus.setText("Active now");
                        } else {
                            UserLastSeenTime lastSeenTime = new UserLastSeenTime();
                            long last_seen = Long.parseLong(active_status);

                            //String lastSeenOnScreenTime = lastSeenTime.getTimeAgo(last_seen).toString();
                            String lastSeenOnScreenTime = lastSeenTime.getTimeAgo(last_seen, getApplicationContext()).toString();
                            Log.e("lastSeenTime", lastSeenOnScreenTime);
                            if (lastSeenOnScreenTime != null){
                                chatUserActiveStatus.setText(lastSeenOnScreenTime);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
        /**
         *  SEND TEXT MESSAGE BUTTON
         */
        send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });


        /** SEND IMAGE MESSAGE BUTTON */
        send_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_PICK_CODE);
            }
        });
    } // ending onCreate

    @Override
    protected void onResume() {
        super.onResume();
        //Register Connectivity Broadcast receiver
        connectivityReceiver = new ConnectivityReceiver();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, intentFilter);
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Unregister Connectivity Broadcast receiver
        unregisterReceiver(connectivityReceiver);
    }

    @Override // for gallery picking
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         //  For image sending
        super.onActivityResult(requestCode, resultCode, data);
               //if (requestCode == GALLERY_PICK_CODE && resultCode == RESULT_OK && data != null && data.getData() != null){
        if (requestCode == GALLERY_PICK_CODE  && resultCode == RESULT_OK && data != null && data.getData() != null){
            int bilk;
            int pangkat;
            int bila; // untuk ciphertext
            int bilb_red, bilb_green, bilb_blue; //untuk ciphertext
            GenerateKey gk = new GenerateKey();
            HitungY hy = new HitungY(gk);
            filePath = data.getData();
            final String message_sender_reference = "messages/" + messageSenderId + "/" + messageReceiverID;
            final String message_receiver_reference = "messages/" + messageReceiverID + "/" + messageSenderId;
            DatabaseReference user_message_key = rootReference.child("messages").child(messageSenderId).child(messageReceiverID).push();
            final String message_push_id = user_message_key.getKey();
            final StorageReference file_path = imageMessageStorageRef.child(message_push_id + ".jpg");
            Random bil_k = new Random();
            bilk = bil_k.nextInt(gk.bilprim-2);
            pangkat = (int) Math.pow(gk.BilG, bilk);
            bila = Math.floorMod(pangkat, gk.bilprim);
            byte[] thumb_byte_data;
            final byte[] byteImage;
            String realPath;
            // SDK < API11
            if (Build.VERSION.SDK_INT < 11)
                realPath = RealPathUtil.getRealPathFromURI_BelowAPI11(this, data.getData());
                // SDK >= 11 && SDK < 19
            else if (Build.VERSION.SDK_INT < 19)
                realPath = RealPathUtil.getRealPathFromURI_API11to18(this, data.getData());
                // SDK > 19 (Android 4.4)
            else
                realPath = RealPathUtil.getRealPathFromURI_API19(this, data.getData());

            //AMBIL IMAGE URI BIAR DAPET REAL PATH NYA. KALAU GA DIGINIIN GA MASUK.
            File fi = new File(realPath);
            final String pathDamn = realPath;
            try {
                int pangkat_b;
                final int redValue, greenValue, blueValue;

                //HARUS DI GINIIN BIAR DAPET IMAGE NYA. BELUM DAPET CARA LAIN SOALNYA HEHE.
                Bitmap bitmap  = new Compressor(this)
                        .setMaxHeight(100)
                        .setMaxWidth(100)
                        .setQuality(75)
                        .compressToBitmap(fi);
                //DISET MUTABLE TRUE BIAR BISA DI EDIT..
                Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                String tx = "";
                final int w = mutableBitmap.getWidth();
                final int h = mutableBitmap.getHeight();
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                       pangkat_b = (int) Math.pow(hy.y, bilk);
                        int col = bitmap.getPixel(x, y);
                        int alpha = col & 0xFF000000;
                        int red = Color.red(col);
                        int green = Color.green(col);
                        int blue = Color.blue(col);
                        bilb_red = Math.floorMod(pangkat_b*red, gk.bilprim);
                        bilb_green = Math.floorMod(pangkat_b*green, gk.bilprim);
                        bilb_blue = Math.floorMod(pangkat_b*blue, gk.bilprim);
                        int gray = (int) (bilb_red + bilb_green  + bilb_blue );
                        mutableBitmap.setPixel(x, y, gray);
                    }
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                thumb_byte_data  = baos.toByteArray();
                UploadTask uploadTask = file_path.putBytes(thumb_byte_data);
                Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (!task.isSuccessful()) {
                            Toasty.error(ChatActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        download_url = file_path.getDownloadUrl().toString();
                        return file_path.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            download_url = task.getResult().toString();
                            //User Terima Message/Image
                            HashMap<String, Object> message_text_body = new HashMap<>();
                            message_text_body.put("message",download_url);
                            message_text_body.put("seen", false);
                            message_text_body.put("type", "image");
                            message_text_body.put("time", ServerValue.TIMESTAMP);
                            message_text_body.put("from", messageSenderId);
                            HashMap<String, Object> messageBodyDetails = new HashMap<>();
                            messageBodyDetails.put(message_sender_reference + "/" + message_push_id, message_text_body);
                            messageBodyDetails.put(message_receiver_reference + "/" + message_push_id, message_text_body);
                            rootReference.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    if (databaseError != null) {
                                        Log.e("from_image_chat: ", databaseError.getMessage());
                                    }
                                    input_user_message.setText("");
                                }
                            });
                            Log.e("tag", "Image sent successfully");
                        } else {
                            Toasty.warning(ChatActivity.this, "Failed to send image. Try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void fetchMessages() {
        rootReference.child("messages").child(messageSenderId).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        if (dataSnapshot.exists()){
                            Message message = dataSnapshot.getValue(Message.class);
                            messageList.add(message);
                            messageAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    }
                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                    }
                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }



    private void sendMessage() {
        String message = input_user_message.getText().toString();
        if (TextUtils.isEmpty(message)){
            Toasty.info(ChatActivity.this, "Please type a message", Toast.LENGTH_SHORT).show();
        } else {
            String message_sender_reference = "messages/" + messageSenderId + "/" + messageReceiverID;
            String message_receiver_reference = "messages/" + messageReceiverID + "/" + messageSenderId;

            DatabaseReference user_message_key = rootReference.child("messages").child(messageSenderId).child(messageReceiverID).push();
            String message_push_id = user_message_key.getKey();

            HashMap<String, Object> message_text_body = new HashMap<>();
            message_text_body.put("message", message);
            message_text_body.put("seen", false);
            message_text_body.put("type", "text");
            message_text_body.put("time", ServerValue.TIMESTAMP);
            message_text_body.put("from", messageSenderId);

            HashMap<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put(message_sender_reference + "/" + message_push_id, message_text_body);
            messageBodyDetails.put(message_receiver_reference + "/" + message_push_id, message_text_body);

            rootReference.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                    if (databaseError != null){
                        Log.e("Sending message", databaseError.getMessage());
                    }
                    input_user_message.setText("");
                }
            });
        }
    }


    // Broadcast receiver for network checking
    public class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            ChatConnectionTV.setVisibility(View.GONE);
            if (networkInfo != null && networkInfo.isConnected()) {
                ChatConnectionTV.setText("Internet connected");
                ChatConnectionTV.setTextColor(Color.WHITE);
                ChatConnectionTV.setVisibility(View.VISIBLE);

                // LAUNCH activity after certain time period
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        ChatActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                ChatConnectionTV.setVisibility(View.GONE);
                            }
                        });
                    }
                }, 1200);
            } else {
                ChatConnectionTV.setText("No internet connection! ");
                ChatConnectionTV.setTextColor(Color.WHITE);
                ChatConnectionTV.setBackgroundColor(Color.RED);
                ChatConnectionTV.setVisibility(View.VISIBLE);
            }
        }
    }

}
