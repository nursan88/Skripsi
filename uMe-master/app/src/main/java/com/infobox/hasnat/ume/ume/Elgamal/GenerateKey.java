package com.infobox.hasnat.ume.ume.Elgamal;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import java.util.*;
import com.infobox.*;
import java.math.*;

public class GenerateKey extends AppCompatActivity {
    public int BilG; //kunci publik
    public int BilX; //kunci private
    //public int bil_prima;
    public int bilprim;
    public GenerateKey() {
        //Hardcode bilangan prima antara 1 - 100
        int[] prima = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};
        // Inisiasi variabel random r
        Random r = new Random();
        // Mengambil satu bilangan prima secara acak
        bilprim = prima[r.nextInt(prima.length)];
        //System.out.println(bilprim);

        //Bilangan x dengan syarat 1 < x < p-2
        int x;
        Random bil_x = new Random();
        x = bil_x.nextInt(bilprim);
        if (x > 1 && x < bilprim - 2) {
            BilX = x;
            //System.out.println(BilX);
        } else if (x <= 1 || x >= bilprim - 2) {
            BilX = 2;
            //System.out.println(BilX);
        }

        // Mencari nilai g dengan syarat g < p
        int g;
        Random bil_g = new Random();
        g = bil_g.nextInt(bilprim);
        if (g < bilprim) {
            BilG = g;
            //System.out.print(BilG);
        } else {
            BilG = 1;
            //System.out.print(BilG);
        }
    }
}