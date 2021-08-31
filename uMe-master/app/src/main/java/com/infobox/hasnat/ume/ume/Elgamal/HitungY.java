package com.infobox.hasnat.ume.ume.Elgamal;

import android.support.v7.app.AppCompatActivity;
import com.infobox.hasnat.ume.ume.Elgamal.GenerateKey;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import java.sql.Array;
import java.util.*;
import com.infobox.*;
import java.util.Scanner;
import java.math.*;

public class HitungY extends AppCompatActivity {

    public int y;
    public int pangkat;
    // menghitung nilai y dengan syarat y = g^x mod p
    public HitungY(GenerateKey gk) {
        pangkat = (int) Math.pow(gk.BilG, gk.BilX);
        y = Math.floorMod(pangkat, gk.bilprim);
    }

}
