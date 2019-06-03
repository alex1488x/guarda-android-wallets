package com.guarda.ethereum.lifecycle;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import com.guarda.ethereum.R;
import com.guarda.ethereum.managers.TransactionsManager;
import com.guarda.ethereum.managers.WalletManager;
import com.guarda.ethereum.models.items.TransactionItem;
import com.guarda.ethereum.models.items.ZecTxListResponse;
import com.guarda.ethereum.models.items.ZecTxResponse;
import com.guarda.ethereum.rest.ApiMethods;
import com.guarda.ethereum.rest.RequestorBtc;
import com.guarda.ethereum.rxcall.CallFillHistory;
import com.guarda.ethereum.views.activity.MainActivity;
import com.guarda.zcash.sapling.rxcall.CallSaplingBalance;

import org.bitcoinj.core.Coin;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class HistoryViewModel extends ViewModel {

    private final WalletManager walletManager;
    private final TransactionsManager transactionsManager;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private MutableLiveData<Boolean> showHistory = new MutableLiveData<>();
    private MutableLiveData<Boolean> showTxError = new MutableLiveData<>();

    public HistoryViewModel(WalletManager walletManager, TransactionsManager transactionsManager) {
        this.walletManager = walletManager;
        this.transactionsManager = transactionsManager;
    }

    public void loadTransactions() {
        RequestorBtc.getTransactionsZecNew(walletManager.getWalletFriendlyAddress(), new ApiMethods.RequestListener() {
            @Override
            public void onSuccess(Object response) {
                ZecTxListResponse txListResponse = (ZecTxListResponse) response;
                List<ZecTxResponse> txList = txListResponse.getTxs();
                if (txList == null) return;
                compositeDisposable.add(Observable
                        .fromCallable(new CallFillHistory(transactionsManager, txList, walletManager.getWalletFriendlyAddress()))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((value) -> {
                            Timber.d("CallFillHistory value=%b", value);
                            showHistory.setValue(true);
                        }));

//                loaderAnimation.cancel();
            }

            @Override
            public void onFailure(String msg) {
                showTxError.setValue(true);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {

        private final WalletManager walletManager;
        private final TransactionsManager transactionsManager;

        public Factory(WalletManager walletManager, TransactionsManager transactionsManager) {
            this.walletManager = walletManager;
            this.transactionsManager = transactionsManager;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new HistoryViewModel(this.walletManager, this.transactionsManager);
        }
    }

    public MutableLiveData<Boolean> getShowHistory() {
        return showHistory;
    }

    public MutableLiveData<Boolean> getShowTxError() {
        return showTxError;
    }
}
