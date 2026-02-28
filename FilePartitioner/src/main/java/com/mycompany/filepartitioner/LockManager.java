/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.filepartitioner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
/**
 *
 * @author ntu-user
 */
public class LockManager {
     private static final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    
    /**
     * Acquire lock for a file - blocks if already locked
     */
    public static void lock(String filename) {
        ReentrantLock lock = locks.computeIfAbsent(filename, k -> new ReentrantLock());
        lock.lock();
        System.out.println("Locked: " + filename);
    }
    
    /**
     * Try to acquire lock - returns false if already locked
     */
    public static boolean tryLock(String filename) {
        ReentrantLock lock = locks.computeIfAbsent(filename, k -> new ReentrantLock());
        boolean acquired = lock.tryLock();
        if (acquired) {
            System.out.println("Locked: " + filename);
        } else {
            System.out.println("Could not lock (busy): " + filename);
        }
        return acquired;
    }
    
    /**
     * Release lock for a file
     */
    public static void unlock(String filename) {
        ReentrantLock lock = locks.get(filename);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            System.out.println("Unlocked: " + filename);
            
            // Clean up if no more threads waiting
            if (!lock.hasQueuedThreads()) {
                locks.remove(filename);
            }
        }
    }
    
    /**
     * Check if file is currently locked
     */
    public static boolean isLocked(String filename) {
        ReentrantLock lock = locks.get(filename);
        return lock != null && lock.isLocked();
    }

}
