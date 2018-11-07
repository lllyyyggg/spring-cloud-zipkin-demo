package com.lanyage.zuul.busservice.bean;

public class Bus {

    private String branch;
    private String version;

    public Bus() {
    }

    public Bus(String branch, String version) {
        this.branch = branch;
        this.version = version;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Bus{" +
                "branch='" + branch + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
