/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.jnfd.deamon.table.cs;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 *
 * @author zht
 */
public interface SearchCsCallback {
    public void hitCallback(Interest interest, Data data);
    
    public void missCallback(Interest interest);
}