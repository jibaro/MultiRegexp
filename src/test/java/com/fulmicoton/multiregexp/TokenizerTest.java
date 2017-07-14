package com.fulmicoton.multiregexp;


import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public class TokenizerTest {


    private static enum TOKEN {
        WHITESPACE,
        WORD,
        PUNCTUATION;
    }


    private static enum TEST {
        A,
        ABC,
        BCD,
        ABD,
        D;
    }


    @Test
    public void testSimpleLexer() {
        final Lexer<TOKEN> lexer = new Lexer<>();
        lexer
                .addRule(TOKEN.WHITESPACE, " ")
                .addRule(TOKEN.WORD, "[a-zA-Z]+")
                .addRule(TOKEN.PUNCTUATION, "[,\\.\\!\\?]");
        final String txt = "Bonjour herve!";
        final Iterator<Token<TOKEN>> tokenIt =  lexer.scan(txt).iterator();
        Assert.assertTrue(tokenIt.hasNext());
        Assert.assertEquals(tokenIt.next(), new Token<>(TOKEN.WORD, "Bonjour"));
        Assert.assertTrue(tokenIt.hasNext());
        Assert.assertEquals(tokenIt.next(), new Token<>(TOKEN.WHITESPACE, " "));
        Assert.assertTrue(tokenIt.hasNext());
        Assert.assertEquals(tokenIt.next(), new Token<>(TOKEN.WORD, "herve"));
        Assert.assertTrue(tokenIt.hasNext());
        Assert.assertEquals(tokenIt.next(), new Token<>(TOKEN.PUNCTUATION, "!"));
        Assert.assertFalse(tokenIt.hasNext());
        Assert.assertEquals(tokenIt.next(), null);
    }


    @Test
    public void testPriority() {
        final Lexer<TEST> lexer = new Lexer<>();
        lexer
                .addRule(TEST.ABC, "abc")
                .addRule(TEST.A, "a")
                .addRule(TEST.ABD, "abd")
                .addRule(TEST.D, "b?d")
        .addRule(TEST.BCD, "bcd");

        {
            final String txt = "abd";
            final Iterator<Token<TEST>> tokenIt =  lexer.scan(txt).iterator();
            Assert.assertTrue(tokenIt.hasNext());
            Assert.assertEquals(tokenIt.next(), new Token<>(TEST.A, "a"));
            Assert.assertTrue(tokenIt.hasNext());
            Assert.assertEquals(tokenIt.next(), new Token<>(TEST.D, "bd"));
            Assert.assertFalse(tokenIt.hasNext());
            Assert.assertEquals(tokenIt.next(), null);
        }
        {
            final String txt = "abcd";
            final Iterator<Token<TEST>> tokenIt =  lexer.scan(txt).iterator();
            Assert.assertTrue(tokenIt.hasNext());
            Assert.assertEquals(tokenIt.next(), new Token<>(TEST.ABC, "abc"));
            Assert.assertTrue(tokenIt.hasNext());
            Assert.assertEquals(tokenIt.next(), new Token<>(TEST.D, "d"));
            Assert.assertFalse(tokenIt.hasNext());
            Assert.assertEquals(tokenIt.next(), null);
        }
        {

            final String txt = "abce";
            try {
                final Iterator<Token<TEST>> tokenIt = lexer.scan(txt).iterator();
                Assert.assertTrue(tokenIt.hasNext());
                Assert.assertEquals(tokenIt.next(), new Token<>(TEST.ABC, "abc"));
                Assert.assertTrue(tokenIt.hasNext());
                Assert.assertEquals(tokenIt.next(), new Token<>(TEST.D, "d"));
            }
            catch (RuntimeException e) {
                final ScanException typedError = (ScanException)e.getCause();
                Assert.assertEquals(typedError.getOffset(), 3);
                Assert.assertEquals(typedError.getMessage(), "Could not find any token at (3):\"abc|e\"");
            }
        }
    }

}
