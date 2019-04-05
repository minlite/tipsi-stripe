package com.gettipsi.stripe.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.Context;
import android.text.TextUtils;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.gettipsi.stripe.R;
import com.gettipsi.stripe.StripeModule;
import com.gettipsi.stripe.util.CardFlipAnimator;
import com.gettipsi.stripe.util.Converters;
import com.gettipsi.stripe.util.Utils;
import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputListener;
import com.stripe.android.view.CardInputWidget;
import com.stripe.android.view.StripeEditText;

/**
 * Created by dmitriy on 11/13/16
 */

public class AddCardDialogFragment extends DialogFragment {

  private static final String KEY = "KEY";
  public static final String ERROR_CODE = "errorCode";
  public static final String ERROR_DESCRIPTION = "errorDescription";

  private static final String CREATE_CARD_SOURCE_KEY = "CREATE_CARD_SOURCE_KEY";
  private static final String TAG = AddCardDialogFragment.class.getSimpleName();

  private String PUBLISHABLE_KEY;
  private String errorCode;
  private String errorDescription;
  private boolean CREATE_CARD_SOURCE;

  private ProgressBar progressBar;
  private CardInputWidget mCardInputWidget;
  private ImageView imageFlipedCard;
  private ImageView imageFlipedCardBack;

  private volatile Promise promise;
  private boolean successful;
  private CardFlipAnimator cardFlipAnimator;
  private Button doneButton;

  public static AddCardDialogFragment newInstance(
    final String PUBLISHABLE_KEY,
    final String errorCode,
    final String errorDescription,
    final boolean CREATE_CARD_SOURCE
  ) {
    Bundle args = new Bundle();
    args.putString(KEY, PUBLISHABLE_KEY);
    args.putString(ERROR_CODE, errorCode);
    args.putString(ERROR_DESCRIPTION, errorDescription);
    args.putBoolean(CREATE_CARD_SOURCE_KEY, CREATE_CARD_SOURCE);

    AddCardDialogFragment fragment = new AddCardDialogFragment();
    fragment.setArguments(args);
    return fragment;
  }


  public void setPromise(Promise promise) {
    this.promise = promise;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle arguments = getArguments();
    if (arguments != null) {
      PUBLISHABLE_KEY = arguments.getString(KEY);
      errorCode = arguments.getString(ERROR_CODE);
      errorDescription = arguments.getString(ERROR_DESCRIPTION);
      CREATE_CARD_SOURCE = arguments.getBoolean(CREATE_CARD_SOURCE_KEY);
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    final View view = View.inflate(getActivity(), R.layout.payment_form_fragment_two, null);
    final AlertDialog dialog = new AlertDialog.Builder(getActivity())
      .setView(view)
      .setTitle(R.string.gettipsi_card_enter_dialog_title)
      .setPositiveButton(R.string.gettipsi_card_enter_dialog_positive_button, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          onSaveCLick();
        }
      })
      .setNegativeButton(R.string.gettipsi_card_enter_dialog_negative_button, null).create();
    dialog.show();

    doneButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    doneButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onSaveCLick();
      }
    });
    doneButton.setTextColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));

    bindViews(view);
    init();

    return dialog;
  }

  @Override
  public void onDismiss(DialogInterface dialog) {
    if (!successful && promise != null) {
      promise.reject(errorCode, errorDescription);
      promise = null;
    }
    super.onDismiss(dialog);
  }

  private void bindViews(final View view) {
    progressBar = (ProgressBar) view.findViewById(R.id.buttonProgress);
    mCardInputWidget = (CardInputWidget) view.findViewById(R.id.card_input_widget);
    imageFlipedCard = (ImageView) view.findViewById(R.id.imageFlippedCard);
    imageFlipedCardBack = (ImageView) view.findViewById(R.id.imageFlippedCardBack);
  }


  private void init() {
    mCardInputWidget.setCardInputListener(new CardInputListener() {
      @Override
      public void onFocusChange(String focusField) {
        if(focusField.equals(FocusField.FOCUS_CVC)) {
          if(cardFlipAnimator.getCurrentSide() == CardFlipAnimator.Side.FRONT) {
            cardFlipAnimator.showBack();
          }
        } else {
          if(cardFlipAnimator.getCurrentSide() == CardFlipAnimator.Side.BACK) {
            cardFlipAnimator.showFront();
          }
        }
      }

      @Override
      public void onCardComplete() {

      }

      @Override
      public void onExpirationComplete() {

      }

      @Override
      public void onCvcComplete() {

      }

      @Override
      public void onPostalCodeComplete() {

      }
    });

    cardFlipAnimator = new CardFlipAnimator(getActivity(), imageFlipedCard, imageFlipedCardBack);
    successful = false;
  }

  public void onSaveCLick() {
    doneButton.setEnabled(false);
    progressBar.setVisibility(View.VISIBLE);
    final Card card = mCardInputWidget.getCard();

    if(card == null) {
      doneButton.setEnabled(true);
      progressBar.setVisibility(View.GONE);
      Toast.makeText(getActivity(), "Please check card info.", Toast.LENGTH_SHORT).show();
      return;
    }

    String errorMessage = Utils.validateCard(card);
    if (errorMessage == null) {
      if (CREATE_CARD_SOURCE) {
        SourceParams cardSourceParams = SourceParams.createCardParams(card);
        StripeModule.getInstance().getStripe().createSource(
          cardSourceParams,
          new SourceCallback() {
            @Override
            public void onSuccess(Source source) {
              // Normalize data with iOS SDK
              final WritableMap sourceMap = Converters.convertSourceToWritableMap(source);
              sourceMap.putMap("card", Converters.mapToWritableMap(source.getSourceTypeData()));
              sourceMap.putNull("sourceTypeData");

              if (promise != null) {
                promise.resolve(sourceMap);
                promise = null;
              }
              successful = true;
              dismiss();
            }

            @Override
            public void onError(Exception error) {
              doneButton.setEnabled(true);
              progressBar.setVisibility(View.GONE);
              showToast(error.getLocalizedMessage());
            }
          }
        );
      } else {
        StripeModule.getInstance().getStripe().createToken(
          card,
          PUBLISHABLE_KEY,
          new TokenCallback() {
            public void onSuccess(Token token) {
              if (promise != null) {
                promise.resolve(Converters.convertTokenToWritableMap(token));
                promise = null;
              }
              successful = true;
              dismiss();
            }

            public void onError(Exception error) {
              doneButton.setEnabled(true);
              progressBar.setVisibility(View.GONE);
              showToast(error.getLocalizedMessage());
            }
          });
      }

    } else {
      doneButton.setEnabled(true);
      progressBar.setVisibility(View.GONE);
      showToast(errorMessage);
    }
  }

  public void showToast(String message) {
    Context context = getActivity();
    if (context != null && !TextUtils.isEmpty(message)) {
      Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
  }
}
