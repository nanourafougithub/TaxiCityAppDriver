package example.com.taxicityappdriver.controller.fragments;


import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import example.com.taxicityappdriver.R;
import example.com.taxicityappdriver.controller.activities.AuthActivity;
import example.com.taxicityappdriver.controller.activities.MainActivity;
import example.com.taxicityappdriver.entities.test;
import example.com.taxicityappdriver.model.backend.ActionCallBack;
import example.com.taxicityappdriver.model.backend.BackEnd;
import example.com.taxicityappdriver.model.backend.BackEndFactory;
import example.com.taxicityappdriver.model.helpers.Helpers;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class SinginFragment extends Fragment {
    private EditText emailEditText;
    private EditText passwordEditText;
    private TextView helperTextView;
    private EditText emailForgotPassword;
    private static BackEnd db = BackEndFactory.getInstance();
    private AlertDialog alertDialog;


    private static final String PREFS = "PREFS";
    private static final String PREFS_EMAIL = "PREFS_EMAIL";
    private static final String PREFS_PASSWORD = "PREFS_PASSWORD";


    private Button btnSubmit;
    SharedPreferences sharedPreferences; //BUG !
    private SignUpFragment signUpFragment = null;

    public SinginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_singin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (db.isSigned())
            initMainActivity();

        //test.insertData();

        //Init Click listener
        btnSubmit = view.findViewById(R.id.submit_sign_in_btn_form);
        LinearLayout joinUsLinearLayout = view.findViewById(R.id.join_us_btn);
        TextView forgotPasswordTextView = view.findViewById(R.id.forgot_password);
        btnSubmit.setOnClickListener(submitListener());
        forgotPasswordTextView.setOnClickListener(forgotPasswordListener());
        joinUsLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configSignUpFragment();
            }
        });

        emailEditText = view.findViewById(R.id.email_input_form);
        passwordEditText = view.findViewById(R.id.password_input_form);
        helperTextView = view.findViewById(R.id.helper_form);
        emailForgotPassword = new EditText(getContext());
        alertDialog = new AlertDialog.Builder(getContext()).create();

        //A tester apres signout
        sharedPreferences = getActivity().getSharedPreferences(PREFS, getContext().MODE_PRIVATE);

        if (sharedPreferences != null) {
            //Restaure old values
            if (sharedPreferences.contains(PREFS_EMAIL) && sharedPreferences.contains(PREFS_PASSWORD)) {
                emailEditText.setText(sharedPreferences.getString(PREFS_EMAIL, null));
                passwordEditText.setText(sharedPreferences.getString(PREFS_PASSWORD, null));
            }
        }


    }


    private View.OnClickListener submitListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (!isValid())
                  //  return;

                btnSubmit.setEnabled(false);
                try {

                    db.signIn(emailEditText.getText().toString(), passwordEditText.getText().toString(), new ActionCallBack<Object>() {
                        @Override
                        public void onSuccess(Object obj) {
                            initMainActivity();
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            helperTextView.setVisibility(View.VISIBLE);
                            helperTextView.setText(exception.getMessage());
                            btnSubmit.setEnabled(true);
                            sharedPreferences
                                    .edit()
                                    .putString(PREFS_EMAIL, emailEditText.getText().toString())
                                    .putString(PREFS_PASSWORD, passwordEditText.getText().toString())
                                    .apply();
                        }

                        @Override
                        public void onProgress(String status, double percent) {

                        }
                    });
                }
                catch (Exception e){
                    helperTextView.setVisibility(View.VISIBLE);
                    helperTextView.setText(e.getMessage());
                    btnSubmit.setEnabled(true);
                }


            }
        };

    }


    private View.OnClickListener forgotPasswordListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogBuilder();

            }
        };

    }

    private boolean isValid() {
        //HELPER
        if (emailEditText == null || passwordEditText == null) {
            helperTextView.setVisibility(View.VISIBLE);
            helperTextView.setText("Please fill in all the required fields.");
            return false;
        }

        if (TextUtils.isEmpty(emailEditText.getText()) || TextUtils.isEmpty(passwordEditText.getText())) {
            helperTextView.setVisibility(View.VISIBLE);
            helperTextView.setText("Please fill in all the required fields.");
            return false;
        }
        if (!Helpers.isValidEmail(emailEditText.getText().toString())) {
            helperTextView.setVisibility(View.VISIBLE);
            helperTextView.setText("Please enter a valid email address.");
            return false;
        }

        return true;
    }

    private void initMainActivity() {

        Intent intent = new Intent(getContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); //reset backstack
        startActivity(intent);
        if (getActivity() != null)
            getActivity().finish();
    }

    //Dialog forgot password
    private void dialogBuilder() {


        FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);


        alertDialog.setTitle("Forgot Password");
        alertDialog.setMessage("Enter email to reset password ");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Reset Password !",
                new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int which) {

                        //Valid form
                        if (TextUtils.isEmpty(emailForgotPassword.getText()) || !Helpers.isValidEmail(emailForgotPassword.getText().toString())) {
                            helperTextView.setVisibility(View.VISIBLE);
                            helperTextView.setText("Please enter a valid email address.");
                            return;
                        }
                        int i = 2;

                        db.forgotPassword(emailForgotPassword.getText().toString(), new ActionCallBack() {
                            @Override
                            public void onSuccess(Object obj) {
                                helperTextView.setVisibility(View.VISIBLE);
                                helperTextView.setTextColor(Color.GREEN);
                                helperTextView.setText("Reset Password Email sent !");
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailure(Exception exception) {
                                helperTextView.setVisibility(View.VISIBLE);
                                helperTextView.setText(exception.getMessage());

                            }

                            @Override
                            public void onProgress(String status, double percent) {

                            }
                        });


                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        emailForgotPassword.setLayoutParams(lp2);
        alertDialog.setView(emailForgotPassword);
        alertDialog.show();

    }

    private void configSignUpFragment() {

        //Singleton pattern of the fragment.
        if (signUpFragment == null)
            signUpFragment = new SignUpFragment();

        //Get Fragment manager from the parent
        FragmentManager fm = getActivity().getSupportFragmentManager();

        //Adding Welcome Fragment
        if (!signUpFragment.isVisible()) {
            fm.beginTransaction().remove(fm.findFragmentById(R.id.main_frame_layout)).commit();
            fm.beginTransaction().replace(R.id.main_frame_layout, signUpFragment).commit();
        }

    }


}
