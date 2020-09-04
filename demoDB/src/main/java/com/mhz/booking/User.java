package com.mhz.booking;

public class User {
    private String userName;
    private int balance;
    private byte[] passwd;
    private byte[] salt;
    public User(String userName, int balance, byte[] passwd, byte[] salt) {
        this.userName = userName;
        this.balance = balance;
        this.passwd = passwd;
        this.salt = salt;
    }
    public User() {}
    public String getUserName() {
        return userName;
    }
    public int getBalance() {
        return balance;
    }
    protected byte[] getPasswd() {
        return passwd;
    }
    protected byte[] getSalt() {
        return salt;
    }
}
