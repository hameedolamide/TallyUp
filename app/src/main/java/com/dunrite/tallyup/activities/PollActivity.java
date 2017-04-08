package com.dunrite.tallyup.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dunrite.tallyup.Poll;
import com.dunrite.tallyup.PollItem;
import com.dunrite.tallyup.R;
import com.dunrite.tallyup.RecyclerItemClickListener;
import com.dunrite.tallyup.adapters.PollChoiceAdapter;
import com.dunrite.tallyup.utility.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
/**
 * Activity that displays a poll
 */
public class PollActivity extends AppCompatActivity {
    private String pollID;
    private Poll currentPoll;
    private ArrayList<PollItem> pollItems;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private PollChoiceAdapter adapter;

    @BindView(R.id.questionText) TextView questionText;
    @BindView(R.id.recyclerview_choices) RecyclerView choicesRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);
        ButterKnife.bind(this);

        pollItems = new ArrayList<>();

        Intent intent = getIntent();
        if (intent.hasExtra("pollID"))
            pollID = intent.getStringExtra("pollID");
        if (intent.hasExtra("pollQuestion"))
            questionText.setText(intent.getStringExtra("pollQuestion"));

        mAuth = FirebaseAuth.getInstance();

    }


    @Override
    public void onStart() {
        super.onStart();
        signInToFirebase();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.poll, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.poll_id_share) {
            //TODO: Share the poll
            Toast.makeText(this, "TODO: Make Sharing Work", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    public void setupRecyclerView() {
        adapter = new PollChoiceAdapter(pollItems);
        LinearLayoutManager manager = new LinearLayoutManager(getApplicationContext());
        choicesRV.setLayoutManager(manager);
        choicesRV.setAdapter(adapter);
        choicesRV.addOnItemTouchListener(
                new RecyclerItemClickListener(this, choicesRV,
                        new RecyclerItemClickListener.OnItemClickListener(){
                            @Override
                            public void onItemClick(View view, int position){

                            }
                            @Override
                            public void onItemLongClick(View view, int position) {

                            }

                        }));

    }

    /**
     * Connect to the Firebase database
     */
    public void signInToFirebase() {
        if(Utils.isOnline(getApplicationContext())) {
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            //Login successful
                            mDatabase = FirebaseDatabase.getInstance().getReference("Polls/" + pollID);

                            // Read from the database
                            mDatabase.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    // This method is called once with the initial value(s) and again
                                    // whenever data at this location is updated.
                                    pollItems.clear();

                                    Map<String, Object> items = (Map<String, Object>) dataSnapshot.getValue();
                                    Log.d("items", items.toString());
                                    for (Map.Entry<String, Object> item : items.entrySet()) {
                                        if (item.getKey().startsWith("Item")) {
                                            Map<String, Object> attributes = (Map<String, Object>) item.getValue();
                                            PollItem pi = new PollItem(attributes.get("Name").toString(),
                                                    Integer.parseInt(attributes.get("Votes").toString()));
                                            pollItems.add(pi);
                                        }
                                    }
                                    setupRecyclerView();
                                }

                                // Data listener cancelled
                                @Override
                                public void onCancelled(DatabaseError error) {
                                    // Failed to read value
                                    Log.w("data", "Failed to read values.", error.toException());
                                }
                            });

                            //login failure
                            if (!task.isSuccessful()) {
                                Log.w("auth", "signInAnonymously", task.getException());
                                Toast.makeText(PollActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Snackbar.make(findViewById(R.id.activity_main), "No Internet Connection", Snackbar.LENGTH_INDEFINITE).setAction("RETRY", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signInToFirebase();
                }
            }).show();
        }
    }
}
