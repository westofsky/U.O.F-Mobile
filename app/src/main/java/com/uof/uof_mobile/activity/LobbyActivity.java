package com.uof.uof_mobile.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.uof.uof_mobile.R;
import com.uof.uof_mobile.adapter.WaitingOrderAdapter;
import com.uof.uof_mobile.dialog.CheckPwDialog;
import com.uof.uof_mobile.dialog.WaitingOrderInfoDialog;
import com.uof.uof_mobile.item.WaitingOrderItem;
import com.uof.uof_mobile.manager.SQLiteManager;
import com.uof.uof_mobile.other.Global;

public class LobbyActivity extends AppCompatActivity {
    private AppCompatImageView ivLobbyRecognizeQr;
    private AppCompatImageButton ibtnLobbyLeft;
    private RecyclerView rvLobbyWaitingOrder;
    private AppCompatImageButton ibtnLobbyRight;
    private ConstraintLayout clLobbyCard;
    private ConstraintLayout clLobbyOrderList;
    private ConstraintLayout clLobbySetting;
    private WaitingOrderAdapter waitingOrderAdapter;
    private SQLiteManager sqLiteManager;
    private boolean isInitDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        init();
    }

    @Override
    protected void onDestroy() {
        Global.activities.remove(this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isInitDone) {
            updateList();
        }
    }

    private void init() {
        for(Activity activity : Global.activities){
            if(activity instanceof LobbyActivity){
                activity.finish();
            }
        }
        Global.activities.add(this);

        ivLobbyRecognizeQr = findViewById(R.id.iv_lobby_recognizeqr);
        ibtnLobbyLeft = findViewById(R.id.ibtn_lobby_left);
        rvLobbyWaitingOrder = findViewById(R.id.rv_lobby_waitingorder);
        ibtnLobbyRight = findViewById(R.id.ibtn_lobby_right);
        clLobbyCard = findViewById(R.id.cl_lobby_card);
        clLobbyOrderList = findViewById(R.id.cl_lobby_orderlist);
        clLobbySetting = findViewById(R.id.cl_lobby_setting);

        sqLiteManager = new SQLiteManager(LobbyActivity.this);
        waitingOrderAdapter = new WaitingOrderAdapter(LobbyActivity.this);

        rvLobbyWaitingOrder.setLayoutManager(new LinearLayoutManager(LobbyActivity.this, LinearLayoutManager.HORIZONTAL, false));
        new PagerSnapHelper().attachToRecyclerView(rvLobbyWaitingOrder);
        rvLobbyWaitingOrder.setAdapter(waitingOrderAdapter);

        isInitDone = true;

        updateList();

        // QR 인식 버튼이 눌렸을 경우
        ivLobbyRecognizeQr.setOnClickListener(view -> {
            Intent intent = new Intent(LobbyActivity.this, QRRecognitionActivity.class);
            startActivity(intent);
        });

        // 좌측 버튼이 눌렸을 경우
        ibtnLobbyLeft.setOnClickListener(view -> {
            int currentPosition = ((LinearLayoutManager) rvLobbyWaitingOrder.getLayoutManager()).findFirstVisibleItemPosition();
            if (currentPosition != 0) {
                rvLobbyWaitingOrder.smoothScrollToPosition(currentPosition - 1);
            }
        });

        // 리스트가 스크롤 될 경우
        rvLobbyWaitingOrder.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                ibtnLobbyLeft.setVisibility(View.VISIBLE);
                ibtnLobbyRight.setVisibility(View.VISIBLE);
                if (waitingOrderAdapter.getItemCount() < 2) {
                    ibtnLobbyLeft.setVisibility(View.INVISIBLE);
                    ibtnLobbyRight.setVisibility(View.INVISIBLE);
                } else if (((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition() == 0) {
                    ibtnLobbyLeft.setVisibility(View.INVISIBLE);
                } else if (((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition() == waitingOrderAdapter.getItemCount() - 1) {
                    ibtnLobbyRight.setVisibility(View.INVISIBLE);
                }
            }
        });

        // 주문대기 목록 아이템이 눌렸을 경우
        waitingOrderAdapter.setOnItemClickListener((view, position) -> {
            WaitingOrderInfoDialog waitingOrderInfoDialog = new WaitingOrderInfoDialog(LobbyActivity.this, false, true, waitingOrderAdapter.getItem(position));
            waitingOrderInfoDialog.setOnDismissListener(dialogInterface -> {
                updateList();
            });
            waitingOrderInfoDialog.show();
        });

        // 우측 버튼이 눌렸을 경우
        ibtnLobbyRight.setOnClickListener(view -> {
            int currentPosition = ((LinearLayoutManager) rvLobbyWaitingOrder.getLayoutManager()).findLastVisibleItemPosition();
            if (currentPosition < waitingOrderAdapter.getItemCount() - 1) {
                rvLobbyWaitingOrder.smoothScrollToPosition(currentPosition + 1);
            }
        });

        // 카드관리 버튼이 눌렸을 경우
        clLobbyCard.setOnClickListener(view -> {
            Intent intent = new Intent(LobbyActivity.this, CardActivity.class);
            startActivity(intent);
        });

        // 주문내역 버튼이 눌렸을 경우
        clLobbyOrderList.setOnClickListener(view -> {
            Intent intent = new Intent(LobbyActivity.this, OrderListActivity.class);
            startActivity(intent);
        });

        // 설정 버튼이 눌렸을 경우
        clLobbySetting.setOnClickListener(view -> {
            Intent intent = new Intent(LobbyActivity.this, SettingActivity.class);
            startActivity(intent);
        });

        Intent lobbyActivityIntent = getIntent();
        if (lobbyActivityIntent.getStringExtra("targetIp") != null) {
            Intent intent = new Intent(LobbyActivity.this, QRRecognitionActivity.class);
            intent.putExtra("targetIp", lobbyActivityIntent.getStringExtra("targetIp"));
            intent.putExtra("targetPort", lobbyActivityIntent.getStringExtra("targetPort"));
            startActivity(intent);
        }

        if (lobbyActivityIntent.getStringExtra("orderNumber") != null) {
            WaitingOrderItem waitingOrderItem = waitingOrderAdapter.getItemByOrderNumber(lobbyActivityIntent.getStringExtra("orderNumber"));
            if (waitingOrderItem == null) {
                Toast.makeText(LobbyActivity.this, "번호가 " + lobbyActivityIntent.getStringExtra("orderNumber") + "인 주문이 없습니다", Toast.LENGTH_SHORT).show();
            } else {
                WaitingOrderInfoDialog waitingOrderInfoDialog = new WaitingOrderInfoDialog(LobbyActivity.this, false, true, waitingOrderItem);
                waitingOrderInfoDialog.setOnDismissListener(dialogInterface -> {
                    updateList();
                });
                waitingOrderInfoDialog.show();
            }
        }
    }

    public void updateList() {
        sqLiteManager.openDatabase();
        waitingOrderAdapter.updateItem(sqLiteManager.loadOrder());
        sqLiteManager.closeDatabase();

        ibtnLobbyLeft.setVisibility(View.VISIBLE);
        ibtnLobbyRight.setVisibility(View.VISIBLE);
        if (waitingOrderAdapter.getItemCount() < 2) {
            ibtnLobbyLeft.setVisibility(View.INVISIBLE);
            ibtnLobbyRight.setVisibility(View.INVISIBLE);
        } else if (((LinearLayoutManager) rvLobbyWaitingOrder.getLayoutManager()).findFirstVisibleItemPosition() == 0) {
            ibtnLobbyLeft.setVisibility(View.INVISIBLE);
        } else if (((LinearLayoutManager) rvLobbyWaitingOrder.getLayoutManager()).findLastVisibleItemPosition() == waitingOrderAdapter.getItemCount() - 1) {
            ibtnLobbyRight.setVisibility(View.INVISIBLE);
        }
    }

    public void moveToOrderNumber(int orderNumber) {
        int position = 0;
        for (WaitingOrderItem waitingOrderItem : waitingOrderAdapter.getWaitingOrderItemArrayList()) {
            if (waitingOrderItem.getOrderNumber() == orderNumber) {
                rvLobbyWaitingOrder.smoothScrollToPosition(position);
                break;
            }
            position++;
        }
    }
}