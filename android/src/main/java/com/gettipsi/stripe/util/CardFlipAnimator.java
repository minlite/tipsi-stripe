package com.gettipsi.stripe.util;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.widget.ImageView;

import com.gettipsi.stripe.R;

/**
 * Created by dmitriy on 11/14/16
 */

public class CardFlipAnimator {

    private final Context context;
    private final ImageView imageViewCardFront;
    private final ImageView imageViewCardBack;
    private Animator animator1;
    private Animator animator2;
    private Animator animator3;
    private Animator animator4;

    public enum Side {
        FRONT,
        BACK
    }

    private Side currentSide;

    public CardFlipAnimator(final Context context, final ImageView imageViewCardFront, final ImageView imageViewCardBack){
        this.context = context;
        this.imageViewCardFront = imageViewCardFront;
        this.imageViewCardBack = imageViewCardBack;
        this.currentSide = Side.FRONT;
        init();
    }

    private void init(){
        animator1 = AnimatorInflater.loadAnimator(context, R.animator.card_flip_right_in);
        animator2 = AnimatorInflater.loadAnimator(context, R.animator.card_flip_right_out);
        animator3 = AnimatorInflater.loadAnimator(context, R.animator.card_flip_left_in);
        animator4 = AnimatorInflater.loadAnimator(context, R.animator.card_flip_left_out);
        animator1.setTarget(imageViewCardBack);
        animator2.setTarget(imageViewCardFront);
        animator3.setTarget(imageViewCardFront);
        animator4.setTarget(imageViewCardBack);
    }

    public void showBack(){
        this.currentSide = Side.BACK;
        animator1.start();
        animator2.start();
    }

    public void showFront(){
        this.currentSide = Side.FRONT;
        animator3.start();
        animator4.start();
    }

    public Side getCurrentSide() {
        return currentSide;
    }
}
