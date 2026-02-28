/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
/**
 *
 * @author ntu-user
 */
public class FileRecord {
    private final StringProperty fileName = new SimpleStringProperty(); 
    private final StringProperty owner = new SimpleStringProperty(); 
    public FileRecord(String fileName, String owner) { this.fileName.set(fileName); 
    this.owner.set(owner); 
    } 
    
    public String getFileName() { 
        return fileName.get(); 
    } 
    public void setFileName(String value) { 
        fileName.set(value); 
    } 
    public StringProperty fileNameProperty() { 
        return fileName; 
    } 
    public String getOwner() { 
        return owner.get(); 
    } 
    public void setOwner(String value) { 
        owner.set(value); 
    } 
    public StringProperty ownerProperty() { 
        return owner; 
    }
}
