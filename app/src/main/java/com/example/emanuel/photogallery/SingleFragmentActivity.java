package com.example.emanuel.photogallery;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by emanuel on 8/1/15.
 */

/* Extend AppCompatActivity instead of FragmentActivity. That way I can use fragments and
the actionbar at the same time.

This is an abstract class. Meaning I can reuse it in several places in my code.
Look at CrimeListActivity and CrimeActivity for an example.
*/
public abstract class SingleFragmentActivity extends AppCompatActivity {
    protected abstract Fragment createFragment();

    @LayoutRes
    protected int getLayoutResId() {
        return R.layout.activity_fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //The layout file that contains the fragments
        setContentView(getLayoutResId());
        //Lets us interact with fragments
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            fragment = createFragment();
            fm.beginTransaction()
                    /*
                        id          Tells the fragment where in the layout the it should
                                    appear. Can refer to createFragment() by this id
                        fragment    The fragment to place
                     */
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}
