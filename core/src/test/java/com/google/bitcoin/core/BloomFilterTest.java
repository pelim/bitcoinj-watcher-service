package com.google.bitcoin.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

public class BloomFilterTest {
    @Test
    public void insertSerializeTest() {
        BloomFilter filter = new BloomFilter(3, 0.01, 0);
        
        filter.insert(Hex.decode("99108ad8ed9bb6274d3980bab5a85c048f0950c8"));
        assertTrue (filter.contains(Hex.decode("99108ad8ed9bb6274d3980bab5a85c048f0950c8")));
        // One bit different in first byte
        assertFalse(filter.contains(Hex.decode("19108ad8ed9bb6274d3980bab5a85c048f0950c8")));

        filter.insert(Hex.decode("b5a2c786d9ef4658287ced5914b37a1b4aa32eee"));
        assertTrue(filter.contains(Hex.decode("b5a2c786d9ef4658287ced5914b37a1b4aa32eee")));
        
        filter.insert(Hex.decode("b9300670b4c5366e95b2699e8b18bc75e5f729c5"));
        assertTrue(filter.contains(Hex.decode("b9300670b4c5366e95b2699e8b18bc75e5f729c5")));
        
        // Value generated by the reference client
        assertTrue(Arrays.equals(Hex.decode("03614e9b0500000000000000"), filter.bitcoinSerialize()));
    }
    
    @Test
    public void insertSerializeTestWithTweak() {
        BloomFilter filter = new BloomFilter(3, 0.01, 2147483649L);
        
        filter.insert(Hex.decode("99108ad8ed9bb6274d3980bab5a85c048f0950c8"));
        assertTrue (filter.contains(Hex.decode("99108ad8ed9bb6274d3980bab5a85c048f0950c8")));
        // One bit different in first byte
        assertFalse(filter.contains(Hex.decode("19108ad8ed9bb6274d3980bab5a85c048f0950c8")));

        filter.insert(Hex.decode("b5a2c786d9ef4658287ced5914b37a1b4aa32eee"));
        assertTrue(filter.contains(Hex.decode("b5a2c786d9ef4658287ced5914b37a1b4aa32eee")));
        
        filter.insert(Hex.decode("b9300670b4c5366e95b2699e8b18bc75e5f729c5"));
        assertTrue(filter.contains(Hex.decode("b9300670b4c5366e95b2699e8b18bc75e5f729c5")));
        
        // Value generated by the reference client
        assertTrue(Arrays.equals(Hex.decode("03ce42990500000001000080"), filter.bitcoinSerialize()));
    }
}
