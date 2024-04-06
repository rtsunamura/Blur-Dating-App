package com.example.blurdatingapplication.mypage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;


import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.example.blurdatingapplication.LaunchActivity;
import com.example.blurdatingapplication.Login;
import com.example.blurdatingapplication.MainActivity;
import com.example.blurdatingapplication.PremiumActivity;
import com.example.blurdatingapplication.R;
import com.example.blurdatingapplication.Setting;
import com.example.blurdatingapplication.data.UserData;
import com.example.blurdatingapplication.utils.FireBaseUtil;
import com.example.blurdatingapplication.utils.FunctionUtil;


public class MyPageFragment extends Fragment {
    private Uri uri1;
    TextView textViewUsername, textViewAge, planNumber;
    Button buttonFind, buttonUpgrade, buttonEdit;
    ImageView imageViewFacePic;
    UserData userData;
    ProgressBar progressBar;
    LinearLayout linearLayout;
    ImageView imageViewEdit;

    public MyPageFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_my_page, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        linearLayout = view.findViewById(R.id.content);

        imageViewEdit = view.findViewById(R.id.image_edit);

        imageViewFacePic = view.findViewById(R.id.image_face);
        textViewUsername = view.findViewById(R.id.txt_username);
        textViewAge = view.findViewById(R.id.text_age);

        buttonFind = view.findViewById(R.id.btn_find);
        buttonUpgrade = view.findViewById(R.id.btn_upgrade);
        buttonEdit = view.findViewById(R.id.btn_edit);

        planNumber = view.findViewById(R.id.planNumber);

        linearLayout.setVisibility(View.GONE);

        getUserData();

        imageViewEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), EditProfile.class));
            }
        });

        buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), EditProfile.class));
            }
        });

        buttonUpgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), PremiumActivity.class));
            }
        });

        return view;
    }

    void getUserData(){

        FireBaseUtil.getCurrentFacePicStorageReference().getDownloadUrl()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        uri1  = task.getResult();
                        FunctionUtil.setFaceImage(getContext(),uri1,imageViewFacePic);
                    }
                });


        FireBaseUtil.currentUserData().get().addOnCompleteListener(task -> {
            userData = task.getResult().toObject(UserData.class);
            textViewAge.setText(Integer.toString(FunctionUtil.calculateAge(userData.getBirthday())));
            textViewUsername.setText(userData.getUsername());

            // Check user's plan and set planNumber TextView
            int userPlan = userData.getPlan();
            if(userPlan == 1) {
                planNumber.setText("Premium");
            } else if (userPlan == 2){
                planNumber.setText("Developer");
            } else {
                planNumber.setText("Free");
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    linearLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            },1500);
        });

        /*FireBaseUtil.otherUserData("nZ3qOKxVu4SUcTfD7B8trBw64FQ2").get().addOnCompleteListener(task -> {
            userData = task.getResult().toObject(UserData.class);
            textViewAge.setText(Integer.toString(FunctionUtil.calculateAge(userData.getBirthday())));
            textViewUsername.setText(userData.getUsername());


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    linearLayout.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            },1500);
        });*/
    }
}