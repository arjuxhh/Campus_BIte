package com.campusbite.model;
public class Admin extends User { public Admin(String name){super(name);} @Override public String getRole(){return "Admin";} @Override public boolean canManageOrders(){return true;} }
