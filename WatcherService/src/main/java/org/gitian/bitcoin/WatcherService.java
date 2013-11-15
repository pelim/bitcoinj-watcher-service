package org.gitian.bitcoin;

/**
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.RegTestParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.common.collect.Lists;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import org.gitian.bitcoin.resources.AddressResource;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.util.ArrayList;

/**
 * ForwardingService demonstrates basic usage of the library. It sits on the network and when it receives coins, simply
 * sends them onwards to an address given on the command line.
 */
public class WatcherService extends Service<WatcherConfiguration> {
    private static WalletAppKit kit;

    public static void main(String[] args) throws Exception {
        // This line makes the log output more compact and easily read, especially when using the JDK log adapter.
        BriefLogFormatter.init();
        if (args.length < 1) {
            System.err.println("Usage: [regtest|testnet]");
            return;
        }

        // Figure out which network we should connect to. Each one gets its own set of files.
        NetworkParameters params;
        String filePrefix;
        String net = args[0];
        if (net.equals("testnet")) {
            params = TestNet3Params.get();
            filePrefix = "watcher-service-testnet";
        } else if (net.equals("regtest")) {
            params = RegTestParams.get();
            filePrefix = "watcher-service-regtest";
        } else {
            params = MainNetParams.get();
            filePrefix = "watcher-service";
        }

        // Start up a basic app using a class that automates some boilerplate.
        kit = new WalletAppKit(params, new File("."), filePrefix);

        if (params == RegTestParams.get()) {
            // Regression test mode is designed for testing and development only, so there's no public network for it.
            // If you pick this mode, you're expected to be running a local "bitcoind -regtest" instance.
            kit.connectToLocalHost();
        }

        FileInputStream checkpointsStream = new FileInputStream("checkpoints");
        kit.setCheckpoints(checkpointsStream);

        // Download the block chain and wait until it's done.
        kit.startAndWait();

        // We want to know when we receive money.
        kit.wallet().addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onCoinsReceived(Wallet w, Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
                // Runs in the dedicated "user thread" (see bitcoinj docs for more info on this).
                //
                // The transaction "tx" can either be pending, or included into a block (we didn't see the broadcast).
                BigInteger value = tx.getValueSentToMe(w);
                System.out.println("Received tx for " + Utils.bitcoinValueToFriendlyString(value) + ": " + tx);
            }
        });

        ArrayList<String> argsList = Lists.newArrayList(args);
        argsList.remove(0);
        args = argsList.toArray(new String[argsList.size()]);
        new WatcherService().run(args);

        ECKey ecKey = kit.wallet().getKeys().get(0);
        Address sendToAddress = ecKey.toAddress(params);

        System.out.println("Send coins to: " + sendToAddress);
        System.out.println("Pubkey: " + ecKey);
        System.out.println("Waiting for coins to arrive. Press Ctrl-C to quit.");

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void initialize(Bootstrap<WatcherConfiguration> bootstrap) {
        bootstrap.setName("watcher");
    }

    @Override
    public void run(WatcherConfiguration configuration, Environment environment) throws Exception {
        environment.addResource(new AddressResource(kit.wallet()));
        environment.getObjectMapperFactory().enable(SerializationFeature.INDENT_OUTPUT);
    }
}
