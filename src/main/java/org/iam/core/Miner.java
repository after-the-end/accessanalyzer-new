package org.iam.core;

import org.iam.common.basetypes.Finding;
import org.iam.common.basetypes.Policy;
import org.iam.utils.TimeMeasure;

import java.util.HashSet;
import java.util.Set;

public class Miner {
    public Set<Finding> mineIntent(Policy policy, TimeMeasure timeMeasure) {
        //TODO: implement the logic to mine findings
        return new HashSet<Finding>();
    }

    public Set<Finding> reduceIntent(Policy policy, Set<Finding> findings) {
        //TODO: implement the logic to reduce findings
        return findings;
    }
}
