package com.campusbite.model;
public abstract class User {
    private final String name;
    public User(String name){this.name=name==null||name.isBlank()?"Campus Guest":name.trim();}
    public String getName(){return name;} public abstract String getRole(); public boolean canManageOrders(){return false;}
}
