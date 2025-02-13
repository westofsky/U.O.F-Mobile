package com.uof.uof_mobile.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.textfield.TextInputLayout;
import com.uof.uof_mobile.R;
import com.uof.uof_mobile.manager.HttpManager;
import com.uof.uof_mobile.manager.PatternManager;
import com.uof.uof_mobile.other.Global;

import org.json.JSONObject;

public class ChangePhoneDialog extends Dialog {
    private final Context context;
    private AppCompatImageButton ibtnDlgChangePhoneClose;
    private AppCompatTextView tvDlgChangePhoneCurrentPhone;
    private TextInputLayout tilDlgChangePhoneChangePhone;
    private TextView tvDlgChangePhoneApply;

    public ChangePhoneDialog(@NonNull Context context, boolean canceledOnTouchOutside, boolean cancelable) {
        super(context, R.style.DialogTheme_FullScreenDialog);
        this.context = context;
        setCanceledOnTouchOutside(canceledOnTouchOutside);
        setCancelable(cancelable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog_changephone);
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        getWindow().setWindowAnimations(R.style.Anim_FullScreenDialog);

        init();
    }

    @Override
    public void dismiss() {
        Global.dialogs.remove(this);
        super.dismiss();
    }

    private void init() {
        for(Dialog dialog : Global.dialogs){
            if(dialog instanceof ChangePhoneDialog){
                dialog.dismiss();
            }
        }
        Global.dialogs.add(this);
        ibtnDlgChangePhoneClose = findViewById(R.id.ibtn_dlgchangephone_close);
        tvDlgChangePhoneCurrentPhone = findViewById(R.id.tv_dlgchangephone_currentphone);
        tilDlgChangePhoneChangePhone = findViewById(R.id.til_dlgchangephone_changephone);
        tvDlgChangePhoneApply = findViewById(R.id.tv_dlgchangephone_apply);

        tvDlgChangePhoneCurrentPhone.setText(Global.User.phone);
        tvDlgChangePhoneApply.setTextColor(context.getResources().getColor(R.color.color_light));
        tvDlgChangePhoneApply.setEnabled(false);

        ibtnDlgChangePhoneClose.setOnClickListener(view -> {
            dismiss();
        });

        tilDlgChangePhoneChangePhone.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                int result = PatternManager.checkPhoneNumber(editable.toString());

                if (result == Global.Pattern.LENGTH_SHORT || result == Global.Pattern.NOT_ALLOWED_CHARACTER) {
                    tilDlgChangePhoneChangePhone.setError("전화번호 형식이 맞지 않습니다");
                    tilDlgChangePhoneChangePhone.setErrorEnabled(true);
                    tvDlgChangePhoneApply.setTextColor(context.getResources().getColor(R.color.color_light));
                    tvDlgChangePhoneApply.setEnabled(false);
                } else {
                    tilDlgChangePhoneChangePhone.setError(null);
                    tilDlgChangePhoneChangePhone.setErrorEnabled(false);
                    tvDlgChangePhoneApply.setTextColor(context.getResources().getColor(R.color.black));
                    tvDlgChangePhoneApply.setEnabled(true);
                }
            }
        });
        tvDlgChangePhoneApply.setOnClickListener(view -> {
            try {
                JSONObject sendData = new JSONObject();
                sendData.put("request_code", Global.Network.Request.CHANGE_PHONE);

                JSONObject message = new JSONObject();
                message.accumulate("id", Global.User.id);
                message.accumulate("change_phone", tilDlgChangePhoneChangePhone.getEditText().getText().toString());
                message.accumulate("type", Global.User.type);

                sendData.accumulate("message", message);

                JSONObject recvData = new JSONObject(new HttpManager().execute(new String[]{Global.Network.EXTERNAL_SERVER_URL, sendData.toString()}).get());

                String responseCode = recvData.getString("response_code");

                if (responseCode.equals(Global.Network.Response.CHANGE_PHONE_SUCCESS)) {
                    // 전화번호 변경 성공
                    Global.User.phone = tilDlgChangePhoneChangePhone.getEditText().getText().toString();
                    Toast.makeText(context, "변경되었습니다", Toast.LENGTH_SHORT).show();
                } else if (responseCode.equals(Global.Network.Response.CHANGE_PHONE_FAILED)) {
                    // 전화번호 변경 실패
                    Toast.makeText(context, "전화번호 변경 실패: " + recvData.getString("message"), Toast.LENGTH_SHORT).show();
                } else if (responseCode.equals(Global.Network.Response.SERVER_NOT_ONLINE)) {
                    // 서버 연결 실패
                    Toast.makeText(context, "서버 점검 중입니다", Toast.LENGTH_SHORT).show();
                } else {
                    // 전화번호 변경 실패 - 기타 오류
                    Toast.makeText(context, "전화번호 변경 실패(기타): " + recvData.getString("message"), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            }

            dismiss();
        });
    }
}
