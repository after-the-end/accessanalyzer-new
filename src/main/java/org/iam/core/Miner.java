package org.iam.core;

import com.microsoft.z3.BoolExpr;
import io.github.cvc5.Term;
import org.iam.common.Z3Encoder;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.basetypes.Finding;
import org.iam.common.basetypes.Policy;
import org.iam.utils.Parameter;
import org.iam.utils.TimeMeasure;

import java.util.*;

public class Miner {
    @SuppressWarnings("unchecked")
    public Set<Finding<?>> mineIntent(Policy<?> policy, TimeMeasure timeMeasure) {
        if (Parameter.getActiveSolver() == Parameter.SolverType.Z3) {
            Set<Finding<BoolExpr>> findings = mineInternal(
                    (Policy<BoolExpr>) policy,
                    new Z3Encoder(),
                    timeMeasure
            );
            return (Set<Finding<?>>)(Set<?>) findings;
        } else {
            Set<Finding<Term>> findings = mineInternal(
                    (Policy<Term>) policy,
                    null, // TODO: add CVC5 encoder
                    timeMeasure
            );
            return (Set<Finding<?>>)(Set<?>) findings;
        }
    }

    private <T> Set<Finding<T>> mineInternal(Policy<T> policy, EncodedAPI<T> encoder, TimeMeasure timeMeasure) {
        Finding<T> rootFinding = new Finding<>(policy);
        KvRelations relations = new KvRelations(policy, encoder);

        Queue<Finding<T>> workList = new LinkedList<>();
        Set<Finding<T>> results = new HashSet<>();

        workList.add(rootFinding);
        while (!workList.isEmpty()) {
            Finding<T> currentFinding = workList.poll();
            if (encoder.checkIntersection(List.of(policy.encode(encoder), currentFinding.reduce(relations, encoder)))) {
                Boolean allNotContain = true;
                for (Finding<T> result : results) {
                    if (result.equals(currentFinding)
                            || encoder.greaterEquals(result.encode(encoder), currentFinding.encode(encoder))) {
                        allNotContain = false;
                        break;
                    }
                }
                if (allNotContain) {
                    results.add(currentFinding);
                }
            } else {
                Set<Finding<T>> refinements = currentFinding.refine(relations);
                for (Finding<T> refinement : refinements) {
                    boolean notContainedInResults = true;
                    for (Finding<T> result : results) {
                        if (result.equals(refinement)
                                || encoder.greaterEquals(result.encode(encoder), refinement.encode(encoder))) {
                            notContainedInResults = false;
                            break;
                        }
                    }

                    boolean notContainedInWorkList = true;
                    for (Finding<T> workItem : workList) {
                        if (workItem.equals(refinement)) {
                            notContainedInWorkList = false;
                            break;
                        }
                    }

                    if (notContainedInResults && notContainedInWorkList) {
                        workList.add(refinement);
                    }
                }
            }
        }
        return results;
    }

    public <T> Set<Finding<T>> reduceIntent(Policy<T> policy, Set<Finding<T>> findings) {
        //TODO: implement the logic to reduce findings
        return findings;
    }
}
