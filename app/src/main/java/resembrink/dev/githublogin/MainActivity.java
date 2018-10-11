package resembrink.dev.githublogin;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GithubAuthProvider;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Button btnGit;
    private TextView txtGit;
    private ImageView imgGit;
    private WebView webView;

    private LinearLayout llGit;


    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;

    private static final String REDIRECT_URL_CALLBACK = "https://githubconexion.firebaseapp.com/__/auth/handler";

    private final String TAG = getClass().getName();
    private boolean signed;

    private SecureRandom random = new SecureRandom();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        llGit = (LinearLayout) findViewById(R.id.llGit);
        imgGit = (ImageView) findViewById(R.id.imgGit);
        txtGit = (TextView) findViewById(R.id.txtGit);
        btnGit = (Button) findViewById(R.id.btnGit);


        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    signed = true;
                    llGit.setVisibility(View.INVISIBLE);
                    txtGit.setText(user.getDisplayName() + "\n" + user.getEmail());
                    Picasso.with(MainActivity.this).load(user.getPhotoUrl()).into(imgGit);
                    btnGit.setText("signOut");
                    Toast.makeText(MainActivity.this, "Sign In", Toast.LENGTH_SHORT).show();

                } else {
                    signed = false;
                    llGit.setVisibility(View.GONE);
                    btnGit.setText("Sign In");
                    Toast.makeText(MainActivity.this, "Signed Out", Toast.LENGTH_SHORT).show();
                }
            }
        };


        webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("https://githubconexion.firebaseapp.com/__/auth/handler")) {
                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");
                    String state = uri.getQueryParameter("state");
                    sendPost(code, state);
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        Uri uri = getIntent().getData();

        if (uri != null && uri.toString().startsWith(REDIRECT_URL_CALLBACK)) {
            String code = uri.getQueryParameter("code");
            String state = uri.getQueryParameter("state");
            if (code != null && state != null)
                sendPost(code, state);
        }
        }







    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
    }


    public void signInOut(View view) {

        if(!signed)
        {
            HttpUrl httpUrl= new HttpUrl.Builder()
                    .scheme("http")
                    .host("github.com")
                    .addPathSegment("login")
                    .addPathSegment("oauth")
                    .addPathSegment("authorize")
                    .addQueryParameter("client_id", "822d1b090ca6e601fedb")
                    .addQueryParameter("redirect_uri", REDIRECT_URL_CALLBACK)
                    .addQueryParameter("state", getRandomString())
                    .addQueryParameter("scope", "user:email")
                    .build();

            Log.d(TAG, httpUrl.toString());

            Intent  intent= new Intent(Intent.ACTION_VIEW, Uri.parse(httpUrl.toString()));
            startActivity(intent);



        }
    }

    private String getRandomString() {

        return new BigInteger(130, random).toString(32);

    }

    private void sendPost(String code, String state) {

        OkHttpClient okHttpClient= new OkHttpClient();

        FormBody  form= new FormBody.Builder()
                .add("client_id", "822d1b090ca6e601fedb")
                .add("client_secret","1712c4a81bb78ba31b50e3cbbc403155ef0093c1")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URL_CALLBACK)
                .add("state", state)
                .build();

        Request request = new Request.Builder()
                .url("https://github.com/login/oauth/access_token")
                .post(form)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this,"onFailure" +e.toString(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {



                String responseBody= response.body().string();
                String[] splitted = responseBody.split("=|&");
                if(splitted[0].equalsIgnoreCase("acces_token"))
                    signInWithToken(splitted[1]);
                else
                    Toast.makeText(MainActivity.this, "splitted[0] = >" +splitted[0], Toast.LENGTH_SHORT).show();

            }
        });




    }

    private void signInWithToken(String token) {

        AuthCredential credential= GithubAuthProvider.getCredential(token);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" +task.isSuccessful());

                        if(!task.isSuccessful())
                        {
                            task.getException().printStackTrace();
                            Log.w(TAG, "signInWithCredential",task.getException());
                            Toast.makeText(MainActivity.this,"Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
