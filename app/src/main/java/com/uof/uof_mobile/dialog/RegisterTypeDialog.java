package com.uof.uof_mobile.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;

import com.uof.uof_mobile.R;
import com.uof.uof_mobile.other.Global;

public class RegisterTypeDialog extends Dialog {
    private final Context context;
    private final RegisterTypeDialogListener registerTypeDialogListener;
    private AppCompatImageButton btnRegisterTypeCustomer;
    private AppCompatImageButton btnRegisterTypeUofPartner;
    private AppCompatButton btnRegisterTypeCancel;

    public RegisterTypeDialog(@NonNull Context context, boolean canceledOnTouchOutside, boolean cancelable
            , RegisterTypeDialogListener registerTypeDialogListener) {
        super(context);
        this.context = context;
        this.registerTypeDialogListener = registerTypeDialogListener;

        setCanceledOnTouchOutside(canceledOnTouchOutside);
        setCancelable(cancelable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_registertype);

        init();
    }

    @Override
    public void dismiss() {
        Global.dialogs.remove(this);
        super.dismiss();
    }

    private void init() {
        for(Dialog dialog : Global.dialogs){
            if(dialog instanceof RegisterTypeDialog){
                dialog.dismiss();
            }
        }
        Global.dialogs.add(this);

        btnRegisterTypeCustomer = findViewById(R.id.btn_registertype_customer);
        btnRegisterTypeUofPartner = findViewById(R.id.btn_registertype_uofpartner);
        btnRegisterTypeCancel = findViewById(R.id.btn_registertype_cancel);

        btnRegisterTypeCustomer.setOnClickListener(view -> {
            this.registerTypeDialogListener.onCustomerClick();
            dismiss();
        });

        btnRegisterTypeUofPartner.setOnClickListener(view -> {
            this.registerTypeDialogListener.onUofPartnerClick();
            dismiss();
        });

        btnRegisterTypeCancel.setOnClickListener(view -> {
            this.registerTypeDialogListener.onCancelClick();
            dismiss();
        });

        getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawableResource(R.drawable.border_registertypedialog);
    }

    public interface RegisterTypeDialogListener {
        void onCustomerClick();

        void onUofPartnerClick();

        void onCancelClick();
    }
}
