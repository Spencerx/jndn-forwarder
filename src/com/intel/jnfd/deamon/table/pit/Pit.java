/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.jnfd.deamon.table.pit;

import com.intel.jnfd.deamon.table.HashMapRepo;
import com.intel.jnfd.deamon.table.Pair;
import java.util.Vector;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 *
 * @author zht
 */
public class Pit {

    public int size() {
        return pit.size();
    }

    /**
     * inserts a PIT entry for Interest If an entry for exact same name and
     * selectors exists, that entry is returned.
     *
     * @param interest
     * @return the entry, and true for new entry, false for existing entry
     */
    public Pair<PitEntry> insert(Interest interest) {
        Vector<PitEntry> pitEntries = pit.findExactMatch(interest.getName());
        if (pitEntries == null) {
            pitEntries = new Vector<>();
            PitEntry pitEntry = new PitEntry();
            pitEntries.add(pitEntry);
            pit.insert(interest.getName(), pitEntries);
            return new Pair(pitEntry, true);
        }
        for (PitEntry one : pitEntries) {
            if (one.getInterest().getChildSelector() == interest.getChildSelector()
                    && one.getName().equals(interest.getName())) {
                return new Pair(one, false);
            }
        }
        PitEntry pitEntry = new PitEntry();
        pitEntries.add(pitEntry);
        return new Pair(pitEntry, true);
    }

    public Vector<PitEntry> findAllDataMatches(Data data) {
        return pit.findLongestPrefixMatch(data.getName());
    }

    public void erase(PitEntry pitEntry) {
        Vector<PitEntry> pitEntries = pit.findExactMatch(pitEntry.getName());
        if (pitEntries == null) {
            return;
        }
        pitEntries.remove(pitEntry);
    }

    private HashMapRepo<Vector<PitEntry>> pit = new HashMapRepo<>();
}