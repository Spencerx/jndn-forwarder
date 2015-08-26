/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.jnfd.deamon.fw;

import com.intel.jnfd.deamon.face.Face;
import com.intel.jnfd.deamon.table.fib.FibEntry;
import com.intel.jnfd.deamon.table.fib.FibNextHop;
import com.intel.jnfd.deamon.table.pit.PitEntry;
import com.intel.jnfd.deamon.table.pit.PitOutRecord;
import java.util.Vector;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

/**
 * Best Route strategy version 3.
 *
 * This strategy forwards a new Interest to the lowest-cost nexthop (except
 * downstream). After that, if consumer retransmits the Interest (and is not
 * suppressed according to exponential backoff algorithm), the strategy forwards
 * the Interest again to the lowest-cost nexthop (except downstream) that is not
 * previously used. If all nexthops have been used, the strategy starts over.
 *
 * @author zht
 */
public class BestRouteStrategy2 extends Strategy {

    public static final Name STRATEGY_NAME
            = new Name("ndn:/localhost/nfd/strategy/best-route/%FD%03");

    /**
     * This should not be used unless you want to define the strategy name by
     * yourself.
     *
     * @param forwarder
     * @param name
     * @deprecated
     */
    @Deprecated
    public BestRouteStrategy2(Forwarder forwarder, Name name) {
        super(forwarder, name);
    }

    /**
     * This should be the default constructor for this class
     *
     * @param forwarder
     */
    public BestRouteStrategy2(Forwarder forwarder) {
        super(forwarder, STRATEGY_NAME);
    }

    @Override
    public void afterReceiveInterest(Face inFace, Interest interest,
            FibEntry fibEntry, PitEntry pitEntry) {
        Vector<FibNextHop> nextHopList = fibEntry.getNextHopList();
        FibNextHop nextHop = null;

        RetxSuppression.Result suppression
                = retxSuppression.decide(inFace, interest, pitEntry);

        if (suppression == RetxSuppression.Result.NEW) {
            // forward to nexthop with lowest cost except downstream
            for (FibNextHop one : nextHopList) {
                if (predicate_NextHop_eligible(pitEntry, one, inFace.getFaceId(),
                        false, 0)) {
                    nextHop = one;
                    break;
                }
            }

            if (nextHop == null) {
                rejectPendingInterest(pitEntry);
                return;
            }

            Face outFace = nextHop.getFace();
            sendInterest(pitEntry, outFace, false);
            return;
        }

        if (suppression == RetxSuppression.Result.SUPPRESS) {
            return;
        }

        // find an unused upstream with lowest cost except downstream
        for (FibNextHop one : nextHopList) {
            if (predicate_NextHop_eligible(pitEntry, one, inFace.getFaceId(),
                    true, System.currentTimeMillis())) {
                nextHop = one;
                break;
            }
        }
        if(nextHop != null) {
            Face outFace = nextHop.getFace();
            sendInterest(pitEntry, outFace, false);
            return;
        }
        
        // find an eligible upstream that is used earliest
        nextHop = findEligibleNextHopWithEarliestOutRecord(
                pitEntry, nextHopList, inFace.getFaceId());
        if(nextHop != null) {
            Face outFace = nextHop.getFace();
            sendInterest(pitEntry, outFace, false);
        }
    }

    @Override
    public void beforeSatisfyInterest(PitEntry pitEntry, Face inFace, Data data) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void beforeExpirePendingInterest(PitEntry pitEntry) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * determines whether a NextHop is eligible.
     *
     * @param pitEntry
     * @param nexthop
     * @param currentDownstream incoming FaceId of current Interest
     * @param wantUnused if true, NextHop must not have unexpired OutRecord
     * @param currentTime ignored if !wantUnused
     * @return
     */
    private static boolean predicate_NextHop_eligible(PitEntry pitEntry,
            FibNextHop nexthop, int currentDownstreamId, boolean wantUnused,
            long currentTime) {
        Face upstream = nexthop.getFace();

        // upstream is current downstream
        if (upstream.getFaceId() == currentDownstreamId) {
            return false;
        }

        // forwarding would violate scope
        if (pitEntry.violatesScope(upstream)) {
            return false;
        }

        if (wantUnused) {
            // NextHop must not have unexpired OutRecord
            PitOutRecord outRecord = pitEntry.getOutRecord(upstream);
            if (outRecord != null && outRecord.getExpiry() > currentTime) {
                return false;
            }
        }

        return true;
    }

    /**
     * Two parameters of normal predicate_NextHop_eligible are preset.
     *
     * @param pitEntry
     * @param nexthop
     * @param currentDownstreamId
     * @return
     */
    private static boolean predicate_NextHop_eligible(PitEntry pitEntry,
            FibNextHop nexthop, int currentDownstreamId) {
        return predicate_NextHop_eligible(pitEntry,
                nexthop, currentDownstreamId, false, 0);
    }

    /**
     * pick an eligible NextHop with earliest OutRecord, It is assumed that
     * every nexthop has an OutRecord
     *
     * @param pitEntry
     * @param nexthops
     * @param currentDownstreamId
     * @return
     */
    private static FibNextHop findEligibleNextHopWithEarliestOutRecord(
            PitEntry pitEntry, Vector<FibNextHop> nexthops,
            int currentDownstreamId) {
        FibNextHop result = null;
        long earliestRenewed = Long.MAX_VALUE;
        for (FibNextHop one : nexthops) {
            if (!predicate_NextHop_eligible(pitEntry, one, currentDownstreamId)) {
                continue;
            }
            PitOutRecord outRecord = pitEntry.getOutRecord(one.getFace());
            if (outRecord != null && outRecord.getLastRenewed() < earliestRenewed) {
                result = one;
                earliestRenewed = outRecord.getLastRenewed();
            }
        }
        return result;
    }

    private RetxSuppressionExponential retxSuppression
            = new RetxSuppressionExponential();
}
