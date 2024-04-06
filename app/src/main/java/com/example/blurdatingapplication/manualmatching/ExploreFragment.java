package com.example.blurdatingapplication.manualmatching;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blurdatingapplication.R;

import com.example.blurdatingapplication.data.CheckedUser;
import com.example.blurdatingapplication.data.UserData;
import com.example.blurdatingapplication.data.WaitUser;
import com.example.blurdatingapplication.utils.FireBaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.RewindAnimationSetting;

import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting;
import com.yuyakaido.android.cardstackview.SwipeableMethod;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExploreFragment extends Fragment implements CardStackListener {
    UserData currentUserData;
    private DrawerLayout drawerLayout;
    private CardStackView cardStackView;
    private CardStackLayoutManager manager;
    private CardStackAdapter adapter;
    Uri uri;
    FirebaseAuth auth;
    FirebaseUser currentUser;
    CheckedUser currentUserCheckedList;
    WaitUser currentUserWaitList;
    public ExploreFragment() {
        //Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        createSpots(new SpotsCallback() {
            @Override
            public void onSpotsReady(List<Spot> spots) {
                if (isAdded()) {
                    //paginate(spots);
                }
            }
        });
        setupCardStackView(view);
        setupButton(view);
        return view;
    }

    @Override
    public void onCardDragging(Direction direction, float ratio) {
        Log.d("CardStackView", "onCardDragging: d = " + direction.name() + ", r = " + ratio);
    }

    @Override
    public void onCardSwiped(Direction direction) {
        Log.d("CardStackView", "onCardSwiped: p = " + manager.getTopPosition() + ", d = " + direction);
        int topPosition = manager.getTopPosition();
        int previousPosition = topPosition - 1;
        List<Spot> spots = adapter.getSpots();

        if (topPosition == spots.size() - 1) {
            // Disable swiping when the last card is reached
            Toast.makeText(requireContext(), "Not Enought Users. Swipe Later", Toast.LENGTH_SHORT).show();
            manager.setSwipeableMethod(SwipeableMethod.None);
        }


        if(direction == Direction.Left || direction == Direction.Right){
            if (topPosition >= 0 && topPosition < spots.size()) {
                // Get the swiped user ID from the spot at the top position
                String swipedUserId = spots.get(previousPosition).getUserId();
                setChekedUserIds(swipedUserId);
            }
        }

        if (direction == Direction.Right) {
            if (topPosition >= 0 && topPosition < spots.size()) {
                String swipedUserId = spots.get(previousPosition).getUserId();
                setWaitedUserIds(swipedUserId);
            } else {
                // Handle the case where topPosition is out of bounds
                Log.e("onCardSwiped", "Top position is out of bounds");
            }
        }

    }

    @Override
    public void onCardRewound() {
        Log.d("CardStackView", "onCardRewound: " + manager.getTopPosition());
    }

    @Override
    public void onCardCanceled() {
        Log.d("CardStackView", "onCardCanceled: " + manager.getTopPosition());
    }

    @Override
    public void onCardAppeared(View view, int position) {
        TextView textView = view.findViewById(R.id.item_username);
        Log.d("CardStackView", "onCardAppeared: (" + position + ") " + textView.getText());
    }

    @Override
    public void onCardDisappeared(View view, int position) {
        TextView textView = view.findViewById(R.id.item_username);
        Log.d("CardStackView", "onCardDisappeared: (" + position + ") " + textView.getText());
    }

    private void setupCardStackView(View view) {
        initialize(view);
    }

    private void setupButton(View view) {
        View skip = view.findViewById(R.id.btn_skip);
        skip.setOnClickListener(v -> {
            SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                    .setDirection(Direction.Left)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(new AccelerateInterpolator())
                    .build();
            manager.setSwipeAnimationSetting(setting);
            cardStackView.swipe();
        });

        View rewind = view.findViewById(R.id.btn_profile);
        rewind.setOnClickListener(v -> {
            RewindAnimationSetting setting = new RewindAnimationSetting.Builder()
                    .setDirection(Direction.Bottom)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(new DecelerateInterpolator())
                    .build();
            manager.setRewindAnimationSetting(setting);
            cardStackView.rewind();
        });

        View like = view.findViewById(R.id.btn_like);
        like.setOnClickListener(v -> {
            SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                    .setDirection(Direction.Right)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(new AccelerateInterpolator())
                    .build();
            manager.setSwipeAnimationSetting(setting);
            cardStackView.swipe();
        });
    }

    private void initialize(View view) {
        manager = new CardStackLayoutManager(requireContext(), this);
        manager.setStackFrom(StackFrom.None);
        manager.setVisibleCount(3);
        manager.setTranslationInterval(8.0f);
        manager.setScaleInterval(0.95f);
        manager.setSwipeThreshold(0.3f);
        manager.setMaxDegree(20.0f);
        manager.setDirections(Direction.HORIZONTAL);
        manager.setCanScrollHorizontal(true);
        manager.setCanScrollVertical(true);
        manager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual);
        manager.setOverlayInterpolator(new LinearInterpolator());

        cardStackView = view.findViewById(R.id.card_stack_view);
        cardStackView.setLayoutManager(manager);

        // Use createSpots with a callback to handle the asynchronous result
        createSpots(new SpotsCallback() {
            @Override
            public void onSpotsReady(List<Spot> spots) {
                if (isAdded()) {
                    adapter = new CardStackAdapter(spots);
                    cardStackView.setAdapter(adapter);

                    RecyclerView.ItemAnimator itemAnimator = cardStackView.getItemAnimator();
                    if (itemAnimator instanceof DefaultItemAnimator) {
                        ((DefaultItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
                    }
                }
            }
        });
    }


    public void createSpots(SpotsCallback callback) {
        List<Spot> spots = new ArrayList<>();

        FireBaseUtil.currentUserData().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot userDocument = task.getResult();
                if (userDocument.exists()) {
                    currentUserData = userDocument.toObject(UserData.class);
                    Query query = FireBaseUtil.allUserCollectionUserData()
                            .whereEqualTo("gender", currentUserData.getPreferredGender());

                    query.get().addOnCompleteListener(queryTask -> {
                        if (queryTask.isSuccessful()) {
                            for (QueryDocumentSnapshot otherDocument : queryTask.getResult()) {
                                UserData otherUserData = otherDocument.toObject(UserData.class);
                                String otherUserId = otherUserData.getUid();
                                isUserInLists(otherUserId, new UserListsCallback() {
                                    @Override
                                    public void onListsChecked(boolean isInLists) {
                                        // Use the result, for example:
                                        if (!isInLists) {
                                            FireBaseUtil.getOtherFacePicStorageReference(otherUserId).getDownloadUrl()
                                                    .addOnCompleteListener(uriTask -> {
                                                        if (uriTask.isSuccessful()) {
                                                            uri = uriTask.getResult();

                                                            spots.add(new Spot(otherUserId, otherUserData.getUsername(),
                                                                    String.valueOf(otherUserData.getAge()),
                                                                    String.valueOf(otherUserData.getLocation()),
                                                                    uri.toString()));

                                                            callback.onSpotsReady(spots);
                                                        } else {
                                                            Log.e("createSpots", "Failed to get image");
                                                            callback.onSpotsReady(Collections.emptyList());
                                                        }
                                                    });
                                        }
                                    }
                                });

                            }
                        } else {
                            Log.e("createSpots", "Failed to get user data from query");
                            callback.onSpotsReady(Collections.emptyList());
                        }
                    });
                } else {
                    Log.e("createSpots", "Empty currentUserData");
                    callback.onSpotsReady(Collections.emptyList());
                }
            } else {
                Log.e("createSpots", "Failed to get currentUserData");
                callback.onSpotsReady(Collections.emptyList());
            }
        });
    }

    private void isUserInLists(String userId, UserListsCallback callback) {
        currentUserCheckedList = null;
        currentUserWaitList = null;

        FireBaseUtil.currentUserCheckedUserList().get().addOnCompleteListener(checkedListTask -> {
            if (checkedListTask.isSuccessful()) {
                CheckedUser temp = checkedListTask.getResult().toObject(CheckedUser.class);
                currentUserCheckedList = temp;
            }

            // Continue with the next asynchronous operation
            FireBaseUtil.currentUserWaitList().get().addOnCompleteListener(waitListTask -> {
                if (waitListTask.isSuccessful()) {
                    WaitUser temp = waitListTask.getResult().toObject(WaitUser.class);
                    currentUserWaitList = temp;
                }

                // Check if currentUserWaitList is not null before using it
                boolean isInWaitList =  currentUserWaitList.containsUserId(userId);
                boolean isCheckedList = currentUserCheckedList.containsUserId(userId);

                // Callback with the result
                callback.onListsChecked(isInWaitList || isCheckedList);
            });
        });
    }

    // Define a callback interface
    interface UserListsCallback {
        void onListsChecked(boolean isInLists);
    }


    interface SpotsCallback {
        void onSpotsReady(List<Spot> spots);
    }

    public void setChekedUserIds(String swipedUserId) {
        FireBaseUtil.currentUserCheckedUserList().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                CheckedUser temp = task.getResult().toObject(CheckedUser.class);

                currentUserCheckedList = temp;
                currentUserCheckedList.addToUserIds(swipedUserId);

                // Update the CheckedUser list in Firestore
                FireBaseUtil.currentUserCheckedUserList().set(currentUserCheckedList)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("onCardSwiped", "User ID added to CheckedList");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("onCardSwiped", "Failed to update CheckedList in Firestore", e);
                        });
            } else {
                // Handle the case where the top position is out of bounds
                Log.e("setChekedUserIds", "Top position is out of bounds");
            }

        });
    }


    void setWaitedUserIds(String otherUserID) {
        // Get the ID of the user being swiped
        FireBaseUtil.otherUserWaitUserList(otherUserID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WaitUser otherUserWaitUserList = task.getResult().toObject(WaitUser.class);
                if (otherUserWaitUserList == null) {
                    otherUserWaitUserList = new WaitUser();
                }
                otherUserWaitUserList.addToUserIds(FireBaseUtil.getUserID());

                // Update the user IDs list in Firestore
                FireBaseUtil.otherUserWaitUserList(otherUserID).set(otherUserWaitUserList)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("onCardSwiped", "User ID added to wait user");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("onCardSwiped", "Failed to update");
                        });
            }
        });
    }
}

















/*
package com.example.blurdatingapplication.manualmatching;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blurdatingapplication.R;

import com.example.blurdatingapplication.data.CheckedUser;
import com.example.blurdatingapplication.data.UserData;
import com.example.blurdatingapplication.data.WaitUser;
import com.example.blurdatingapplication.utils.FireBaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.RewindAnimationSetting;

import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting;
import com.yuyakaido.android.cardstackview.SwipeableMethod;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExploreFragment extends Fragment implements CardStackListener {

    UserData currentUserData;

    private DrawerLayout drawerLayout;
    private CardStackView cardStackView;
    private CardStackLayoutManager manager;
    private CardStackAdapter adapter;

    Uri uri;

    FirebaseAuth auth;
    FirebaseUser currentUser;

    CheckedUser currentUserCheckedList;

    WaitUser currentUserWaitList;



    public ExploreFragment() {
        //Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        createSpots(new SpotsCallback() {
            @Override
            public void onSpotsReady(List<Spot> spots) {
                if (isAdded()) {
                    //paginate(spots);
                }
            }
        });

        setupCardStackView(view);
        setupButton(view);
        return view;
    }


    @Override
    public void onCardDragging(Direction direction, float ratio) {
        Log.d("CardStackView", "onCardDragging: d = " + direction.name() + ", r = " + ratio);
    }

    @Override
    public void onCardSwiped(Direction direction) {
        Log.d("CardStackView", "onCardSwiped: p = " + manager.getTopPosition() + ", d = " + direction);
        int topPosition = manager.getTopPosition();
        int previousPosition = topPosition - 1;
        List<Spot> spots = adapter.getSpots();

        if (topPosition == spots.size() - 1) {
            // Disable swiping when the last card is reached
            manager.setSwipeableMethod(SwipeableMethod.None);
        }


        if(direction == Direction.Left || direction == Direction.Right){
            if (topPosition >= 0 && topPosition < spots.size()) {
                // Get the swiped user ID from the spot at the top position
                String swipedUserId = spots.get(previousPosition).getUserId();
                setChekedUserIds(swipedUserId);
            }
        }

        if (direction == Direction.Right) {
            if (topPosition >= 0 && topPosition < spots.size()) {
                String swipedUserId = spots.get(previousPosition).getUserId();
                setWaitedUserIds(swipedUserId);
            } else {
                // Handle the case where topPosition is out of bounds
                Log.e("onCardSwiped", "Top position is out of bounds");
            }
        }

    }

    @Override
    public void onCardRewound() {
        Log.d("CardStackView", "onCardRewound: " + manager.getTopPosition());
    }

    @Override
    public void onCardCanceled() {
        Log.d("CardStackView", "onCardCanceled: " + manager.getTopPosition());
    }

    @Override
    public void onCardAppeared(View view, int position) {
        TextView textView = view.findViewById(R.id.item_username);
        Log.d("CardStackView", "onCardAppeared: (" + position + ") " + textView.getText());
    }

    @Override
    public void onCardDisappeared(View view, int position) {
        TextView textView = view.findViewById(R.id.item_username);
        Log.d("CardStackView", "onCardDisappeared: (" + position + ") " + textView.getText());
    }

    private void setupCardStackView(View view) {
        initialize(view);
    }

    private void setupButton(View view) {
        View skip = view.findViewById(R.id.btn_skip);
        skip.setOnClickListener(v -> {
            SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                    .setDirection(Direction.Left)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(new AccelerateInterpolator())
                    .build();
            manager.setSwipeAnimationSetting(setting);
            cardStackView.swipe();
        });

        View rewind = view.findViewById(R.id.btn_profile);
        rewind.setOnClickListener(v -> {
            RewindAnimationSetting setting = new RewindAnimationSetting.Builder()
                    .setDirection(Direction.Bottom)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(new DecelerateInterpolator())
                    .build();
            manager.setRewindAnimationSetting(setting);
            cardStackView.rewind();
        });

        View like = view.findViewById(R.id.btn_like);
        like.setOnClickListener(v -> {
            SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                    .setDirection(Direction.Right)
                    .setDuration(Duration.Normal.duration)
                    .setInterpolator(new AccelerateInterpolator())
                    .build();
            manager.setSwipeAnimationSetting(setting);
            cardStackView.swipe();
        });
    }

    private void initialize(View view) {
        manager = new CardStackLayoutManager(requireContext(), this);
        manager.setStackFrom(StackFrom.None);
        manager.setVisibleCount(3);
        manager.setTranslationInterval(8.0f);
        manager.setScaleInterval(0.95f);
        manager.setSwipeThreshold(0.3f);
        manager.setMaxDegree(20.0f);
        manager.setDirections(Direction.HORIZONTAL);
        manager.setCanScrollHorizontal(true);
        manager.setCanScrollVertical(true);
        manager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual);
        manager.setOverlayInterpolator(new LinearInterpolator());

        cardStackView = view.findViewById(R.id.card_stack_view);
        cardStackView.setLayoutManager(manager);

        // Use createSpots with a callback to handle the asynchronous result
        createSpots(new SpotsCallback() {
            @Override
            public void onSpotsReady(List<Spot> spots) {
                if (isAdded()) {
                    adapter = new CardStackAdapter(spots);
                    cardStackView.setAdapter(adapter);

                    RecyclerView.ItemAnimator itemAnimator = cardStackView.getItemAnimator();
                    if (itemAnimator instanceof DefaultItemAnimator) {
                        ((DefaultItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
                    }
                }
            }
        });
    }



    public void createSpots(SpotsCallback callback) {
        List<Spot> spots = new ArrayList<>();

        FireBaseUtil.currentUserData().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot userDocument = task.getResult();
                if (userDocument.exists()) {
                    currentUserData = userDocument.toObject(UserData.class);
                    Query query = FireBaseUtil.allUserCollectionUserData()
                            .whereEqualTo("gender", currentUserData.getPreferredGender());

                    query.get().addOnCompleteListener(queryTask -> {
                        if (queryTask.isSuccessful()) {
                            for (QueryDocumentSnapshot otherDocument : queryTask.getResult()) {
                                UserData otherUserData = otherDocument.toObject(UserData.class);
                                String otherUserId = otherUserData.getUid();

                                if (!isUserInLists(otherUserId)) {
                                    FireBaseUtil.getOtherFacePicStorageReference(otherUserId).getDownloadUrl()
                                            .addOnCompleteListener(uriTask -> {
                                                if (uriTask.isSuccessful()) {
                                                    uri = uriTask.getResult();

                                                    spots.add(new Spot(otherUserId, otherUserData.getUsername(), String.valueOf(otherUserData.getAge()), String.valueOf(otherUserData.getLocation()), uri.toString()));
                                                    if (spots.size() == queryTask.getResult().size()) {
                                                        spots.add(new Spot("","","","",""));
                                                    }

                                                    callback.onSpotsReady(spots);
                                                } else {
                                                    Log.e("createSpots", "Failed to get image URL");
                                                    callback.onSpotsReady(Collections.emptyList());
                                                }
                                            });
                                }
                            }
                        } else {
                            Log.e("createSpots", "Failed to get user data from query");
                            callback.onSpotsReady(Collections.emptyList());
                        }
                    });
                } else {
                    Log.e("createSpots", "Empty currentUserData");
                    callback.onSpotsReady(Collections.emptyList());
                }
            } else {
                Log.e("createSpots", "Failed to get currentUserData");
                callback.onSpotsReady(Collections.emptyList());
            }
        });
    }

    private boolean isUserInLists(String userId) {
        FireBaseUtil.currentUserCheckedUserList().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                CheckedUser temp = task.getResult().toObject(CheckedUser.class);
                currentUserCheckedList = temp;
            }
            // Check if currentUserWaitList is not null before using i
        });
        FireBaseUtil.currentUserWaitList().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WaitUser temp = task.getResult().toObject(WaitUser.class);
                currentUserWaitList = temp;
            }
        });
        return (currentUserWaitList != null && currentUserWaitList.containsUserId(userId))||( (currentUserCheckedList != null && currentUserCheckedList.containsUserId(userId)));
    }



    interface SpotsCallback {
        void onSpotsReady(List<Spot> spots);
    }

    public void setChekedUserIds(String swipedUserId) {
        FireBaseUtil.currentUserCheckedUserList().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                CheckedUser temp = task.getResult().toObject(CheckedUser.class);

                currentUserCheckedList = temp;
                currentUserCheckedList.addToUserIds(swipedUserId);

                    // Update the CheckedUser list in Firestore
                    FireBaseUtil.currentUserCheckedUserList().set(currentUserCheckedList)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("onCardSwiped", "User ID added to CheckedList");
                            })
                            .addOnFailureListener(e -> {
                                Log.e("onCardSwiped", "Failed to update CheckedList in Firestore", e);
                            });
                } else {
                    // Handle the case where the top position is out of bounds
                    Log.e("setChekedUserIds", "Top position is out of bounds");
                }

        });
    }


    void setWaitedUserIds(String otherUserID) {
        // Get the ID of the user being swiped
        FireBaseUtil.otherUserWaitUserList(otherUserID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                WaitUser otherUserWaitUserList = task.getResult().toObject(WaitUser.class);
                if (otherUserWaitUserList == null) {
                    otherUserWaitUserList = new WaitUser();
                }
                otherUserWaitUserList.addToUserIds(FireBaseUtil.getUserID());

                // Update the user IDs list in Firestore
                FireBaseUtil.otherUserWaitUserList(otherUserID).set(otherUserWaitUserList)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("onCardSwiped", "User ID added to wait user");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("onCardSwiped", "Failed to update", e);
                        });
            }
        });
    }
}
*/