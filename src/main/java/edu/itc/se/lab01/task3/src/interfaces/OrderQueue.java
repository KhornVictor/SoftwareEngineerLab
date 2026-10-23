package edu.itc.se.lab01.task3.src.interfaces;

import edu.itc.se.lab01.task3.src.action.Order;

public interface OrderQueue {
    void put(Order o) throws InterruptedException;
    Order take() throws InterruptedException;
}
