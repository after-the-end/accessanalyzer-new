package org.iam.core;

import com.microsoft.z3.BoolExpr;
import io.github.cvc5.Term;
import org.iam.common.SetCoverSolver;
import org.iam.common.VarAtomicPredicates;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.basetypes.Finding;
import org.iam.common.basetypes.Policy;
import org.iam.common.reduce.StaticVar;
import org.iam.utils.Parameter;
import org.iam.utils.TimeMeasure;

import java.util.*;
import java.util.stream.Collectors;

public class Miner {
    @SuppressWarnings("unchecked")
    public Set<Finding<?>> mineIntent(Policy<?> policy, TimeMeasure timeMeasure, EncodedAPI<?> encoder) {
        if (Parameter.getActiveSolver() == Parameter.SolverType.Z3) {
            Set<Finding<BoolExpr>> findings = mineInternal(
                    (Policy<BoolExpr>) policy,
                    (EncodedAPI<BoolExpr>) encoder,
                    timeMeasure
            );
            return (Set<Finding<?>>)(Set<?>) findings;
        } else {
            Set<Finding<Term>> findings = mineInternal(
                    (Policy<Term>) policy,
                    (EncodedAPI<Term>) encoder,
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

            long startTime = System.nanoTime();

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

            long endTime = System.nanoTime();
            timeMeasure.addRound(endTime - startTime);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public Set<Finding<?>> reduceIntent(Policy<?> policy, Set<Finding<?>> findings, EncodedAPI<?> encoder) {
        if (Parameter.getActiveSolver() == Parameter.SolverType.Z3) {
            Set<Finding<BoolExpr>> ansFindings = reduceInternal(
                    (Policy<BoolExpr>) policy,
                    (Set<Finding<BoolExpr>>) (Set<?>) findings,
                    (EncodedAPI<BoolExpr>) encoder
            );
            return (Set<Finding<?>>)(Set<?>) ansFindings;
        } else {
            Set<Finding<Term>> ansFindings = reduceInternal(
                    (Policy<Term>) policy,
                    (Set<Finding<Term>>) (Set<?>) findings,
                    (EncodedAPI<Term>) encoder
            );
            return (Set<Finding<?>>)(Set<?>) ansFindings;
        }
    }

    private <T> Set<Finding<T>> reduceInternal(Policy<T> policy, Set<Finding<T>> findings, EncodedAPI<T> encoder) {
        try {
            if (findings == null || findings.isEmpty()) {
                return Collections.emptySet();
            }

            // 1. Create StaticVars for policy and findings
            Set<StaticVar<T>> smtVars = findings.stream()
                    .map(f -> new StaticVar.Builder<T>().setFinding(f).build())
                    .collect(Collectors.toSet());
            smtVars.add(new StaticVar.Builder<T>().setPolicy(policy).build());

            // 2. Create Logic True
            // We assume EncodedAPI has a mkTrue() or we derive it
            T trueExpr = encoder.mkTrue();
            StaticVar<T> logicTrue = new StaticVar.Builder<T>()
                    .setExpr(trueExpr)
                    .build();

            // 3. Compute Atomic Predicates
            // Now VarAtomicPredicates is generic and takes EncodedAPI
            VarAtomicPredicates<T> varAtomicPredicates = new VarAtomicPredicates<>(smtVars, logicTrue, encoder);

            // 4. Partition atomic predicates
            Map<Object, Set<Integer>> findingsVarToAPs = new HashMap<>();
            Set<Integer> policyAPs = null;

            for (Map.Entry<StaticVar<T>, Set<Integer>> entry : varAtomicPredicates.getAtomicPredicates().entrySet()) {
                StaticVar<T> var = (StaticVar<T>) entry.getKey();
                if (var.getVarType() == StaticVar.VarType.FINDING) {
                    findingsVarToAPs.put(var, entry.getValue());
                } else if (var.getVarType() == StaticVar.VarType.POLICY) {
                    policyAPs = entry.getValue();
                }
            }

            // 5. Solve Set Cover
            // Solve needs Map<Object, Set<Integer>>, our key is StaticVar which is Object
            if (policyAPs == null) policyAPs = Collections.emptySet();
            Map<Object, Set<Integer>> solution = SetCoverSolver.solve(findingsVarToAPs, policyAPs);

            return solution.keySet().stream()
                    .map(k -> (Finding<T>) ((StaticVar<T>) k).getValue())
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            throw new RuntimeException("Error during reduction: " + e.getMessage(), e);
        }
    }
}
