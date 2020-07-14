package com.app.skc;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.IOException;

public class Test {

    public static void main(String[] args) throws IOException, CipherException {
        Credentials credentials = WalletUtils.loadCredentials("", "/Users/Dylan/Desktop/wallet/UTC--2020-06-13T06-41-47.382000000Z--5bc413403be2d5c0503e89b569b42c0b8f690273.json");
        System.out.println(credentials.getEcKeyPair().getPrivateKey().toString(16));

    }
}
