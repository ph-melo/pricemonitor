package com.paulo.pricemonitor.monitor;

public enum MonitorStatus {
    INITIAL,     // primeira vez (não tinha lastPrice)
    NO_CHANGE,   // checou e não mudou
    CHANGED,     // mudou
    ERROR,       // erro ao checar
    PAUSED       // produto desativado (active=false)
}