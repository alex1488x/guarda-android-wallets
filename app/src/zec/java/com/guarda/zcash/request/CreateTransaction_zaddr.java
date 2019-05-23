package com.guarda.zcash.request;

import com.guarda.zcash.WalletCallback;
import com.guarda.zcash.ZCashException;
import com.guarda.zcash.ZCashTransactionOutput;
import com.guarda.zcash.ZCashTransaction_zaddr;
import com.guarda.zcash.crypto.DumpedPrivateKey;
import com.guarda.zcash.sapling.note.SpendProof;

import java.util.LinkedList;
import java.util.List;


public class CreateTransaction_zaddr extends AbstractZCashRequest implements Runnable {
  private String fromAddr;
  private String toAddr;
  private String privateKey;
  private WalletCallback<String, ZCashTransaction_zaddr> callback;
  private long fee;
  private long value;
  private List<ZCashTransactionOutput> utxos;
  private int expiryHeight;

  public CreateTransaction_zaddr(String fromAddr,
                                 String toAddr,
                                 long value,
                                 long fee,
                                 String privatekey,
                                 int expiryHeight,
                                 WalletCallback<String, ZCashTransaction_zaddr> callback,
                                 List<ZCashTransactionOutput> utxos) {
    this.fromAddr = fromAddr;
    this.toAddr = toAddr;
    this.value = value;
    this.fee = fee;
    this.privateKey = privatekey;
    this.callback = callback;
    this.utxos = utxos;
    this.expiryHeight = expiryHeight;
  }

  @Override
  public void run() {
    try {
      ZCashTransaction_zaddr tx = createTransaction();
      callback.onResponse("ok", tx);
    } catch (ZCashException e) {
      callback.onResponse(e.getMessage(), null);
    }
  }

  private ZCashTransaction_zaddr createTransaction() throws ZCashException {

    List<ZCashTransactionOutput> outputs = new LinkedList<>();
    long realValue = chooseUTXOs(outputs);
    if (realValue < fee + value) {
      throw new ZCashException("Not enough balance.");
    }

//    TestScanBlocks tsb = new TestScanBlocks();
//    tsb.getWintesses();
//    SpendProof spendProof = tsb.addSpendS();
    SpendProof spendProof = new SpendProof(new byte[0], new byte[0], new byte[0], new byte[0], new byte[0], new byte[0]);

    return new ZCashTransaction_zaddr(DumpedPrivateKey.fromBase58(privateKey), fromAddr, toAddr,
            value, fee, expiryHeight, outputs, spendProof);
  }


  private long chooseUTXOs(List<ZCashTransactionOutput> outputs) {
    long realValue = value + fee;
    long sum = 0;
    for (ZCashTransactionOutput out : utxos) {
      outputs.add(out);
      sum += out.value;
      if (sum >= realValue) {
        break;
      }

    }

    return sum;
  }
}